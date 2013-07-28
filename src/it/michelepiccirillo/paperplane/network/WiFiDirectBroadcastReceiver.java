package it.michelepiccirillo.paperplane.network;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "WiFiDirectBroadcastReceiver";

	private WifiP2pManager manager;
    private Channel channel;
    
    private NetworkingService service;
    

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, NetworkingService service) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	try {
	        String action = intent.getAction();
	
	        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
	        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
	            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
	            	Log.d(TAG, "Broadcast: P2P enabled");
	                //networkListener.onP2PEnabled();
	            } else {
	            	Log.d(TAG, "Broadcast: P2P disabled");
	                //networkListener.onP2PDisabled();
	            }
	        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
	        	Log.d(TAG, "Connection status changed");
	        	if (manager == null) {
	                return;
	            }

	            NetworkInfo networkInfo = (NetworkInfo) intent
	                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

	            if (networkInfo.isConnected()) {
	            	Log.d(TAG, "Broadcast: Connected to a peer");
	                manager.requestConnectionInfo(channel, service);
	            } else {
	            	Log.w(TAG, "Broadcast: Disconnected from peer");
	            }
	        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
	        	
	            // Request available peers from the wifi p2p manager. This is an
	            // asynchronous call and the calling activity is notified with a
	            // callback on PeerListListener.onPeersAvailable()
	            if (service != null) {
	                service.refreshPeers();
	            }
	            Log.d(TAG, "Broadcast: Peers changed");
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    		Log.e(TAG, "Error", e);
    	}
    }

}
