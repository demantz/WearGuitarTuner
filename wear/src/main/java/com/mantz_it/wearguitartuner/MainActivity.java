package com.mantz_it.wearguitartuner;

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

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

	private AudioProcessingEngine audioProcessingEngine;
	private GuitarTuner guitarTuner;
	private FrameLayout fl_root;
	private TunerSurface tunerSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		fl_root = (FrameLayout) findViewById(R.id.fl_root);
		fl_root.setOnApplyWindowInsetsListener(this);

		// Create the tuner surface:
		tunerSurface = new TunerSurface(MainActivity.this);
		tunerSurface.setRound(roundScreen);

		// Create a GuitarTuner instance:
		guitarTuner = new GuitarTuner(tunerSurface, (Vibrator) getSystemService(VIBRATOR_SERVICE));

		// Add the surface view to the root frameLayout:
		fl_root.addView(tunerSurface);

		// Keep screen on:
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
		return insets;
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(LOGTAG, "onStart");
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
		}
	}


}
