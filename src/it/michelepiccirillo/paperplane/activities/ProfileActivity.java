package it.michelepiccirillo.paperplane.activities;

import java.util.concurrent.Callable;

import it.michelepiccirillo.paperplane.R;
import it.michelepiccirillo.paperplane.R.id;
import it.michelepiccirillo.paperplane.R.layout;
import it.michelepiccirillo.paperplane.R.menu;
import it.michelepiccirillo.paperplane.async.FutureListener;
import it.michelepiccirillo.paperplane.async.ListenableFuture;
import it.michelepiccirillo.paperplane.domain.Peer;
import it.michelepiccirillo.paperplane.domain.Profile;
import android.net.Uri;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class ProfileActivity extends Activity {
	public static final String EXTRA_PEER = "peer";
	private static final String TAG = "ProfileActivity";
	
	private Profile profile = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		// Show the Up button in the action bar.
		
		final ImageView profilePic = (ImageView) findViewById(R.id.profilePicture);
		final TextView displayName = (TextView) findViewById(R.id.displayName);
		final TextView bio = (TextView) findViewById(R.id.bio);
		
		final View bioSection = findViewById(R.id.section_bio);
		
		final View profilesSection = findViewById(R.id.section_profiles);
		final TextView email = (TextView) findViewById(R.id.profile_email);
		final TextView googlePlus = (TextView) findViewById(R.id.profile_googleplus);
		
		Peer p = getIntent().getParcelableExtra(EXTRA_PEER);
		

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		
		final ProgressDialog progress = ProgressDialog.show(this, "", "Retrieving profile info...");
		
		ListenableFuture.runAsync(p.getProfile(), new FutureListener<Profile>() {

			@Override
			public void onSuccess(Profile object) {
				profile = object;
				
				actionBar.setTitle(object.getDisplayName());
				displayName.setText(object.getDisplayName());
				
				String description = object.getDescription();
				if(description != null && !description.isEmpty()) {
					bio.setText(object.getDescription());
					bioSection.setVisibility(View.VISIBLE);
				}
				
				email.setText(object.getEmail());
				googlePlus.setText(object.getGooglePlus());
				profilesSection.setVisibility(View.VISIBLE);
				
				progress.dismiss();
				
				try {
					Callable<Bitmap> task = object.getPicture();
					ListenableFuture.runAsync(task, new FutureListener<Bitmap> () {
	
						@Override
						public void onSuccess(Bitmap object) {
	
							BitmapDrawable pic = null;
							try {
								 pic = new BitmapDrawable(getResources(), object);
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							if(pic != null) {
								profilePic.setImageDrawable(pic);
							}
							
						}
	
						@Override
						public void onError(Throwable e) {
							Log.e(TAG, "Error retrieving image", e);
						}
						
					});
				} catch (Exception e) {
					Log.e(TAG, "Exception", e);
				}
				
			}

			@Override
			public void onError(Throwable e) {
				Log.e(TAG, "Error retrieving profile", e);				
			}
			
		});
	}

	public void goToMail(View v) {
		if(profile == null)
			return;
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_EMAIL, profile.getEmail());
		i.setType("text/plain");
		startActivity(Intent.createChooser(i, "Send email..."));
	}
	
	public void goToGooglePlus(View v) {
		if(profile == null)
			return;
		
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(profile.getGooglePlus()));
		startActivity(i);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			//NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
