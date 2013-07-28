package it.michelepiccirillo.paperplane.domain;

import it.michelepiccirillo.paperplane.network.WebServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;


abstract class NetworkProfile implements Profile {
	public static class Transcoder implements it.michelepiccirillo.paperplane.client.Transcoder<Profile> {
		private Class<? extends Profile> impl;
		private Gson gson;
		
		public Transcoder(Class<? extends Profile> implClass) {
			this.impl = implClass;
			this.gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		}
		
		@Override
		public Profile read(InputStream in) throws Exception {
			return gson.fromJson(new InputStreamReader(in), impl);
		}

		@Override
		public void write(Profile object, OutputStream out) throws Exception {
			new OutputStreamWriter(out).write(gson.toJson(object, impl));
		}

		@Override
		public boolean isReadonly() {
			return false;
		}
		
	}
	
	@Expose protected String displayName;
	@Expose protected String description;
	@Expose protected String email;
	@Expose protected String googlePlus;
	
	@Expose protected final String picture = WebServer.ENDPOINT_VCARD_PROFILE_PICTURE;


	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getGooglePlus() {
		return googlePlus;
	}

	@Override
	public String getEmail() {
		return email;
	}
}
