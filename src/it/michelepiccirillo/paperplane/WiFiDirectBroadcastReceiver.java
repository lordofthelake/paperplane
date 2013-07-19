package it.michelepiccirillo.paperplane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
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
	            	Log.d(TAG, "P2P enabled");
	                //networkListener.onP2PEnabled();
	            } else {
	            	Log.d(TAG, "P2P disabled");
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
	            	Log.d(TAG, "Connected to a peer");
	                manager.requestConnectionInfo(channel, service);
	            } else {
	            	Log.w(TAG, "Disconnected from peer");
	            }
	        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
	        	
	            // Request available peers from the wifi p2p manager. This is an
	            // asynchronous call and the calling activity is notified with a
	            // callback on PeerListListener.onPeersAvailable()
	            if (service != null) {
	                service.refreshPeers();
	            }
	            Log.d(TAG, "P2P peers changed");
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    		Log.e(TAG, "Error", e);
    	}
    }

}
