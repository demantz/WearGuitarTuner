package com.mantz_it.wearguitartuner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * <h1>Wear Guitar Tuner - Intro Activity</h1>
 *
 * Module:      IntroActivity.java
 * Description: Intro Activity that is only invoked on first application start. It shows
 *              tutorial slides.
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
public class IntroActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

	private static final int NUM_PAGES = 3;
	private ViewPager pager;
	private Button bt_skip;
	private Button bt_next;
	private PagerAdapter pagerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);
		pager = (ViewPager) findViewById(R.id.vp_intro_pager);
		bt_skip = (Button) findViewById(R.id.bt_intro_skip);
		bt_next = (Button) findViewById(R.id.bt_intro_next);

		// Instantiate a PagerAdapter.
		pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		pager.setAdapter(pagerAdapter);
		pager.setOnPageChangeListener(this);
	}

	public void onBtSkipClicked(View view) {
		endIntro();
	}

	public void onBtNextClicked(View view) {
		if(pager.getCurrentItem() < NUM_PAGES - 1) {
			pager.setCurrentItem(pager.getCurrentItem() + 1);
		}
		else {
			endIntro();
		}
	}

	public void endIntro() {
		// update the value in the preferences:
		SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putBoolean(getString(R.string.pref_mainActivityFirstStart), false);
		edit.apply();

		// Start the main activity:
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	@Override
	public void onBackPressed() {
		if (pager.getCurrentItem() == 0) {
			// If the user is currently looking at the first page, allow the system to handle the
			// Back button. This calls finish() on this activity and pops the back stack.
			super.onBackPressed();
		} else {
			// Otherwise, select the previous page.
			pager.setCurrentItem(pager.getCurrentItem() - 1);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		if(position == NUM_PAGES - 1) {	// last page
			bt_skip.setVisibility(View.GONE);
			bt_next.setText(getString(R.string.finish));
		} else {
			bt_skip.setVisibility(View.VISIBLE);
			bt_next.setText(getString(R.string.next));
		}
	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}


	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
			switch (position) {
				case 0:
					fragment.setHeading(getString(R.string.intro_page_0_heading));
					fragment.setText(getString(R.string.intro_page_0_text));
					fragment.setImageResource(R.drawable.ic_launcher);
					break;
				case 1:
					fragment.setHeading(getString(R.string.intro_page_1_heading));
					fragment.setText(getString(R.string.intro_page_1_text));
					fragment.setImageResource(R.drawable.ic_launcher);
					break;
				case 2:
					fragment.setHeading(getString(R.string.intro_page_2_heading));
					fragment.setText(getString(R.string.intro_page_2_text));
					fragment.setImageResource(R.drawable.ic_launcher);
					break;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return NUM_PAGES;
		}
	}

	public static class ScreenSlidePageFragment extends Fragment {
		private ImageView iv_image = null;
		private TextView tv_text = null;
		private TextView tv_heading = null;
		private int imageResource = -1;
		private String text = null;
		private String heading = null;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			ViewGroup root = (ViewGroup) inflater.inflate(R.layout.intro_page, container, false);
			iv_image = (ImageView) root.findViewById(R.id.iv_intro_page_image);
			tv_text = (TextView) root.findViewById(R.id.tv_intro_page_text);
			tv_heading = (TextView) root.findViewById(R.id.tv_intro_page_heading);
			if(imageResource >= 0)
				iv_image.setImageResource(imageResource);
			if(text != null)
				tv_text.setText(text);
			if(heading != null)
				tv_heading.setText(heading);
			return root;
		}

		public void setImageResource(int res) {
			this.imageResource = res;
			if(iv_image != null)
				iv_image.setImageResource(res);
		}

		public void setText(String text) {
			this.text = text;
			if(tv_text != null)
				tv_text.setText(text);
		}

		public void setHeading(String heading) {
			this.heading = heading;
			if(tv_heading != null)
				tv_heading.setText(heading);
		}
	}
}