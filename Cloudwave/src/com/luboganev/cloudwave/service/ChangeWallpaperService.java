package com.luboganev.cloudwave.service;


import java.util.Random;

import android.content.Intent;
import android.net.Uri;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.luboganev.cloudwave.data.LocalStorage;
import com.luboganev.cloudwave.data.LocalStorageManager;
import com.luboganev.cloudwave.data.Track;

/**
 *	
 */
public class ChangeWallpaperService extends WakefulIntentService {
	public ChangeWallpaperService() {
		super("ChangeWallpaperIntentService");
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		// load local storage
		LocalStorageManager manager = new LocalStorageManager(getApplicationContext());
		LocalStorage storage = manager.getLocalStorage();
		Track nextTrack = storage.userTracks.get(storage.nextRandomIndex);
		//TODO: check if this track's soundwave is locally available
		if(true) {
			storage.currentTrack = nextTrack;
			Random r = new Random(System.currentTimeMillis());
			storage.nextRandomIndex = r.nextInt(storage.userTracks.size());
			manager.saveLocalStorageToFile();
			//TODO: signal the wallpaper that it needs to change
		}
		else {
			// we will need to download the soundwave from the server
			if(CommunicationUtils.hasInternetConnectivity(getApplicationContext())) {
				//TODO: get a local file uri
				Uri localFileUri = null;
				if(CommunicationUtils.executeSoundwaveDownload(nextTrack.waveformUrl, localFileUri.toString())) {
					// file downloaded successfully
					storage.currentTrack = nextTrack;
					Random r = new Random(System.currentTimeMillis());
					storage.nextRandomIndex = r.nextInt(storage.userTracks.size());
					manager.saveLocalStorageToFile();
					//TODO: signal the wallpaper that it needs to change
				}
			}
			else {
				// wait for internet connection
				CommunicationUtils.setConnectivityChangeReceiverEnabled(getApplicationContext(), true);
			}
		}
	}
}
