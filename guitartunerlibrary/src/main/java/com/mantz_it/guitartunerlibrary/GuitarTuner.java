package com.mantz_it.guitartunerlibrary;

import android.os.Vibrator;
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
	private static final float CONCERT_PITCH = 440.0f;
	private static final int HPS_ORDER = 3;
	private static final long[] VIBRATE_PATTERN_UP = {0, 200};
	private static final long[] VIBRATE_PATTERN_DOWN = {0, 200, 200, 200};
	private static final long[] VIBRATE_PATTERN_TUNED = {0, 100, 100, 100, 100, 100};
	private GuitarTunerCallbackInterface callbackInterface;
	private Vibrator vibrator;

	private float[] mag;					// magnitudes of the spectrum
	private float[] hps;					// harmonic product spectrum
	private float updateRate;				// indicates how often processFFTSamples() will be called per second
	private long lastUpdateTimestamp;		// time of the last call to processFFTSamples()
	private float hzPerSample;				// frequency step of one index in mag
	private float strongestFrequency;		// holds the frequency of the strongest (max mag) frequency component (after HPS)
	private float detectedFrequency;		// holds the frequency that was calculated to be the most likely/relevant frequency component
	private float targetFrequency;			// desired frequency to tune to
	private int targetPitchIndex;			// pitch index of the targetFrequency
	private int pitchHoldCounter = 0;		// number of cycles the same pitch was detected in series.
	private float lastDetectedFrequency;	// detected frequency of the last cycle
	private float lastTargetFrequency;		// target frequency of the last cycle
	private boolean valid;					// indicates if the current result is valid
	private boolean vibrate = false;		// on/off switch for the vibration feedback

	public GuitarTuner(GuitarTunerCallbackInterface callbackInterface, Vibrator vibrator) {
		this.callbackInterface = callbackInterface;
		this.vibrator = vibrator;
	}

	public boolean processFFTSamples(float[] mag, int sampleRate, float updateRate) {
		this.lastUpdateTimestamp = System.currentTimeMillis();
		this.updateRate = updateRate;
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
			if(hps[maxIndex] < hps[i])
				maxIndex = i;
		}
		strongestFrequency = maxIndex * hzPerSample;

		// detect the relevant frequency component:
		detectedFrequency = strongestFrequency; 	// DEBUG
		targetPitchIndex = frequencyToPitchIndex(detectedFrequency);
		targetFrequency = pitchIndexToFrequency(targetPitchIndex);
		valid = detectedFrequency >= pitchIndexToFrequency(0);

		// check against the results of the past cycles:
		if(detectedFrequency > lastDetectedFrequency*0.99 && detectedFrequency < lastDetectedFrequency*1.01) {
			Log.d(LOGTAG, "processFFTSamples: detected frequency matches the old one!");
			pitchHoldCounter++;
		} else {
			Log.d(LOGTAG, "processFFTSamples: detected frequency differs from the last by " + detectedFrequency/lastDetectedFrequency*100 + "%");
			pitchHoldCounter = 0;
		}

		// If we have a stable pitch since more than 2 cycles, give feedback to the user:
		if(pitchHoldCounter > 2) {
			if(detectedFrequency < getLowerToleranceBoundaryFrequency(targetPitchIndex)) {
				Log.i(LOGTAG, "processFFTSamples: Result: Tune up by " + (targetFrequency-detectedFrequency) + " Hz! "
								+ "Target frequency is " + targetFrequency + " Hz.");
				if(vibrate)
					vibrator.vibrate(VIBRATE_PATTERN_UP, -1);
			} else if(detectedFrequency > getUpperToleranceBoundaryFrequency(targetPitchIndex)) {
				Log.i(LOGTAG, "processFFTSamples: Result: Tune down by " + (detectedFrequency-targetFrequency) + " Hz! "
								+ "Target frequency is " + targetFrequency + " Hz.");
				if(vibrate)
					vibrator.vibrate(VIBRATE_PATTERN_DOWN, -1);
			} else {
				Log.i(LOGTAG, "processFFTSamples: Result: TUNED! Target frequency is " + targetFrequency + " Hz (Error: "
								+ (detectedFrequency-targetFrequency) + " Hz).");
				if(vibrate)
					vibrator.vibrate(VIBRATE_PATTERN_TUNED, -1);
			}
			pitchHoldCounter = 0;
		}

		// inform the callback interface about updated values:
		boolean success = callbackInterface.process(this);

		lastDetectedFrequency = detectedFrequency;
		lastTargetFrequency = targetFrequency;
		return success;
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

	public int frequencyToPitchIndex(float frequency) {
		float A1 = CONCERT_PITCH / 8;
		return Math.round((float) (12 * Math.log(frequency / A1) / Math.log(2)));
	}

	public float pitchIndexToFrequency(int index) {
		float A1 = CONCERT_PITCH / 8;
		return (float) (A1 * Math.pow(2, index/12f));
	}

	public String pitchLetterFromIndex(int index) {
		String letters;
		int octaveNumber = ((index+9) / 12) + 1;
		switch(index%12) {
			case 0:  letters = "a" + octaveNumber; break;
			case 1:  letters = "a" + octaveNumber + "#"; break;
			case 2:  letters = "b" + octaveNumber; break;
			case 3:  letters = "c" + octaveNumber; break;
			case 4:  letters = "c" + octaveNumber + "#"; break;
			case 5:  letters = "d" + octaveNumber; break;
			case 6:  letters = "d" + octaveNumber + "#"; break;
			case 7:  letters = "e" + octaveNumber; break;
			case 8:  letters = "f" + octaveNumber; break;
			case 9:  letters = "f" + octaveNumber + "#"; break;
			case 10: letters = "g" + octaveNumber; break;
			case 11: letters = "g" + octaveNumber + "#"; break;
			default: letters = "err";
		}
		return letters;
	}

	public float getLowerToleranceBoundaryFrequency(int pitchIndex) {
		float frequency = pitchIndexToFrequency(pitchIndex);
		float nextLowerFrequency = pitchIndexToFrequency(pitchIndex-1);
		return frequency - 0.05f * (frequency-nextLowerFrequency);
	}

	public float getUpperToleranceBoundaryFrequency(int pitchIndex) {
		float frequency = pitchIndexToFrequency(pitchIndex);
		float nextUpperFrequency = pitchIndexToFrequency(pitchIndex+1);
		return frequency + 0.05f * (nextUpperFrequency-frequency);
	}

	public boolean isTuned() {
		return detectedFrequency > getLowerToleranceBoundaryFrequency(targetPitchIndex)
				&& detectedFrequency < getUpperToleranceBoundaryFrequency(targetPitchIndex);
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

	public float getUpdateRate() {
		return updateRate;
	}

	public long getLastUpdateTimestamp() {
		return lastUpdateTimestamp;
	}

	public float getHzPerSample() {
		return hzPerSample;
	}

	public float getDetectedFrequency() {
		return detectedFrequency;
	}

	public float getTargetFrequency() {
		return targetFrequency;
	}

	public int getTargetPitchIndex() {
		return targetPitchIndex;
	}

	public boolean isValid() {
		return valid;
	}

	public boolean isVibrate() {
		return vibrate;
	}

	public void setVibrate(boolean vibrate) {
		this.vibrate = vibrate;
	}

	public float getLastDetectedFrequency() {
		return lastDetectedFrequency;
	}

	public float getLastTargetFrequency() {
		return lastTargetFrequency;
	}

	public interface GuitarTunerCallbackInterface {
		public boolean process(GuitarTuner guitarTuner);
	}

}
