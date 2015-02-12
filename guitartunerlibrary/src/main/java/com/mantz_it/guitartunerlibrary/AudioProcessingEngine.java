package com.mantz_it.guitartunerlibrary;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * <h1>Wear Guitar Tuner - Audio Processing Engine</h1>
 *
 * Module:      AudioProcessingEngine.java
 * Description: This class will record audio from the device's microphone and
 *              compute the FFT in real time. The data will be forwarded to
 *              the Guitar Tuner class.
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
public class AudioProcessingEngine extends Thread{
	private static final String LOGTAG = "AudioProcessingEngine";
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final int RECORDER_ELEMENT_SIZE = 2;	// 16-bit
	private static final int BUFFER_SIZE = 1024 * 4;	// 4k buffer will need half a sec to fill at 8000Sps
	private static final int FFT_SIZE = 1024 * 32;		// make the fft size much bigger to get a higher resolution in frequency domain
	private float[] lookupTable;	// used to convert shorts to floats
	private short[] audioBuffer;	// the buffer which is filled with audio samples
	private float[] realSamples;	// buffer to keep the real values of the complex signal
	private float[] imagSamples;	// buffer to keep the imaginary values of the complex signal
	private float[] mag;			// buffer to keep the magnitude of the frequency spectrum (fft)
	private AudioRecord audioRecord;
	private FFT fftInstance;
	private GuitarTuner guitarTuner;

	private boolean stopRequested = true;
	private int failCounter = 0;			// will count how often the call to processFFTSamples()
											// failed in a row

	/**
	 * Constructor.
	 *
	 * @param guitarTuner	guitar tuner instance that receive the results of this audio processing engine
	 */
	public AudioProcessingEngine(GuitarTuner guitarTuner) {
		this.guitarTuner = guitarTuner;
		createLookupTable();
		fftInstance = new FFT(FFT_SIZE);
	}

	/**
	 * Will initialize the lookup table (short to float) and populate it correctly
	 */
	private void createLookupTable() {
		lookupTable = new float[65536];
		for (int i = 0; i < lookupTable.length; i++)
			lookupTable[i] = (i - 32768f) / 32768f;
	}

	/**
	 * Will convert a 16-bit short array to a float array. Uses the lookup table which has to be created first!
	 * If the size of the arrays differ, the small array will be processed completely and the big one not!
	 *
	 * @param in	16-bit short array (containing the PCM samples)
	 * @param out	float array that will be filled with the result
	 */
	public void short2float(short[] in, float[] out) {
		for (int i = 0; i < Math.min(in.length, out.length); i++)
			out[i] = lookupTable[in[i]+32768];
	}

	/**
	 * Set the stopRequest flag. Will stop after the next cycle.
	 */
	public void stopProcessing() {
		stopRequested = true;
	}

	/**
	 * This method runs in a separate thread and does the audio recording and processing.
	 * From within this method the processFFTSamples() method of the guitarTuner is called and every
	 * work that is done in the guitarTuner is also assigned to this thread (this includes drawing
	 * the results on the surface view!)
	 */
	public void run() {
		float realPower;
		float imagPower;
		stopRequested = false;
		Log.i(LOGTAG, "run: AudioProcessingEngine '" + this.getName() + "' started.");

		// Determine buffer size for the audioRecord:
		int minBufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
		Log.d(LOGTAG, "constructor: min. buffer size is " + minBufferSize);
		int audioBufferSize = Math.max(minBufferSize, BUFFER_SIZE * RECORDER_ELEMENT_SIZE) * 2;

		// initialize the AudioRecord instance
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, audioBufferSize);

		// allocate the buffers:
		audioBuffer = new short[BUFFER_SIZE];
		realSamples = new float[FFT_SIZE];
		imagSamples = new float[FFT_SIZE];
		mag = new float[FFT_SIZE / 2];

		// Check if AudioRecord is correctly initialized:
		if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
			Log.e(LOGTAG, "run: audioRecord is null or not initialized! Abort!");
			stopRequested = true;
			return;
		}

		// Start recording:
		audioRecord.startRecording();

		while (!stopRequested) {
			// Read new audio samples into the buffer:
			if(audioRecord.read(audioBuffer, 0, audioBuffer.length) != audioBuffer.length) {
				Log.e(LOGTAG, "run: Error while reading from AudioRecord. stop.");
				stopRequested = true;
				break;
			}
			Log.d(LOGTAG, "run: audioBuffer: " + audioBuffer[0] + ", " + audioBuffer[1] + ", " + audioBuffer[2] + ", ..., " + audioBuffer[500]);

			// convert the shorts to floats and zero the imagSamples buffer:
			for (int i = 0; i < realSamples.length; i++) {
				realSamples[i] = 0f;
				imagSamples[i] = 0f;
			}
			short2float(audioBuffer, realSamples);

			// do the fft:
			fftInstance.applyWindow(realSamples, imagSamples);
			fftInstance.fft(realSamples, imagSamples);

			// calculate the logarithmic magnitude:
			// note: the spectrum is symetrical around zero Hz and we are only interested in the positive
			// part of it.
			for (int i = 0; i < realSamples.length / 2; i++) {
				// Calc the magnitude = log(sqrt(re^2 + im^2))
				// note that we still have to divide re and im by the fft size
				realPower = realSamples[i]/FFT_SIZE;
				realPower = realPower * realPower;
				imagPower = imagSamples[i]/FFT_SIZE;
				imagPower = imagPower * imagPower;
				mag[i] = (float) Math.log10(Math.sqrt(realPower + imagPower));
			}

			// pass the magnitude samples to the Guitar Tuner:
			if(!guitarTuner.processFFTSamples(mag, RECORDER_SAMPLERATE, (float)RECORDER_SAMPLERATE/(float)BUFFER_SIZE))
				failCounter++;
			else
				failCounter = 0;

			// We stop the thread if processFFTSamples() failed 10 times in a row.
			// Usually this happens if the surface view is not initialized/valid and the app
			// is in the background.
			if(failCounter > 10) {
				Log.w(LOGTAG, "run: Calling processFFTSamples() failed 10 times in a row. stop.");
				stopRequested = true;
			}
		}

		// Stop recording:
		audioRecord.stop();
		audioRecord.release();

		Log.i(LOGTAG, "run: AudioProcessingEngine '" + this.getName() + "' stopped");
		stopRequested = true;
	}

}
