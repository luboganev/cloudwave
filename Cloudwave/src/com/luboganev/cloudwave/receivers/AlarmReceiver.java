package com.luboganev.cloudwave.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.luboganev.cloudwave.service.ChangeWallpaperService;

/**
 * This receiver gets called on regular basis and starts the
 * change wallpaper service in a wakeful manner
 */
public class AlarmReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent serviceIntent = new Intent(context, ChangeWallpaperService.class);
		WakefulIntentService.sendWakefulWork(context, serviceIntent);
	}
}