package com.mantz_it.wearguitartuner;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mantz_it.guitartunerlibrary.AudioProcessingEngine;
import com.mantz_it.guitartunerlibrary.GuitarTuner;
import com.mantz_it.guitartunerlibrary.PreferenceSyncHelper;
import com.mantz_it.guitartunerlibrary.TunerSkin;
import com.mantz_it.guitartunerlibrary.TunerSurface;
import com.mantz_it.guitartunerlibrary.TunerWearableListenerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, SharedPreferences.OnSharedPreferenceChangeListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {
	private static final String LOGTAG = "MainActivity";
	private LinearLayout ll_welcomeCard;
	private LinearLayout ll_skinChooser;
	private FrameLayout fl_preview;
	private ImageView[] iv_skins;
	private Switch sw_vibrate;
	ProgressDialog progressDialog;

	private TunerSurface tunerSurface;
	private GuitarTuner guitarTuner;
	private AudioProcessingEngine audioProcessingEngine;
	private GoogleApiClient googleApiClient;
	private Node wearableNode;
	private SharedPreferences preferences;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get reference to the shared preferences:
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		// Show intro slides if this is the first execution:
		if(preferences.getBoolean(getString(R.string.pref_mainActivityFirstStart), true)) {
			Intent intent = new Intent(this,IntroActivity.class);
			startActivity(intent);
			finish();
		}

		ll_welcomeCard = (LinearLayout) findViewById(R.id.ll_welcomeCard);
		fl_preview = (FrameLayout) findViewById(R.id.fl_preview);
		ll_skinChooser = (LinearLayout) findViewById(R.id.ll_skinChooser);
		sw_vibrate = (Switch) findViewById(R.id.sw_vibrate);
		sw_vibrate.setChecked(preferences.getBoolean(getString(R.string.pref_vibration_enabled), true));
		sw_vibrate.setOnCheckedChangeListener(this);

		// Fill the list of available skins:
		iv_skins = new ImageView[TunerSkin.getTunerSkinCount()];
		for (int i = 0; i < TunerSkin.getTunerSkinCount(); i++) {
			iv_skins[i] = new ImageView(this);
			iv_skins[i].setImageResource(TunerSkin.getTunerSkinThumbnailResource(i,
					preferences.getBoolean(getString(R.string.pref_roundScreen), false)));
			iv_skins[i].setAdjustViewBounds(true);
			iv_skins[i].setMaxHeight(300);
			iv_skins[i].setId(i);		// To extract the index later
			iv_skins[i].setOnClickListener(this);
			iv_skins[i].setClickable(true);
			ll_skinChooser.addView(iv_skins[i]);
		}

		// Create a TunerSurface for the surface
		tunerSurface = new TunerSurface(this);
		fl_preview.addView(tunerSurface);

		// Create a GuitarTuner instance:
		guitarTuner = new GuitarTuner(tunerSurface, (Vibrator) getSystemService(VIBRATOR_SERVICE));

		// Show welcome card until dismissed by the user:
		if(preferences.getBoolean(getString(R.string.pref_showWelcomeCard), true)) {
			ll_welcomeCard.setVisibility(View.VISIBLE);
		}

		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();

		// Apply preferences:
		// tuner skin:
		int skinIndex = preferences.getInt(getString(R.string.pref_skinIndex),0);
		tunerSurface.setTunerSkin(TunerSkin.getTunerSkinInstance(skinIndex, this));
		iv_skins[skinIndex].setBackgroundColor(Color.DKGRAY);
		// screen shape:
		tunerSurface.setRound(preferences.getBoolean(getString(R.string.pref_roundScreen), false));
		// vibration:
		guitarTuner.setVibrate(preferences.getBoolean(getString(R.string.pref_vibration_enabled), true));
	}

	@Override
	protected void onStart() {
		super.onStart();

		// connect the google api client
		googleApiClient.connect();
	}

	@Override
	protected void onResume() {
		super.onResume();
		audioProcessingEngine = new AudioProcessingEngine(guitarTuner);
		audioProcessingEngine.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(audioProcessingEngine != null) {
			audioProcessingEngine.stopProcessing();
			try {
				audioProcessingEngine.join(250);
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "onPause: Interrupted while joining audioProcessingEngine!");
			}
		}
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

		if(messageEvent.getPath().equals(TunerWearableListenerService.GET_LOG_RESPONSE_MESSAGE_PATH)) {
			try {
				final String log = new String(messageEvent.getData(), "UTF-8");
				// Show dialog:
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
								.setTitle("Wearable Log")
								.setMessage(log)
								.setPositiveButton("Share", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// Invoke email app:
										Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "dennis.mantz@googlemail.com", null));
										intent.putExtra(Intent.EXTRA_SUBJECT, "Wear Guitar Tuner log report (wearable)");
										intent.putExtra(Intent.EXTRA_TEXT, log);
										startActivity(Intent.createChooser(intent, getString(R.string.chooseMailApp)));
									}
								})
								.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										// do nothing
									}
								})
								.create();
						dialog.setOnShowListener(new DialogInterface.OnShowListener() {
							@Override
							public void onShow(DialogInterface dialog) {
								// dismiss process dialog:
								if(progressDialog != null)
									progressDialog.dismiss();
							}
						});
						dialog.show();
					}
				});
			} catch (UnsupportedEncodingException e) {
				Log.e(LOGTAG, "onMessageReceived: unsupported Encoding (UTF-8): " + e.getMessage());
				if(progressDialog != null)
					progressDialog.dismiss();
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
			sw_vibrate.setChecked(vibrate);
		} else if(key.equals(getString(R.string.pref_skinIndex))) {
			int skinIndex = preferences.getInt(key, 0);
			tunerSurface.setTunerSkin(TunerSkin.getTunerSkinInstance(skinIndex, this));
			for(ImageView iv: iv_skins)
				iv.setBackgroundColor(Color.TRANSPARENT);
			iv_skins[skinIndex].setBackgroundColor(Color.DKGRAY);
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

		if (id == R.id.action_showHandheldLog) {
			showHandheldLog();
			return true;
		} else if (id == R.id.action_showWearableLog) {
			queryWearableLog();
			return true;
		} else if (id == R.id.action_donate) {
			// open in browser:
			String donationUrl = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CWZL4HQC9SE86";
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(donationUrl));
			startActivity(i);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onBtHideWelcomeMsgClicked(View view) {
		ll_welcomeCard.setVisibility(View.GONE);

		// update the value in the preferences:
		SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putBoolean(getString(R.string.pref_showWelcomeCard), false);
		edit.apply();
	}

	public void queryWearableLog() {
		if(!googleApiClient.isConnected()) {
			Log.e(LOGTAG, "queryWearableLog: google api client not connected!");
			Toast.makeText(this, getString(R.string.googleApiClient_not_connected), Toast.LENGTH_LONG).show();
			return;
		}
		if(wearableNode == null) {
			Log.e(LOGTAG, "queryWearableLog: wearable node not connected!");
			Toast.makeText(this, getString(R.string.wearable_node_not_connected), Toast.LENGTH_LONG).show();
			return;
		}

		if(progressDialog == null)
			progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("Loading");
		progressDialog.setMessage("Querying wearable device for the log...");
		progressDialog.show();

		// Send it to the wearable device:
		Wearable.MessageApi.sendMessage(googleApiClient, wearableNode.getId(),
				TunerWearableListenerService.GET_LOG_MESSAGE_PATH, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
			@Override
			public void onResult(MessageApi.SendMessageResult sendMessageResult) {
				if (!sendMessageResult.getStatus().isSuccess()) {
					Log.e(LOGTAG, "queryWearableLog: Failed to query log from the wearable: "
							+ sendMessageResult.getStatus().getStatusMessage());
					Toast.makeText(MainActivity.this, "Failed to query log from the wearable: "
							+ sendMessageResult.getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
				}
				else
					Log.d(LOGTAG, "queryWearableLog: message (" + sendMessageResult.getRequestId() + ") was sent!");
			}
		});
	}

	public void showHandheldLog() {
		// Read the log:
		StringBuilder log = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec("logcat -d");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			String line = "";
			String newline = System.getProperty("line.separator");
			while ((line = bufferedReader.readLine()) != null) {
				log.append(line);
				log.append(newline);
			}
		} catch (IOException e) {
			Log.e(LOGTAG, "showHandheldLog: Couldn't read log: " + e.getMessage());
			return;
		}
		final String logString = log.toString();
		new AlertDialog.Builder(MainActivity.this)
				.setTitle("Handheld Log")
				.setMessage(log)
				.setPositiveButton("Share", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Invoke email app:
						Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "dennis.mantz@googlemail.com", null));
						intent.putExtra(Intent.EXTRA_SUBJECT, "Wear Guitar Tuner log report (handheld)");
						intent.putExtra(Intent.EXTRA_TEXT, logString);
						startActivity(Intent.createChooser(intent, getString(R.string.chooseMailApp)));
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				})
				.create()
				.show();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// only take action if the new setting is actually different from the setting in the prefs:
		if(preferences.getBoolean(getString(R.string.pref_vibration_enabled), true) != isChecked) {
			// Change in prefs:
			SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
			edit.putBoolean(getString(R.string.pref_vibration_enabled), isChecked);
			edit.apply();

			// send message to wearable:
			PreferenceSyncHelper.syncBooleanPref(googleApiClient, wearableNode.getId(),
					getString(R.string.pref_vibration_enabled), isChecked);
		}
	}

	/**
	 * This is the callback method of the OnClickListener of the ImageViews in the skin chooser
	 * @param v		ImageView (thumbnail). Contains the skin index in the ID field of the view
	 */
	@Override
	public void onClick(View v) {
		// Change in prefs:
		SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putInt(getString(R.string.pref_skinIndex), v.getId());
		edit.apply();

		// send message to wearable:
		PreferenceSyncHelper.syncIntegerPref(googleApiClient, wearableNode.getId(),
				getString(R.string.pref_skinIndex), v.getId());
	}
}
