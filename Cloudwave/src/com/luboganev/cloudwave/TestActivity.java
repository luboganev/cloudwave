package com.luboganev.cloudwave;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Shader.TileMode;
import android.graphics.BitmapShader;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.luboganev.cloudwave.data.LocalStorageManager;
import com.luboganev.cloudwave.data.Track;
import com.luboganev.cloudwave.receivers.AlarmReceiver;
import com.luboganev.cloudwave.service.ChangeWallpaperService;

public class TestActivity extends Activity {
	/**
     * Builds and returns the pending intent for the 
     * change wallpaper alarm
     * 
     * @param context
     * 		Context needed when building the intent and pending intent objects
     */
    private static PendingIntent getAlarmPendingIntent(Context context) {
    	Intent i = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, i, 0);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		Button mStartAlarm = (Button) findViewById(R.id.btn_start_alarm);
		Button mStopAlarm = (Button) findViewById(R.id.btn_stop_alarm);
		
		mStartAlarm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				LogUtils.d(this, "Starting the alarms");
//				// setup a repeating alarm for change of wallpaper
//                AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
//                // alarm does not have to be exact, e.g. nobody sees the wallpaper 
//                // when device is sleeping, so no point changing it
//                mgr.setRepeating(AlarmManager.RTC,
//                                  System.currentTimeMillis() + 1000L,
//                                  10000L,
//                                  getAlarmPendingIntent(getApplicationContext()));
				
				TextView tv = (TextView)findViewById(R.id.tv_track_info);
				tv.setText("");
				
				Intent serviceIntent = new Intent(getApplicationContext(), ChangeWallpaperService.class);
				WakefulIntentService.sendWakefulWork(getApplicationContext(), serviceIntent);
			}
		});
		
		mStopAlarm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				LogUtils.d(this, "Stopping the alarms");
//				AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
//	            mgr.cancel(getAlarmPendingIntent(getApplicationContext()));
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
				mDownloadStateReceiver, new IntentFilter(ChangeWallpaperService.INTENT_ACTION_NOTIFY_WALLPAPER_CHANGE));
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mDownloadStateReceiver);
	}
	
	private class DownloadStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			LocalStorageManager manager = new LocalStorageManager(getApplicationContext());
			manager.loadFromFile();
			Track track = manager.getCurrentTrack();
			
			TextView tv = (TextView)findViewById(R.id.tv_track_info);
			tv.setText(track.title + "\r\n" + track.permalinkUrl + "\r\n" + track.waveformUrl);
			
			ImageView iv = (ImageView)findViewById(R.id.iv_track_soundwave);
			Options opt = new Options();
			opt.inPreferredConfig = Config.ALPHA_8;
			Bitmap soundwavePattern = BitmapFactory.decodeFile(manager.generateSoundwaveFileUri(track.id).getPath(), opt);
			
			// create background
            Bitmap background = Bitmap.createBitmap(soundwavePattern.getWidth(), soundwavePattern.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(background);
            canvas.drawColor(getApplicationContext().getResources().getColor(R.color.black));
            
            // draw the inside of the sound wave
            int sc = canvas.saveLayer(0, 0, background.getWidth(), background.getHeight(), null,
                                      Canvas.MATRIX_SAVE_FLAG |
                                      Canvas.CLIP_SAVE_FLAG |
                                      Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                                      Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                                      Canvas.CLIP_TO_LAYER_SAVE_FLAG);
            
            Paint paint = new Paint();
            paint.setFilterBitmap(false);
            paint.setColor(getApplicationContext().getResources().getColor(R.color.orange));
            canvas.drawPaint(paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            canvas.drawBitmap(soundwavePattern, 0, 0, paint);
            paint.setXfermode(null);
            canvas.restoreToCount(sc);
            
			iv.setImageBitmap(background);
		}
	}
	
	private DownloadStateReceiver mDownloadStateReceiver = new DownloadStateReceiver();
}
