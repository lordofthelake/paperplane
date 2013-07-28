package it.michelepiccirillo.paperplane.network;

import it.michelepiccirillo.paperplane.activities.SetupActivity;
import it.michelepiccirillo.paperplane.domain.OwnProfile;
import it.michelepiccirillo.paperplane.domain.Peer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class NetworkingService extends Service implements ConnectionInfoListener {

	private static final String TAG = "NetworkingService";
	
	public class NetworkingBinder extends Binder {
		public NetworkingService getService() {
			return NetworkingService.this;
		}
	}
	
	private boolean booted = false;
	
	private final IBinder binder = new NetworkingBinder();
	
	private WifiP2pManager manager;
	private Channel channel;
	
	private BroadcastReceiver receiver;
	
	private WebServer webServer;
	
	private WifiP2pDnsSdServiceInfo serviceInfo;

	private WeakReference<PeerListener> peerListener;
	
	private List<WifiP2pDevice> available = new ArrayList<WifiP2pDevice>();
	private Map<String, Peer> known =  new HashMap<String, Peer>();
	
	private List<String> pendingConnections = new LinkedList<String>();
	
	private OwnProfile profile;


	// ----------- Application lifecycle events handling -----------
	
	@Override
	public IBinder onBind(Intent arg0) {
		Log.i(TAG, "Activity bound, disconnecting WiFi");
		WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiMan.disconnect();
		
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "All activities unbound, reconnecting to WiFi");
		WifiManager wifiMan = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiMan.reconnect();
		
		Log.e(TAG, "Debug mode: shutting down after activity exit");
		stopSelf();
		
		return false;
	}
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		profile = intent.getParcelableExtra(SetupActivity.EXTRA_PROFILE);
		if(profile == null)
			throw new IllegalArgumentException("A profile must be provided to the service");
		
		Log.w(TAG, "Service starting! I'm " + profile.getDisplayName());
		
		start();
		
		if(webServer != null)
			webServer.setProfile(profile);
		
		Log.w(TAG, "Service started!");
		
	    return START_REDELIVER_INTENT;
	}
	

	@Override
	public void onDestroy() {
		stop();
		Log.w(TAG, "Service stopped!");
		super.onDestroy();
	}
	
	// ----------- Startup/Shutdown methods -----------
	
	private void start() {
		if(!booted) {
			Log.w(TAG, "Starting up");
			
			startWifiDirect();
			startWebserver();
			startPeerDiscovery();
			startServiceDiscovery();
			
			booted = true;
		}
	}
	
	private void stop() {
		if(booted) {
			Log.w(TAG, "Shutting down");

			stopServiceDiscovery();
			stopPeerDiscovery();
			stopWebserver();
			stopWifiDirect();
			
			booted = false;
		}
	}
	
	private void startWifiDirect() {
		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
	    channel = manager.initialize(this, getMainLooper(), null);
	    
		IntentFilter intentFilter;
	    receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
	    
	    intentFilter = new IntentFilter();
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
	    
        registerReceiver(receiver, intentFilter);
	}
	
	private void stopWifiDirect() {
		if(receiver != null)
			unregisterReceiver(receiver);
		
		if(manager != null)
			manager.removeGroup(channel, null);
	}
	
	private void startPeerDiscovery() {
        manager.discoverPeers(channel, new ActionListener() {

			@Override
			public void onFailure(int reason) {
				Log.w(TAG, "Peer discovery failure: " + reason);
				
			}

			@Override
			public void onSuccess() {
				Log.i(TAG, "Peer discovery started");
				
			}
        	
        });
	}
	
	private void stopPeerDiscovery() {
		if(manager != null)
			manager.stopPeerDiscovery(channel, null);
		
		available.clear();
	}
	
	private void startServiceDiscovery() {
		DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
	        public void onDnsSdTxtRecordAvailable(
	                String fullDomain, Map<String, String> record, WifiP2pDevice device) {
	                Log.d(TAG, "DnsSdTxtRecord available - " + fullDomain + " " + record.toString());
	                Peer peer = getOrCreatePeer(device);
	                peer.setPort(Integer.valueOf(record.get("listenport")));
	                peer.setDisplayName(record.get("displayname"));
	            }
	        };
	        
	        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
	            @Override
	            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
	                    WifiP2pDevice resourceType) {
	            	
	                    Log.d(TAG, "Bonjour Service available - " + instanceName);
	                    notifyPeerChange();
	            }
	        };

	        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
	        
	        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
	        
	        manager.addServiceRequest(channel,
	                serviceRequest,
	                new ActionListener() {
	                    @Override
	                    public void onSuccess() {
	                        Log.i(TAG, "Service request added");
	                    }

	                    @Override
	                    public void onFailure(int code) {
	                        Log.w(TAG, "Service request addition failure: " + code);
	                    }
	                });
	        
	        manager.discoverServices(channel, new ActionListener() {
	        	
	            @Override
	            public void onSuccess() {
	            	Log.i(TAG, "Service discovery started");
	            }
	
	            @Override
	            public void onFailure(int code) {
	            	Log.w(TAG, "Service discovery failure");
	                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
	                    Log.d(TAG, "P2P isn't supported on this device.");
	                }
	            }
	        });

	}
	
	private void stopServiceDiscovery() {
		if(manager != null)
			manager.clearServiceRequests(channel, null);
		
		known.clear();
	}	
	
    private void startWebserver() {
    	
    	webServer = new WebServer();
    	Thread serverThread = new Thread(webServer);
    	serverThread.start();
    	
    	final int port = webServer.getUsedPort();
    	
        Map<String, String> record = new HashMap<String, String>();
        record.put("listenport", String.valueOf(port));
        record.put("displayname", profile.getDisplayName());
        
        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("PaperPlane", "_http._tcp", record);
        
        manager.addLocalService(channel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Webserver exposed on " + port);
            }

            @Override
            public void onFailure(int arg0) {
            	Log.e(TAG, "Cannot expose service: " + arg0);
            }
        });
    }
    
	private void stopWebserver() {
		if(webServer != null) {
			try {
				webServer.stop();
			} catch (IOException e) { }
		}
		
		if(manager != null && serviceInfo != null)
			manager.removeLocalService(channel, serviceInfo, null);
	}
	
	// ----------- Network operations -----------
    
    public void refreshPeers() {
    	manager.requestPeers(channel, new PeerListListener() {

			@Override
			public void onPeersAvailable(WifiP2pDeviceList peers) {
				Log.d(TAG, "New peer list available");
				
				Collection<WifiP2pDevice> coll = peers.getDeviceList();
				available.clear();
				available.addAll(coll);
				notifyPeerChange();
			}
    	});
    }
    
    public void connect(Peer peer) {
    	Log.d(TAG, "Requesting a connection to " + peer);
    	if(peer.isConnected() && peer.hasInetAddress()) {
    		notifyPeerConnected(peer);
    	} else {
    		pendingConnections.add(peer.getDevice().deviceAddress);
    		
    		if(peer.isConnected()) {
    			manager.requestConnectionInfo(channel, this);
    		} else {
		    	manager.connect(channel, peer.getConfig(), new ActionListener () {
		
					@Override
					public void onFailure(int reason) {
						Log.w(TAG, "Connection request failed " + reason);
					}
		
					@Override
					public void onSuccess() {
						Log.i(TAG, "Made connection request");	
					}
		    		
		    	});
    		}
    	}
    }

	public void setPeerListListener(PeerListener listener) {
		this.peerListener = new WeakReference<PeerListener>(listener);
	}
    
    private void notifyPeerChange() {
    	PeerListener listener = null;
    	if(peerListener != null) 
    		listener = peerListener.get();
    	
    	if(known.isEmpty() || peerListener == null)
    		return;
    	
    	List<Peer> peers = new ArrayList<Peer>();
    	for(WifiP2pDevice d : available) {
    		Peer p = known.get(d.deviceAddress);
    		Log.i(TAG, "Peer status: " + d.status);
    		if(p != null) {
    			p.setDevice(d);
    			peers.add(p);
    			
    			if(p.isConnected() && pendingConnections.contains(d.deviceAddress)) {
        			pendingConnections.remove(d.deviceAddress);
        			notifyPeerConnected(p);
        		}
    		}
    	}
    	
		Log.d(TAG, "Peers change: " + peers);
    	
    	listener.onPeerListChanged(peers);
    }
    
    private void notifyPeerConnected(Peer p) {
    	PeerListener listener = null;
    	if(peerListener != null) 
    		listener = peerListener.get();
    	
    	if(listener != null)
    		listener.onPeerConnected(p);
    }
   
    
    private Peer getOrCreatePeer(WifiP2pDevice device) {
    	String deviceAddress = device.deviceAddress;
    	Peer peer = known.get(deviceAddress);
    	if(peer == null) {
    		peer = new Peer(device);
    		known.put(deviceAddress, peer);
    	} else {
    		peer.setDevice(device);
    	}
    	
    	return peer;
    }

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		InetAddress groupOwnerAddress = info.groupOwnerAddress;
		refreshPeers();
		if(info.groupFormed) {
			
			Log.i(TAG, "I'm connected. Owner is " + groupOwnerAddress + ". I'm available on " + webServer.getPublicSocketAddress());
			
			if(info.isGroupOwner)
				Log.i(TAG, "I'm the group owner");
		}		
	}
}
