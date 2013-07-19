package it.michelepiccirillo.paperplane;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class ProfileActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		// Show the Up button in the action bar.
		
		ImageView profilePic = (ImageView) findViewById(R.id.profilePicture);
		TextView displayName = (TextView) findViewById(R.id.displayName);
		TextView bio = (TextView) findViewById(R.id.bio);
		
		Profile p = getIntent().getParcelableExtra(SetupActivity.EXTRA_PROFILE);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(p.getDisplayName());
		
		BitmapDrawable pic = null;
		try {
			 pic = new BitmapDrawable(getResources(),p.getPicture().call());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(pic != null) {
			actionBar.setIcon(pic);
			profilePic.setImageDrawable(pic);
		}
		
		displayName.setText(p.getDisplayName());
		bio.setText(p.getDescription());
			
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_profile, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			//NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
