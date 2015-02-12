package com.mantz_it.guitartunerlibrary;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;

/**
 * <h1>Wear Guitar Tuner - Default Tuner Skin</h1>
 *
 * Module:      DefaultTunerSkin.java
 * Description: This is a simple and very basic skin without any additional features.
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
public class DefaultTunerSkin extends TunerSkin {

	protected Paint gradientPaint;
	protected float maxAngle = 0.8f; 			// max angle of the scale (measured from the midpoint in radian)
	protected float sideLettersPosition = 0.7f;	// position of the side pitch letters. 0 is the middle of the
												// screen and 1 is the left/right edge of the screen

	/**
	 * constructor
	 */
	public DefaultTunerSkin() {
		super();
		gradientPaint = new Paint();
		gradientPaint.setAntiAlias(true);
		animationEnabled = true;	// this skin supports animation. The surface will call draw(Canvas, GuitarTuner, int, int)
	}

	@Override
	public void updateWidthAndHeight(int width, int height) {
		super.updateWidthAndHeight(width, height);
		foregroundPaint.setTextSize(height * 0.2f);
		invalidPaint.setTextSize(height * 0.2f);
		highlightPaint.setTextSize(height * 0.2f);
		gradientPaint.setTextSize(height * 0.2f);
		gradientPaint.setShader(new LinearGradient(0, 0, width / 2, 0, Color.DKGRAY, Color.LTGRAY, Shader.TileMode.MIRROR));
	}

	@Override
	public void setRound(boolean round) {
		super.setRound(round);
	}

	@Override
	public void draw(Canvas c, GuitarTuner tuner) {
		draw(c, tuner, 0, 1);
	}

	@Override
	public void draw(Canvas c, GuitarTuner tuner, int frameNumber, int framesPerCycle) {
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

			// determine the horizontal offset of the position of the letters
			// (this depends on the current step within the animation.
			float letterOffset = 0;
			if(targetPitchIndex == lastTargetPitchIndex + 1)		// one pitch up --> shift letters to the left
				letterOffset = -((frameNumber+1)/(float)framesPerCycle) * sideLettersPosition;
			else if (targetPitchIndex == lastTargetPitchIndex - 1)	// one pitch down --> shift letters to the right
				letterOffset = ((frameNumber+1)/(float)framesPerCycle) * sideLettersPosition;
			else if (targetPitchIndex != lastTargetPitchIndex) {	// more than one pitch difference to the last one --> fade
				float tmp = 2f*frameNumber/(float)framesPerCycle - 1;
				int alpha = (int) (255*tmp*tmp);
				gradientPaint.setAlpha(alpha);		// fade the old letters out and the new ones in
				foregroundPaint.setAlpha(alpha);	// also fade the needle
			}

			// if we are in the first half of the animation, we use the old results of the tuner.
			// Otherwise we use the new ones
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
				// if we do a shift animation, this is the point were we have to correct our
				// offset position because now we use the latest results of the tuner:
				if(letterOffset > 0)
					letterOffset -= sideLettersPosition;
				else if (letterOffset < 0)
					letterOffset += sideLettersPosition;
			}
			// draw the letters at height 0.2f:
			drawPitchLetter(c, centerLetter, letterOffset, 0.2f, round, gradientPaint);
			drawPitchLetter(c, leftLetter, letterOffset - sideLettersPosition, 0.2f, round, gradientPaint);
			drawPitchLetter(c, rightLetter, letterOffset + sideLettersPosition, 0.2f, round, gradientPaint);

			// determine the old and the new angle of the needle:
			float newAngle = (float) (maxAngle / (Math.pow(2,1/24f) - 1) * (tuner.getDetectedFrequency() / targetFrequency - 1));
			float oldAngle = (float) (maxAngle / (Math.pow(2,1/24f) - 1) * (tuner.getLastDetectedFrequency() / lastTargetFrequency - 1));
			float animationSpan = newAngle - oldAngle;		// default: we animate between the old angle and the new one...

			// if the target pitch has changed, we have to animate the needle either to the left or to the
			// right end of the scale. Exception: the target pitch changed exactly by one octave.
			// note that we have to correct the clipping angle down below...
			if(targetPitchIndex > lastTargetPitchIndex && targetPitchIndex-lastTargetPitchIndex != 12)
				animationSpan += 2* maxAngle;	// animate from old angle to top of scale and from the bottom of the scale to the new angle
			else if (targetPitchIndex < lastTargetPitchIndex && lastTargetPitchIndex-targetPitchIndex != 12)
				animationSpan -= 2* maxAngle;	// animate from old angle to bottom of scale and from the top of the scale to the new angle

			// determine the current angle (depending of the current step of the animation)
			float angle = oldAngle + ((frameNumber+1)/(float)framesPerCycle) * animationSpan;

			// correct the angle if it is clipping (happens when animating to the left/right end of the scale:
			if(angle > maxAngle)
				angle = angle - 2 * maxAngle;
			else if(angle < -maxAngle)
				angle = angle + 2 * maxAngle;

			// draw the needle:
			drawNeedle(c, angle, tuner.isTuned() ? highlightPaint : foregroundPaint);

			// reset alpha to default
			gradientPaint.setAlpha(255);
			foregroundPaint.setAlpha(255);
		}
	}

	/**
	 * Draws one pitch letter on the canvas.
	 * @param c				canvas to draw
	 * @param letter		the letter (e.g. "a2#")
	 * @param xPosition		horizontal position relative to the middle of the screen (-1 is left edge; 1 is right edge)
	 * @param yPosition		vertical position relative to the top of the screen: 0 is top edge; 1 is bottom edge)
	 * @param round			if set to true, the xPosition will also affect the yPosition to arrange the letters
	 *                      into a circle and make the side letters smaller.
	 * @param paint			paint that should be used
	 */
	protected void drawPitchLetter(Canvas c, String letter, float xPosition, float yPosition, boolean round, Paint paint) {
		Rect bounds = new Rect();
		gradientPaint.getTextBounds(letter, 0, letter.length(), bounds);
		float x = (xPosition + 1)/2 * width - bounds.width()/2;
		float y = height * yPosition;	// default y position. (for linear arrangement)
		float textSize = paint.getTextSize();
		if(round) {
			// if the screen is round we lower the side letters and make them smaller:
			y = height * (yPosition + 0.25f * xPosition * xPosition);
			paint.setTextSize(textSize * (1 - 0.4f*Math.abs(xPosition)));
		}
		c.drawText(letter, 0, letter.length(), x, y, paint);
		paint.setTextSize(textSize);
	}

	/**
	 * Draws the scale (21 dashes) on the canvas
	 * @param c		canvas to draw
	 */
	protected void drawScale(Canvas c) {
		// center dash (large):
		c.drawLine(width/2, height*0.37f, width/2, height*0.27f, gradientPaint);

		// side dashes:
		for (int i = 1; i < 11; i++) {
			float dashLenght = i==10 ? height*0.1f : height*0.05f;
			float x0 = (float) Math.sin(maxAngle * i / 10) * height*0.58f;
			float x1 = (float) Math.sin(maxAngle * i / 10) * (height*0.58f + dashLenght);
			float y0 = height*0.05f + (float) Math.cos(maxAngle * i / 10) * height*0.58f;
			float y1 = height*0.05f + (float) Math.cos(maxAngle * i / 10) * (height*0.58f + dashLenght);
			c.drawLine(width/2 + x0, height - y0, width/2 + x1, height - y1, gradientPaint);
			c.drawLine(width/2 - x0, height - y0, width/2 - x1, height - y1, gradientPaint);
		}
	}

	/**
	 * Draws the needle on the screen at the given angle
	 * @param c			canvas to draw
	 * @param angle		angle in radian. 0 will result in a straight vertical needle.
	 * @param paint		paint that should be used
	 */
	protected void drawNeedle(Canvas c, float angle, Paint paint) {
		float x = (float) Math.sin(angle) * height*0.58f;
		float y = height*0.05f + (float) Math.cos(angle) * height*0.58f;
		c.drawCircle(width/2, height*0.95f, height*0.01f, paint);
		c.drawLine(width/2, height*0.95f, width/2 + x, height - y, paint);
	}
}
