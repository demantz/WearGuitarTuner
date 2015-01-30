package com.mantz_it.guitartunerlibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * <h1>Wear Guitar Tuner - Tuner Surface</h1>
 *
 * Module:      TunerSurface.java
 * Description: This class implements the GuitarTunerCallbackInterface and extends the SurfaceView.
 *              It calls the draw method of the current TunerSkin to draw the UI with the results from the GuitarTuner.
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
	private TunerSkin tunerSkin;
	private int width = -1;
	private int height = -1;
	private boolean round;

	public TunerSurface(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
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
		if(tunerSkin != null)
			tunerSkin.updateWidthAndHeight(width, height);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	public void setRound(boolean round) {
		this.round = round;
		if(tunerSkin != null)
			tunerSkin.setRound(round);
	}

	public void setTunerSkin(TunerSkin skin) {
		this.tunerSkin = skin;
		tunerSkin.updateWidthAndHeight(width, height);
		tunerSkin.setRound(round);
	}

	@Override
	public void process(GuitarTuner guitarTuner) {
		if(!this.getHolder().getSurface().isValid()) {
			Log.d(LOGTAG, "process: Surface is not valid!");
			return;
		}

		if(height < 0 || width < 0) {
			Log.d(LOGTAG, "process: height and width are not yet set!");
			return;
		}

		// Draw:
		Canvas c = null;
		try {
			c = this.getHolder().lockCanvas();

			synchronized (this.getHolder()) {
				if(c != null) {
					// Draw
					if(tunerSkin != null)
						tunerSkin.draw(c, guitarTuner);
					else
						Log.d(LOGTAG, "draw: Skin is null.");
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
}
