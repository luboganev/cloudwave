package com.luboganev.cloudwave;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * This live wallpaper draws the soundwave and name of a random Soundcloud track by an artist.
 * It loads any necessary data from local storage in order to function properly offline
 */
public class CloudWaveWallpaper extends WallpaperService {

    public static final String SHARED_PREFS_NAME="soundwave_settings";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new CubeEngine();
    }
    
    class CubeEngine extends Engine 
        implements SharedPreferences.OnSharedPreferenceChangeListener {

        private float mOffset;
        private float mTouchX = -1;
        private float mTouchY = -1;
        private float mCenterX;
        private float mCenterY;

        private boolean mVisible;
        private SharedPreferences mPrefs;

        CubeEngine() {
            mPrefs = CloudWaveWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        	// TODO: react on changed preferences
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
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

        @Override
        public void onTouchEvent(MotionEvent event) {
        	//TODO: implement the recognition of double tap
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                mTouchX = event.getX();
                mTouchY = event.getY();
            } else {
                mTouchX = -1;
                mTouchY = -1;
            }
            super.onTouchEvent(event);
        }
    }
}
