package it.michelepiccirillo.paperplane;

import it.michelepiccirillo.paperplane.client.HttpClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
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
	
	class NetworkingBinder extends Binder {
		NetworkingService getService() {
			return NetworkingService.this;
		}
	}
	
	private final IBinder binder = new NetworkingBinder();
	
	private WifiP2pManager manager;
	private Channel channel;
	
	private BroadcastReceiver receiver;
	
	private WebServer webServer;
	
	private boolean booted = false;

	private WeakReference<PeerListener> peerListener;
	
	private List<WifiP2pDevice> available = new ArrayList<WifiP2pDevice>();
	private Map<String, Peer> known =  new HashMap<String, Peer>();
	
	private OwnProfile profile;


	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		
		profile = intent.getParcelableExtra(SetupActivity.EXTRA_PROFILE);
		if(profile == null)
			throw new IllegalArgumentException("A profile must be provided to the service");
		
		Log.i(TAG, "Service started! I'm " + profile.getDisplayName());

		if(!booted) {
			manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		    channel = manager.initialize(this, getMainLooper(), null);
		    
			IntentFilter intentFilter;
		    receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
		    
		    intentFilter = new IntentFilter();
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		    
		    receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
	        registerReceiver(receiver, intentFilter);
	        
	        startWebserver();
	        booted = true;
	        
		}
		
		if(webServer != null)
			webServer.setProfile(profile);
	    
	    return START_REDELIVER_INTENT;
	}
	
	
    private void startWebserver() {
    	
    	webServer = new WebServer();
    	Thread serverThread = new Thread(webServer);
    	serverThread.start();
    	
    	final int port = webServer.getUsedPort();
    	
        Map<String, String> record = new HashMap<String, String>();
        record.put("listenport", String.valueOf(port));
        record.put("displayname", profile.getDisplayName());
        
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("PaperPlane", "_http._tcp", record);
        
        manager.addLocalService(channel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Webserver started and listening on " + port);
            }

            @Override
            public void onFailure(int arg0) {
            	Log.e(TAG, "Cannot expose service: " + arg0);
            }
        });
        
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
	                        Log.i(TAG, "Service discovery started");
	                    }

	                    @Override
	                    public void onFailure(int code) {
	                        Log.w(TAG, "Service discovery failure: " + code);
	                    }
	                });
	        
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
    
    public void refreshPeers() {
    	
    	manager.requestPeers(channel, new PeerListListener() {

			@Override
			public void onPeersAvailable(WifiP2pDeviceList peers) {
				Collection<WifiP2pDevice> coll = peers.getDeviceList();
				available.clear();
				available.addAll(coll);
				notifyPeerChange();
			}
    		
    	});
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
    		if(p != null)
    			peers.add(p);
    	}
    	

		Log.d(TAG, "Peers change: " + peers);
    	
    	listener.onPeerListChanged(peers);
    }
    
    private Peer getOrCreatePeer(WifiP2pDevice device) {
    	String deviceAddress = device.deviceAddress;
    	Peer peer = known.get(deviceAddress);
    	if(peer == null) {
    		peer = new Peer(this, device);
    		known.put(deviceAddress, peer);
    	} else {
    		peer.setDevice(device);
    	}
    	
    	return peer;
    }
    
    void connect(Peer peer) {
    	Log.d(TAG, "Requesting a connection to " + peer);
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
	
	@Override
	public void onDestroy() {
		if(receiver != null)
			unregisterReceiver(receiver);
		
		if(webServer != null) {
			try {
				webServer.stop();
			} catch (IOException e) {
				Log.d(TAG, "Exception while shutting down webserver", e);
			}
		}
		
		booted = false;
		
		Log.w(TAG, "Service shut down");
		super.onDestroy();
	}

	public void setPeerListListener(PeerListener listener) {
		this.peerListener = new WeakReference<PeerListener>(listener);
	}


	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info) {
		InetAddress groupOwnerAddress = info.groupOwnerAddress;
		notifyPeerChange();
		if(info.groupFormed) {
			
			Log.i(TAG, "I'm connected. Owner is " + groupOwnerAddress + ". I'm available on " + webServer.getPublicSocketAddress());
			
			if(info.isGroupOwner)
				Log.i(TAG, "I'm the group owner");
		}		
	}
}
