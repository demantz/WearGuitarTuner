package com.mantz_it.guitartunerlibrary;

import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * <h1>Wear Guitar Tuner - Tuner WearableListenerService</h1>
 *
 * Module:      TunerWearableListenerService.java
 * Description: Service that implements callback methods of the Google Android
 * 				Wear Data Layer API.
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
public class TunerWearableListenerService extends WearableListenerService {
	private final static String LOGTAG = "TunerWearableListenerService";
	public final static String GET_LOG_MESSAGE_PATH = "/getLog";
	public final static String GET_LOG_RESPONSE_MESSAGE_PATH = "/getLogResponse";
	private GoogleApiClient googleApiClient;

	@Override
	public void onCreate() {
		super.onCreate();

		// create a google api client instance. It will be connected and disconnected every time
		// sendMessageAsync() has to send a message.
		googleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Wearable.API)
				.build();
		Log.i(LOGTAG, "onCreate: Service created!");
	}

	/**
	 * Will be called from the Android System if a message was received.
	 * We will check the type of the message and handle it correctly:
	 * - if it is a get-log-message we collect the log data and send it back to the originator of the message
	 * - if it is a sync-preference-message we pass it to the handler method of the PreferenceSyncHelper
	 * @param messageEvent		the received message
	 */
	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.i(LOGTAG, "onMessageReceived: received a message (" + messageEvent.getPath() + ") from "
				+ messageEvent.getSourceNodeId());
		if (messageEvent.getPath().equals(GET_LOG_MESSAGE_PATH)) {
			try {
				// Read the log:
				Process process = Runtime.getRuntime().exec("logcat -d");
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));

				StringBuilder log=new StringBuilder();
				String line = "";
				String newline = System.getProperty("line.separator");
				while ((line = bufferedReader.readLine()) != null) {
					log.append(line);
					log.append(newline);
				}

				// Send it to the handheld device:
				sendMessageAsync(messageEvent.getSourceNodeId(), GET_LOG_RESPONSE_MESSAGE_PATH, log.toString().getBytes("UTF-8"));
			}
			catch (IOException e) {
				Log.e(LOGTAG, "onMessageReceived: Error while reading log: " + e.getMessage());
			}
		} else if(messageEvent.getPath().startsWith("/syncPref/")) {
			PreferenceSyncHelper.handleSyncMessage(PreferenceManager.getDefaultSharedPreferences(this).edit(),
					messageEvent.getPath(), messageEvent.getData());
		}
	}

	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {
		super.onDataChanged(dataEvents);
	}

	/**
	 * Sends a message asynchronously. If this method is called multiple times in a row, each message
	 * will be send separately each after another but with undefined order!
	 *
	 * @param nodeID	node id of the receiver
	 * @param path		path of this message
	 * @param payload	payload data of the message
	 */
	private void sendMessageAsync(final String nodeID, final String path, final byte[] payload) {
		// run it in the background:
		Thread asyncThread = new Thread() {
			public void run() {
				Log.d(LOGTAG, "sendMessageAsync: Thread " + this.getName() + " started!");

				// only one thread at a time is allowed to use the googleApiClient:
				synchronized (this) {
					// connect the googleApiClient:
					googleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
					if(!googleApiClient.isConnected()) {
						Log.e(LOGTAG, "sendMessageAsync (Thread="+this.getName()+"): Can't connect to google API client! stop.");
						return;
					}

					// send the message:
					MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
							googleApiClient, nodeID, path, payload).await(1000, TimeUnit.MILLISECONDS);
					if (!result.getStatus().isSuccess()) {
						Log.e(LOGTAG, "sendMessageAsync: Failed to send Message: " + result.getStatus());
					} else {
						Log.d(LOGTAG, "sendMessageAsync: Message " + result.getRequestId() + " was sent! (" + payload.length + " Byte)");
					}

					// disconnect the api client:
					googleApiClient.disconnect();
				}
				Log.d(LOGTAG, "sendMessageAsync: Thread " + this.getName() + " stopped!");
			}
		};
		asyncThread.start();
	}
}
