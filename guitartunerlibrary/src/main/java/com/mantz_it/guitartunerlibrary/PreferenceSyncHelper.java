package com.mantz_it.guitartunerlibrary;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * <h1>Wear Guitar Tuner - Preference Synchronization Helper</h1>
 *
 * Module:      PreferenceSyncHelper.java
 * Description: This class contains a collection of static helper functions to synchronize shared
 *              preferences between a Android smartphone and a wearable device running Android Wear
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
public class PreferenceSyncHelper {
	private static final String LOGTAG = "PreferenceSyncHelper";

	/**
	 * Will synchronize a boolean preference to the other device by using the Wearable MessageAPI.
	 * Note: synchronization might fail even if this method returns true. Don't rely on this mechanism.
	 *
	 * @param googleApiClient		connected instance of the Google API Client to send a message
	 * @param nodeID				node id of the other device
	 * @param prefKey				key of the preference that should be synchronized
	 * @param newValue				new value of the preference
	 * @return true if the message was sent successfully. This does not guarantee that the other device has received the msg
	 */
	public static boolean syncBooleanPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Boolean newValue) {
		byte[] val = new byte[1];
		val[0] = newValue ? (byte)1 : (byte)0;
		return syncPref(googleApiClient, nodeID, prefKey, "boolean", val);
	}

	/**
	 * Will synchronize an integer preference to the other device by using the Wearable MessageAPI.
	 * Note: synchronization might fail even if this method returns true. Don't rely on this mechanism.
	 *
	 * @param googleApiClient		connected instance of the Google API Client to send a message
	 * @param nodeID				node id of the other device
	 * @param prefKey				key of the preference that should be synchronized
	 * @param newValue				new value of the preference
	 * @return true if the message was sent successfully. This does not guarantee that the other device has received the msg
	 */
	public static boolean syncIntegerPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Integer newValue) {
		return syncPref(googleApiClient, nodeID, prefKey, "integer", ByteBuffer.allocate(4).putInt(newValue).array());
	}

	/**
	 * Will synchronize a float preference to the other device by using the Wearable MessageAPI.
	 * Note: synchronization might fail even if this method returns true. Don't rely on this mechanism.
	 *
	 * @param googleApiClient		connected instance of the Google API Client to send a message
	 * @param nodeID				node id of the other device
	 * @param prefKey				key of the preference that should be synchronized
	 * @param newValue				new value of the preference
	 * @return true if the message was sent successfully. This does not guarantee that the other device has received the msg
	 */
	public static boolean syncFloatPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Float newValue) {
		return syncPref(googleApiClient, nodeID, prefKey, "float", ByteBuffer.allocate(4).putFloat(newValue).array());
	}

	/**
	 * Will synchronize a long preference to the other device by using the Wearable MessageAPI.
	 * Note: synchronization might fail even if this method returns true. Don't rely on this mechanism.
	 *
	 * @param googleApiClient		connected instance of the Google API Client to send a message
	 * @param nodeID				node id of the other device
	 * @param prefKey				key of the preference that should be synchronized
	 * @param newValue				new value of the preference
	 * @return true if the message was sent successfully. This does not guarantee that the other device has received the msg
	 */
	public static boolean syncLongPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Long newValue) {
		return syncPref(googleApiClient, nodeID, prefKey, "long", ByteBuffer.allocate(4).putLong(newValue).array());
	}

	/**
	 * Will synchronize a string preference to the other device by using the Wearable MessageAPI.
	 * Note: synchronization might fail even if this method returns true. Don't rely on this mechanism.
	 *
	 * @param googleApiClient		connected instance of the Google API Client to send a message
	 * @param nodeID				node id of the other device
	 * @param prefKey				key of the preference that should be synchronized
	 * @param newValue				new value of the preference
	 * @return true if the message was sent successfully. This does not guarantee that the other device has received the msg
	 */
	public static boolean syncStringPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, String newValue) {
		try {
			return syncPref(googleApiClient, nodeID, prefKey, "string", newValue.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(LOGTAG, "syncStringPref: UTF-8 encoding not available: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Will synchronize a preference to the other device by using the Wearable MessageAPI.
	 * Note: synchronization might fail even if this method returns true. Don't rely on this mechanism.
	 * Don't use this method but the above wrapper methods instead!
	 *
	 * @param googleApiClient		connected instance of the Google API Client to send a message
	 * @param nodeID				node id of the other device
	 * @param prefKey				key of the preference that should be synchronized
	 * @param type					lowercase type of the preference (e.g. "string", "long", or "boolean")
	 * @param newValue				new value of the preference encoded as byte array
	 * @return true if the message was sent successfully. This does not guarantee that the other device has received the msg
	 */
	public static boolean syncPref(GoogleApiClient googleApiClient, String nodeID, final String prefKey, String type, byte[] newValue) {
		if(googleApiClient == null || !googleApiClient.isConnected()) {
			Log.e(LOGTAG, "syncPref: GoogleApiClient is not connected!");
			return false;
		}
		if(nodeID == null || nodeID.equals("")) {
			Log.e(LOGTAG, "syncPref: Node ID is invalid!");
			return false;
		}

		// Send it to the handheld device:
		Wearable.MessageApi.sendMessage(googleApiClient, nodeID,
				"/syncPref/" + type + "/" + prefKey, newValue).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
			@Override
			public void onResult(MessageApi.SendMessageResult sendMessageResult) {
				if (!sendMessageResult.getStatus().isSuccess())
					Log.e(LOGTAG, "syncPref: Failed to sync preference ("+prefKey+") to the handheld: " + sendMessageResult.toString());
				else
					Log.d(LOGTAG, "syncPref: Synced preference "+prefKey+" (messageID=" + sendMessageResult.getRequestId() + ")!");
			}
		});
		return true;
	}

	/**
	 * This method will handle the reception of a sync message which was sent by the other device
	 * (using above methods)
	 *
	 * @param edit			a preference editor to update the received preference value
	 * @param messagePath	the message path that was associated with the message (containing the preference key and type)
	 * @param messageData	data of the message
	 * @return true if the preference was successfully extracted and updated from the message
	 */
	public static boolean handleSyncMessage(SharedPreferences.Editor edit, String messagePath, byte[] messageData) {
		String[] splitPath = messagePath.split("/");
		String type = splitPath[2];
		String prefKey = splitPath[3];
		String newValue = null;
		if(type.equals("boolean")) {
			Boolean newBool = messageData[0] > 0;
			newValue = "" + newBool;
			edit.putBoolean(prefKey, newBool);
		} else if(type.equals("integer")) {
			Integer newInt = ByteBuffer.wrap(messageData).getInt();
			newValue = "" + newInt;
			edit.putInt(prefKey, newInt);
		} else if(type.equals("long")) {
			Long newLong = ByteBuffer.wrap(messageData).getLong();
			newValue = "" + newLong;
			edit.putLong(prefKey, newLong);
		} else if(type.equals("float")) {
			Float newFloat = ByteBuffer.wrap(messageData).getFloat();
			newValue = "" + newFloat;
			edit.putFloat(prefKey, newFloat);
		} else if(type.equals("string")) {
			try {
				newValue = new String(messageData, "UTF-8");
				edit.putString(prefKey, newValue);
			} catch (UnsupportedEncodingException e) {
				Log.e(LOGTAG, "handleSyncMessage: Failed to extract String: " + e.getMessage());
			}
		} else {
			Log.d(LOGTAG, "handleSyncMessage: Received a syncPref message with unknown type: " + type);
			return false;
		}
		Log.d(LOGTAG, "handleSyncMessage: Received a syncPref message. Type is " + type + ". New value is " + newValue);
		edit.apply();
		return true;
	}
}
