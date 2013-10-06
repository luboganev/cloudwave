
package com.luboganev.cloudwave.remote;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * This class represents a service which can download 
 * Soundcloud sound wave images. It can receive download requests 
 * via Intent and provide callback about the status of the 
 * download via LocalBroadcast of specific intents. It also allows
 * the cancellation of currently running download.
 */
public class SoundwaveDownloader extends IntentService {
	public SoundwaveDownloader() {
		super("SoundwaveDownloader");
		mDownloadState = DownloadState.NONE;
	}
	
	/**
	 * The states of the current download can be one of the following:
	 * <ul>
     * 	<li>{@link DownloadState#NONE NONE}: The initial state of the download</li>
     * 	<li>{@link DownloadState#RUNNING RUNNING}: The download is currently running</li>
     * 	<li>{@link DownloadState#CANCELED CANCELED}: The download was explicitly canceled</li>
     * 	<li>{@link DownloadState#FAILED FAILED}: The download failed due to error</li>
     * 	<li>{@link DownloadState#COMPLETED COMPLETED}: The download was completed</li>
     * </ul>
	 */
	public static enum DownloadState {NONE, RUNNING, CANCELED, FAILED, COMPLETED};
    
    /**
     * The current state of the download. 
     */
    private DownloadState mDownloadState;
    
    /**
     * Cancels the current download and sends
     * callback.
     * 
     * @return
     * 		true if the current download was cancelled.
     * 		false if there was no running download
     */
    private synchronized boolean cancelDownload() {
    	if(mDownloadState == DownloadState.RUNNING) {
    		mDownloadState = DownloadState.CANCELED;
    		sendDownloadStateCallback();
    		return true;
    	}
    	else return false;
    }
    
    /**
     * Sends a callback local broadcast with the current internal
     * value of the download status.
     */
    private void sendDownloadStateCallback() {
    	Intent callbackIntent = new Intent(CALLBACK_INTENT_ACTION);
		callbackIntent.putExtra(CALLBACK_EXTRA_DOWNLOAD_STATE, mDownloadState);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(callbackIntent);
    }
    
    /**
     * Starts a new download if there is no
     * currently running download in progress
     * 
     * @return
     * 		True if download is started. False if there is a 
     * 		currently running download.
     */
    private synchronized boolean startDownload() {
    	if(mDownloadState != DownloadState.RUNNING) {
    		mDownloadState = DownloadState.RUNNING;
    		sendDownloadStateCallback();
			return true;
    	}
    	else return false;
    }
    
    private static final String PACKAGE_NAME_PREFIX = "com.luboganev.cloudwave.remote.SoundwaveDownloader.";
    public static final String CALLBACK_INTENT_ACTION = PACKAGE_NAME_PREFIX + "callback";
	public static final String CALLBACK_EXTRA_DOWNLOAD_STATE = PACKAGE_NAME_PREFIX + "download_state";
	
	public static final String INPUT_EXTRA_START_DOWNLOAD = PACKAGE_NAME_PREFIX + "start_download";
	public static final String INPUT_EXTRA_CANCEL_DOWNLOAD = PACKAGE_NAME_PREFIX + "cancel_download";
	public static final String INPUT_INTENT_EXTRA_LOCAL_URI = PACKAGE_NAME_PREFIX + "local_uri";
	public static final String INPUT_INTENT_EXTRA_SERVER_URI = PACKAGE_NAME_PREFIX + "server_uri";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent.hasExtra(INPUT_EXTRA_START_DOWNLOAD)) {
			if(startDownload()) {
				// use the scheduling functionality of IntentService only when
				// starting a new download
				return super.onStartCommand(intent, flags, startId);
			}
			else return START_NOT_STICKY;
		}
		else if(intent.hasExtra(INPUT_EXTRA_CANCEL_DOWNLOAD)) {
			cancelDownload();
			return START_NOT_STICKY;
		}
		else throw new IllegalArgumentException("Intent has to contain either 'start_download' or 'cancel_download'");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if(mDownloadState == DownloadState.RUNNING) {
			// Execute the transfer only if it was not cancelled before even starting it
			executeTransfer(intent);
		}
	}
	
	/**
	 * This method should be overridden instead of {@link #onHandleIntent(Intent)} for implementing the transfers
	 * 
	 * @param intent
	 */
	private boolean executeTransfer(Intent intent) {
		String serverUri = intent.getStringExtra(INPUT_INTENT_EXTRA_SERVER_URI);
		String localFileUri = intent.getStringExtra(INPUT_INTENT_EXTRA_LOCAL_URI);
		File localFile = new File(Uri.parse(localFileUri).getPath());
		
		InputStream input = null;
		OutputStream output = null;
		try {
            URL url = new URL(serverUri);
            URLConnection connection = url.openConnection();
            connection.connect();
            // this will be useful so in order to log 0-100% progress
            int fileLength = connection.getContentLength();

            // download the file
            input = new BufferedInputStream(url.openStream());
            output = new FileOutputStream(localFile);

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
            	
            	// Check if download was cancelled
            	if(mDownloadState == DownloadState.CANCELED) {
            		output.flush();
                    output.close();
                    input.close();
                    localFile.delete();
                    return true; // explicit cancel is not an error so we return true
            	}
            	
                total += count;
                // publishing the progress....
                int currentProgress = (int) (total * 100 / fileLength);
                Log.d("ProfilePicture", currentProgress + "%"); //TODO: fix messages
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
            
            // completed, send completed callback and return
            mDownloadState = DownloadState.COMPLETED;
            sendDownloadStateCallback();
            return true;
		} catch (FileNotFoundException e) {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e1) {
					// should not happen
				}
			}
            mDownloadState = DownloadState.FAILED;
            sendDownloadStateCallback();
            return false;
        } catch (IOException e) {
        	if(input != null) {
        		try {
					input.close();
				} catch (IOException e1) {
					// should not happen
				}
        	}
        	if(output != null) {
        		try {
					output.flush();
					output.close();
				} catch (IOException e1) {
					// should not happen
				}
        	}
        	localFile.delete();
    		mDownloadState = DownloadState.FAILED;
            sendDownloadStateCallback();
            return false;
        }
	}
}
