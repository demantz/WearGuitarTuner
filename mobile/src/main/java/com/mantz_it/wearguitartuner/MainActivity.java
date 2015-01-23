package com.mantz_it.wearguitartuner;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.mantz_it.guitartunerlibrary.TunerWearableListenerService;

import java.io.UnsupportedEncodingException;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
	private static final String LOGTAG = "MainActivity";
	private TextView tv_log;
	private GoogleApiClient googleApiClient;
	private Node wearableNode;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		tv_log = (TextView) findViewById(R.id.tv_log);

		tv_log.setText(getString(R.string.app_label));

		googleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Wearable.API)
				.build();
	}

	@Override
	protected void onStart() {
		super.onStart();
		googleApiClient.connect();
	}

	@Override
	protected void onStop() {
		if (googleApiClient != null && googleApiClient.isConnected()) {
			Wearable.MessageApi.removeListener(googleApiClient, this);
			googleApiClient.disconnect();
		}
		super.onStop();
	}

	/**
	 * Gets called after googleApiClient.connect() was executed successfully
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		Log.d(LOGTAG, "onConnected: googleApiClient connected!");

		// Enumerate nodes:
		Wearable.NodeApi.getConnectedNodes(googleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
			@Override
			public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
				for (Node node : getConnectedNodesResult.getNodes()) {
					Log.i(LOGTAG, "onConnected: Found node: " + node.getDisplayName() + " (" + node.getId() + ")");
					wearableNode = node;	// for now we just expect one single node to be found..
				}
			}
		});

		// Register message listener:
		Wearable.MessageApi.addListener(googleApiClient, this);		// will execute onMessageReceived() if a message arrives
	}

	/**
	 * Gets called after googleApiClient.connect() was executed successfully and the api connection is suspended again
	 */
	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(LOGTAG, "onConnectionSuspended: googleApiClient suspended: " + cause);
	}

	/**
	 * Gets called after googleApiClient.connect() was executed and failed
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(LOGTAG, "onConnectionFailed: googleApiClient connection failed: " + result.toString());
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.i(LOGTAG, "onMessageReceived: received a message (" + messageEvent.getPath() + ") from "
				+ messageEvent.getSourceNodeId());

		try {
			final String log = new String(messageEvent.getData(), "ASCII");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					tv_log.setText(log);
				}
			});
		} catch (UnsupportedEncodingException e) {
			Log.e(LOGTAG, "onMessageReceived: unsupported Encoding (ASCII): " + e.getMessage());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onBtShowLogClicked(View view) {
		if(!googleApiClient.isConnected()) {
			tv_log.setText("GoogleApiClient is not connected!");
			return;
		}
		if(wearableNode == null) {
			tv_log.setText("Wearable node is not connected!");
			return;
		}

		tv_log.setText("Querying wearable device for the log...");

		// Send it to the handheld device:
		Wearable.MessageApi.sendMessage(googleApiClient, wearableNode.getId(),
				TunerWearableListenerService.GET_LOG_MESSAGE_PATH, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
			@Override
			public void onResult(MessageApi.SendMessageResult sendMessageResult) {
				if (!sendMessageResult.getStatus().isSuccess())
					tv_log.setText("Failed to query log from the wearable: " + sendMessageResult.toString());
				else
					Log.d(LOGTAG, "onBtShowLogClicked: message (" + sendMessageResult.getRequestId() + ") was sent!");
			}
		});
	}
}
