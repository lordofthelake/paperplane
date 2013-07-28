package it.michelepiccirillo.paperplane.activities;

import it.michelepiccirillo.paperplane.R;
import it.michelepiccirillo.paperplane.R.id;
import it.michelepiccirillo.paperplane.R.layout;
import it.michelepiccirillo.paperplane.R.menu;
import it.michelepiccirillo.paperplane.domain.OwnProfile;
import it.michelepiccirillo.paperplane.domain.Peer;
import it.michelepiccirillo.paperplane.network.NetworkingService;
import it.michelepiccirillo.paperplane.network.PeerListener;
import it.michelepiccirillo.paperplane.network.NetworkingService.NetworkingBinder;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements PeerListener, OnItemClickListener, OnItemLongClickListener {
	
	
	private List<Peer> list = new ArrayList<Peer>();
	private ArrayAdapter<Peer> adapter;
	
	private ListView profileList;
	private View loader;
	
	private NetworkingService service = null;
	private ServiceConnection serviceConnection = null;
	
	private Dialog connectionDialog;
	private boolean loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new PeerAdapter(this, list);
		
		setContentView(R.layout.activity_main);	
		
		profileList = (ListView) findViewById(R.id.profileList);
		profileList.setAdapter(adapter);
		profileList.setOnItemClickListener(this);
		profileList.setOnItemLongClickListener(this);
		
		loader = findViewById(R.id.loader);
		
		setLoading(true);
		
		Intent i = getIntent();
		OwnProfile p = i.getParcelableExtra(SetupActivity.EXTRA_PROFILE);
		
		Toast.makeText(this, "Hello " + p.getDisplayName() + "!", Toast.LENGTH_LONG).show();
		
		serviceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder srv) {
				NetworkingBinder binder = (NetworkingBinder) srv;
				service = binder.getService();
				service.setPeerListListener(MainActivity.this);
				invalidateOptionsMenu();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				service = null;			
				invalidateOptionsMenu();
			}
			
		};
		
		bindService(new Intent(this, NetworkingService.class), serviceConnection, 0);
		
		/*Intent profileActivity = new Intent(this, ProfileActivity.class);
		profileActivity.putExtra(SetupActivity.EXTRA_PROFILE, (Parcelable) p);
		startActivity(profileActivity);*/
		
		//Log.d("MainActivity", getEmail(this));

	}
	
	@Override
	protected void onDestroy() {
		unbindService(serviceConnection);
		super.onDestroy();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_refresh).setEnabled(service != null && !loading);
		return true;
	}
	
	public void setLoading(boolean loading) {
		this.loading = loading;
		profileList.setVisibility(loading ? View.GONE : View.VISIBLE);
		loader.setVisibility(loading ? View.VISIBLE : View.GONE);
		invalidateOptionsMenu();
	}
	
	public void onPeerListChanged(List<Peer> peers) {
		list.clear();
		list.addAll(peers);
		adapter.notifyDataSetChanged();
		setLoading(false);
	}

    @Override
    protected void onResume() {
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			if(service != null) {
				setLoading(true);
				service.refreshPeers();
			}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		connectionDialog = ProgressDialog.show(this, "", "Connecting...", true);
		Peer p = (Peer) parent.getItemAtPosition(position);
		service.connect(p);
	}


	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View arg1, int position,
			long arg3) {
		Peer p = (Peer) parent.getItemAtPosition(position);
		Toast.makeText(this, "Peer info: " + p.toString(), Toast.LENGTH_LONG).show();
		
		return true;
	}

	@Override
	public void onPeerConnected(Peer p) {
		connectionDialog.dismiss();	
		if(p.hasInetAddress()) {
		Intent profileIntent = new Intent(this, ProfileActivity.class);
		profileIntent.putExtra(ProfileActivity.EXTRA_PEER, p);
		
		startActivity(profileIntent);
		
		//Toast.makeText(this, "Connected to: " + p.toString(), Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Connected, but no IP address is available for the peer at this time. Please try again later", Toast.LENGTH_LONG).show();
		}
	}
}
