package it.michelepiccirillo.paperplane.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;


import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import it.michelepiccirillo.paperplane.client.BitmapTranscoder;
import it.michelepiccirillo.paperplane.client.HttpClient;
import it.michelepiccirillo.paperplane.network.WebServer;

public class Peer implements Parcelable {

	private class PeerProfile extends NetworkProfile {
		private Peer owner = null;
		
		@Override
		public Callable<Bitmap> getPicture() {
			if(owner == null)
				throw new IllegalStateException("This should never happen!");
			
			HttpClient client = owner.getClient();
			
			return client.get(WebServer.ENDPOINT_VCARD_PROFILE_PICTURE, 
							new BitmapTranscoder(CompressFormat.PNG, 0));
		}
	}
	
	private WifiP2pDevice device;
	private int port;
	private String display;
	
	private transient InetAddress inetAddress;
	private transient HttpClient client;
	private transient SoftReference<Profile> profile;
	

	public Peer(WifiP2pDevice device) {
		this.device = device;
	}
	
	public boolean hasCachedProfile() {
		return profile != null && profile.get() != null;
	}
	
	public WifiP2pDevice getDevice() {
		return device;
	}
	
	public synchronized HttpClient getClient() {
		if(client == null) {
			client = new HttpClient(getInetSocketAddress());
			client.setTimeout(10000);
		}
		
		return client;
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
	
	public Callable<Profile> getProfile() {
		final Profile cached = (profile != null) ? profile.get() : null;
		if(cached != null) {
			return new Callable<Profile>() {

				@Override
				public Profile call() throws Exception {
					return cached;
				}
				
			};
		} else {
			return new Callable<Profile>() {

				@Override
				public Profile call() throws Exception {
					PeerProfile p = (PeerProfile) getClient().get(WebServer.ENDPOINT_VCARD_PROFILE_JSON, new NetworkProfile.Transcoder(PeerProfile.class)).call();
					p.owner = Peer.this;
					profile = new SoftReference<Profile>(p);
					
					return p;
				}
				
			};
			
		}
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
	
	public boolean hasInetAddress() {
		return getInetAddress() != null;
	}
	
	public boolean isConnected() {
		return device.status == WifiP2pDevice.CONNECTED;
	}
	
	public boolean isAvailable() {
		return device.status == WifiP2pDevice.AVAILABLE;
	}
	
	public synchronized InetSocketAddress getInetSocketAddress() {
		if(!hasInetAddress())
			throw new IllegalStateException("The IP of this device is unknown");
		
		return new InetSocketAddress(getInetAddress(), getPort());
	}
	
	public synchronized InetAddress getInetAddress() {
		if(inetAddress == null) {
			String MAC = device.deviceAddress;
			
		    BufferedReader br = null;
		    try {
		        br = new BufferedReader(new FileReader("/proc/net/arp"));
		        String line;
		        while ((line = br.readLine()) != null) {
		        	//Log.w("ARP", line);
		            String[] splitted = line.split(" +");
		            if (splitted != null && splitted.length >= 4) {
		                // Basic sanity check
		                String device = splitted[5];
		                if (device.matches(".*p2p-p2p0.*")){
		                    String mac = splitted[3];
		                    //Log.e("ARP", "mac '" + mac + "' MAC '" + MAC + "'" );
		                    //Log.e("ARP", "splitted " + Arrays.toString(splitted));
		                    if (match(mac, MAC)) {
		                    	//Log.e("ARP", "IP is: " + splitted[0]);
		                        inetAddress = InetAddress.getByName(splitted[0]);
		                        break;
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
		}
	    
	    return inetAddress;
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
		return 	(hasInetAddress() ? getInetSocketAddress() + " -> " : "")
				+ device.deviceAddress + " (" + display + ")";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel p, int arg1) {
		p.writeParcelable(device, arg1);
		p.writeInt(port);
		p.writeString(display);
	}
	
	public static final Creator<Peer> CREATOR = new Creator<Peer>() {

		@Override
		public Peer createFromParcel(Parcel source) {
			WifiP2pDevice device = source.readParcelable(null);
			Peer p = new Peer(device);
			p.setPort(source.readInt());
			p.setDisplayName(source.readString());
			
			return p;
		}

		@Override
		public Peer[] newArray(int size) {
			return new Peer[size];
		}
		
	};	
}