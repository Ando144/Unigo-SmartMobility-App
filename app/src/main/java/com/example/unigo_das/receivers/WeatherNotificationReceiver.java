package com.example.unigo_das.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.unigo_das.R;

import org.json.JSONObject;

public class WeatherNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        fetchWeatherAndNotify(context);
        // Fix F2: exact alarms fire only once, so the next day's alarm is scheduled here.
        WeatherAlarmScheduler.rescheduleFromSettings(context);
    }

    private void fetchWeatherAndNotify(final Context context) {
        // Bilbao coordinates (can be adjusted)
        double lat = 43.26;
        double lon = -2.94;

        // Extended API call: also asks for windspeed_10m_max (wind) and weathercode (general state)
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&current_weather=true" +
                "&daily=precipitation_sum,uv_index_max,windspeed_10m_max,weathercode" +
                "&timezone=auto";

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        JsonObjectRequest apiRequest = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Read the current weather
                            JSONObject current = response.getJSONObject("current_weather");
                            double currentTemp = current.getDouble("temperature");

                            // Read today's forecast
                            JSONObject daily = response.getJSONObject("daily");
                            double totalRain = daily.getJSONArray("precipitation_sum").optDouble(0, 0.0);
                            double uvMax = daily.getJSONArray("uv_index_max").optDouble(0, 0.0);
                            double maxWind = daily.getJSONArray("windspeed_10m_max").optDouble(0, 0.0);
                            int weatherCode = daily.getJSONArray("weathercode").optInt(0, 0);

                            // Start building the message
                            String title = context.getString(R.string.weather_in_bilbao) + Math.round(currentTemp) + "ºC)";
                            StringBuilder message = new StringBuilder();

                            // 1. Temperature analysis
                            if (currentTemp <= 10) {
                                message.append(context.getString(R.string.cold_wrap_up));
                            } else if (currentTemp >= 25) {
                                message.append(context.getString(R.string.hot_day));
                            }

                            // 2. WMO code analysis (special events) and rain
                            if (weatherCode == 0) {
                                message.append(context.getString(R.string.clear_sky2));
                            } else if (weatherCode == 45 || weatherCode == 48) {
                                message.append(context.getString(R.string.fog_warning));
                            } else if (weatherCode >= 71 && weatherCode <= 77) {
                                message.append(context.getString(R.string.snow_risk_warning));
                            } else if (weatherCode >= 95) {
                                message.append(context.getString(R.string.thunderstorm_alert));
                            } else if (totalRain > 15.0) {
                                message.append(context.getString(R.string.heavy_rain_today)).append(totalRain).append(context.getString(R.string.mm_umbrella_needed));
                            } else if (totalRain > 1.0) {
                                message.append(context.getString(R.string.some_showers_expected)).append(totalRain).append("mm). ");
                            } else {
                                message.append(context.getString(R.string.no_rain_expected));
                            }

                            // 3. Wind analysis
                            if (maxWind > 40.0) {
                                message.append(context.getString(R.string.wind_gusts_warning)).append(Math.round(maxWind)).append(" km/h. ");
                            }

                            // 4. UV analysis
                            if (uvMax >= 6) {
                                message.append(context.getString(R.string.sun_protection_needed_uv)).append(Math.round(uvMax)).append(").");
                            }

                            // Show the generated notification
                            showFinalNotification(context, title, message.toString());

                        } catch (Exception e) {
                            showFinalNotification(context, context.getString(R.string.unigo_campus_weather), context.getString(R.string.open_app_for_detailed_forecast));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        showFinalNotification(context, context.getString(R.string.unigo_campus_weather2), context.getString(R.string.connect_to_see_forecast));
                    }
                }
        );

        requestQueue.add(apiRequest);
    }

    private void showFinalNotification(Context context, String title, String text) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "canal_clima";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.weather_alerts), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.daily_environment_alerts));
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_weather_sun) // This icon can be replaced, e.g. android.R.drawable.ic_dialog_info
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true);

        notificationManager.notify(1001, notificationBuilder.build());
    }
}