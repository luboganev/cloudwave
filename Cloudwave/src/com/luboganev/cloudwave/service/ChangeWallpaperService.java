package com.luboganev.cloudwave.service;

import java.io.File;
import java.util.Random;
import android.content.Intent;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.luboganev.cloudwave.data.LocalStorage;
import com.luboganev.cloudwave.data.LocalStorageManager;
import com.luboganev.cloudwave.data.Track;

/**
 *	This IntentService manages the automatic change 
 *  of random wallpaper in the background
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
		File file = manager.generateSoundwaveFileUri(nextTrack.id);
		if(file.exists()) {
			// we already have it
			storage.currentTrack = nextTrack;
			Random r = new Random(System.currentTimeMillis());
			storage.nextRandomIndex = r.nextInt(storage.userTracks.size());
			manager.saveLocalStorageToFile();
			notifyUpdateWallpaper();
		}
		else {
			// we will need to download the soundwave from the server first
			if(CommunicationUtils.hasInternetConnectivity(getApplicationContext())) {
				if(CommunicationUtils.executeSoundwaveDownload(nextTrack.waveformUrl, file)) {
					// file downloaded successfully
					storage.currentTrack = nextTrack;
					Random r = new Random(System.currentTimeMillis());
					storage.nextRandomIndex = r.nextInt(storage.userTracks.size());
					manager.saveLocalStorageToFile();
					notifyUpdateWallpaper();
				}
			}
			else {
				// wait for internet connection
				CommunicationUtils.setConnectivityChangeReceiverEnabled(getApplicationContext(), true);
			}
		}
	}
	
	private void notifyUpdateWallpaper() {
		//TODO: signal the wallpaper that it needs to update
	}
}
