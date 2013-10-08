package com.luboganev.cloudwave.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.luboganev.cloudwave.LogUtils;

/**
 * The local storage manager is responsible for reading and writing all locally cached data
 */
public class LocalStorageManager {
	private static final String LOCAL_STORAGE_FILE_NAME="soundwave_storage_json.txt";
	private LocalStorage mLocalStorage;
	private final Gson mGson;
	private final Context mApplicationContext;
	
	private static final String SOUNDWAVE_FILE_PREFIX = "soundwave_";
	private static final String SOUNDWAVE_FILE_SUFFIX = ".png";
	private static final String SOUNDWAVE_FILES_DIR = "soundwaves";
	
	public LocalStorageManager(Context applicationContext) {
		mApplicationContext = applicationContext;
		mGson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}
	
	/**
	 * Checks for existing persisted local storage
	 * 
	 * @return
	 * 		Persisted local storage exists
	 */
	public boolean hasSavedLocalStorage() {
		File storageFile = new File(mApplicationContext.getFilesDir(), LOCAL_STORAGE_FILE_NAME);
		return storageFile.exists();
	}

	/**
	 * @return
	 * 		Loads a previously saved local storage from disk
	 */
	public boolean loadFromFile() {
		FileInputStream fis = null;
		try {
			fis = mApplicationContext.openFileInput(LOCAL_STORAGE_FILE_NAME);
			String jsonString = readStreamAsString(fis);
			mLocalStorage = mGson.fromJson(jsonString, LocalStorage.class);
			return true;
		} catch (IOException x) {
			LogUtils.e(this, "Cannot read the file");
			return false;
		} finally {
			closeStreamSilently(fis);
		}
	}
	
	/**
	 * Loads a default empty storage for the default artist
	 */
	public void loadDefaultStorage() {
		mLocalStorage = new LocalStorage();
		mLocalStorage.artistUsername = "heedthesound";
		mLocalStorage.artistTracks = new ArrayList<Track>();
		mLocalStorage.currentTrackIndex = -1;
		mLocalStorage.nextRandomIndex = -1;
	}
	
	/**
	 * Adds a new track object to the list of tracks
	 * 
	 * @param id
	 * 		Track id
	 * @param title
	 * 		Track title
	 * @param permalinkUrl
	 * 		Track permalink url
	 * @param waveformUrl
	 * 		Track waveform url
	 */
	public void addTrack(long id, String title, String permalinkUrl, String waveformUrl) {
		Track newTrack = new Track();
		newTrack.id = id;
		newTrack.title = title;
		newTrack.permalinkUrl = permalinkUrl;
		newTrack.waveformUrl = waveformUrl;
		mLocalStorage.artistTracks.add(newTrack);
	}
	
	/**
	 * Picks a new random index from the tracks list as next track index
	 */
	public void pickNewNextRandomTrack() {
		Random r = new Random(System.currentTimeMillis());
		mLocalStorage.nextRandomIndex = r.nextInt(mLocalStorage.artistTracks.size());
	}
	
	/**
	 * @return
	 * 		The next track
	 */
	public Track getNextTrack() {
		if(mLocalStorage.nextRandomIndex >= 0)
			return mLocalStorage.artistTracks.get(mLocalStorage.nextRandomIndex);
		else return null;
	}
	
	/**
	 * @return
	 * 		The current track
	 */
	public Track getCurrentTrack() {
		return mLocalStorage.artistTracks.get(mLocalStorage.currentTrackIndex);
	}
	
	/**
	 * Picks the next track as the current track
	 */
	public void setNextAsCurrentTrack() {
		mLocalStorage.currentTrackIndex = mLocalStorage.nextRandomIndex;
	}
	
	/**
	 * @return
	 * 		String containing the artist's name
	 */
	public String getArtistName() {
		return mLocalStorage.artistUsername;
	}
	
	/**
	 * Persists the currently loaded {@link LocalStorage}
	 */
	public void saveToFile() {
		String json = mGson.toJson(mLocalStorage);
		FileOutputStream fos = null;
		try {
			fos = mApplicationContext.openFileOutput(LOCAL_STORAGE_FILE_NAME, Context.MODE_PRIVATE);
			fos.write(json.getBytes());
		} catch (IOException x) {
			LogUtils.e(this, "Cannot create or write to file");
		} finally {
			closeStreamSilently(fos);
		}
	}

	/**
	 * Helper method which reads all inputstream contents 
	 * and writes them to an outputstream
	 */
	private void copy(InputStream reader, OutputStream writer)
			throws IOException {
		byte byteArray[] = new byte[4092];
		while (true) {
			int numOfBytesRead = reader.read(byteArray, 0, 4092);
			if (numOfBytesRead == -1) {
				break;
			}
			// else
			writer.write(byteArray, 0, numOfBytesRead);
		}
		return;
	}

	/**
	 * Helper method which reads all inputstream contents as string
	 */
	private String readStreamAsString(InputStream is)
			throws FileNotFoundException, IOException {
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			copy(is, baos);
			return baos.toString();
		} finally {
			if (baos != null)
				closeStreamSilently(baos);
		}
	}

	/**
	 * Helper method that closes OutputStreams
	 */
	private void closeStreamSilently(OutputStream os) {
		if (os == null)
			return;
		// os is not null
		try {
			os.close();
		} catch (IOException x) {
			throw new RuntimeException(
					"This shouldn't happen. exception closing a file", x);
		}
	}

	/**
	 * Helper method that closes InputStreams
	 */
	private void closeStreamSilently(InputStream os) {
		if (os == null)
			return;
		// os is not null
		try {
			os.close();
		} catch (IOException x) {
			throw new RuntimeException(
					"This shouldn't happen. exception closing a file", x);
		}
	}
	
	/**
	 * Gets a file of the local soundwave image of a particular track
	 * 
	 * @param trackId
	 * 		The id of the track
	 * @return
	 * 		A File object with proper name
	 */
	public File generateSoundwaveFileUri(long trackId) {
		File soundwaveDir = mApplicationContext.getDir(SOUNDWAVE_FILES_DIR, Context.MODE_PRIVATE);
		File trackSoundwave = new File(soundwaveDir, SOUNDWAVE_FILE_PREFIX + trackId + SOUNDWAVE_FILE_SUFFIX);
		return trackSoundwave;
	}
}
