package it.michelepiccirillo.paperplane;

import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Callable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import it.michelepiccirillo.tympanon.AbstractRoute;
import it.michelepiccirillo.tympanon.HttpRequest;
import it.michelepiccirillo.tympanon.HttpResponse;
import it.michelepiccirillo.tympanon.HttpServer;;

public class WebServer extends HttpServer {
	private class WebProfile extends OwnProfile {
		@Expose private String picture;
		
		WebProfile(OwnProfile profile) {
			setDisplayName(profile.getDisplayName());
			setBio(profile.getDescription());
			setEmail(profile.getEmail());
			try {
				setPicture(profile.getPicture().call());
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.picture = "http://" + getPublicSocketAddress() + "/vcard/picture.png";
		}		
	}
	
	private WebProfile profile;
	
	public WebServer() {
		super(new InetSocketAddress(0));
		init();
	}
	
	private InetAddress getPublicIPAddress() {
	    try { 
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
	        		en.hasMoreElements();) { 
	            NetworkInterface intf = en.nextElement(); 
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) { 
	                InetAddress inetAddress = enumIpAddr.nextElement(); 
	                if (!inetAddress.isLoopbackAddress()) { 
	                    if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-) 
	                        return inetAddress; 
	                    } 
	                } 
	            } 
	        } 
	    } catch (Exception ex) { 
	        Log.e("WebServer", "getLocalIPAddress()", ex); 
	    } 
	    
	    return null; 
	}
	
	public InetSocketAddress getPublicSocketAddress() {
		return new InetSocketAddress(getPublicIPAddress(), getUsedPort());
	}
	
	private void init() {
		route("/vcard/profile.json", new AbstractRoute() {

			@Override
			protected void get(HttpRequest req, HttpResponse res)
					throws Exception {
				res.setStatus(HttpResponse.Status.OK);
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				res.getWriter().write(gson.toJson(profile));
			}
			
		});
		
		route("/vcard/picture.png", new AbstractRoute() {
			@Override
			protected void get(HttpRequest req, HttpResponse res)
					throws Exception {
				res.setStatus(HttpResponse.Status.OK);
				OutputStream out = res.getOutputStream();
				Bitmap bmp = profile.getPicture().call();
				bmp.compress(CompressFormat.PNG, 0, out);
				res.flush();
			}
		});
	}
	
	public void setProfile(OwnProfile profile) {
		this.profile = new WebProfile(profile);
	}
}
