package com.mantz_it.wearguitartuner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * <h1>Wear Guitar Tuner - Settings Activity</h1>
 *
 * Module:      SettingsActivity.java
 * Description: Settings Activity that lets the user choose between different tuner skins and
 *              edit important settings.
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
public class SettingsActivity extends Activity {
	private static final String LOGTAG = "SettingsActivity";
	public static final String[] TUNER_SKIN_NAMES = new String[] {	"Default Skin",
																	"Debug Skin" };
	public static final int[] TUNER_SKIN_THUMBNAILS_ROUND = new int[] {	R.drawable.thumbnail_default_skin_round,
																		R.drawable.thumbnail_debug_skin_round };
	public static final int[] TUNER_SKIN_THUMBNAILS_RECT = new int[] {	R.drawable.thumbnail_default_skin_rect,
																		R.drawable.thumbnail_debug_skin_rect };

	private SharedPreferences preferences;
	private boolean roundScreen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		final GridViewPager pager = (GridViewPager) findViewById(R.id.gvp_settings);
		pager.setAdapter(new SettingsGridViewPagerAdapter(this, getFragmentManager()));

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		roundScreen = preferences.getBoolean(getString(R.string.pref_roundScreen), false);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Show Toast on first startup:
		boolean firstStart = preferences.getBoolean(getString(R.string.pref_settingsActivityFirstStart), true);
		if(firstStart) {
			SharedPreferences.Editor edit = preferences.edit();
			edit.putBoolean(getString(R.string.pref_settingsActivityFirstStart), false);
			edit.apply();
			Toast.makeText(this, getString(R.string.toast_settings_activity_first_start_scroll_right), Toast.LENGTH_LONG).show();
			Toast.makeText(this, getString(R.string.toast_settings_activity_first_start_scroll_down), Toast.LENGTH_LONG).show();
		}
	}

	private void returnToMainActivity() {
		// exit the settings activity (return to main activity):
		Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public class SettingsGridViewPagerAdapter extends FragmentGridPagerAdapter {

		private Context context;

		public SettingsGridViewPagerAdapter(Context ctx, FragmentManager fm) {
			super(fm);
			context = ctx;
		}

		@Override
		public Fragment getFragment(int row, int column) {
			Fragment fragment = null;
			switch(row) {
				case 0:	// Skin chooser
					fragment = new SkinPreviewFragment();
					((SkinPreviewFragment)fragment).setSkinIndex(column);
					break;
				case 1:	// Vibrate ON/OFF
					final boolean vibrationEnabled = preferences.getBoolean(getString(R.string.pref_vibration_enabled), true);
					fragment = ActionFragment.create(R.drawable.ic_watch_vibrate,
													vibrationEnabled ? R.string.turn_vibration_off : R.string.turn_vibration_on,
													new ActionFragment.Listener() {
						@Override
						public void onActionPerformed() {
							// update the value in the preferences:
							SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit();
							edit.putBoolean(getString(R.string.pref_vibration_enabled), !vibrationEnabled);
							edit.apply();
							returnToMainActivity();
						}
					});
					break;
			}
			return fragment;
		}

		@Override
		public int getRowCount() {
			return 2;
		}

		@Override
		public int getColumnCount(int i) {
			return TUNER_SKIN_NAMES.length;
		}

	}

	@SuppressLint("ValidFragment")
	public class SkinPreviewFragment extends Fragment {
		private ImageView iv_thumbnail;
		private int skinIndex = -1;
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View v = inflater.inflate(R.layout.skin_preview, container, false);
			iv_thumbnail = (ImageView) v.findViewById(R.id.iv_thumbnail);
			if(skinIndex >= 0)
				iv_thumbnail.setImageResource(roundScreen ? TUNER_SKIN_THUMBNAILS_ROUND[skinIndex] : TUNER_SKIN_THUMBNAILS_RECT[skinIndex]);

			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.i(LOGTAG, "onClick (SkinPreviewFragment): changing skin to " + TUNER_SKIN_NAMES[skinIndex]);

					// update the value in the preferences:
					SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit();
					edit.putInt(getString(R.string.pref_skinIndex), skinIndex);
					edit.apply();

					returnToMainActivity();
				}
			});
			return v;
		}

		public void setSkinIndex(int index) {
			this.skinIndex = index;
			if(iv_thumbnail != null)
				iv_thumbnail.setImageResource(roundScreen ? TUNER_SKIN_THUMBNAILS_ROUND[skinIndex] : TUNER_SKIN_THUMBNAILS_RECT[skinIndex]);
		}
	}

}
