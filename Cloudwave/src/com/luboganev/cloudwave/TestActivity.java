package com.luboganev.cloudwave;

import java.io.File;
import java.io.IOException;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.luboganev.cloudwave.remote.SoundCloudApiService;
import com.luboganev.cloudwave.remote.SoundCloudApiService.RequestState;
import com.luboganev.cloudwave.remote.SoundCloudApiService.RequestType;

public class TestActivity extends Activity {
	private Button mSetArtist;
	private EditText mArtistName;
	private Button mLoadRandomTrack;
	private ImageView mTrackSoundwave;
	private TextView mTrackText;
	private TextView mArtistTracks;
	
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
		mArtistTracks = (TextView) findViewById(R.id.tv_artist_tracks);
		
		mSetArtist.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TestActivity.this, SoundCloudApiService.class);
				intent.putExtra(SoundCloudApiService.INPUT_EXTRA_START_REQUEST, 1);
				intent.putExtra(SoundCloudApiService.INPUT_EXTRA_REQUEST_TYPE, RequestType.ARTIST);
				intent.putExtra(SoundCloudApiService.INPUT_INTENT_EXTRA_ARTIST_NAME, mArtistName.getText().toString());
				startService(intent);
			}
		});
		
		mLoadRandomTrack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(TestActivity.this, SoundCloudApiService.class);
				intent.putExtra(SoundCloudApiService.INPUT_EXTRA_START_REQUEST, 1);
				intent.putExtra(SoundCloudApiService.INPUT_EXTRA_REQUEST_TYPE, RequestType.SOUNDWAVE);
				intent.putExtra(SoundCloudApiService.INPUT_INTENT_EXTRA_LOCAL_URI, Uri.fromFile(mPictureFile).toString());
				intent.putExtra(SoundCloudApiService.INPUT_INTENT_EXTRA_SERVER_URI, DUMMY_PICTURE);
				startService(intent);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
				mDownloadStateReceiver, new IntentFilter(SoundCloudApiService.CALLBACK_INTENT_ACTION));
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
			RequestState state = (RequestState)intent.getSerializableExtra(SoundCloudApiService.CALLBACK_EXTRA_REQUEST_STATE);
			RequestType type = (RequestType)intent.getSerializableExtra(SoundCloudApiService.CALLBACK_EXTRA_REQUEST_TYPE);
			if(state == RequestState.RUNNING) {
				s = "running";
			}
			else if(state == RequestState.COMPLETED) {
				s = "completed";
				String payload = intent.getStringExtra(SoundCloudApiService.CALLBACK_EXTRA_PAYLOAD);
				if(type == RequestType.ARTIST) {
					mArtistTracks.setText(payload);
				}
				else if(type == RequestType.SOUNDWAVE) {
					Bitmap b = BitmapFactory.decodeFile(new File(Uri.parse(payload).getPath()).getPath());
					mTrackSoundwave.setImageBitmap(b);
				}
			}
			else if(state == RequestState.CANCELED) {
				s = "cancelled";
			}
			else if(state == RequestState.FAILED) {
				s = "failed";
			}
			mTrackText.setText(s);
		}
	}
	
	private DownloadStateReceiver mDownloadStateReceiver = new DownloadStateReceiver();
}
