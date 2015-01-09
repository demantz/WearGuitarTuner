package com.mantz_it.wearguitartuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by dennis on 09/01/15.
 */
public class TunerSurface extends SurfaceView implements GuitarTuner.GuitarTunerCallbackInterface, SurfaceHolder.Callback {
	private static final String LOGTAG = "TunerSurface";
	private static final int TUNER_LAYOUT_DEFAULT = 1;
	private int tunerLayout = TUNER_LAYOUT_DEFAULT;

	private Paint backgroundPaint;
	private Paint foregroundPaint;

	private int width;
	private int height;

	public TunerSurface(Context context) {
		super(context);

		// Initialize paint objects:
		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		foregroundPaint = new Paint();
		foregroundPaint.setColor(Color.WHITE);

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

	@Override
	public void process(GuitarTuner guitarTuner) {
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
		// draw depending on selected layout:
		switch (tunerLayout) {
			case TUNER_LAYOUT_DEFAULT:
				drawDefault(c, guitarTuner.getStrongestFrequency());
				break;
			default:
				Log.e(LOGTAG, "draw: illegal layout: " + tunerLayout);
		}
	}

	private void drawDefault(Canvas c, float strongestFrequency) {
		// Clear the canvas
		c.drawRect(0, 0, width, height, backgroundPaint);

		c.drawText("" + strongestFrequency, 100, 100, foregroundPaint);
	}


}
