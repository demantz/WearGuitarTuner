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
	protected float maxAngle = 0.8f; // max angle of the scale (measured from the midpoint in radian)
	protected float sideLettersPosition = 0.7f;	// position of the side pitch letters. 0 is the middle of the
															// screen and 1 is the left/right edge of the screen

	public DefaultTunerSkin() {
		super();
		gradientPaint = new Paint();
		gradientPaint.setAntiAlias(true);
		animationEnabled = true;
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
			drawPitchLetter(c, centerLetter, letterOffset, 0.2f, round, gradientPaint);
			drawPitchLetter(c, leftLetter, letterOffset - sideLettersPosition, 0.2f, round, gradientPaint);
			drawPitchLetter(c, rightLetter, letterOffset + sideLettersPosition, 0.2f, round, gradientPaint);

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
	}

	protected void drawPitchLetter(Canvas c, String letter, float xPosition, float yPosition, boolean round, Paint paint) {
		Rect bounds = new Rect();
		gradientPaint.getTextBounds(letter, 0, letter.length(), bounds);
		float x = (xPosition + 1)/2 * width - bounds.width()/2;
		float y = height * yPosition;
		float textSize = paint.getTextSize();
		if(round) {
			// if the screen is round we lower the side letters and make them smaller:
			y = height * (yPosition + 0.25f * xPosition * xPosition);
			paint.setTextSize(textSize * (1 - 0.4f*Math.abs(xPosition)));
		}
		c.drawText(letter, 0, letter.length(), x, y, paint);
		paint.setTextSize(textSize);
	}

	protected void drawScale(Canvas c) {
		c.drawLine(width/2, height*0.37f, width/2, height*0.27f, gradientPaint);
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

	protected void drawNeedle(Canvas c, float angle, Paint paint) {
		float x = (float) Math.sin(angle) * height*0.58f;
		float y = height*0.05f + (float) Math.cos(angle) * height*0.58f;
		c.drawCircle(width/2, height*0.95f, height*0.01f, paint);
		c.drawLine(width/2, height*0.95f, width/2 + x, height - y, paint);
	}
}
