package com.mantz_it.wearguitartuner;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.mantz_it.guitartunerlibrary.AudioProcessingEngine;
import com.mantz_it.guitartunerlibrary.DebugTunerSkin;
import com.mantz_it.guitartunerlibrary.DefaultTunerSkin;
import com.mantz_it.guitartunerlibrary.GuitarTuner;
import com.mantz_it.guitartunerlibrary.TunerSkin;
import com.mantz_it.guitartunerlibrary.TunerSurface;

import java.io.File;

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

public class MainActivity extends Activity implements View.OnApplyWindowInsetsListener {
	private static final String LOGTAG = "MainActivity";
	private boolean roundScreen = false;
	private boolean loggingEnabled = false;
	private String logFilename = "WearGuitarTuner.log";

	private SharedPreferences preferences;
	private Process logcat;
	private GestureDetector gestureDetector;
	private AudioProcessingEngine audioProcessingEngine;
	private GuitarTuner guitarTuner;
	private FrameLayout fl_root;
	private TunerSurface tunerSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		fl_root = (FrameLayout) findViewById(R.id.fl_root);
		fl_root.setOnApplyWindowInsetsListener(this);	// register for this event to detect round/rect screen

		// Create the tuner surface:
		tunerSurface = new TunerSurface(MainActivity.this);
		tunerSurface.setRound(roundScreen);

		// Create a GuitarTuner instance:
		guitarTuner = new GuitarTuner(tunerSurface, (Vibrator) getSystemService(VIBRATOR_SERVICE));

		// Add the surface view to the root frameLayout:
		fl_root.addView(tunerSurface);

		// Get reference to the shared preferences:
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

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

		// Start logging if enabled:
		if(loggingEnabled) {
			try{
				// Get path to the external storage:
				String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
				File logfile = new File(extStorage + "/" + logFilename);
				if(logfile.exists())
					logfile.delete();
				logcat = Runtime.getRuntime().exec("logcat -f " + logfile);
				Log.i("MainActivity", "onCreate: started logcat ("+logcat.toString()+") to " + logfile.getAbsolutePath());
			} catch (Exception e) {
				Log.e("MainActivity", "onCreate: Failed to start logging: " + e.getMessage());
			}
		}

		Log.d(LOGTAG, "onCreate: Wear Guitar Tuner was started!");
	}

	@Override
	public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
		if(insets.isRound()) {
			roundScreen = true;
			Log.i(LOGTAG, "onCreate: detected a round Screen!");
		}
		else {
			roundScreen = false;
			Log.i(LOGTAG, "onCreate: detected a rectangular Screen!");
		}

		// Update the tunerSurface:
		if(tunerSurface != null)
			tunerSurface.setRound(roundScreen);

		// also update the value in the preferences:
		SharedPreferences.Editor edit = preferences.edit();
		edit.putBoolean(getString(R.string.pref_roundScreen), roundScreen);
		edit.apply();

		// unregister the listener:
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
	protected void onStart() {
		super.onStart();
		Log.d(LOGTAG, "onStart");

		// Apply preferences:
		// tuner skin:
		int skinIndex = preferences.getInt(getString(R.string.pref_skinIndex),0);
		TunerSkin tunerSkin = null;
		switch (skinIndex) {
			case 0: tunerSkin = new DefaultTunerSkin();
				break;
			case 1: tunerSkin = new DebugTunerSkin();
				break;
			default:
				Log.e(LOGTAG, "onStart: unknown tunerSkinIndex: " + skinIndex + ". Use default!");
				tunerSkin = new DebugTunerSkin();
				break;
		}
		tunerSurface.setTunerSkin(tunerSkin);

		// vibration:
		guitarTuner.setVibrate(preferences.getBoolean(getString(R.string.pref_vibration_enabled), true));

		// Show Toast on first startup:
		boolean firstStart = preferences.getBoolean(getString(R.string.pref_mainActivityFirstStart), true);
		if(firstStart) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putBoolean(getString(R.string.pref_mainActivityFirstStart), false);
			edit.apply();
			Toast.makeText(this, getString(R.string.toast_main_activity_first_start), Toast.LENGTH_LONG).show();
		}

		// Keep screen on:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(LOGTAG, "onRestart");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOGTAG, "onResume");
		audioProcessingEngine = new AudioProcessingEngine(guitarTuner);
		audioProcessingEngine.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOGTAG, "onStop");

		// allow screen to turn off:
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// stop logging:
		if(logcat != null) {
			try {
				logcat.destroy();
				logcat.waitFor();
				Log.i(LOGTAG, "onDestroy: logcat exit value: " + logcat.exitValue());
			} catch (Exception e) {
				Log.e(LOGTAG, "onDestroy: couldn't stop logcat: " + e.getMessage());
			}
		}
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
		if(audioProcessingEngine != null) {
			audioProcessingEngine.stopProcessing();
			try {
				audioProcessingEngine.join(250);
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "onPause: Interrupted while joining audioProcessingEngine!");
			}
		}
	}


}
