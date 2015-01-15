package com.mantz_it.wearguitartuner;

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

	private Paint gradientPaint;
	private static final float MAX_ANGLE = 0.8f; // max angle of the scale (measured from the midpoint in radian)

	public DefaultTunerSkin() {
		super();
		gradientPaint = new Paint();
		gradientPaint.setAntiAlias(true);
	}

	@Override
	public void updateWidthAndHeight(int width, int height) {
		super.updateWidthAndHeight(width, height);
		foregroundPaint.setTextSize(height * 0.2f);
		invalidPaint.setTextSize(height * 0.2f);
		highlightPaint.setTextSize(height * 0.2f);
		if(round)
			gradientPaint.setTextSize(height * 0.15f);
		else
			gradientPaint.setTextSize(height * 0.2f);
		gradientPaint.setShader(new LinearGradient(0, 0, width / 2, 0, Color.DKGRAY, Color.LTGRAY, Shader.TileMode.MIRROR));
	}

	@Override
	public void setRound(boolean round) {
		super.setRound(round);
		if(round)
			gradientPaint.setTextSize(height * 0.15f);
		else
			gradientPaint.setTextSize(height * 0.2f);
	}

	@Override
	public void draw(Canvas c, GuitarTuner tuner) {
		Paint paint = foregroundPaint;
		if(tuner.getDetectedFrequency() > tuner.getTargetFrequency()*0.99 && tuner.getDetectedFrequency() < tuner.getTargetFrequency()*1.01)
			paint = highlightPaint;
		if(!tuner.isValid())
			paint = invalidPaint;

		// Clear the canvas
		c.drawRect(0, 0, width, height, backgroundPaint);

		// draw pitch letters
		// target pitch:
		String text = tuner.pitchLetterFromIndex(tuner.frequencyToPitchIndex(tuner.getTargetFrequency()));
		Rect boundsTargetPitch = new Rect();
		paint.getTextBounds(text, 0, text.length(), boundsTargetPitch);
		c.drawText(text, 0, text.length(), width/2 - boundsTargetPitch.width()/2, height*0.2f, paint);

		// left and right pitch:
		if(tuner.isValid()) {
			float y = round ? height * 0.3f : height * 0.2f;
			text = tuner.pitchLetterFromIndex(tuner.frequencyToPitchIndex(tuner.getTargetFrequency()) - 1);
			Rect bounds = new Rect();
			gradientPaint.getTextBounds(text, 0, text.length(), bounds);
			c.drawText(text, 0, text.length(), width / 2 - boundsTargetPitch.width() / 2 - bounds.width() - width * 0.1f,
							y, gradientPaint);

			text = tuner.pitchLetterFromIndex(tuner.frequencyToPitchIndex(tuner.getTargetFrequency()) + 1);
			c.drawText(text, 0, text.length(), width / 2 + boundsTargetPitch.width() / 2 + width * 0.1f, y, gradientPaint);
		}

		// draw scale (21 dashes)
		c.drawLine(width/2, height*0.37f, width/2, height*0.27f, gradientPaint);
		for (int i = 1; i < 11; i++) {
			float dashLenght = i==10 ? height*0.1f : height*0.05f;
			float x0 = (float) Math.sin(MAX_ANGLE * i / 10) * height*0.58f;
			float x1 = (float) Math.sin(MAX_ANGLE * i / 10) * (height*0.58f + dashLenght);
			float y0 = height*0.05f + (float) Math.cos(MAX_ANGLE * i / 10) * height*0.58f;
			float y1 = height*0.05f + (float) Math.cos(MAX_ANGLE * i / 10) * (height*0.58f + dashLenght);
			c.drawLine(width/2 + x0, height - y0, width/2 + x1, height - y1, gradientPaint);
			c.drawLine(width/2 - x0, height - y0, width/2 - x1, height - y1, gradientPaint);
		}

		// draw needle
		float angle = (float) (MAX_ANGLE / (Math.pow(2,1/24f) - 1) * (tuner.getDetectedFrequency() / tuner.getTargetFrequency() - 1));
		float x = (float) Math.sin(angle) * height*0.58f;
		float y = height*0.05f + (float) Math.cos(angle) * height*0.58f;
		c.drawCircle(width/2, height*0.95f, height*0.01f, paint);
		c.drawLine(width/2, height*0.95f, width/2 + x, height - y, paint);
	}
}
