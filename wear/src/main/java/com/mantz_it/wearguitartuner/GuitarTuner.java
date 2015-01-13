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
	private static final int LOW_CUT_OFF_FREQUENCY = 50;
	private static final int HIGH_CUT_OFF_FREQUENCY = 2500;
	private static final int HPS_ORDER = 3;
	private GuitarTunerCallbackInterface callbackInterface;

	private float[] mag;					// magnitudes of the spectrum
	private float[] hps;					// harmonic product spectrum
	private float hzPerSample;				// frequency step of one index in mag
	private float strongestFrequency;		// holds the frequency of the strongest (max mag) frequency component
	private float detectedFrequency;		// holds the frequency that was calculated to be the most likely/relevant frequency component
	private String[] pitchScale;
	private float[] pitchScaleInHz;
	private String detectedPitchIndex;

	public GuitarTuner(GuitarTunerCallbackInterface callbackInterface) {
		this.callbackInterface = callbackInterface;
	}

	public void processFFTSamples(float[] mag, int sampleRate) {
		this.mag = mag;
		hzPerSample = ((float)(sampleRate / 2)) / mag.length;

		// Eliminate frequency components outside the interesting band:
		for (int i = 0; i < LOW_CUT_OFF_FREQUENCY / hzPerSample; i++)
			mag[i] = Float.NEGATIVE_INFINITY;	// set magnitude to 0 (== -invinity dB)
		for (int i = (int)(HIGH_CUT_OFF_FREQUENCY / hzPerSample); i < mag.length; i++)
			mag[i] = Float.NEGATIVE_INFINITY;	// set magnitude to 0 (== -invinity dB)

		// Calculate Harmonic Product Spectrum
		if(hps == null || hps.length != mag.length)
			hps = new float[mag.length];
		calcHarmonicProductSpectrum(mag, hps, HPS_ORDER);

		// calculate the max (strongest frequency) of the HPS
		int maxIndex = 0;
		for (int i = 1; i < hps.length; i++) {
			// max value:
			if(hps[maxIndex] < hps[i])
				maxIndex = i;
		}
		strongestFrequency = maxIndex * hzPerSample;
		Log.d(LOGTAG, "processFFTSamples: Strongest frequency component: " + strongestFrequency);

		// detect the relevant frequency component:
		detectedFrequency = strongestFrequency; 	// DEBUG

		// inform the callback interface about updated values:
		callbackInterface.process(this);
	}

	/**
	 * calculates the harmonic product spectrum from an array of magnitudes (in dB)
	 * @param mag		magnitude array (in dB)
	 * @param hps		result array (will be overwritten with the result)
	 * @param order		order of the product; 1 = up to the first harmonic ...
	 */
	private void calcHarmonicProductSpectrum(float[] mag, float[] hps, int order) {
		if(mag.length != hps.length) {
			Log.e(LOGTAG, "calcHarmonicProductSpectrum: mag[] and hps[] have to be of the same length!");
			throw new IllegalArgumentException("mag[] and hps[] have to be of the same length");
		}

		// initialize the hps array
		int hpsLength = mag.length / (order+1);
		for (int i = 0; i < hps.length; i++) {
			if(i < hpsLength)
				hps[i] = mag[i];
			else
				hps[i] = Float.NEGATIVE_INFINITY;
		}

		// do every harmonic in a big loop:
		for (int harmonic = 1; harmonic <= order; harmonic++) {
			int downsamplingFactor = harmonic + 1;
			for (int index = 0; index < hpsLength; index++) {
				// Calculate the average (downsampling):
				float avg = 0;
				for (int i = 0; i < downsamplingFactor; i++) {
					avg += mag[index*downsamplingFactor + i];
				}
				hps[index] += avg / downsamplingFactor;
			}
		}
	}

	public float getStrongestFrequency() {
		return strongestFrequency;
	}

	public float[] getMag() {
		return mag;
	}

	public float[] getHPS() {
		return hps;
	}

	public float getHzPerSample() {
		return hzPerSample;
	}

	public float getDetectedFrequency() {
		return detectedFrequency;
	}

	public String[] getPitchScale() {
		return pitchScale;
	}

	public float[] getPitchScaleInHz() {
		return pitchScaleInHz;
	}

	public String getDetectedPitchIndex() {
		return detectedPitchIndex;
	}

	public interface GuitarTunerCallbackInterface {
		public void process(GuitarTuner guitarTuner);
	}

}
