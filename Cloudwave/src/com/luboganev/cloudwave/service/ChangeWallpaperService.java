package com.luboganev.cloudwave.service;

import java.io.File;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luboganev.cloudwave.LogUtils;
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
		LogUtils.d(this, "Doing wakeful work");
		// load local storage
		LocalStorageManager manager = new LocalStorageManager(getApplicationContext());
		if(manager.hasSavedLocalStorage()) {
			LogUtils.d(this, "Loading local storage from file");
			manager.loadFromFile();
		}
		else {
			if(CommunicationUtils.hasInternetConnectivity(getApplicationContext())) {
				LogUtils.d(this, "Loading default storage");
				manager.loadDefaultStorage();
				
				String userTracksString = CommunicationUtils.executeFetchArtistTracks(manager.getArtistName());
				JsonArray jarrayTracks = new JsonParser().parse(userTracksString).getAsJsonArray();
				for (JsonElement jsonElement : jarrayTracks) {
					JsonObject track = jsonElement.getAsJsonObject();
					long id = track.get("id").getAsLong();
					String title = track.get("title").getAsString();
					String permalinkUrl = track.get("permalink_url").getAsString();
					String waveformUrl = track.get("waveform_url").getAsString();
					manager.addTrack(id, title, permalinkUrl, waveformUrl);
				}
				if(jarrayTracks.size() > 0) {
					manager.pickNewNextRandomTrack();
				}
				manager.saveToFile();
			}
			else {
				// wait for internet connection
				LogUtils.d(this, "Waiting for internet to load tracks");
				CommunicationUtils.setConnectivityChangeReceiverEnabled(getApplicationContext(), true);
				return; // we have no track so we cannot load any soundwaves anyway
			}
		}
		
		LogUtils.d(this, "Processing the soundwave");
		
		Track nextTrack = manager.getNextTrack();
		if(nextTrack == null) {
			// this means that there were no tracks for the Heed The Sound user,
			// which is currently not possible cause it is my favorite user and
			// I know there are many tracks there
		}
		
		File file = manager.generateSoundwaveFileUri(nextTrack.id);
		if(file.exists()) {
			// we already have it
			manager.setNextAsCurrentTrack();
			manager.pickNewNextRandomTrack();
			manager.saveToFile();
			notifyUpdateWallpaper();
		}
		else {
			// we will need to download the soundwave from the server first
			if(CommunicationUtils.hasInternetConnectivity(getApplicationContext())) {
				if(CommunicationUtils.executeSoundwaveDownload(nextTrack.waveformUrl, file)) {
					// file downloaded successfully
					manager.setNextAsCurrentTrack();
					manager.pickNewNextRandomTrack();
					manager.saveToFile();
					notifyUpdateWallpaper();
				}
			}
			else {
				// wait for internet connection
				LogUtils.d(this, "Waiting for internet to load track soundwave");
				CommunicationUtils.setConnectivityChangeReceiverEnabled(getApplicationContext(), true);
			}
		}
	}
	
	/** The callback intent action which clients can register for */
    public static final String INTENT_ACTION_NOTIFY_WALLPAPER_CHANGE = "com.luboganev.cloudwave.service.ChangeWallpaperService.NOTIFY_WALLPAPER_CHANGE";
	
	private void notifyUpdateWallpaper() {
		LogUtils.d(this, "Done, notifying for wallpaper change");
		Intent callbackIntent = new Intent(INTENT_ACTION_NOTIFY_WALLPAPER_CHANGE);
    	LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(callbackIntent);
	}
}
