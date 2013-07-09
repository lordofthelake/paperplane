package it.michelepiccirillo.paperplane;

import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.plus.GooglePlusUtil;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusClient.OnPersonLoadedListener;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.Person.Emails;
import com.google.android.gms.plus.model.people.Person.Image;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SigninActivity extends Activity implements
	ConnectionCallbacks, OnConnectionFailedListener, OnPersonLoadedListener {
	
	private static final int REQUEST_CODE_RESOLVE_ERR = 9000;
	
	private PlusClient plusClient;
	private ProgressDialog progress;
	
	private ConnectionResult connectionResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.signin_layout);
		
		int errorCode = GooglePlusUtil.checkGooglePlusApp(this);
		if (errorCode != GooglePlusUtil.SUCCESS) {
		  GooglePlusUtil.getErrorDialog(errorCode, this, 0).show();
		}
		
		plusClient = new PlusClient.Builder(this, this, this).build();
		progress = new ProgressDialog(this);
        progress.setMessage("Signing in...");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		plusClient.connect();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		plusClient.disconnect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_CODE_RESOLVE_ERR);
            } catch (SendIntentException e) {
                plusClient.connect();
            }
        }
        // Save the result and resolve the connection failure upon a user click.
       connectionResult = result;
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_RESOLVE_ERR && resultCode == RESULT_OK) {
            connectionResult = null;
            plusClient.connect();
        }
	}

	@Override
	public void onConnected(Bundle arg0) {
		String accountName = plusClient.getAccountName();
        Toast.makeText(this, accountName + " is connected.", Toast.LENGTH_LONG).show();
		
        plusClient.loadPerson(this, "me");
	}

	@Override
	public void onDisconnected() {
		Log.d("SigninActivity", "disconnected");
		
	}

	@Override
	public void onPersonLoaded(ConnectionResult result, Person person) {
		if(result.isSuccess()) {
			String bio = person.getAboutMe();
			String name = person.getDisplayName();
			String email = null;
			
			List<Emails> emails = person.getEmails(); 
			if(emails != null && !emails.isEmpty()) {
				for(Emails e : emails) {
					if(e.isPrimary())
						email = e.getValue();
				}
			}
			
			Image picture = person.getImage();
			String url = picture.getUrl();
			
			// Create profile
			Log.i("Profile", "Name: " + name);
			Log.i("Profile", "Bio: " + bio);
			Log.i("Profile", "E-mail: " + email);
			Log.i("Profile", "Picture url: " + url);
		} else {
			Log.e("Signin Activity", "Error loading person: " + result.getErrorCode());
		}
		
	}
}
