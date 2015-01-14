package com.mantz_it.wearguitartuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.DecimalFormat;

/**
 * <h1>Wear Guitar Tuner - Tuner Surface</h1>
 *
 * Module:      TunerSurface.java
 * Description: This class implements the GuitarTunerCallbackInterface and extends the SurfaceView.
 *              It draws the UI with the results from the GuitarTuner.
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
public class TunerSurface extends SurfaceView implements GuitarTuner.GuitarTunerCallbackInterface, SurfaceHolder.Callback {
	private static final String LOGTAG = "TunerSurface";
	private static final int TUNER_SKIN_DEBUG = 0;
	private static final int TUNER_SKIN_DEFAULT = 1;
	private int tunerSkin = TUNER_SKIN_DEBUG;

	private Paint backgroundPaint;
	private Paint foregroundPaint;
	private Paint fftPaint;
	private Paint highlightPaint;
	private Paint invalidPaint;

	private int width;
	private int height;
	private boolean round = false;

	public TunerSurface(Context context) {
		super(context);

		// Initialize paint objects:
		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		foregroundPaint = new Paint();
		foregroundPaint.setColor(Color.WHITE);
		foregroundPaint.setTextSize(30);
		fftPaint = new Paint();
		fftPaint.setColor(Color.BLUE);
		highlightPaint = new Paint();
		highlightPaint.setColor(Color.RED);
		invalidPaint = new Paint();
		invalidPaint.setColor(Color.GRAY);
		invalidPaint.setTextSize(30);

		// Add a Callback to get informed when the dimensions of the SurfaceView changes:
		this.getHolder().addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	public void setRound(boolean round) {
		this.round = round;
	}

	@Override
	public void process(GuitarTuner guitarTuner) {
		if(!this.getHolder().getSurface().isValid())
			Log.d(LOGTAG, "process: Surface is not valid!");

		// Draw:
		Canvas c = null;
		try {
			c = this.getHolder().lockCanvas();

			synchronized (this.getHolder()) {
				if(c != null) {
					// Draw
					draw(c, guitarTuner);
				} else
					Log.d(LOGTAG, "draw: Canvas is null.");
			}
		} catch (Exception e)
		{
			Log.e(LOGTAG, "draw: Error while drawing on the canvas. Stop!");
			e.printStackTrace();
		} finally {
			if (c != null) {
				this.getHolder().unlockCanvasAndPost(c);
			}
		}
	}

	private void draw(Canvas c, GuitarTuner guitarTuner) {
		// draw depending on selected skin:
		switch (tunerSkin) {
			case TUNER_SKIN_DEBUG:
				drawDebug(c, guitarTuner);
				break;
			case TUNER_SKIN_DEFAULT:
				drawDefault(c, guitarTuner);
				break;
			default:
				Log.e(LOGTAG, "draw: illegal skin: " + tunerSkin);
		}
	}

	private void drawDefault(Canvas c, GuitarTuner tuner) {
		Paint paint = foregroundPaint;
		if(tuner.getDetectedFrequency() > tuner.getTargetFrequency()*0.99 && tuner.getDetectedFrequency() < tuner.getTargetFrequency()*1.01)
			paint = highlightPaint;
		if(!tuner.isValid())
			paint = invalidPaint;

		// Clear the canvas
		c.drawRect(0, 0, width, height, backgroundPaint);

		// draw pitch letters


		// draw scale

		// draw needle
		c.drawCircle(width/2, height*0.95f, height*0.01f, paint);
		c.drawLine(width/2, height*0.95f, width/2, height*0.5f, paint);
	}

	public void drawDebug(Canvas c, GuitarTuner tuner) {
		// narrow to the range: 50Hz-500Hz:
		int startFrequency = 50;
		int endFrequency = 500;
		int startIndex = (int) (startFrequency / tuner.getHzPerSample());
		int endIndex = (int) (endFrequency / tuner.getHzPerSample());
		float samplesPerPx 	= (float) (endIndex-startIndex) / (float) width;		// number of fft samples per one pixel
		float hzPerPx 		= tuner.getHzPerSample() * samplesPerPx;	// frequency span (in Hz) of one pixel

		// Clear the canvas
		c.drawRect(0, 0, width, height, backgroundPaint);


		drawSpectrum(c, tuner.isValid() ? fftPaint : invalidPaint, tuner.getMag(), startIndex, endIndex, -8f, -2f, tuner.getHzPerSample());
		drawSpectrum(c, tuner.isValid() ? highlightPaint : invalidPaint, tuner.getHPS(), startIndex, endIndex, -30f, -15f, tuner.getHzPerSample());

		// Draw detected (relevant) frequency component and pitch + debug info
		if(tuner.getDetectedFrequency() > 0) {
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
			int labelPosition = frequencyPosition <= width/2 ? frequencyPosition + 5 : frequencyPosition - bounds.width() - 5;
			c.drawText(text, 0, text.length(), labelPosition, yPos, paint);

			// draw pitch in letters
			yPos += bounds.height() * 1.1f;
			text = tuner.pitchLetterFromIndex(pitchIndex) + new DecimalFormat(" (###.# Hz)").format(tuner.pitchIndexToFrequency(pitchIndex));;
			paint.getTextBounds(text, 0, text.length(), bounds);
			labelPosition = frequencyPosition <= width/2 ? frequencyPosition + 5 : frequencyPosition - bounds.width() - 5;
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
