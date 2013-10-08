package com.luboganev.cloudwave.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.luboganev.cloudwave.LogUtils;
import com.luboganev.cloudwave.service.ChangeWallpaperService;

/**
 * This receiver gets called on regular basis and starts the
 * change wallpaper service in a wakeful manner
 */
public class AlarmReceiver extends BroadcastReceiver {
	public static final String INTENT_ACTION = "com.luboganev.cloudwave.receivers.ALARM";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		LogUtils.d(this, "Received change wallpaper alarm");
		Intent serviceIntent = new Intent(context, ChangeWallpaperService.class);
		WakefulIntentService.sendWakefulWork(context, serviceIntent);
	}
	
	/**
	 * Generates the pending intent broadcasted by the alarm
	 * 
	 * @param context
	 * 		Context needed to build intents
	 * @return
	 */
	private static PendingIntent getAlarmIntent(Context context) {
		Intent i = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, i, 0);
	}
	
	/**
	 * Sets up a new repreating alarm for starting the change wallpaper service
	 * 
	 * @param context
	 * 		Used to get the alarm manager
	 */
	public static void setAlarm(Context context) {
		LogUtils.d("AlarmReceiver", "Setting up an alarm");
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        // alarm does not have to be exact, e.g. nobody sees the wallpaper 
        // when device is sleeping, so no point waking up
		// does not also need to be that exact
        mgr.setInexactRepeating(AlarmManager.RTC,
                          System.currentTimeMillis(),
                          60 * 60 * 1000, // 1 hour
                          getAlarmIntent(context));
	}
	
	/**
	 * Cancels the repreating alarm for starting the change wallpaper service
	 * 
	 * @param context
	 * 		Used to get the alarm manager
	 */
	public static void cancelAlarm(Context context) {
		LogUtils.d("AlarmReceiver", "Cancelling the alarm");
		AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(getAlarmIntent(context));
	}
}