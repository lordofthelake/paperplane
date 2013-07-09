package it.michelepiccirillo.paperplane;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class MainActivity extends Activity {
	
	private List<Profile> list = new ArrayList<Profile>();
	private ArrayAdapter<Profile> adapter = new ProfileAdapter(this, list);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		
		Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
		int count = c.getCount();
		String[] columnNames = c.getColumnNames();
		c.moveToFirst();
		int position = c.getPosition();
		
		Log.d("MainActivity", String.valueOf(count));
		if (count == 1 && position == 0) {
		    for (int j = 0; j < columnNames.length; j++) {
		        String columnName = columnNames[j];
		        String columnValue = c.getString(c.getColumnIndex(columnName));
		        
		        Log.d("MainActivity", columnName + ": " + columnValue);
		        //Use the values
		    }
		}
		c.close();
	}

    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    	
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
