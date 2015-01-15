package com.mantz_it.wearguitartuner;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.text.DecimalFormat;

/**
 * <h1>Wear Guitar Tuner - Debug Tuner Skin</h1>
 *
 * Module:      DebugTunerSkin.java
 * Description: This skin is used for debugging and testing. It shows the raw fft and HPS data.
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
public class DebugTunerSkin extends TunerSkin {

	@Override
	public void updateWidthAndHeight(int width, int height) {
		super.updateWidthAndHeight(width, height);
		foregroundPaint.setTextSize(height*0.1f);
		invalidPaint.setTextSize(height*0.1f);
	}

	@Override
	public void draw(Canvas c, GuitarTuner tuner) {
		// narrow to the range: 50Hz-500Hz:
		int startFrequency = 50;
		int endFrequency = 500;
		int startIndex = (int) (startFrequency / tuner.getHzPerSample());
		int endIndex = (int) (endFrequency / tuner.getHzPerSample());
		float samplesPerPx = (float) (endIndex - startIndex) / (float) width;        // number of fft samples per one pixel
		float hzPerPx = tuner.getHzPerSample() * samplesPerPx;    // frequency span (in Hz) of one pixel

		// Clear the canvas
		c.drawRect(0, 0, width, height, backgroundPaint);


		drawSpectrum(c, tuner.isValid() ? fftPaint : invalidPaint, tuner.getMag(), startIndex, endIndex, -8f, -2f, tuner.getHzPerSample());
		drawSpectrum(c, tuner.isValid() ? highlightPaint : invalidPaint, tuner.getHPS(), startIndex, endIndex, -30f, -15f, tuner.getHzPerSample());

		// Draw detected (relevant) frequency component and pitch + debug info
		if (tuner.getDetectedFrequency() > 0) {
			float detectedFrequency = tuner.getDetectedFrequency();
			int pitchIndex = tuner.frequencyToPitchIndex(detectedFrequency);
			Paint paint = tuner.isValid() ? foregroundPaint : invalidPaint;

			// draw a line at the detected pitch:
			int frequencyPosition = (int) ((detectedFrequency - startFrequency) / hzPerPx);
			c.drawLine(frequencyPosition, 0, frequencyPosition, height, paint);

			// draw frequency (in hz)
			float yPos = height * 0.3f;
			String text = new DecimalFormat("###.# Hz").format(detectedFrequency);
			Rect bounds = new Rect();
			paint.getTextBounds(text, 0, text.length(), bounds);
			int labelPosition = frequencyPosition <= width / 2 ? frequencyPosition + 5 : frequencyPosition - bounds.width() - 5;
			c.drawText(text, 0, text.length(), labelPosition, yPos, paint);

			// draw pitch in letters
			yPos += bounds.height() * 1.1f;
			text = tuner.pitchLetterFromIndex(pitchIndex) + new DecimalFormat(" (###.# Hz)").format(tuner.pitchIndexToFrequency(pitchIndex));
			;
			paint.getTextBounds(text, 0, text.length(), bounds);
			labelPosition = frequencyPosition <= width / 2 ? frequencyPosition + 5 : frequencyPosition - bounds.width() - 5;
			c.drawText(text, 0, text.length(), labelPosition, yPos, paint);
		}
	}

	private void drawSpectrum(Canvas c, Paint paint, float[] values, int start, int end, float minDB, float maxDB, float hzPerSample) {
		float previousY		 = height;	// y coordinate of the previously processed pixel
		float currentY;					// y coordinate of the currently processed pixel
		float samplesPerPx 	= (float) (end - start) / (float) width;		// number of fft samples per one pixel
		float dbDiff 		= maxDB - minDB;
		float dbWidth 		= height / dbDiff; 	// Size (in pixel) per 1dB in the fft
		float avg;				// Used to calculate the average of multiple values in mag (horizontal average)
		int counter;			// Used to calculate the average of multiple values in mag

		// Draw FFT pixel by pixel:
		// We start at 1 because of integer round off error
		for (int i = 1; i < width; i++) {
			// Calculate the average value for this pixel (horizontal average - not the time domain average):
			avg = 0;
			counter = 0;
			for (int j = (int)(i*samplesPerPx); j < (i+1)*samplesPerPx; j++) {
				avg += values[j + start];
				counter++;
			}
			avg = avg / counter;

			// FFT:
			currentY = height - (avg - minDB) * dbWidth;
			if(currentY < 0 )
				currentY = 0;
			if(currentY > height)
				currentY = height;

			c.drawLine(i-1,previousY,i,currentY, paint);
			previousY = currentY;

			// We have to draw the last line to the bottom if we're in the last round:
			if(i+1 == width)
				c.drawLine(i,previousY,i+1,height, paint);
		}
	}
}
