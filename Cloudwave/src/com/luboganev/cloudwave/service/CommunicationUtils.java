package com.luboganev.cloudwave.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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

import com.luboganev.cloudwave.receivers.ConnectivityChangeReceiver;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CommunicationUtils {
	/**
	 * Checks if there is an usable Internet connection
	 * 
	 * @param applicationContext
	 * 		Needs the context to get the ConnectivityManager
	 * @return
	 * 		true if there is an active Internet connection.
	 * 		false otherwise 
	 */
	public static boolean hasInternetConnectivity(Context applicationContext) {
		ConnectivityManager connManager = (ConnectivityManager)applicationContext.
				   getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if(info != null) return info.isConnected();
		else return false;
	}
	
	/**
	 * This method enables or disables the {@link ConnectivityChangeReceiver}
	 * 
	 * @param applicationContext
	 * 		Needed to get the PackageManager and Component
	 * @param enabled
	 * 		If it should be enabled
	 */
	public static void setConnectivityChangeReceiverEnabled(Context applicationContext, boolean enabled) {
		ComponentName receiver = new ComponentName(applicationContext, ConnectivityChangeReceiver.class);
		PackageManager pm = applicationContext.getPackageManager();
		pm.setComponentEnabledSetting(receiver,
				enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
		        PackageManager.DONT_KILL_APP);
	}
	
	/**
	 * This method implements the functionality of downloading a soundwave file
	 * for requests of type {@link RequestType#SOUNDWAVE SOUNDWAVE}
	 * 
	 * @param intent
	 * 		The starting intent containing the necessary params
	 */
	public static boolean executeSoundwaveDownload(String serverUrl, File localFile) {
		InputStream input = null;
		try {
			localFile.createNewFile(); // create the file
			HttpURLConnection conn = requestGet(serverUrl);
			conn.connect();
			if(conn.getResponseCode() == HttpStatus.SC_OK) {
				input = new BufferedInputStream(conn.getInputStream());
				readToFile(input, localFile);
				input.close();
			    return true;
			}
			else {
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
            return false;
        } catch (IOException e) {
        	if(input != null) {
        		try {
					input.close();
				} catch (IOException e1) {
					// should not happen
				}
        	}
        	if(localFile.exists()) localFile.delete(); // we need to delete the local file on error
            return false;
        }
	}
	
	/**
	 * This method implements the functionality of fetching artist's tracks
	 * for requested artist name
	 * 
	 * @param artistName
	 * 		The artist name
	 * @return
	 * 		The string with the JSON contents
	 */
	public static String executeFetchArtistTracks(String artistName) {
		String username = "";
	    try {
			username = URLEncoder.encode(artistName, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			return null;
		}
		String requestUrl = "https://api.soundcloud.com/users/"+username+"/tracks.json?consumer_key=f15a8f33d2b9a4eb9ab8e3f96f8baa35";
		InputStream responseStream = null;
		try {
			HttpURLConnection conn = requestGet(requestUrl);
			conn.connect();
			if(conn.getResponseCode() == HttpStatus.SC_OK) {
				responseStream = conn.getInputStream();
				String response = readIt(responseStream);
				responseStream.close();
				return response;
			}
			else {
				return null;
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
			return null;
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
	private static HttpURLConnection requestGet(String url) throws IOException {
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
	 * @return
	 * 		The read string
	 * @throws IOException
	 * 		If reading fails
	 */
	private static String readIt(InputStream stream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        return sb.toString();
	}
	
	/**
	 * Reads the whole InputStream content into a local file
	 * 
	 * @param in
	 *          The InputStream
	 *  @param localFile
	 *  		The local file
	 * @throws IOException
	 *             If reading or writing fails
	 */
	private static void readToFile(InputStream in, File localFile) throws IOException, FileNotFoundException {
		OutputStream output = null;
		try {
			output = new FileOutputStream(localFile);
			// download the file
			in = new BufferedInputStream(in);
			byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data)) != -1) {
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
