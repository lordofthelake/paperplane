package it.michelepiccirillo.paperplane;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Callable;

import org.apache.http.conn.util.InetAddressUtils;

import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import it.michelepiccirillo.async.ListenableFuture;
import it.michelepiccirillo.paperplane.client.HttpClient;

public class Peer {
	public class Client extends HttpClient {

		private Client(InetSocketAddress host) {
			super(host);
		}
		
	}
	
	private WifiP2pDevice device;
	private int port;
	private InetSocketAddress address;
	private String display;
	private Client client;
	private Profile profile;
	
	private WeakReference<NetworkingService> service;

	public Peer(NetworkingService service, WifiP2pDevice device) {
		this.device = device;
		this.service = new WeakReference<NetworkingService>(service);
	}
	
	public boolean hasCachedProfile() {
		return profile != null;
	}
	
	public void connect() {
		NetworkingService ns = service.get();
		if(ns == null)
			throw new IllegalStateException("Service reference not held!");
		
		ns.connect(this);
	}
	
	public void setDevice(WifiP2pDevice dev) {
		this.device = dev;
	}
	
	public WifiP2pConfig getConfig() {
		WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        
        return config;
	}
	
	public void setDisplayName(String name) {
		this.display = name;
	}
	
	public String getDisplayName() {
		return display;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isConnected() {
		return device.status == WifiP2pDevice.CONNECTED;
	}
	
	public boolean isAvailable() {
		return device.status == WifiP2pDevice.AVAILABLE;
	}
	
	public InetAddress getInetAddress() {
		String MAC = device.deviceAddress;
		
	    BufferedReader br = null;
	    try {
	        br = new BufferedReader(new FileReader("/proc/net/arp"));
	        String line;
	        while ((line = br.readLine()) != null) {
	        	Log.w("ARP", line);
	            String[] splitted = line.split(" +");
	            if (splitted != null && splitted.length >= 4) {
	                // Basic sanity check
	                String device = splitted[5];
	                if (device.matches(".*p2p-p2p0.*")){
	                    String mac = splitted[3];
	                    Log.e("ARP", "mac '" + mac + "' MAC '" + MAC + "'" );
	                    Log.e("ARP", "splitted " + Arrays.toString(splitted));
	                    if (match(mac, MAC)) {
	                    	Log.e("ARP", "IP is: " + splitted[0]);
	                        return InetAddress.getByName(splitted[0]);
	                    }
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            br.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	    
	    return null;
	}
	
	private boolean match(String mac1, String mac2) {
		String[] split1 = mac1.split(":");
		String[] split2 = mac2.split(":");
		
		int matches = 0;
		for(int i = 0; i < split1.length; ++i) {
			if(split1[i].equals(split2[i]))
				matches++;
		}
		
		return matches >= 5;
	}
	
	@Override
	public String toString() {
		InetAddress addr = null;// getInetAddress();
		return (addr == null ? device.deviceAddress : addr) + " (" + display + ")";
	}
	
}