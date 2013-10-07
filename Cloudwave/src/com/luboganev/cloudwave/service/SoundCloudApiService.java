
package com.luboganev.cloudwave.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.http.HttpStatus;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
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
	public static final String INPUT_INTENT_EXTRA_SERVER_URL = "server_url";
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
		stopSelf();
	}
	
	/**
	 * This method implements the functionality of downloading a soundwave file
	 * for requests of type {@link RequestType#SOUNDWAVE SOUNDWAVE}
	 * 
	 * @param intent
	 * 		The starting intent containing the necessary params
	 */
	private boolean executeSoundwaveDownload(Intent intent) {
		String serverUrl = intent.getStringExtra(INPUT_INTENT_EXTRA_SERVER_URL);
		String localFileUri = intent.getStringExtra(INPUT_INTENT_EXTRA_LOCAL_URI);
		File localFile = new File(Uri.parse(localFileUri).getPath());
		
		InputStream input = null;
		try {
			HttpURLConnection conn = requestGet(serverUrl);
			conn.connect();
			if(conn.getResponseCode() == HttpStatus.SC_OK) {
				input = new BufferedInputStream(conn.getInputStream());
				readToFile(input, conn.getContentLength(), localFile);
				input.close();
				// completed, send completed callback and return
				mRequestState = RequestState.COMPLETED;
				sendRequestStateCallback(Uri.fromFile(localFile).toString());
			    return true;
			}
			else {
				LogUtils.e(this, "HTTP Status " + conn.getResponseCode());
				mRequestState = RequestState.FAILED;
				sendRequestStateCallback();
				return false;
			}
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
        	localFile.delete(); // we need to delete the local file on error
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
		InputStream responseStream = null;
		try {
			HttpURLConnection conn = requestGet(requestUrl);
			conn.connect();
			if(conn.getResponseCode() == HttpStatus.SC_OK) {
				responseStream = conn.getInputStream();
				String response = readIt(responseStream, conn.getContentLength());
				responseStream.close();
				// completed, send completed callback and return
				mRequestState = RequestState.COMPLETED;
				sendRequestStateCallback(response);
			    return true;
			}
			else {
				LogUtils.e(this, "HTTP Status " + conn.getResponseCode());
				mRequestState = RequestState.FAILED;
				sendRequestStateCallback();
				return false;
			}
		}
		catch (IOException e) {
			if(responseStream != null) {
				try {
					responseStream.close();
				} catch (IOException e1) {
					// this should never happen
				}
			}
			mRequestState = RequestState.FAILED;
			sendRequestStateCallback();
			return false;
		}
	}
	
	/**
	 * Sets up and opens a new HttpURLConnection to perform a HTTP GET request. 
	 * One can then directly call the {@link HttpURLConnection#connect()} and
	 * read the response code and read the response through its InputStream.
	 * 
	 * @param url
	 * 		The url of the request
	 * @return
	 * 		The setup HttpURLConnection object
	 * @throws IOException
	 * 		If something went wrong during establishing connection
	 */
	private HttpURLConnection requestGet(String url) throws IOException {
        URL getUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) getUrl.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        return conn;
	}
	
	/**
	 * Reads an InputStream as an UTF-8 string
	 * 
	 * @param stream
	 * 		The InputStream
	 * @param len
	 * 		The length of the data to be read from the InputStream
	 * @return
	 * 		The read string
	 * @throws IOException
	 * 		If reading fails
	 */
	public String readIt(InputStream stream, int len) throws IOException {
		Reader reader = null;
		try {
			reader = new InputStreamReader(stream, "UTF-8");        
		}
		catch(UnsupportedEncodingException e) {
			// this should not happen
			LogUtils.e(this, "How come that UTF-8 is unsupported?");
			return "";
		}
		char[] buffer = new char[len];
		reader.read(buffer);
		return new String(buffer);
	}
	
	/**
	 * Reads an InputStream into a local file
	 * 
	 * @param stream
	 *            The InputStream
	 * @param len
	 *            The length of the data to be read from the InputStream
	 * @throws IOException
	 *             If reading or writing fails
	 */
	public void readToFile(InputStream in, int length, File localFile) throws IOException, FileNotFoundException {
		OutputStream output = null;
		try {
			output = new FileOutputStream(localFile);
			// download the file
			in = new BufferedInputStream(in);
			byte data[] = new byte[1024];
			long total = 0;
			int count;
			while ((count = in.read(data)) != -1) {
	
				// Check if download was cancelled
				if (mRequestState == RequestState.CANCELED) {
					output.flush();
					output.close();
					localFile.delete(); // remove the created local file
				}
	
				total += count;
				// publishing the progress....
				int currentProgress = (int) (total * 100 / length);
				LogUtils.d(this, currentProgress + "%");
				output.write(data, 0, count);
			}
			output.flush();
			output.close();
		}
		finally {
			if(output != null) {
				output.flush(); // close the local file in any case
				output.close();
			}
		}
	}
}
