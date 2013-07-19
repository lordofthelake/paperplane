package it.michelepiccirillo.paperplane;

import android.app.Application;
import android.content.SharedPreferences;

public class PaperPlane extends Application {
	private static final String SHARED_PREF_NAME = "PaperPlane";
	private static final String SHARED_PREF_KEY_PROFILE = "profile";
	
	private OwnProfile profile;

	public PaperPlane() {
		// TODO Auto-generated constructor stub
	}

	public void setProfile(OwnProfile profile) {
		this.profile = profile;
		
	}
	
	public OwnProfile getProfile() {
		if(profile == null) {
			// Try reading from shared prefs
			SharedPreferences prefs = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
			// FIXME
			
		}
		
		return profile;
	}

}
