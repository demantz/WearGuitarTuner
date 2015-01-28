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
 * Created by dennis on 27/01/15.
 */
public class PreferenceSyncHelper {
	private static final String LOGTAG = "PreferenceSyncHelper";

	public static boolean syncBooleanPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Boolean newValue) {
		byte[] val = new byte[1];
		val[0] = newValue ? (byte)1 : (byte)0;
		return syncPref(googleApiClient, nodeID, prefKey, "boolean", val);
	}

	public static boolean syncIntegerPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Integer newValue) {
		return syncPref(googleApiClient, nodeID, prefKey, "integer", ByteBuffer.allocate(4).putInt(newValue).array());
	}

	public static boolean syncFloatPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Float newValue) {
		return syncPref(googleApiClient, nodeID, prefKey, "float", ByteBuffer.allocate(4).putFloat(newValue).array());
	}

	public static boolean syncLongPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, Long newValue) {
		return syncPref(googleApiClient, nodeID, prefKey, "long", ByteBuffer.allocate(4).putLong(newValue).array());
	}

	public static boolean syncStringPref(GoogleApiClient googleApiClient, String nodeID, String prefKey, String newValue) {
		try {
			return syncPref(googleApiClient, nodeID, prefKey, "string", newValue.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			Log.e(LOGTAG, "syncStringPref: UTF-8 encoding not available: " + e.getMessage());
			return false;
		}
	}

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
