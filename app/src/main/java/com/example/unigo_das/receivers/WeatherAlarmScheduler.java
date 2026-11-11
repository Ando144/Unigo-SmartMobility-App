package com.example.unigo_das.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import java.util.Calendar;

/**
 * Schedules and cancels the daily weather notification.
 *
 * <p>Fix F2: {@code setExactAndAllowWhileIdle} fires only once, so the alarm is
 * scheduled again for the next day every time it fires (see
 * {@link WeatherNotificationReceiver}) and after the phone restarts. Before,
 * the "daily" notification was shown only once.</p>
 *
 * <p>The scheduling code is the same as before; it was moved here from
 * {@code SettingsFragment} so the receiver can use it too.</p>
 */
public final class WeatherAlarmScheduler {

    /** Key of the notifications switch in the default SharedPreferences (unchanged). */
    public static final String PREF_NOTIFICATIONS_ENABLED = "pref_notificaciones_clima";
    /** Key of the notification time ("HH:mm") in the default SharedPreferences (unchanged). */
    public static final String PREF_NOTIFICATION_TIME = "pref_hora_notificacion";
    public static final String DEFAULT_NOTIFICATION_TIME = "07:30";

    private WeatherAlarmScheduler() {
    }

    /** Schedules the next notification at the given time (today, or tomorrow if it has passed). */
    public static void scheduleDailyAlarm(Context context, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the chosen time has already passed today, add 1 day to the calendar.
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = buildPendingIntent(context);

        // 'setExactAndAllowWhileIdle' makes sure the alarm fires even if the phone is idle.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    /** Schedules the next notification with the time saved in the settings, if notifications are on. */
    public static void rescheduleFromSettings(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false)) {
            return;
        }
        String time = prefs.getString(PREF_NOTIFICATION_TIME, DEFAULT_NOTIFICATION_TIME);
        try {
            String[] parts = time.split(":");
            scheduleDailyAlarm(context, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (RuntimeException e) {
            String[] parts = DEFAULT_NOTIFICATION_TIME.split(":");
            scheduleDailyAlarm(context, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    /** Cancels the scheduled notification. */
    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // When cancelled, the operating system unlinks that PendingIntent.
        alarmManager.cancel(buildPendingIntent(context));
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, WeatherNotificationReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, 1, intent, flags);
    }
}
