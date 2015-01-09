package com.mantz_it.wearguitartuner;

import android.util.Log;

/**
 * <h1>Wear Guitar Tuner - Guitar Tuner</h1>
 *
 * Module:      GuitarTuner.java
 * Description: This class will extract the pitch information from the fft samples
 *              and generate the tuner output.
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
public class GuitarTuner {
	private static final String LOGTAG = "GuitarTuner";
	private GuitarTunerCallbackInterface callbackInterface;

	private float strongestFrequency;

	public GuitarTuner(GuitarTunerCallbackInterface callbackInterface) {
		this.callbackInterface = callbackInterface;
	}

	public void processFFTSamples(float[] mag, int sampleRate) {
		float hzPerSample = ((float)(sampleRate / 2)) / mag.length;
		int maxIndex = 0;
		for (int i = 0; i < mag.length; i++) {
			if(mag[maxIndex] < mag[i])
				maxIndex = i;
		}
		strongestFrequency = maxIndex * hzPerSample;
		Log.d(LOGTAG, "processFFTSamples: Strongest frequency component: " + strongestFrequency);
		callbackInterface.process(this);
	}

	public float getStrongestFrequency() {
		return strongestFrequency;
	}

	public interface GuitarTunerCallbackInterface {
		public void process(GuitarTuner guitarTuner);
	}

}
