package com.luboganev.cloudwave;

import com.luboganev.cloudwave.data.LocalStorage;
import com.luboganev.cloudwave.data.LocalStorageManager;
import com.luboganev.cloudwave.receivers.AlarmReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * This live wallpaper draws the soundwave and name of a random Soundcloud track by an artist.
 * It loads any necessary data from local storage in order to function properly offline
 */
public class CloudWaveWallpaper extends WallpaperService {
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
    public Engine onCreateEngine() {
        return new CubeEngine();
    }
    
    class CubeEngine extends Engine  {
        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        private float mCenterX;
        private float mCenterY;
        private boolean mVisible;
        
        private LocalStorageManager mLocalStorageManager;

        CubeEngine() {
        	mLocalStorageManager = new LocalStorageManager(getApplicationContext());
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            if(!isPreview()) {
            	// setup a repeating alarm for change of wallpaper
                AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                // alarm does not have to be exact, e.g. nobody sees the wallpaper 
                // when device is sleeping, so no point changing it
                mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                                  SystemClock.elapsedRealtime() + 1000,
                                  AlarmManager.INTERVAL_HOUR,
                                  getAlarmPendingIntent(getApplicationContext()));
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            // cancel the repeating change wallpaper alarm
            AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            mgr.cancel(getAlarmPendingIntent(getApplicationContext()));
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                //TODO: draw
            } else {
            	//TODO: remove anything that may take resources
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // Update the center of the screen coordinates
            mCenterX = width/2.0f;
            mCenterY = height/2.0f;
            
            //TODO: draw again
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            
            //TODO: release resources
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            mOffset = xOffset;
            //TODO: draw the wallpaper
        }

//        @Override
//        public void onTouchEvent(MotionEvent event) {
//        	//TODO: implement the recognition of double tap
//            if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                mTouchX = event.getX();
//                mTouchY = event.getY();
//            } else {
//                mTouchX = -1;
//                mTouchY = -1;
//            }
//            super.onTouchEvent(event);
//        }
    }
}
