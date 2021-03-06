package it.michelepiccirillo.paperplane.activities;

import it.michelepiccirillo.paperplane.R;
import it.michelepiccirillo.paperplane.R.layout;
import it.michelepiccirillo.paperplane.async.FutureListener;
import it.michelepiccirillo.paperplane.async.ListenableFuture;
import it.michelepiccirillo.paperplane.client.BitmapTranscoder;
import it.michelepiccirillo.paperplane.client.HttpTask;
import it.michelepiccirillo.paperplane.client.HttpTask.Method;
import it.michelepiccirillo.paperplane.domain.OwnProfile;
import it.michelepiccirillo.paperplane.network.NetworkingService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.plus.GooglePlusUtil;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusClient.OnPersonLoadedListener;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.Person.Image;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

public class SetupActivity extends Activity implements
	ConnectionCallbacks, OnConnectionFailedListener, OnPersonLoadedListener {
	public static final String EXTRA_PROFILE = "profile";
	
	private static final String TAG = "SetupActivity";
	
	private static final String PROFILE_FILE = "profile.obj";
	
	private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
	
	private PlusClient plusClient;
	

	private OwnProfile profile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);
		
		if(profile != null)
			startApplication();
		else {	
			tryFileLoading();   
		}
	}
	
	private void tryFileLoading() {
		ListenableFuture.runAsync(new Callable<OwnProfile>() {

			@Override
			public OwnProfile call() throws Exception {
					try {
					FileInputStream fis = openFileInput(PROFILE_FILE);
					ObjectInputStream in = new ObjectInputStream(fis);
					OwnProfile profile = (OwnProfile) in.readObject();
					
					return profile;
				} catch (InvalidClassException icEx) {
					File f = getFileStreamPath(PROFILE_FILE);
					f.delete();
					throw new FileNotFoundException();
				}
			}
			
		}, new FutureListener<OwnProfile>() {

			@Override
			public void onSuccess(OwnProfile object) {
				profile = object;
				startApplication();
			}

			@Override
			public void onError(Throwable e) {
				if(e instanceof FileNotFoundException)
					Log.i(TAG, "Profile file doesn't exist");
				else
					Log.e(TAG, "Error during profile reading", e);
				
				loadGooglePlusProfile();
				
			}
		});
	}
	
	private void loadGooglePlusProfile() {
		
		plusClient = new PlusClient.Builder(this, this, this).build(); 
		
		int errorCode = GooglePlusUtil.checkGooglePlusApp(this);
		
		if (errorCode != GooglePlusUtil.SUCCESS) {
		  GooglePlusUtil.getErrorDialog(errorCode, this, 0).show();
		} else {
	        
			if(plusClient.isConnected())
				plusClient.disconnect();
			
			plusClient.connect();
			
			// next: onConnection();
		}
	}
	
	private void startApplication() {
		
		Intent service = new Intent(this, NetworkingService.class);
		service.putExtra(EXTRA_PROFILE, (Parcelable) profile);
		startService(service);
		
		Intent main = new Intent(this, MainActivity.class);
		main.putExtra(EXTRA_PROFILE, (Parcelable) profile);
		startActivity(main);
	}	

	@Override
	public void onConnectionFailed(ConnectionResult result) {

		Log.w(TAG, "onConnectionFailed(): " + result.getErrorCode());
		if (result.hasResolution() || result.getErrorCode() == ConnectionResult.SIGN_IN_REQUIRED) {
			Log.d(TAG, "Resolving!");
            try {
                result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
            } catch (SendIntentException e) {
            	Log.d("StartActivity", e.toString());
                plusClient.connect();
            }
        }
		
		//Toast.makeText(this, "Connection failed", Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("SigninActivity", "onActivityResult()");
		if (requestCode == REQUEST_CODE_RESOLVE_ERR && resultCode == RESULT_OK) {
            plusClient.connect();
        }
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.d("SigninActivity", "onConnected()");
        plusClient.loadPerson(this, "me");
        // next: onPersonLoaded()
	}

	@Override
	public void onDisconnected() {
		Log.d("SigninActivity", "disconnected");
		
	}

	@Override
	public void onPersonLoaded(ConnectionResult result, Person person) {
		if(result.isSuccess()) {
			final String bio = person.getAboutMe();
			final String name = person.getDisplayName();
			final String email = plusClient.getAccountName();
			final String plusUrl = "https://plus.google.com/" + person.getId();
			
			Image picture = person.getImage();
			final String url = picture.getUrl();
			

			plusClient.disconnect();
			
			try {
				ListenableFuture.runAsync(
						new HttpTask<Bitmap>(new URL(url), Method.GET, new BitmapTranscoder(CompressFormat.PNG, 0), null), 
						new FutureListener<Bitmap>() {

					@Override
					public void onSuccess(Bitmap object) {
						OwnProfile profile = new OwnProfile();
						profile.setDisplayName(name);
						profile.setDescription(bio == null ? "" : bio);
						profile.setEmail(email);
						profile.setGooglePlus(plusUrl);
						profile.setPictureBitmap(object);
						
						SetupActivity.this.profile = profile;
						
						saveProfileAndStartApplication();
					}

					@Override
					public void onError(Throwable e) {
						// TODO Auto-generated method stub
						
					}
					
				});
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, "Error loading person: " + result.getErrorCode());
		}
		
	}
	
	private void saveProfileAndStartApplication() {
		ListenableFuture.runAsync(new Callable<Void> () {

			@Override
			public Void call() throws Exception {
				
				FileOutputStream fos = openFileOutput(PROFILE_FILE, MODE_PRIVATE);
				ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(profile);
				
				return null;
			}
			
		}, new FutureListener<Void>() {

			@Override
			public void onSuccess(Void object) {
				startApplication();
			}

			@Override
			public void onError(Throwable e) {
				Log.e(TAG, "Error saving profile to disk", e);
				startApplication();
			}
			
		});
	}
}
