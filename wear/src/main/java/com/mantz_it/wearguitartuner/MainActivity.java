package com.mantz_it.wearguitartuner;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mantz_it.guitartunerlibrary.AudioProcessingEngine;
import com.mantz_it.guitartunerlibrary.GuitarTuner;
import com.mantz_it.guitartunerlibrary.PreferenceSyncHelper;
import com.mantz_it.guitartunerlibrary.TunerSkin;
import com.mantz_it.guitartunerlibrary.TunerSurface;

/**
 * <h1>Wear Guitar Tuner - Main Activity</h1>
 *
 * Module:      MainActivity.java
 * Description: Main Activity of the Wear application
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

public class MainActivity extends Activity implements View.OnApplyWindowInsetsListener,
		SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, NodeApi.NodeListener {
	private static final String LOGTAG = "MainActivity";
	private boolean roundScreen = false;

	private SharedPreferences preferences;
	private GestureDetector gestureDetector;
	private AudioProcessingEngine audioProcessingEngine;
	private GuitarTuner guitarTuner;
	private FrameLayout fl_root;
	private TunerSurface tunerSurface;
	private GoogleApiClient googleApiClient;
	private Node handheldNode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		fl_root = (FrameLayout) findViewById(R.id.fl_root);
		tunerSurface = (TunerSurface) findViewById(R.id.sv_tunerSurface);
		tunerSurface.setZOrderOnTop(true);	// WORKAROUND (see: https://code.google.com/p/android/issues/detail?id=82985)
		fl_root.setOnApplyWindowInsetsListener(this);	// register for this event to detect round/rect screen

		// Get reference to the shared preferences:
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);
		roundScreen = preferences.getBoolean(getString(R.string.pref_roundScreen), false);
		tunerSurface.setRound(roundScreen);

		// Create a GuitarTuner instance:
		guitarTuner = new GuitarTuner(tunerSurface, (Vibrator) getSystemService(VIBRATOR_SERVICE));

		// Initialize the gesture detector
		gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			public void onLongPress(MotionEvent ev) {
				// A long press starts the settings activity
				Log.i(LOGTAG, "onLongPress: Long press detected. Starting SettingsActivity...");
				Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		// create a google api client
		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();

		Log.d(LOGTAG, "onCreate: Wear Guitar Tuner was started!");
	}

	@Override
	public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
		if(insets.isRound()) {
			roundScreen = true;
			Log.i(LOGTAG, "onApplyWindowInsets: detected a round Screen!");
		}
		else {
			roundScreen = false;
			Log.i(LOGTAG, "onApplyWindowInsets: detected a rectangular Screen!");
		}

		// Update the value in the preferences:
		SharedPreferences.Editor edit = preferences.edit();
		edit.putBoolean(getString(R.string.pref_roundScreen), roundScreen);
		edit.apply();

		// note: Because at this early stage of execution, the googleApiClient is most likely
		// not connected. So we will sync the roundScreen setting in the onConnected() callback...

		// unregister the window insets listener:
		fl_root.setOnApplyWindowInsetsListener(null);

		return insets;
	}

	// Capture long presses
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		gestureDetector.onTouchEvent(ev);
		return super.dispatchTouchEvent(ev);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(LOGTAG, "onRestart");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOGTAG, "onStart");

		// Apply preferences:
		// tuner skin:
		tunerSurface.setTunerSkin(TunerSkin.getTunerSkinInstance(preferences.getInt(getString(R.string.pref_skinIndex),0), this));

		// vibration:
		guitarTuner.setVibrate(preferences.getBoolean(getString(R.string.pref_vibration_enabled), true));

		// Show Toast on first startup:
		if(preferences.getBoolean(getString(R.string.pref_settingsActivityFirstStart), true)) {
			Toast.makeText(this, getString(R.string.toast_main_activity_first_start), Toast.LENGTH_LONG).show();
		}

		// connect the google api client:
		googleApiClient.connect();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOGTAG, "onResume");

		// create and start the audio processing thread (guitar tuner thread)
		audioProcessingEngine = new AudioProcessingEngine(guitarTuner);
		audioProcessingEngine.start();
	}

	@Override
	protected void onStop() {
		//disconnect the google api client:
		if(googleApiClient != null && googleApiClient.isConnected())
			googleApiClient.disconnect();
		super.onStop();
		Log.d(LOGTAG, "onStop");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(LOGTAG, "onDestroy");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOGTAG, "onPause");

		// stop the audio processing thread
		if(audioProcessingEngine != null) {
			audioProcessingEngine.stopProcessing();
			try {
				audioProcessingEngine.interrupt();
				audioProcessingEngine.join(250);
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "onPause: Interrupted while joining audioProcessingEngine!");
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(LOGTAG, "onSharedPreferenceChanged: preference changed! (key=" + key + ")");
		if(key.equals(getString(R.string.pref_roundScreen))) {
			tunerSurface.setRound(preferences.getBoolean(key, false));
		} else if(key.equals(getString(R.string.pref_vibration_enabled))) {
			boolean vibrate = preferences.getBoolean(key, true);
			guitarTuner.setVibrate(vibrate);
		} else if(key.equals(getString(R.string.pref_skinIndex))) {
			tunerSurface.setTunerSkin(TunerSkin.getTunerSkinInstance(preferences.getInt(key, 0), this));
		}
	}

	/**
	 * (ConnectionCallbacks)
	 * Gets called after googleApiClient.connect() was executed successfully
	 */
	@Override
	public void onConnected(Bundle bundle) {
		Log.d(LOGTAG, "onConnected: googleApiClient connected!");

		// Enumerate nodes:
		Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
			@Override
			public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
				for (Node node : getConnectedNodesResult.getNodes()) {
					Log.i(LOGTAG, "onConnected: Found node: " + node.getDisplayName() + " (" + node.getId() + ")");
					handheldNode = node;	// for now we just expect one single node to be found..
				}

				// After the api client is now connected and we have the handheld node, we can sync the
				// roundScreen pref to the handheld:
				if(handheldNode != null) {
					PreferenceSyncHelper.syncBooleanPref(googleApiClient, handheldNode.getId(),
							getString(R.string.pref_roundScreen), roundScreen);
				}
			}
		});

		// Register node listener:
		Wearable.NodeApi.addListener(googleApiClient, this);	// will execute onPeerConnected() and onPeerDisconnected()
	}

	/**
	 * (ConnectionCallbacks)
	 * Gets called after googleApiClient.connect() was executed successfully and the api connection is suspended again
	 */
	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(LOGTAG, "onConnectionSuspended: googleApiClient suspended: " + cause);
	}

	/**
	 * (OnConnectionFailedListener)
	 * Gets called after googleApiClient.connect() was executed and failed
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(LOGTAG, "onConnectionFailed: googleApiClient connection failed: " + result.toString());
	}

	/**
	 * (NodeListener)
	 * Gets called if a new node (a handheld) is connected to the watch
	 */
	@Override
	public void onPeerConnected(Node node) {
		Log.i(LOGTAG, "onPeerConnected: Node " + node.getId() + " connected!");
		handheldNode = node;
	}

	/**
	 * (NodeListener)
	 * Gets called if a node (a handheld) disconnects from the watch
	 */
	@Override
	public void onPeerDisconnected(Node node) {
		Log.i(LOGTAG, "onPeerDisconnected: Node " + node.getId() + " has disconnected!");
		if(handheldNode.getId().equals(node.getId())) {
			Log.i(LOGTAG, "onPeerDisconnected: Setting wearable node to null!");
			handheldNode = null;
		}
	}
}
