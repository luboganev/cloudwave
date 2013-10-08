package com.luboganev.cloudwave;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.luboganev.cloudwave.data.LocalStorageManager;
import com.luboganev.cloudwave.data.Track;
import com.luboganev.cloudwave.receivers.AlarmReceiver;

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
        private Bitmap mCurrentSoundwave;
        private String mTitle;
        private String mPermalinkUrl;

        CubeEngine() {
        	reloadTrack();
        }
        
        private void reloadTrack() {
        	LocalStorageManager manager = new LocalStorageManager(getApplicationContext());
        	if(mCurrentSoundwave != null) mCurrentSoundwave.recycle();
        	if(manager.hasSavedLocalStorage()) {
        		// nothing downloaded, so we load the sample data
        		mPermalinkUrl = getResources().getString(R.string.heed_the_sound_sample_permalink);
        		mTitle = getResources().getString(R.string.heed_the_sound_sample_title);
        		Options opt = new Options();
				opt.inPreferredConfig = Config.ALPHA_8;
				mCurrentSoundwave = BitmapFactory.decodeResource(getResources(), R.drawable.heed_the_sound_sample, opt);
        	}
        	else {
        		manager.loadFromFile();
        		Track track = manager.getCurrentTrack();
        		Options opt = new Options();
				opt.inPreferredConfig = Config.ALPHA_8;
				mPermalinkUrl = track.permalinkUrl;
				mTitle = track.title;
				mCurrentSoundwave = BitmapFactory.decodeFile(manager.generateSoundwaveFileUri(track.id).getPath(), opt);
        	}
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
            if(!isPreview()) {
            	// cancel the repeating change wallpaper alarm
            	AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            	mgr.cancel(getAlarmPendingIntent(getApplicationContext()));
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if(isVisible() || isPreview()) redraw();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            if(isVisible()) redraw();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
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
        
        public void changeWallpaper() {
        	reloadTrack();
        	if(isVisible()) redraw();
        }
        
		private void redraw() {
			SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					int width = c.getWidth();
					int height = c.getHeight();
					int offsetY = height / 4;
					
					Rect soundwaveBoundingRect = new Rect(0, offsetY, width, height - offsetY);
					
					c.drawColor(getApplicationContext().getResources().getColor(R.color.black));
					
					c.clipRect(soundwaveBoundingRect);
					
					// draw the inside of the sound wave
		            int sc = c.saveLayer(0, 0, width, height, null,
		                                      Canvas.MATRIX_SAVE_FLAG |
		                                      Canvas.CLIP_SAVE_FLAG |
		                                      Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
		                                      Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
		                                      Canvas.CLIP_TO_LAYER_SAVE_FLAG);
		            
		            Paint paint = new Paint();
		            paint.setFilterBitmap(false);
		            paint.setColor(getApplicationContext().getResources().getColor(R.color.orange));
		            c.drawPaint(paint);
		            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
		            c.drawBitmap(mCurrentSoundwave, 
		            		new Rect(0,0,mCurrentSoundwave.getWidth(), mCurrentSoundwave.getHeight()), 
		            		soundwaveBoundingRect, paint);
		            paint.setXfermode(null);
		            c.restore();
		            
		            
		            c.restoreToCount(sc);
		            
		            //TODO: Draw title text
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}
		}
    }
}
