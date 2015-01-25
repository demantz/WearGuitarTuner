package com.mantz_it.wearguitartuner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mantz_it.guitartunerlibrary.TunerWearableListenerService;

import java.io.UnsupportedEncodingException;

/**
 * <h1>Wear Guitar Tuner - Handheld Main Activity</h1>
 *
 * Module:      MainActivity.java
 * Description: Main Activity on the handheld device. Will show a quick intro at the first
 *              execution and provide settings for the wear application.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2014 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
	private static final String LOGTAG = "MainActivity";
	private TextView tv_log;
	private Button bt_showLog;
	private Button bt_shareLog;
	private GoogleApiClient googleApiClient;
	private Node wearableNode;
	private SharedPreferences preferences;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get reference to the shared preferences:
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Show intro slides if this is the first execution:
		if(preferences.getBoolean(getString(R.string.pref_mainActivityFirstStart), true)) {
			Intent intent = new Intent(this,IntroActivity.class);
			startActivity(intent);
			finish();
		}

		tv_log = (TextView) findViewById(R.id.tv_log);
		bt_showLog = (Button) findViewById(R.id.bt_showLog);
		bt_shareLog = (Button) findViewById(R.id.bt_shareLog);

		tv_log.setText(getString(R.string.app_label));
		tv_log.setMovementMethod(new ScrollingMovementMethod());	// make it scroll!

		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();
	}

	@Override
	protected void onStart() {
		super.onStart();
		googleApiClient.connect();
	}

	@Override
	protected void onStop() {
		if (googleApiClient != null && googleApiClient.isConnected()) {
			Wearable.MessageApi.removeListener(googleApiClient, this);
			googleApiClient.disconnect();
		}
		super.onStop();
	}

	/**
	 * Gets called after googleApiClient.connect() was executed successfully
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Log.d(LOGTAG, "onConnected: googleApiClient connected!");

		// Enumerate nodes:
		Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
			@Override
			public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
				for (Node node : getConnectedNodesResult.getNodes()) {
					Log.i(LOGTAG, "onConnected: Found node: " + node.getDisplayName() + " (" + node.getId() + ")");
					wearableNode = node;	// for now we just expect one single node to be found..
				}
			}
		});

		// Register message listener:
		Wearable.MessageApi.addListener(googleApiClient, this);		// will execute onMessageReceived() if a message arrives
	}

	/**
	 * Gets called after googleApiClient.connect() was executed successfully and the api connection is suspended again
	 */
	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(LOGTAG, "onConnectionSuspended: googleApiClient suspended: " + cause);
	}

	/**
	 * Gets called after googleApiClient.connect() was executed and failed
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(LOGTAG, "onConnectionFailed: googleApiClient connection failed: " + result.toString());
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.i(LOGTAG, "onMessageReceived: received a message (" + messageEvent.getPath() + ") from "
				+ messageEvent.getSourceNodeId());

		try {
			final String log = new String(messageEvent.getData(), "UTF-8");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tv_log.setText(log);
					bt_shareLog.setEnabled(true);
				}
			});
		} catch (UnsupportedEncodingException e) {
			Log.e(LOGTAG, "onMessageReceived: unsupported Encoding (UTF-8): " + e.getMessage());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onBtShowLogClicked(View view) {
		if(!googleApiClient.isConnected()) {
			tv_log.setText("GoogleApiClient is not connected!");
			return;
		}
		if(wearableNode == null) {
			tv_log.setText("Wearable node is not connected!");
			return;
		}

		tv_log.setText("Querying wearable device for the log...");

		// Send it to the handheld device:
		Wearable.MessageApi.sendMessage(googleApiClient, wearableNode.getId(),
				TunerWearableListenerService.GET_LOG_MESSAGE_PATH, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
			@Override
			public void onResult(MessageApi.SendMessageResult sendMessageResult) {
				if (!sendMessageResult.getStatus().isSuccess())
					tv_log.setText("Failed to query log from the wearable: " + sendMessageResult.toString());
				else
					Log.d(LOGTAG, "onBtShowLogClicked: message (" + sendMessageResult.getRequestId() + ") was sent!");
			}
		});
	}

	public void onBtShareLogClicked(View view) {
		// Invoke email app:
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "dennis.mantz@googlemail.com", null));
		intent.putExtra(Intent.EXTRA_SUBJECT, "Wear Guitar Tuner log report");
		intent.putExtra(Intent.EXTRA_TEXT, tv_log.getText().toString());
		startActivity(Intent.createChooser(intent, getString(R.string.chooseMailApp)));
	}
}
