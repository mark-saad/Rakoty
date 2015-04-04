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
import android.widget.Toast;

public class RakotyService extends Service implements
		OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
		OnErrorListener, OnSeekCompleteListener, OnInfoListener {

	private MediaPlayer mediaPlayer;
	private String strAudioLink;
	// setup a notification ID
	private static final int NOTIFICATION_ID = 1;
	// setup broadcast identifier and intent
	public static final String BROADCAST_BUFFER = "com.mark.rakoty.broadcastbuffer";
	Intent bufferIntent;
	// declare headset switch variable
	private int headsetSwitch = 1;
    // Audio Manager variables
    AudioManager audioManager;
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

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

		// register headset receiver
		registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        // Manage audio focus
        if(!isAudioFocusGranted()) {
            return START_NOT_STICKY;
        }
        // Start Notification
		initNotification();
		strAudioLink = intent.getExtras().getString("AudioLink");
		if (!mediaPlayer.isPlaying()) {
			try {
				playMedia();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		return START_STICKY;
	}

    private boolean isAudioFocusGranted() {
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                    // Stop playback
                    stopMedia();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback
                    playMedia();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                    // Stop playback and stop service
                    stopMedia();
                    stopSelf();
                }
            }
        };
        // Request audio focus for playback
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //audioManager.registerMediaButtonEventReceiver(componentRemoteControlReceiver);
            // Start playback.
            return true;
        }
        else {
            return false;
        }
    }

	protected void playMedia()  {
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(strAudioLink);
        }catch (IOException e){
            e.printStackTrace();
        }
        // send message to activity to prepare dialogue
        sendbufferingBroadcast();
        // prepare media player
        mediaPlayer.prepareAsync();
	}

	protected void stopMedia(){
		if(mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mediaPlayer != null) {
			stopMedia();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		// Stop Notification
		cancelNotification();
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
            mediaPlayer.reset();
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
		private boolean isHeadsetConnected = false;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// 
			if(intent.hasExtra("state")){
				if(isHeadsetConnected && intent.getIntExtra("state",0)==0){
					isHeadsetConnected = false;
					headsetSwitch = 0;
				}
				else if(!isHeadsetConnected && intent.getIntExtra("state",0)==1){
					isHeadsetConnected = true;
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
        if(mediaPlayer.isPlaying()) {
            stopMedia();
            resetButtonPlayStopBroadcast();
            stopSelf();
        }
	}

    // send message to activity to reset the play button
	private void resetButtonPlayStopBroadcast() {
		bufferIntent.putExtra("buffering", "0");
		sendBroadcast(bufferIntent);
	}

}