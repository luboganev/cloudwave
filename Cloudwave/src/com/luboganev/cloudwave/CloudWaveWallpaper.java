package com.luboganev.cloudwave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.service.wallpaper.WallpaperService;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.luboganev.cloudwave.data.LocalStorageManager;
import com.luboganev.cloudwave.data.Track;
import com.luboganev.cloudwave.receivers.AlarmReceiver;
import com.luboganev.cloudwave.service.ChangeWallpaperService;

/**
 * This live wallpaper draws the soundwave and name of a random Soundcloud track by an artist.
 * It loads any necessary data from local storage in order to function properly offline
 */
public class CloudWaveWallpaper extends WallpaperService {
	private CubeEngine mWallpaperEngine;
	private ChangedWallpaperReceiver mChangeWallpaperReceiver = new ChangedWallpaperReceiver();
	
	@Override
	public void onCreate() {
		super.onCreate();
		LogUtils.d(this, "onCreate");
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
				mChangeWallpaperReceiver, new IntentFilter(ChangeWallpaperService.INTENT_ACTION_NOTIFY_WALLPAPER_CHANGE));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		LogUtils.d(this, "onDestroy");
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mChangeWallpaperReceiver);
	}
	
    @Override
    public Engine onCreateEngine() {
    	LogUtils.d(this, "onCreateEngine");
    	mWallpaperEngine = new CubeEngine();
        return mWallpaperEngine;
    }
    
    private class ChangedWallpaperReceiver extends BroadcastReceiver {
    	@Override
		public void onReceive(Context context, Intent intent) {
    		LogUtils.d(this, "onReceiveWallpaperChange");
    		if(mWallpaperEngine != null) {
				mWallpaperEngine.changeWallpaper();
    		}
    		//schedule the next time
    		AlarmReceiver.setOneTimeAlarm(getApplicationContext(), 60 * 60 * 1000);
    	}
    }
    
    class CubeEngine extends Engine  {
        private Bitmap mCurrentSoundwave;
        private String mTitle;
        private String mPermalinkUrl;
        private boolean mIsVisible;
        private GestureDetector mGestureDetector;

        CubeEngine() {
        	LogUtils.d(this, "CubeEngine constructor");
        	mIsVisible = false;
        }
        
        private void loadDefaultTrack() {
        	LogUtils.d(this, "loading default" + " preview:" + isPreview());
    		mPermalinkUrl = getResources().getString(R.string.heed_the_sound_sample_permalink);
    		mTitle = getResources().getString(R.string.heed_the_sound_sample_title);
    		Options opt = new Options();
    		opt.inPreferredConfig = Config.ALPHA_8;
    		mCurrentSoundwave = BitmapFactory.decodeResource(getResources(), R.drawable.heed_the_sound_sample, opt);
        }
        
        private void reloadTrack() {
        	LogUtils.d(this, "reloadTrack");
        	LocalStorageManager manager = new LocalStorageManager(getApplicationContext());
        	if(mCurrentSoundwave != null) mCurrentSoundwave.recycle();
        	if(manager.hasSavedLocalStorage()) {
        		LogUtils.d(this, "from file");
        		manager.loadFromFile();
        		Track track = manager.getCurrentTrack();
        		Options opt = new Options();
				opt.inPreferredConfig = Config.ALPHA_8;
				mPermalinkUrl = track.permalinkUrl;
				mTitle = track.title;
				mCurrentSoundwave = BitmapFactory.decodeFile(manager.generateSoundwaveFileUri(track.id).getPath(), opt);
        	}
        	else {
        		// nothing downloaded, so we load the sample data
        		loadDefaultTrack();
        	}
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            LogUtils.d(this, "onCreate");
            setTouchEventsEnabled(true);
            
            mGestureDetector = new GestureDetector(getApplicationContext(), new DoubleTapListener());
            
            if(!isPreview()) {
            	AlarmReceiver.setOneTimeAlarm(getApplicationContext(), 0);
            	reloadTrack();
            }
            else loadDefaultTrack();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            LogUtils.d(this, "onDestroy" + " preview:" + isPreview());
            if(!isPreview()) {
            	AlarmReceiver.cancelAlarm(getApplicationContext());
            }
        }

//        @Override
//        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            super.onSurfaceChanged(holder, format, width, height);
//            LogUtils.d(this, "onSurfaceChanged visible:" + mIsVisible + " preview:" + isPreview());
//            if(mIsVisible || isPreview()) redraw();
//        }

        @Override
        public void onVisibilityChanged(boolean visible) {
        	super.onVisibilityChanged(visible);
        	if(visible) redraw();
        	LogUtils.d(this, "onVisibilityChanged visible:" + mIsVisible);
        	mIsVisible = visible;
        }
        
		private class DoubleTapListener extends
				GestureDetector.SimpleOnGestureListener {

			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}

			// event when double tap occurs
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(mPermalinkUrl));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
				return true;
			}
		}

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
        }
        
        public void changeWallpaper() {
        	LogUtils.d(this, "changeWallpaper");
        	reloadTrack();
        	if(mIsVisible) redraw();
        }
        
		private int dipToPixels(int dipValue) {
			Resources r = getResources();
			int px = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, dipValue,
					r.getDisplayMetrics());
			return px;
		}
        
		private void redraw() {
			LogUtils.d(this, "redraw");
			SurfaceHolder holder = getSurfaceHolder();
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					int width = c.getWidth();
					int height = c.getHeight();
					int soundWaveHeight = height / 3;
					
					Rect soundwaveBoundingRect = new Rect(0, soundWaveHeight, width, soundWaveHeight * 2);
					Rect titleBoundingRect = new Rect(dipToPixels(8), 
							soundwaveBoundingRect.bottom + dipToPixels(8), 
							width - dipToPixels(8), 
							soundwaveBoundingRect.bottom + soundWaveHeight / 2);
					
					c.drawColor(getApplicationContext().getResources().getColor(R.color.black));
					c.save();
					
					// draw the inside of the sound wave
		            int sc = c.saveLayer(0, 0, width, height, null,
		                                      Canvas.MATRIX_SAVE_FLAG |
		                                      Canvas.CLIP_SAVE_FLAG |
		                                      Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
		                                      Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
		                                      Canvas.CLIP_TO_LAYER_SAVE_FLAG);
		            
		            c.clipRect(soundwaveBoundingRect);
		            
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
		            
		            // Draw the title
		            int startTextSizeInPixels = dipToPixels(16);
		            TextPaint textPaint = new TextPaint();
		            textPaint.setTextSize(startTextSizeInPixels);
		            textPaint.setColor(Color.WHITE);
		            textPaint.setAntiAlias(true);
		            
		            // setup the bounding box for the text
		            int maxRowsCount = (int)Math.floor(((double)titleBoundingRect.height()) / (1.5d * (double)startTextSizeInPixels));
		            int maxTitleWidth = maxRowsCount * titleBoundingRect.width();
		            float wholeWidth = textPaint.measureText(mTitle);
		            StaticLayout sl;
		            if(wholeWidth > maxTitleWidth) {
		            	int symbolsCount = (int)(maxTitleWidth / (wholeWidth / mTitle.length())) - 4;
		            	String truncated = mTitle.substring(0, symbolsCount) + "...";
		            	sl = new StaticLayout(truncated, 0, truncated.length(), 
		            			textPaint, titleBoundingRect.width(), 
		            			Alignment.ALIGN_CENTER, 1.0f, 1.0f,
		            			false, null, 0);
		            }
		            else {
		            	sl = new StaticLayout(mTitle, 0, mTitle.length(), 
		            			textPaint, titleBoundingRect.width(), 
		            			Alignment.ALIGN_CENTER, 1.0f, 1.0f,
		            			false, null, 0);
		            }
		            
		            // draw the text
		            c.save();
		            c.translate(titleBoundingRect.left, titleBoundingRect.top);
		            sl.draw(c);
		            c.restore();
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}
		}
    }
}
