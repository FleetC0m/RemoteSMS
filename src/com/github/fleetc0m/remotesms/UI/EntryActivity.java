package com.github.fleetc0m.remotesms.UI;

import com.github.fleetc0m.remotesms.R;
import com.github.fleetc0m.remotesms.R.layout;
import com.github.fleetc0m.remotesms.R.menu;
import com.github.fleetc0m.remotesms.server.ServerService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

public class EntryActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entry);
		startServerService();
	}
	
	private void startServerService(){
		Intent i = new Intent(this, ServerService.class);
		this.startService(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.entry, menu);
		return true;
	}

}
