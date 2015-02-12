package com.mantz_it.guitartunerlibrary;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

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
	protected Paint backgroundPaint;	// default paint for the background (black)
	protected Paint foregroundPaint;	// default paint for the foreground (white)
	protected Paint highlightPaint;		// paint to highlight (red)
	protected Paint invalidPaint;		// default paint for invalid content (grey)

	protected int width;				// surface width in px
	protected int height;				// surface height in px
	protected boolean round = false;	// indicates if the screen is round or rectangular
	protected int desiredRefreshRate = 30;		// refreshRate of the Surface in fps (if animation is enabled)
	protected boolean animationEnabled = false;	// indicates if the skin supports animation

	/**
	 * constructor.
	 */
	public TunerSkin() {
		// Initialize paint objects:
		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		foregroundPaint = new Paint();
		foregroundPaint.setColor(Color.WHITE);
		foregroundPaint.setAntiAlias(true);
		highlightPaint = new Paint();
		highlightPaint.setColor(Color.RED);
		highlightPaint.setAntiAlias(true);
		invalidPaint = new Paint();
		invalidPaint.setColor(Color.GRAY);
		invalidPaint.setAntiAlias(true);
	}

	/**
	 * This method will be called by the surface every time the dimensions change
	 * @param width		new	 width of the surface (in px)
	 * @param height	new height of the surface (in px)
	 */
	public void updateWidthAndHeight(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * This method will be called by the surface if the screen shape changes
	 * @param round		true if the new screen shape is round. false if it is rectangular
	 */
	public void setRound(boolean round) {
		this.round = round;
	}

	public void setDesiredRefreshRate(int refreshRateInMs) {
		this.desiredRefreshRate = refreshRateInMs;
	}

	public int getDesiredRefreshRate() {
		return desiredRefreshRate;
	}

	public boolean isAnimationEnabled() {
		return animationEnabled && desiredRefreshRate > 0;
	}

	/**
	 * This method will be called by the surface if a new frame (with new tuner results) should be drawn
	 * and animation is disabled.
	 * @param c			canvas to draw
	 * @param tuner		GuitarTuner instance containing the latest results
	 */
	public abstract void draw(Canvas c, GuitarTuner tuner);

	/**
	 * This method will be called by the surface if a new animated frame should be drawn.
	 * Depending on the desired frame rate and the rate at which the tuner delivers new results,
	 * this method will be called multiple times with the same results in order to animate between
	 * the results.
	 * @param c					canvas to draw
	 * @param tuner				GuitarTuner instance containing the latest (and the old) results
	 * @param frameNumber		current frame number within this animation cycle
	 * @param framesPerCycle	total number of animation frames for this cycle
	 */
	public void draw(Canvas c, GuitarTuner tuner, int frameNumber, int framesPerCycle) {
		Log.w("TunerSkin", "draw: Animated draw is not supported by this skin!");
		draw(c, tuner);
	}


	// STATIC methods for easy handling of all available skins:

	/**
	 * @return total number of available tuner skins
	 */
	public static int getTunerSkinCount() {
		return 3;
	}

	/**
	 * Will instantiate a TunerSkin object
	 * @param skinIndex		index of the skin that should be instantiated
	 * @param activity		activity instance (e.g. needed to access resources)
	 * @return a new instance of the desired tuner skin
	 */
	public static TunerSkin getTunerSkinInstance(int skinIndex, Activity activity) {
		switch (skinIndex) {
			case 0:  return new DefaultTunerSkin();
			case 1:  return new VintageNeedleTunerSkin(activity.getResources());
			case 2:  return new DebugTunerSkin();
			default: return null;
		}
	}

	/**
	 * Will extract a thumbnail resource id for the desired tuner skin
	 * @param skinIndex		index of the skin
	 * @param round			if true, this method will return a round thumbnail
	 * @return a drawable resource id of the correct thumbnail
	 */
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

	/**
	 * Returns the name (label) of the desired tuner skin
	 * @param skinIndex		index of the skin
	 * @return String containing the human readable label of the skin
	 */
	public static String getTunerSkinName(int skinIndex) {
		switch (skinIndex) {
			case 0:  return "Default Skin";
			case 1:  return "Vintage Needle Skin";
			case 2:  return "Debug Skin";
			default: return null;
		}
	}
}
