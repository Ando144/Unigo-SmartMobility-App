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
                            String titulo = context.getString(R.string.clima_en_bilbao) + Math.round(tempActual) + "ºC)";
                            StringBuilder mensaje = new StringBuilder();

                            // 1. Análisis de Temperatura
                            if (tempActual <= 10) {
                                mensaje.append(context.getString(R.string.hace_fr_o_abr_gate_bien));
                            } else if (tempActual >= 25) {
                                mensaje.append(context.getString(R.string.d_a_caluroso));
                            }

                            // 2. Análisis del Código WMO (Eventos Especiales) y Lluvia
                            if (codigoClima == 0) {
                                mensaje.append(context.getString(R.string.cielo_despejado2));
                            } else if (codigoClima == 45 || codigoClima == 48) {
                                mensaje.append(context.getString(R.string.ojo_hay_niebla_precauci_n_si_vas_en_coche_o_bici));
                            } else if (codigoClima >= 71 && codigoClima <= 77) {
                                mensaje.append(context.getString(R.string.atenci_n_riesgo_de_nevadas));
                            } else if (codigoClima >= 95) {
                                mensaje.append(context.getString(R.string.alerta_por_tormentas_el_ctricas));
                            } else if (lluviaTotal > 15.0) {
                                mensaje.append(context.getString(R.string.lluvia_fuerte_hoy)).append(lluviaTotal).append(context.getString(R.string.mm_imprescindible_paraguas));
                            } else if (lluviaTotal > 1.0) {
                                mensaje.append(context.getString(R.string.se_esperan_algunos_chubascos)).append(lluviaTotal).append("mm). ");
                            } else {
                                mensaje.append(context.getString(R.string.no_se_esperan_lluvias));
                            }

                            // 3. Análisis de Viento
                            if (vientoMax > 40.0) {
                                mensaje.append(context.getString(R.string.precauci_n_rachas_de_viento_de)).append(Math.round(vientoMax)).append(" km/h. ");
                            }

                            // 4. Análisis de UV
                            if (uvMax >= 6) {
                                mensaje.append(context.getString(R.string.protecci_n_solar_necesaria_uv)).append(Math.round(uvMax)).append(").");
                            }

                            // Lanzamos la notificación generada
                            lanzarNotificacionFinal(context, titulo, mensaje.toString());

                        } catch (Exception e) {
                            lanzarNotificacionFinal(context, context.getString(R.string.unigo_clima_del_campus), context.getString(R.string.abre_la_app_para_consultar_la_previsi_n_detallada_antes_de_salir));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        lanzarNotificacionFinal(context, context.getString(R.string.unigo_clima_del_campus2), context.getString(R.string.con_ctate_a_internet_para_ver_el_pron_stico_de_hoy));
                    }
                }
        );

        requestQueue.add(peticion);
    }

    private void lanzarNotificacionFinal(Context context, String titulo, String texto) {
        NotificationManager elManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String idCanal = "canal_clima";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel elCanal = new NotificationChannel(idCanal, context.getString(R.string.alertas_de_clima), NotificationManager.IMPORTANCE_HIGH);
            elCanal.setDescription(context.getString(R.string.avisos_diarios_sobre_el_entorno));
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