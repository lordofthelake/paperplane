package it.michelepiccirillo.paperplane;

import java.net.InetSocketAddress;

import it.michelepiccirillo.tympanon.AbstractRoute;
import it.michelepiccirillo.tympanon.HttpRequest;
import it.michelepiccirillo.tympanon.HttpResponse;
import it.michelepiccirillo.tympanon.HttpServer;;

public class WebServer extends HttpServer {
	public WebServer() {
		super(new InetSocketAddress(0));
		init();
	}
	
	private void init() {
		route("profile", new AbstractRoute() {

			@Override
			protected void get(HttpRequest req, HttpResponse res)
					throws Exception {
				res.setStatus(HttpResponse.Status.OK);
				res.getWriter().println("I'm " + Math.round(Math.random()*1000) + getUsedPort());
			}
			
		});
	}
}
