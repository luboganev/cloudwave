package com.luboganev.cloudwave.remote;

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
			//TODO: call service to do some useful work
			
			// disable this broadcastReceiver
			CommunicationUtils.setConnectivityChangeReceiverEnabled(context, false);
		}
	}
}
