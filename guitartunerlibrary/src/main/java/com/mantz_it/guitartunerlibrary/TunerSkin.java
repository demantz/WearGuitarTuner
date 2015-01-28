package com.mantz_it.guitartunerlibrary;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * <h1>Wear Guitar Tuner - Tuner Skin</h1>
 *
 * Module:      TunerSkin.java
 * Description: This is the base class for skins. Skins define how to draw the
 *              tuner UI and hold all necessary objects needed for this job.
 *              The surface will call the draw method of a skin and pass in the
 *              canvas of the surface.
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
public abstract class TunerSkin {
	protected Paint backgroundPaint;
	protected Paint foregroundPaint;
	protected Paint fftPaint;
	protected Paint highlightPaint;
	protected Paint invalidPaint;

	protected int width;
	protected int height;
	protected boolean round = false;
	protected int desiredRefreshRate = -1;	// refreshRate of the Surface in ms

	public TunerSkin() {
		// Initialize paint objects:
		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		foregroundPaint = new Paint();
		foregroundPaint.setColor(Color.WHITE);
		foregroundPaint.setAntiAlias(true);
		fftPaint = new Paint();
		fftPaint.setColor(Color.BLUE);
		highlightPaint = new Paint();
		highlightPaint.setColor(Color.RED);
		highlightPaint.setAntiAlias(true);
		invalidPaint = new Paint();
		invalidPaint.setColor(Color.GRAY);
		invalidPaint.setAntiAlias(true);
	}

	public void updateWidthAndHeight(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setRound(boolean round) {
		this.round = round;
	}

	public void setDesiredRefreshRate(int refreshRateInMs) {
		this.desiredRefreshRate = refreshRateInMs;
	}

	public int getDesiredRefreshRate() {
		return desiredRefreshRate;
	}

	public abstract void draw(Canvas c, GuitarTuner tuner);


	// STATIC methods for easy handling of all available skins:

	public static int getTunerSkinCount() {
		return 3;
	}

	public static TunerSkin getTunerSkinInstance(int skinIndex, Activity activity) {
		switch (skinIndex) {
			case 0:  return new DefaultTunerSkin();
			case 1:  return new VintageNeedleTunerSkin(activity.getResources());
			case 2:  return new DebugTunerSkin();
			default: return null;
		}
	}

	public static int getTunerSkinThumbnailResource(int skinIndex, boolean round) {
		switch (skinIndex) {
			case 0:
				return round ? R.drawable.thumbnail_default_skin_round : R.drawable.thumbnail_default_skin_rect;
			case 1:
				return round ? R.drawable.thumbnail_vintage_needle_skin_round : R.drawable.thumbnail_vintage_needle_skin_rect;
			case 2:
				return round ? R.drawable.thumbnail_debug_skin_round : R.drawable.thumbnail_debug_skin_rect;
			default:
				return -1;
		}
	}

	public static String getTunerSkinName(int skinIndex) {
		switch (skinIndex) {
			case 0:  return "Default Skin";
			case 1:  return "Vintage Needle Skin";
			case 2:  return "Debug Skin";
			default: return null;
		}
	}
}
