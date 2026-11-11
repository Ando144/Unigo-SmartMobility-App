package com.example.unigo_das.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Fix F2: Android removes all alarms when the phone restarts. This receiver
 * schedules the daily weather notification again (if it is enabled in the
 * settings), without showing a notification.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            WeatherAlarmScheduler.rescheduleFromSettings(context);
        }
    }
}
