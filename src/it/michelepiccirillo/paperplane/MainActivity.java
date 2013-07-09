package it.michelepiccirillo.paperplane;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {
	
	private List<Profile> list = new ArrayList<Profile>();
	private ArrayAdapter<Profile> adapter;
	
	private ListView profileList;
	private View loader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new ProfileAdapter(this, list);
		
		setContentView(R.layout.activity_main);	
		
		profileList = (ListView) findViewById(R.id.profileList);
		loader = findViewById(R.id.loader);
		
		setLoading(true);
		startActivity(new Intent(this, SigninActivity.class));
		
		//startService(new Intent(this, NetworkingService.class));
		
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
		
		//Log.d("MainActivity", getEmail(this));

	}
	
	public void setLoading(boolean loading) {
		profileList.setVisibility(loading ? View.GONE : View.VISIBLE);
		loader.setVisibility(loading ? View.VISIBLE : View.GONE);
	}
	
	static String getEmail(Context context) {
	    AccountManager accountManager = AccountManager.get(context); 
	    Account account = getAccount(accountManager);

	    if (account == null) {
	      return null;
	    } else {
	      return account.name;
	    }
	  }

	  private static Account getAccount(AccountManager accountManager) {
	    Account[] accounts = accountManager.getAccountsByType("com.google");
	    Account account;
	    if (accounts.length > 0) {
	      account = accounts[0];      
	    } else {
	      account = null;
	    }
	    return account;
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
