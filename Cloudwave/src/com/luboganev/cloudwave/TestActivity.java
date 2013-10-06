package com.luboganev.cloudwave;

import java.io.File;
import java.io.IOException;

import com.luboganev.cloudwave.remote.SoundwaveDownloader;
import com.luboganev.cloudwave.remote.SoundwaveDownloader.DownloadState;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class TestActivity extends Activity {
	private Button mSetArtist;
	private EditText mArtistName;
	private Button mLoadRandomTrack;
	private ImageView mTrackSoundwave;
	private TextView mTrackText;
	
	public static final String DUMMY_PICTURE = "https://w1.sndcdn.com/RhJ436DPf2Vx_m.png";
	public static final String DUMMY_DIR = "soundwaves";
	
	public File mPictureFile; 
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		
		File imagesDir = getApplicationContext().getDir(DUMMY_DIR, Context.MODE_PRIVATE);
		mPictureFile = new File(imagesDir, "RhJ436DPf2Vx_m.png");
		if(mPictureFile.exists()) mPictureFile.delete();
		try {
			mPictureFile.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mSetArtist = (Button) findViewById(R.id.btn_set_artist);
		mArtistName = (EditText) findViewById(R.id.et_username);
		mLoadRandomTrack = (Button) findViewById(R.id.btn_load_random);
		mTrackSoundwave = (ImageView) findViewById(R.id.iv_track_soundwave);
		mTrackText = (TextView) findViewById(R.id.tv_track_text);
		
		mSetArtist.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//TODO: try reload artist from server
			}
		});
		
		mLoadRandomTrack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TestActivity.this, SoundwaveDownloader.class);
				intent.putExtra(SoundwaveDownloader.INPUT_EXTRA_START_DOWNLOAD, 1);
				intent.putExtra(SoundwaveDownloader.INPUT_INTENT_EXTRA_LOCAL_URI, Uri.fromFile(mPictureFile).toString());
				intent.putExtra(SoundwaveDownloader.INPUT_INTENT_EXTRA_SERVER_URI, DUMMY_PICTURE);
				startService(intent);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
				mDownloadStateReceiver, new IntentFilter(SoundwaveDownloader.CALLBACK_INTENT_ACTION));
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mDownloadStateReceiver);
	}
	
	private class DownloadStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String s = "";
			DownloadState state = (DownloadState)intent.getSerializableExtra(SoundwaveDownloader.CALLBACK_EXTRA_DOWNLOAD_STATE);
			if(state == DownloadState.RUNNING) {
				s = "running";
			}
			else if(state == DownloadState.COMPLETED) {
				s = "completed";
				Bitmap b = BitmapFactory.decodeFile(mPictureFile.getPath());
				mTrackSoundwave.setImageBitmap(b);
			}
			else if(state == DownloadState.CANCELED) {
				s = "cancelled";
			}
			else if(state == DownloadState.FAILED) {
				s = "failed";
			}
			mTrackText.setText(s);
		}
	}
	
	private DownloadStateReceiver mDownloadStateReceiver = new DownloadStateReceiver();
}
