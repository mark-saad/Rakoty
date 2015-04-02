package com.mark.rakoty;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class RakotyService extends Service implements
		OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
		OnErrorListener, OnSeekCompleteListener, OnInfoListener {

	private MediaPlayer mediaPlayer;
	//WifiLock wifiLock;
	private String strAudioLink;
	// setup a notification ID
	private static final int NOTIFICATION_ID = 1;
	// call management
	private boolean isPausedInCall = false;
	private PhoneStateListener phoneStateListener;
	private TelephonyManager telephonyManager;
	// setup broadcast identifier and intent
	public static final String BROADCAST_BUFFER = "com.mark.rakoty.broadcastbuffer";
	Intent bufferIntent;
	// declare headset switch variable
	private int headsetSwitch = 1;

	@Override
	public void onCreate() {

		mediaPlayer  = new MediaPlayer();
		// instantiate buffer intent
		bufferIntent = new Intent(BROADCAST_BUFFER);
		// media player settings
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnInfoListener(this);
		mediaPlayer.reset();
		/**
		mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
		
		wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
			    .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
		wifiLock.acquire();
		*/
		// register headset receiver
		registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Manage incoming phone calls during media play
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		phoneStateListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case TelephonyManager.CALL_STATE_OFFHOOK:
				case TelephonyManager.CALL_STATE_RINGING:
					if (mediaPlayer != null) {
						//pauseMedia();
						stopMedia();
						isPausedInCall = true;
					}
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					if (mediaPlayer != null && isPausedInCall) {
						isPausedInCall = false;
						playMedia();
					}
					break;
				}
			}
		};
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

		// Start Notification
		initNotification();
		strAudioLink = intent.getExtras().getString("AudioLink");
		mediaPlayer.reset();
		if (!mediaPlayer.isPlaying()) {
			try {
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mediaPlayer.setDataSource(strAudioLink);
				// send message to activity to prepare dialogue
				sendbufferingBroadcast();
				// prepare media player
				mediaPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return START_STICKY;
	}

	protected void playMedia() {
		if(!mediaPlayer.isPlaying())
			mediaPlayer.start();
	}

	protected void pauseMedia() {
		if (mediaPlayer. isPlaying())
			mediaPlayer.pause();
	}

	protected void stopMedia(){
		if(mediaPlayer.isPlaying())
			mediaPlayer.stop();
		stopSelf();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			stopMedia();
			mediaPlayer.release();
			//wifiLock.release();
		}
		// Stop Notification
		cancelNotification();
		// Stop phone state listener
		if(phoneStateListener!=null){
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
		// Reset play button
		resetButtonPlayStopBroadcast();
		// Unregister headset receiver
		unregisterReceiver(headsetReceiver);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
		// TODO Auto-generated method stub
		//Toast.makeText(this, "Buffering update", Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			Toast.makeText(this,
					"MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK" + extra,
					Toast.LENGTH_SHORT).show();
			break;
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			Toast.makeText(this, "MEDIA ERROR SERVER DIED" + extra,
					Toast.LENGTH_SHORT).show();
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			Toast.makeText(this, "MEDIA ERROR UNKNOWN" + extra,
					Toast.LENGTH_SHORT).show();
			break;
		default:
			Toast.makeText(this, "Media Player Error!" + extra,
					Toast.LENGTH_SHORT).show();
			break;
		}
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		
		// Play Media
		if (!mediaPlayer.isPlaying()) {
			mediaPlayer.start();
		}
		sendbufferingCompleteBroadcast();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// Stop Media and end the service
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
		}
		stopSelf();
	}

	/**
	 * Handle Notifications
	 */
	private void initNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager myNotificationManager = (NotificationManager) getSystemService(ns);

		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "Rakoty: Alexandria Coptic Radio";
		Context myContext = getApplicationContext();
		CharSequence contentTitle = "Rakoty";
		CharSequence contentText = "Alexandria Coptic Radio";

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
				this).setContentTitle(contentTitle).setContentText(contentText)
				.setTicker(tickerText).setSmallIcon(icon).setOngoing(true);

		// Creates an explicit intent for an Activity
		Intent notificationIntent = new Intent(this, AudioActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(myContext, 0,
				notificationIntent, 0);
		notificationBuilder.setContentIntent(contentIntent);

		Notification notification = notificationBuilder.build();
		myNotificationManager.notify(NOTIFICATION_ID, notification);
		startForeground(NOTIFICATION_ID, notification);

		/**
		 * // The stack builder object will contain an artificial back stack for
		 * the // started Activity. // This ensures that navigating backward
		 * from the Activity leads out of // your application to the Home
		 * screen. TaskStackBuilder stackBuilder =
		 * TaskStackBuilder.create(this); // Adds the back stack for the Intent
		 * (but not the Intent itself)
		 * stackBuilder.addParentStack(AudioActivity.class); // Adds the Intent
		 * that starts the Activity to the top of the stack
		 * stackBuilder.addNextIntent(notificationIntent); PendingIntent
		 * contentIntent = stackBuilder.getPendingIntent( 0,
		 * PendingIntent.FLAG_UPDATE_CURRENT );
		 * notificationBuilder.setContentIntent(contentIntent);
		 * NotificationManager mNotificationManager = (NotificationManager)
		 * getSystemService(Context.NOTIFICATION_SERVICE); // mId allows you to
		 * update the notification later on.
		 */

		/**
		 * Notification myNotification = new Notification.Builder(myContext)
		 * .setContentTitle
		 * ("My Player").setContentText(tickerText).setSmallIcon(
		 * icon).setTicker("This is a ticker").
		 * setContentIntent(contentIntent).build();
		 */
		// myNotificationManager.notify(NOTIFICATION_ID, myNotification);
		// myNotification.flags = Notification.FLAG_ONGOING_EVENT;
	}

	private void cancelNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager myNotificationManager = (NotificationManager) getSystemService(ns);
		myNotificationManager.cancel(NOTIFICATION_ID);

	}

	/**
	 * End Notification Handling
	 */

	// send a message to activity that audio is being prepared and buffering
	// started
	private void sendbufferingBroadcast() {
		bufferIntent.putExtra("buffering", "1");
		sendBroadcast(bufferIntent);
	}

	private void sendbufferingCompleteBroadcast() {
		bufferIntent.putExtra("buffering", "0");
		sendBroadcast(bufferIntent);
	}
	
	private BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
		private boolean headsetConnected = false;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// 
			if(intent.hasExtra("state")){
				if(headsetConnected && intent.getIntExtra("state",0)==0){
					headsetConnected = false;
					headsetSwitch = 0;
				}
				else if(!headsetConnected && intent.getIntExtra("state",0)==1){
					headsetConnected = true;
					headsetSwitch = 1;
				}
			}
			switch(headsetSwitch){
			case 0:
				headsetDisconnected();
				break;
			case 1:
				break;
			}
			
		}
	};
	
	private void headsetDisconnected(){
		// Stop Media and end the service
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				stopSelf();
	}
	// send message to activity to reset the play button
	private void resetButtonPlayStopBroadcast() {
		bufferIntent.putExtra("buffering", "0");
		sendBroadcast(bufferIntent);
	}

}