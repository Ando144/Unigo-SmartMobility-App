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

public class NotificacionClimaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        obtenerDatosClimaYLanzarNotificacion(context);
    }

    private void obtenerDatosClimaYLanzarNotificacion(final Context context) {
        // Coordenadas de Bilbao (puedes ajustarlo)
        double lat = 43.26;
        double lon = -2.94;

        // API ampliada: Pedimos también windspeed_10m_max (viento) y weathercode (estado general)
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&current_weather=true" +
                "&daily=precipitation_sum,uv_index_max,windspeed_10m_max,weathercode" +
                "&timezone=auto";

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Extraemos el clima actual
                            JSONObject current = response.getJSONObject("current_weather");
                            double tempActual = current.getDouble("temperature");

                            // Extraemos la previsión del día
                            JSONObject daily = response.getJSONObject("daily");
                            double lluviaTotal = daily.getJSONArray("precipitation_sum").optDouble(0, 0.0);
                            double uvMax = daily.getJSONArray("uv_index_max").optDouble(0, 0.0);
                            double vientoMax = daily.getJSONArray("windspeed_10m_max").optDouble(0, 0.0);
                            int codigoClima = daily.getJSONArray("weathercode").optInt(0, 0);

                            // Empezamos a construir el mensaje
                            String titulo = "Clima en Bilbao (" + Math.round(tempActual) + "ºC)";
                            StringBuilder mensaje = new StringBuilder();

                            // 1. Análisis de Temperatura
                            if (tempActual <= 10) {
                                mensaje.append("Hace frío, ¡abrígate bien! ");
                            } else if (tempActual >= 25) {
                                mensaje.append("Día caluroso. ");
                            }

                            // 2. Análisis del Código WMO (Eventos Especiales) y Lluvia
                            if (codigoClima == 0) {
                                mensaje.append("Cielo despejado. ");
                            } else if (codigoClima == 45 || codigoClima == 48) {
                                mensaje.append("¡Ojo! Hay niebla, precaución si vas en coche o bici. ");
                            } else if (codigoClima >= 71 && codigoClima <= 77) {
                                mensaje.append("¡Atención! Riesgo de nevadas. ");
                            } else if (codigoClima >= 95) {
                                mensaje.append("Alerta por tormentas eléctricas. ");
                            } else if (lluviaTotal > 15.0) {
                                mensaje.append("Lluvia fuerte hoy (").append(lluviaTotal).append("mm). Imprescindible paraguas. ");
                            } else if (lluviaTotal > 1.0) {
                                mensaje.append("Se esperan algunos chubascos (").append(lluviaTotal).append("mm). ");
                            } else {
                                mensaje.append("No se esperan lluvias. ");
                            }

                            // 3. Análisis de Viento
                            if (vientoMax > 40.0) {
                                mensaje.append("Precaución: Rachas de viento de ").append(Math.round(vientoMax)).append(" km/h. ");
                            }

                            // 4. Análisis de UV
                            if (uvMax >= 6) {
                                mensaje.append("Protección solar necesaria (UV: ").append(Math.round(uvMax)).append(").");
                            }

                            // Lanzamos la notificación generada
                            lanzarNotificacionFinal(context, titulo, mensaje.toString());

                        } catch (Exception e) {
                            lanzarNotificacionFinal(context, "UniGo: Clima del Campus", "Abre la app para consultar la previsión detallada antes de salir.");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        lanzarNotificacionFinal(context, "UniGo: Clima del Campus", "Conéctate a internet para ver el pronóstico de hoy.");
                    }
                }
        );

        requestQueue.add(peticion);
    }

    private void lanzarNotificacionFinal(Context context, String titulo, String texto) {
        NotificationManager elManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String idCanal = "canal_clima";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel elCanal = new NotificationChannel(idCanal, "Alertas de Clima", NotificationManager.IMPORTANCE_HIGH);
            elCanal.setDescription("Avisos diarios sobre el entorno");
            elManager.createNotificationChannel(elCanal);
        }

        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(context, idCanal)
                .setSmallIcon(R.drawable.ic_clima_sol) // Cambia este icono por uno que tengas, ej: android.R.drawable.ic_dialog_info
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true);

        elManager.notify(1001, elBuilder.build());
    }
}