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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.unigo_das.R;

import org.json.JSONObject;

public class NotificacionClimaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // En lugar de lanzar la notificación estática de inmediato,
        // llamamos a la API para obtener los datos de Bilbao.
        obtenerDatosClimaYLanzarNotificacion(context);
    }

    private void obtenerDatosClimaYLanzarNotificacion(final Context context) {
        // Coordenadas de Bilbao (puedes cambiarlas a la ubicación guardada si la tuvieras)
        double lat = 43.26;
        double lon = -2.94;

        // Pedimos la temperatura actual y el pico de lluvia/nieve para el día
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&current_weather=true" +
                "&daily=precipitation_sum,uv_index_max" +
                "&timezone=auto";

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                response -> {
                    try {
                        JSONObject current = response.getJSONObject("current_weather");
                        double tempActual = current.getDouble("temperature");

                        JSONObject daily = response.getJSONObject("daily");
                        double lluviaTotal = daily.getJSONArray("precipitation_sum").optDouble(0, 0.0);
                        double uvMax = daily.getJSONArray("uv_index_max").optDouble(0, 0.0);

                        // Construimos el mensaje de forma inteligente
                        String titulo = "Clima en el Campus (" + Math.round(tempActual) + "ºC)";
                        StringBuilder mensaje = new StringBuilder();

                        if (lluviaTotal > 1.0) {
                            mensaje.append("Coge el paraguas, se esperan ").append(lluviaTotal).append("mm de lluvia. ");
                        } else {
                            mensaje.append("Día sin lluvias importantes. ");
                        }

                        if (uvMax >= 6) {
                            mensaje.append("¡Atención! Índice UV alto hoy (").append(Math.round(uvMax)).append(").");
                        }

                        lanzarNotificacionFinal(context, titulo, mensaje.toString());

                    } catch (Exception e) {
                        // Si falla el parseo, lanzamos una genérica para que la alarma no se pierda
                        lanzarNotificacionFinal(context, "UniGo: Clima del Campus", "Toca la app para revisar el pronóstico antes de salir.");
                    }
                },
                error -> {
                    // Si no hay internet, lanzamos la genérica
                    lanzarNotificacionFinal(context, "UniGo: Clima del Campus", "Abre la app para ver las condiciones de tu trayecto.");
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
                .setSmallIcon(R.drawable.ic_clima_sol) // O usa tu icono nativo si prefieres
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto)) // Para que quepa todo el texto
                .setAutoCancel(true);

        elManager.notify(1001, elBuilder.build());
    }
}