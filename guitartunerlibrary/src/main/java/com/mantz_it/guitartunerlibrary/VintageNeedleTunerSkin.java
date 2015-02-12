package com.mantz_it.guitartunerlibrary;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;

/**
 * <h1>Wear Guitar Tuner - Vintage Needle Tuner Skin</h1>
 *
 * Module:      VintageNeedleTunerSkin.java
 * Description: This is nice skin with a vintage look and a needle. It is based on the default skin
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
public class VintageNeedleTunerSkin extends DefaultTunerSkin {

	private Resources resources;		// resources instance to load bitmaps
	private Bitmap scaledBackground;	// will hold the background bitmap scaled to the surface dimensions

	/**
	 * constructor.
	 * @param resources		resources instance to load bitmaps
	 */
	public VintageNeedleTunerSkin(Resources resources) {
		super();
		this.resources = resources;
		sideLettersPosition = 0.45f;	// bring the side letters closer to the center
		maxAngle = 0.6f;				// also narrow the scale to fit inside the window of the background image
	}

	@Override
	public void updateWidthAndHeight(int width, int height) {
		super.updateWidthAndHeight(width, height);
		foregroundPaint.setTextSize(height * 0.15f);
		invalidPaint.setTextSize(height * 0.15f);
		highlightPaint.setTextSize(height * 0.15f);
		gradientPaint.setTextSize(height * 0.12f);
		gradientPaint.setShader(new LinearGradient(width/5, 0, width / 2, 0, Color.DKGRAY, Color.LTGRAY, Shader.TileMode.MIRROR));

		// Load skin background and scale it to the surface dimensions
		loadBackground();
	}

	@Override
	public void setRound(boolean round) {
		super.setRound(round);

		// Load skin background and scale it to the surface dimensions
		loadBackground();
	}

	/**
	 * Loads the background bitmap and scales it according to the surface dimensions
	 */
	private void loadBackground() {
		if(width > 0 && height > 0) {
			if(round)
				scaledBackground = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.vintage_tuner_skin_round), width, height, false);
			else
				scaledBackground = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.vintage_tuner_skin_rect), width, height, false);
		}
	}

	@Override
	public void draw(Canvas c, GuitarTuner tuner) {
		draw(c, tuner, 0, 1);
	}

	@Override
	public void draw(Canvas c, GuitarTuner tuner, int frameNumber, int framesPerCycle) {
		// for detailed comments refer to the draw() implementation in DefaultTunerSkin!

		// Clear the canvas
		c.drawRect(0, 0, width, height, backgroundPaint);

		// draw scale (21 dashes)
		drawScale(c);

		// only draw pitch letters and needle if data is valid
		if(tuner.isValid()) {
			float targetFrequency = tuner.getTargetFrequency();
			float lastTargetFrequency = tuner.getLastTargetFrequency();
			int targetPitchIndex = tuner.getTargetPitchIndex();
			int lastTargetPitchIndex = tuner.frequencyToPitchIndex(lastTargetFrequency);

			// draw pitch letters
			float letterOffset = 0;
			if(targetPitchIndex == lastTargetPitchIndex + 1)
				letterOffset = -((frameNumber+1)/(float)framesPerCycle) * sideLettersPosition;		// shift letters to the left
			else if (targetPitchIndex == lastTargetPitchIndex - 1)
				letterOffset = ((frameNumber+1)/(float)framesPerCycle) * sideLettersPosition;		// shift letters to the right
			else if (targetPitchIndex != lastTargetPitchIndex) {
				float tmp = 2f*frameNumber/(float)framesPerCycle - 1;
				int alpha = (int) (255*tmp*tmp);
				gradientPaint.setAlpha(alpha);		// fade the old letters out and the new ones in
				foregroundPaint.setAlpha(alpha);	// also fade the needle
			}

			String centerLetter;
			String leftLetter;
			String rightLetter;
			if(frameNumber < (float)framesPerCycle/2) {
				centerLetter = tuner.pitchLetterFromIndex(lastTargetPitchIndex);
				leftLetter = tuner.pitchLetterFromIndex(lastTargetPitchIndex - 1);
				rightLetter = tuner.pitchLetterFromIndex(lastTargetPitchIndex + 1);
			} else {
				centerLetter = tuner.pitchLetterFromIndex(targetPitchIndex);
				leftLetter = tuner.pitchLetterFromIndex(targetPitchIndex - 1);
				rightLetter = tuner.pitchLetterFromIndex(targetPitchIndex + 1);
				if(letterOffset > 0)
					letterOffset -= sideLettersPosition;
				else if (letterOffset < 0)
					letterOffset += sideLettersPosition;
			}
			drawPitchLetter(c, centerLetter, letterOffset, 0.48f, true, gradientPaint);
			drawPitchLetter(c, leftLetter, letterOffset - sideLettersPosition, 0.48f, true, gradientPaint);
			drawPitchLetter(c, rightLetter, letterOffset + sideLettersPosition, 0.48f, true, gradientPaint);

			// draw needle
			float newAngle = (float) (maxAngle / (Math.pow(2,1/24f) - 1) * (tuner.getDetectedFrequency() / targetFrequency - 1));
			float oldAngle = (float) (maxAngle / (Math.pow(2,1/24f) - 1) * (tuner.getLastDetectedFrequency() / lastTargetFrequency - 1));
			float animationSpan = newAngle - oldAngle;		// we animate between the old angle and the new one...
			if(targetPitchIndex > lastTargetPitchIndex && targetPitchIndex-lastTargetPitchIndex != 12)
				animationSpan += 2* maxAngle;	// animate from old angle to top of scale and from the bottom of the scale to the new angle
			else if (targetPitchIndex < lastTargetPitchIndex && lastTargetPitchIndex-targetPitchIndex != 12)
				animationSpan -= 2* maxAngle;	// animate from old angle to bottom of scale and from the top of the scale to the new angle
			float angle = oldAngle + ((frameNumber+1)/(float)framesPerCycle) * animationSpan;
			if(angle > maxAngle)
				angle = angle - 2 * maxAngle;
			else if(angle < -maxAngle)
				angle = angle + 2 * maxAngle;
			drawNeedle(c, angle, tuner.isTuned() ? highlightPaint : foregroundPaint);

			gradientPaint.setAlpha(255);	// reset alpha to default
			foregroundPaint.setAlpha(255);
		}

		// draw the background over the canvas :
		if(scaledBackground != null)
			c.drawBitmap(scaledBackground, 0, 0, backgroundPaint);
	}
}
