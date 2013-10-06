
package com.luboganev.cloudwave.remote;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.luboganev.cloudwave.LogUtils;

/**
 * This class represents a service which fetches the tracks 
 * by a particular Soundcloud user via the Java SoundCloudSDK and 
 * download Soundcloud sound wave images. It receives commands
 * via Intents and provides callback about their status via 
 * LocalBroadcast of specific intents. It also allows the 
 * cancellation of currently running requests.
 */
public class SoundCloudApiService extends IntentService {
	public SoundCloudApiService() {
		super("SoundwaveDownloader");
		mRequestState = RequestState.NONE;
		mRequestType = RequestType.NONE;
	}
	
	/**
	 * The states of the current request can be one of the following:
	 * <ul>
     * 	<li>{@link RequestState#NONE NONE}: The initial state of the request</li>
     * 	<li>{@link RequestState#RUNNING RUNNING}: The request is currently running</li>
     * 	<li>{@link RequestState#CANCELED CANCELED}: The request was explicitly canceled</li>
     * 	<li>{@link RequestState#FAILED FAILED}: The request failed due to error</li>
     * 	<li>{@link RequestState#COMPLETED COMPLETED}: The request was completed</li>
     * </ul>
	 */
	public static enum RequestState {NONE, RUNNING, CANCELED, FAILED, COMPLETED};
	
	/**
	 * The type of the current request:
	 * <ul>
	 * 	<li>{@link RequestType#NONE NONE}: A default value for no request</li>
	 * 	<li>{@link RequestType#ARTIST ARTIST}: This request fetches the tracks of a particular artist</li>
	 * 	<li>{@link RequestType#SOUNDWAVE SOUNDWAVE}: This request downloads the sound wave image of a particular track</li>
	 * </ul>
	 */
	public static enum RequestType {NONE, ARTIST, SOUNDWAVE};
    
    /**
     * The current state of the request. 
     */
    private RequestState mRequestState;
    
    /**
     * The current request type. 
     */
    private RequestType mRequestType;
    
    /**
     * Cancels the current request and sends
     * callback.
     * 
     * @return
     * 		true if the current request was cancelled.
     * 		false if there was no running request
     */
    private synchronized boolean cancelRequest() {
    	if(mRequestState == RequestState.RUNNING) {
    		mRequestState = RequestState.CANCELED;
    		sendRequestStateCallback();
    		mRequestType = RequestType.NONE; // important to set if after we send the callback
    		return true;
    	}
    	else return false;
    }
    
    /**
     * Sends a callback local broadcast with the current internal
     * value of the request status and request type.
     */
    private void sendRequestStateCallback() {
    	sendRequestStateCallback(null);
    }
    
    /**
     * Sends a callback local broadcast with the current internal
     * value of the request status, request type and the payload string
     * 
     * @param payload
     * 		A string containing the callback extra of the request
     */
    private void sendRequestStateCallback(String payload) {
    	Intent callbackIntent = new Intent(CALLBACK_INTENT_ACTION);
    	callbackIntent.putExtra(CALLBACK_EXTRA_REQUEST_STATE, mRequestState);
    	callbackIntent.putExtra(CALLBACK_EXTRA_REQUEST_TYPE, mRequestType);
    	if(payload != null) {
    		callbackIntent.putExtra(CALLBACK_EXTRA_PAYLOAD, payload);
    	}
    	LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(callbackIntent);
    }
    
    /**
     * Starts a new request if there is no
     * currently running request in progress
     * 
     * @return
     * 		True if request is started. False if there is a 
     * 		currently running request.
     */
    private synchronized boolean startRequest(RequestType requestType) {
    	if(mRequestState != RequestState.RUNNING) {
    		mRequestState = RequestState.RUNNING;
    		mRequestType = requestType; // important to set if before we send the callback
    		sendRequestStateCallback();
			return true;
    	}
    	else return false;
    }
    
    // Callback extras
    /** The callback intent action which clients can register for */
    public static final String CALLBACK_INTENT_ACTION = "com.luboganev.cloudwave.remote.SoundCloudApiService.CALLBACK_ACTION";
    /** The callback extra containing the state of the running request */
	public static final String CALLBACK_EXTRA_REQUEST_STATE = "request_state";
	/** The callback extra containing the state of the running request */
	public static final String CALLBACK_EXTRA_REQUEST_TYPE = "request_type";
	/** The callback extra containing the request result */
	public static final String CALLBACK_EXTRA_PAYLOAD = "payload";
	
	// Command extras
	/** The input extra containing a start request command */
	public static final String INPUT_EXTRA_START_REQUEST = "start_request";
	/** The input extra containing a cancel request command */
	public static final String INPUT_EXTRA_CANCEL_REQUEST = "cancel_request";
	/** The input extra containing the type of request */
	public static final String INPUT_EXTRA_REQUEST_TYPE = "request_type";
	
	// Command parameters extras
	/** The input extra containing the Uri of the local downloaded sound wave file */
	public static final String INPUT_INTENT_EXTRA_LOCAL_URI = "local_uri";
	/** The input extra containing the Url of the remote sound wave file */
	public static final String INPUT_INTENT_EXTRA_SERVER_URI = "server_uri";
	/** The callback extra containing the name of the artist whose tracks are to be fetched */
	public static final String INPUT_INTENT_EXTRA_ARTIST_NAME = "artist_name";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent.hasExtra(INPUT_EXTRA_START_REQUEST)) {
			if(!intent.hasExtra(INPUT_EXTRA_REQUEST_TYPE)) 
				throw new IllegalArgumentException("A start request intent has to contain a request type extra");
			RequestType type = (RequestType)intent.getSerializableExtra(INPUT_EXTRA_REQUEST_TYPE);
			if(startRequest(type)) {
				// use the scheduling functionality of IntentService only when
				// starting a new request
				return super.onStartCommand(intent, flags, startId);
			}
			else return START_NOT_STICKY;
		}
		else if(intent.hasExtra(INPUT_EXTRA_CANCEL_REQUEST)) {
			cancelRequest();
			return START_NOT_STICKY;
		}
		else throw new IllegalArgumentException("Intent has to contain either start or cancel request extra");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if(mRequestState == RequestState.RUNNING) {
			// Execute the transfer only if it was not cancelled before even starting it
			if(mRequestType == RequestType.SOUNDWAVE) {
				executeSoundwaveDownload(intent);
			}
			else if(mRequestType == RequestType.ARTIST) {
				executeFetchArtistTracks(intent);
			}
		}
	}
	
	/**
	 * This method implements the functionality of downloading a soundwave file
	 * for requests of type {@link RequestType#SOUNDWAVE SOUNDWAVE}
	 * 
	 * @param intent
	 * 		The starting intent containing the necessary params
	 */
	private boolean executeSoundwaveDownload(Intent intent) {
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
            	if(mRequestState == RequestState.CANCELED) {
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
            mRequestState = RequestState.COMPLETED;
            sendRequestStateCallback(Uri.fromFile(localFile).toString());
            return true;
		} catch (FileNotFoundException e) {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e1) {
					// should not happen
				}
			}
            mRequestState = RequestState.FAILED;
            sendRequestStateCallback();
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
    		mRequestState = RequestState.FAILED;
            sendRequestStateCallback();
            return false;
        }
	}
	
	/**
	 * This method implements the functionality of fetching artist's tracks
	 * for requests of type {@link RequestType#ARTIST ARTIST}
	 * 
	 * @param intent
	 * 		The starting intent containing the necessary params
	 */
	private boolean executeFetchArtistTracks(Intent intent) {
		String username = "";
	    try {
			username = URLEncoder.encode(intent.getStringExtra(INPUT_INTENT_EXTRA_ARTIST_NAME), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// this should not happen
			mRequestState = RequestState.FAILED;
			sendRequestStateCallback();
			return false;
		}
		String requestUrl = "https://api.soundcloud.com/users/"+username+"/tracks.json?consumer_key=f15a8f33d2b9a4eb9ab8e3f96f8baa35";
		try {
			HttpGet getRequest = new HttpGet(requestUrl);
			HttpClient client = new DefaultHttpClient();
			HttpResponse resp = client.execute(getRequest);
			
			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;) {
				    builder.append(line).append("\n");
				}
				// completed, send completed callback and return
				mRequestState = RequestState.COMPLETED;
				sendRequestStateCallback(builder.toString());
			    return true;
			} else {
				LogUtils.e(this, resp.getStatusLine().toString());
				mRequestState = RequestState.FAILED;
				sendRequestStateCallback();
				return false;
			}
		} catch (IOException e) {
			mRequestState = RequestState.FAILED;
			sendRequestStateCallback();
			return false;
		}
        
	}
}
