package com.luboganev.cloudwave.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * This receiver gets called on boot and sets up the
 * alarms for auto change of wallaper on regular basis
 */
public class BootReceiver extends BroadcastReceiver {
  
  @Override
  public void onReceive(Context context, Intent intent) {
    AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    Intent i = new Intent(context, AlarmReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
    
    //TODO: read the default change wallpaper interval time from settings
    
//    mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                      SystemClock.elapsedRealtime()+60000,
//                      PERIOD,
//                      pi);
  }
}