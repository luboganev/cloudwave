package com.luboganev.cloudwave.receivers;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.luboganev.cloudwave.service.ChangeWallpaperService;
import com.luboganev.cloudwave.service.CommunicationUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This broadcast receiver gets notified when network connectivity 
 * has changed. It is registered in the app's manifest but is not
 * enabled the whole time. It is being enabled when we want to get
 * some new sound wave image or other info from Soundcloud API, but 
 * there is currently no Internet connectivity. When the Internet 
 * connectivity is available again, it calls the execution of all 
 * the necessary work and disables itself again. 
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(CommunicationUtils.hasInternetConnectivity(context)) {
			// we have internet, have the change wallpaper service called
			Intent serviceIntent = new Intent(context, ChangeWallpaperService.class);
			WakefulIntentService.sendWakefulWork(context, serviceIntent);
			// disable this broadcastReceiver
			CommunicationUtils.setConnectivityChangeReceiverEnabled(context, false);
		}
	}
}
