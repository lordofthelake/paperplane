package it.michelepiccirillo.paperplane;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.IBinder;
import android.util.Log;

public class NetworkingService extends Service implements NetworkListener {
	
	private WifiP2pManager manager;
	private Channel channel;
	
	private BroadcastReceiver receiver;
	
	private WebServer webServer;

	public NetworkingService() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

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
        startRegistration();
	    
	    return START_STICKY;
	}
	
public static final String TAG = "Paper Plane";
	

	
	private HashMap<String, String> buddies = new HashMap<String, String>();
	
    private void startRegistration() {
    	
    	webServer = new WebServer();
    	Thread serverThread = new Thread(webServer);
    	serverThread.start();
    	
    	final int port = webServer.getUsedPort();
    	
        //  Create a string map containing information about your service.
        Map<String, String> record = new HashMap<String, String>();
        record.put("listenport", String.valueOf(port));

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("PaperPlane", "_http._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        manager.addLocalService(channel, serviceInfo, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("NetworkingService", "Service registered on port " + port);
            }

            @Override
            public void onFailure(int arg0) {
            	String reason = "";
            	switch(arg0) {
            	case WifiP2pManager.P2P_UNSUPPORTED:
            		reason = "P2P_UNSUPPORTED";
            		break;
            	case WifiP2pManager.BUSY:
            		reason = "BUSY";
            		break;
            	case WifiP2pManager.ERROR:
            		reason = "ERROR";
            		break;
            	}
            	
            	Log.e("NetworkingService", "Cannot register service on port " + port + ": " + reason);
            }
        });
        
	    
	    //discoverService();
    }
	
	private void discoverService() {
	    DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
	        @Override
	        /* Callback includes:
	         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
	         * record: TXT record dta as a map of key/value pairs.
	         * device: The device running the advertised service.
	         */

	        public void onDnsSdTxtRecordAvailable(
	                String fullDomain, Map<String, String> record, WifiP2pDevice device) {
	                Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
	                buddies.put(device.deviceAddress, record.get("buddyname"));
	            }
	        };
	    //	...
	        
	        DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
	            @Override
	            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
	                    WifiP2pDevice resourceType) {

	                    // Update the device name with the human-friendly version from
	                    // the DnsTxtRecord, assuming one arrived.
	                    resourceType.deviceName = buddies
	                            .containsKey(resourceType.deviceAddress) ? buddies
	                            .get(resourceType.deviceAddress) : resourceType.deviceName;

	                    // Add to the custom adapter defined specifically for showing
	                    // wifi devices.
	                    /*WiFiDirectServicesList fragment = (WiFiDirectServicesList) getFragmentManager()
	                            .findFragmentById(R.id.frag_peerlist);
	                    WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) fragment
	                            .getListAdapter());

	                    adapter.add(resourceType);
	                    adapter.notifyDataSetChanged();*/
	                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
	            }
	        };

	        manager.setDnsSdResponseListeners(channel, servListener, txtListener);
	        
	        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
	        manager.addServiceRequest(channel,
	                serviceRequest,
	                new ActionListener() {
	                    @Override
	                    public void onSuccess() {
	                        // Success!
	                    }

	                    @Override
	                    public void onFailure(int code) {
	                        // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	                    }
	                });
	        
	        manager.discoverServices(channel, new ActionListener() {

	            @Override
	            public void onSuccess() {
	                // Success!
	            }

	            @Override
	            public void onFailure(int code) {
	                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
	                if (code == WifiP2pManager.P2P_UNSUPPORTED) {
	                    Log.d(TAG, "P2P isn't supported on this device.");
	                } //else if(...)
	                    //...
	            }
	        });
	}
	
	void onPeerDiscovered() {
		
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		if(webServer != null) {
			// FIXME webServer.stop();
		}
		super.onDestroy();
	}

	@Override
	public void onP2PEnabled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onP2PDisabled() {
		// TODO Auto-generated method stub
		
	}
}
