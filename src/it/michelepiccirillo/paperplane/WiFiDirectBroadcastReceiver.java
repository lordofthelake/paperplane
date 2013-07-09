package it.michelepiccirillo.paperplane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

	private WifiP2pManager manager;
    private Channel channel;
    
    private NetworkListener networkListener;
    
    
    private HashMap<String, String> buddies = new HashMap<String, String>();
	
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
	                Log.d(NetworkingService.TAG, "DnsSdTxtRecord available -" + record.toString());
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

	                    Log.d(NetworkingService.TAG, "onBonjourServiceAvailable " + instanceName);
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
	                    Log.d(NetworkingService.TAG, "P2P isn't supported on this device.");
	                } //else if(...)
	                    //...
	            }
	        });
	}

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, NetworkListener listener) {
        super();
        this.manager = manager;
        this.channel = channel;
        
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
        	int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                networkListener.onP2PEnabled();
            } else {
                networkListener.onP2PDisabled();
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // Request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                //manager.requestPeers(channel, peerListListener);
            }
            Log.d(NetworkingService.TAG, "P2P peers changed");
        }
    }
}
