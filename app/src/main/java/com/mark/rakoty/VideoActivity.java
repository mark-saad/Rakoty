package com.mark.rakoty;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;


public class VideoActivity extends Activity {
	
	public static final String STR_VIDEO_LINK = "rtsp://sc-e1.streamcyclone.com:1935/rakoty_live/rakoty";
	private VideoView videoView;
	private ProgressDialog bufferProgreesDialog = null;
	
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.rakoty_video);
			videoView = (VideoView) findViewById(R.id.video_view);
			videoView.setVideoURI(Uri.parse(STR_VIDEO_LINK));
			MediaController mediaController = new MediaController(this); 
			videoView.setMediaController(mediaController);
			mediaController.show();
			videoView.requestFocus();
			videoView.start();
			
			
			bufferProgreesDialog = ProgressDialog.show(VideoActivity.this,"Buffering", videoView.getBufferPercentage()+"%");
			
			videoView.setOnPreparedListener(new OnPreparedListener() {
				
				@Override
				public void onPrepared(MediaPlayer mp) {
					// TODO Auto-generated method stub
					bufferProgreesDialog.dismiss();
									}
			});
			
			/**
			videoView.setOnInfoListener(new OnInfoListener() {
				
				@Override
				public boolean onInfo(MediaPlayer mp, int what, int extra) {
					switch(what){
					case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_BAD_INTERLEAVING" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_BUFFERING_END:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_BUFFERING_END" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_BUFFERING_START:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_BUFFERING_START" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_METADATA_UPDATE" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_NOT_SEEKABLE" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_UNKNOWN:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_UNKNOWN" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_VIDEO_RENDERING_START" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
						Toast.makeText(VideoActivity.this, "MEDIA_INFO_VIDEO_TRACK_LAGGING" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					}
					return true;
				}
			});
			*/
			videoView.setOnErrorListener(new OnErrorListener() {
				
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					// TODO Auto-generated method stub
					switch (what) {
					case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
						Toast.makeText(VideoActivity.this, "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
						Toast.makeText(VideoActivity.this, "MEDIA ERROR SERVER DIED" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					case MediaPlayer.MEDIA_ERROR_UNKNOWN:
						Toast.makeText(VideoActivity.this, "MEDIA ERROR UNKNOWN" + extra,
								Toast.LENGTH_SHORT).show();
						break;
					}
					return true;
				}
			});
			
	    }

}
