package it.michelepiccirillo.paperplane.network;

import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

import it.michelepiccirillo.paperplane.domain.OwnProfile;
import it.michelepiccirillo.tympanon.AbstractRoute;
import it.michelepiccirillo.tympanon.HttpRequest;
import it.michelepiccirillo.tympanon.HttpResponse;
import it.michelepiccirillo.tympanon.HttpResponse.Status;
import it.michelepiccirillo.tympanon.HttpServer;;

public class WebServer extends HttpServer {
	public static final String ENDPOINT_VCARD_PROFILE_JSON = "/vcard/profile.json";
	public static final String ENDPOINT_VCARD_PROFILE_HTML = "/vcard/profile.html";
	public static final String ENDPOINT_VCARD_PROFILE_PICTURE = "/vcard/profile/picture.png";
	
	private OwnProfile profile;
	
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
		route(ENDPOINT_VCARD_PROFILE_JSON, new AbstractRoute() {

			@Override
			protected void get(HttpRequest req, HttpResponse res)
					throws Exception {
				res.setStatus(HttpResponse.Status.OK);
				res.setHeader("Content-Type", "application/json");
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				res.getWriter().write(gson.toJson(profile));
			}
			
		});
		
		route(ENDPOINT_VCARD_PROFILE_PICTURE, new AbstractRoute() {
			@Override
			protected void get(HttpRequest req, HttpResponse res)
					throws Exception {
				Bitmap bmp = profile.getPictureBitmap();
				
				if(bmp == null) {
					res.setStatus(Status.NOT_FOUND);
				} else {
					res.setStatus(HttpResponse.Status.OK);
					res.setHeader("Content-Type", "image/png");
					OutputStream out = res.getOutputStream();
					bmp.compress(CompressFormat.PNG, 100, out);
					res.flush();
				}
			}
		});
	}
	
	public void setProfile(OwnProfile profile) {
		this.profile = profile;
	}
}
