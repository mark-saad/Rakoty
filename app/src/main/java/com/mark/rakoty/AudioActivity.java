package com.mark.rakoty;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class AudioActivity extends Activity {

	public static final String WEB_VIEW_URL = "http://www.rakoty.net/Audio.php";
	public static final String AUDIO_LINK = "rtsp://sc-e1.streamcyclone.com:1935/rakotyaudio_live/rakotyaudio";
	
	Intent audioIntent;
	private Button buttonAudioPlayStop;
	//private Button buttonVideoPlayStop;
	private static boolean isAudioPlaying = false;
	//private boolean isVideoPlaying = false;
	private boolean isOnline = false;
	// Buffering dialogue
	boolean isBufferBroadcastRegistered;
	private ProgressDialog bufferProgreesDialog = null;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		try {
			audioIntent = new Intent(this, RakotyService.class);
			initViews();
			//setListeners();

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(),
					e.getClass().getName() + " " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	private void initViews() {
		// WebView Settings
		WebView myWebView = (WebView) findViewById(R.id.webview);
		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDefaultTextEncodingName("UTF-8");
		myWebView.loadUrl(WEB_VIEW_URL);
		// Initialize button variables
		buttonAudioPlayStop = (Button) findViewById(R.id.buttonAudioPlayStop);
		//buttonVideoPlayStop = (Button) findViewById(R.id.buttonVideoPlayStop);
		// Set button to correct state
		if (isAudioPlaying) {
			buttonAudioPlayStop.setText(R.string.buttonAudioTextStop);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.stop_media, 0, 0, 0);
		} else {
			buttonAudioPlayStop.setText(R.string.buttonAudioText);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.start_audio, 0, 0, 0);
		}
	}

	public void buttonAudioPlayStopClick(View view) {
		if (isAudioPlaying) {
			buttonAudioPlayStop.setText(R.string.buttonAudioText);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.start_audio, 0, 0, 0);
			stopRakotyAudioService();
			isAudioPlaying = false;
		} else {
			buttonAudioPlayStop.setText(R.string.buttonAudioTextStop);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.stop_media, 0, 0, 0);
			startRakotyAudio();
			isAudioPlaying = true;
		}
	}

	private void startRakotyAudio() {

		// Check Connectivity before playing audio
		checkConnectivity();
		if (isOnline) {
			audioIntent.putExtra("AudioLink", AUDIO_LINK);
			try {
				startService(audioIntent);
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(),
						e.getClass().getName() + " " + e.getMessage(),
						Toast.LENGTH_LONG).show();
			}

		} else {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("No Network Found");
			alertDialog.setMessage("Please connect to network");
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

						}
					});
			alertDialog.setIcon(R.drawable.ic_launcher);
			// button setting
			buttonAudioPlayStop.setText(R.string.buttonAudioText);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.start_audio, 0, 0, 0);
			isAudioPlaying = false;

			alertDialog.show();

		}

	}

	private void checkConnectivity() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting()
				|| cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
						.isConnectedOrConnecting()) {
			isOnline = true;
		} else
			isOnline = false;

	}

	private void stopRakotyAudioService() {
		try {
			stopService(audioIntent);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(),
					e.getClass().getName() + " " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}

	}

	public void buttonVideoPlayStopClick(View view) {
		// Stop Audio and reset audio button
		if(isAudioPlaying){
			buttonAudioPlayStop.setText(R.string.buttonAudioText);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.start_audio, 0, 0, 0);
			stopRakotyAudioService();
			isAudioPlaying = false;
		}
		Intent videoIntent = new Intent(this, VideoActivity.class);
		startActivity(videoIntent);
		//isVideoPlaying = true;

	}

	// handle progress dialog for buffering
	private void showBuffering(Intent bufferIntent) {
		String bufferVal = bufferIntent.getStringExtra("buffering");
		int bufferValue = Integer.parseInt(bufferVal);
		switch (bufferValue) {
		case 0:
			if (bufferProgreesDialog != null) {
				bufferProgreesDialog.dismiss();
			}
			break;
		case 1:
			initBufferDialog();
			break;
		case 2:
			// button setting
			buttonAudioPlayStop.setText(R.string.buttonAudioText);
			buttonAudioPlayStop.setCompoundDrawablesWithIntrinsicBounds(
					R.drawable.start_audio, 0, 0, 0);
			isAudioPlaying = false;
			break;

		}
	}

	private void initBufferDialog() {
		bufferProgreesDialog = ProgressDialog.show(AudioActivity.this,
				"Buffering", "Loading...");
	}

	// setup broadcast receiver
	private BroadcastReceiver bufferBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			showBuffering(intent);
		}
	};

	@Override
	protected void onPause() {
		// unregister broadcast receiver
		if (isBufferBroadcastRegistered) {
			unregisterReceiver(bufferBroadcastReceiver);
			isBufferBroadcastRegistered = false;
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		// register broadcast receiver
		if (!isBufferBroadcastRegistered) {
			registerReceiver(bufferBroadcastReceiver, new IntentFilter(
					RakotyService.BROADCAST_BUFFER));
			isBufferBroadcastRegistered = true;
		}
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
