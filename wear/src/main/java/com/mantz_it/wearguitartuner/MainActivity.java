package com.mantz_it.wearguitartuner;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
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

public class MainActivity extends Activity {
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

		// Create the tuner surface:
		tunerSurface = new TunerSurface(MainActivity.this);

		// Create a GuitarTuner instance:
		guitarTuner = new GuitarTuner(tunerSurface);

		final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
		stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
			@Override
			public void onLayoutInflated(WatchViewStub watchViewStub) {
				fl_root = (FrameLayout) watchViewStub.findViewById(R.id.fl_root_round);
				if(fl_root == null) {
					roundScreen = false;
					fl_root = (FrameLayout) watchViewStub.findViewById(R.id.fl_root_rect);
					Log.i(LOGTAG, "onCreate: detected a rectangular Screen!");
				}
				else {
					roundScreen = true;
					Log.i(LOGTAG, "onCreate: detected a round Screen!");
				}

				// Add the surface view to the root frameLayout:
				fl_root.addView(tunerSurface);
			}
		});
		Log.d(LOGTAG, "onCreate: Wear Guitar Tuner was started!");
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
