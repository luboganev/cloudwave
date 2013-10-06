package com.luboganev.cloudwave.remote;

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
		return info.isConnected();
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
}
