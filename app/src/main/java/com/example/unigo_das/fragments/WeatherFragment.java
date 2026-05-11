package com.example.unigo_das.fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.unigo_das.R;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.item.Centro;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;

public class WeatherFragment extends Fragment {

    private ImageView ivIconoClimaPrincipal;
    private TextView tvTemperaturaPrincipal;
    private TextView tvPronosticoBilbao;
    private TextView tvUbicacionPrincipal;
    private TextView tvDescripcionClima;
    private WebView wvRadarLluvia;
    private FusedLocationProviderClient fusedLocationClient;

    private com.android.volley.RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        ivIconoClimaPrincipal = view.findViewById(R.id.ivIconoClimaPrincipal);
        tvTemperaturaPrincipal = view.findViewById(R.id.tvTemperaturaPrincipal);
        tvPronosticoBilbao = view.findViewById(R.id.tvPronosticoBilbao);
        tvUbicacionPrincipal = view.findViewById(R.id.tvUbicacionPrincipal);
        tvDescripcionClima = view.findViewById(R.id.tvDescripcionClima);
        wvRadarLluvia = view.findViewById(R.id.wvRadarLluvia);

        requestQueue = Volley.newRequestQueue(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        androidx.cardview.widget.CardView cvMapaAire = view.findViewById(R.id.cvMapaAire);
        androidx.cardview.widget.CardView cvMapaPolen = view.findViewById(R.id.cvMapaPolen);

        cvMapaAire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirPantallaMapaCalor(getString(R.string.aire));
            }
        });

        cvMapaPolen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirPantallaMapaCalor(getString(R.string.polen));
            }
        });

        // Le damos esquinas redondeadas al radar por código para que encaje con el diseño de las tarjetas
        wvRadarLluvia.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 40f);
            }
        });
        wvRadarLluvia.setClipToOutline(true);

        // Solucionamos el problema del scroll. Al tocar el WebView, le decimos al padre (el ScrollView)
        // que no intercepte el toque, así podemos mover el mapa libremente sin que la pantalla suba o baje.
        wvRadarLluvia.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        cargarRadarRainViewer();
        obtenerUbicacionYClima();
        obtenerPronosticoOpenData();

        return view;
    }

    private void abrirPantallaMapaCalor(String tipoMapa) {
        android.content.Intent intent = new android.content.Intent(requireContext(), com.example.unigo_das.activities.HeatmapActivity.class);
        intent.putExtra("TIPO_MAPA", tipoMapa);
        startActivity(intent);
    }

    // Metodo principal que coordina los datos del clima
    private void obtenerUbicacionYClima() {
        if (!isAdded() || getContext() == null) return;

        // Si no nos han dado permisos de ubicación, cargamos Bilbao por defecto
        // para que la pantalla no se quede en blanco.
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            actualizarClimaConNombre(43.26, -2.94, "Bilbao");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (!isAdded() || getContext() == null) return;
                if (location != null) {
                    String nombreCiudad = obtenerNombreCiudad(location.getLatitude(), location.getLongitude());
                    actualizarClimaConNombre(location.getLatitude(), location.getLongitude(), nombreCiudad);
                } else {
                    actualizarClimaConNombre(43.26, -2.94, "Bilbao");
                }
            }
        });
    }

    private String obtenerNombreCiudad(double lat, double lon) {
        if (!isAdded() || getContext() == null) return getString(R.string.tu_ubicaci_n);

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                return (city != null) ? city : getString(R.string.tu_ubicaci_n2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Tu ubicación";
    }

    private void actualizarClimaConNombre(double lat, double lon, String nombre) {
        if (tvUbicacionPrincipal != null) {
            tvUbicacionPrincipal.setText(nombre);
        }
        obtenerClimaPorCoordenadas(lat, lon, ivIconoClimaPrincipal, tvTemperaturaPrincipal);

        obtenerCalidadAire(lat, lon);
        obtenerNivelPolen(lat, lon);
    }

    // A diferencia de OpenMeteo, la API de Google requiere que le pasemos las coordenadas
    // en formato JSON dentro de una petición POST, por eso preparamos el 'body' primero.
    private void obtenerCalidadAire(double lat, double lon) {
        String url = "https://airquality.googleapis.com/v1/currentConditions:lookup?key=" + com.example.unigo_das.BuildConfig.DIRECTIONS_API_KEY;

        try {
            JSONObject body = new JSONObject();
            JSONObject location = new JSONObject();
            location.put("latitude", lat);
            location.put("longitude", lon);
            body.put("location", location);
            body.put("languageCode", "en");

            JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.POST, url, body,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Muy importante comprobar isAdded() en los callbacks  para que la app
                            // no pete si el usuario ya ha cambiado de pestaña cuando llega la respuesta.
                            if (!isAdded() || getView() == null) return;
                            try {
                                String categoriaIngles = response.getJSONArray("indexes")
                                        .getJSONObject(0)
                                        .getString("category");

                                String categoriaTraducida = traducirCalidadAire(categoriaIngles);

                                TextView tvIndiceAire = getView().findViewById(R.id.tvIndiceAire);
                                if (tvIndiceAire != null) {
                                    tvIndiceAire.setText(categoriaTraducida);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                TextView tv = getView().findViewById(R.id.tvIndiceAire);
                                if (tv != null) tv.setText(getString(R.string.error_api));
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (isAdded() && getView() != null) {
                                TextView tvIndiceAire = getView().findViewById(R.id.tvIndiceAire);
                                if (tvIndiceAire != null) tvIndiceAire.setText(getString(R.string.no_data_short));
                            }
                        }
                    }
            );
            requestQueue.add(peticion);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Petición GET  a la API del polen de Google
    private void obtenerNivelPolen(double lat, double lon) {
        String url = "https://pollen.googleapis.com/v1/forecast:lookup?key=" + com.example.unigo_das.BuildConfig.DIRECTIONS_API_KEY +
                "&location.latitude=" + lat +
                "&location.longitude=" + lon +
                "&days=1&languageCode=en";

        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!isAdded() || getView() == null) return;
                        try {
                            if (!response.has("dailyInfo") || response.getJSONArray("dailyInfo").length() == 0) {
                                actualizarTextoPolen(getString(R.string.no_data_long));
                                return;
                            }

                            // Google devuelve varios tipos de polen (hierba, árboles, maleza).
                            // Recorremos el array y nos quedamos con el valor más alto para avisar al usuario.
                            JSONObject daily = response.getJSONArray("dailyInfo").getJSONObject(0);
                            String nivelMaximo = "None";

                            if (daily.has("pollenTypeInfo")) {
                                org.json.JSONArray types = daily.getJSONArray("pollenTypeInfo");
                                int maxVal = -1;

                                for (int i = 0; i < types.length(); i++) {
                                    JSONObject item = types.getJSONObject(i);
                                    if (item.has("indexInfo")) {
                                        JSONObject indexInfo = item.getJSONObject("indexInfo");
                                        int val = indexInfo.optInt("value", 0);
                                        if (val >= maxVal) {
                                            maxVal = val;
                                            nivelMaximo = indexInfo.optString("category", "None");
                                        }
                                    }
                                }
                            }

                            actualizarTextoPolen(traducirPolen(nivelMaximo));

                        } catch (Exception e) {
                            e.printStackTrace();
                            actualizarTextoPolen(getString(R.string.no_data_short));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded()) {
                            actualizarTextoPolen(getString(R.string.no_data_short));
                        }
                    }
                }
        );
        requestQueue.add(peticion);
    }

    private void actualizarTextoPolen(String texto) {
        if (getView() != null) {
            TextView tv = getView().findViewById(R.id.tvIndicePolen);
            if (tv != null) tv.setText(texto);
        }
    }

    private String traducirCalidadAire(String categoriaEn) {
        if (categoriaEn == null || !isAdded()) return getString(R.string.no_data_short);

        String cat = categoriaEn.toLowerCase().trim();

        if (cat.contains("excellent")) return getString(R.string.air_excellent);
        if (cat.contains("good")) return getString(R.string.air_good);
        if (cat.contains("moderate")) return getString(R.string.air_moderate);
        if (cat.contains("poor") && !cat.contains("very")) return getString(R.string.air_poor);
        if (cat.contains("very poor")) return getString(R.string.air_very_poor);
        if (cat.contains("severe")) return getString(R.string.air_severe);

        return getString(R.string.air_unknown);
    }

    private String traducirPolen(String categoriaEn) {
        if (categoriaEn == null || !isAdded()) return getString(R.string.no_data_short);

        String cat = categoriaEn.toLowerCase().trim();

        if (cat.contains("none")) return getString(R.string.pollen_none);
        if (cat.contains("very low")) return getString(R.string.pollen_very_low);
        if (cat.contains("low") && !cat.contains("very")) return getString(R.string.pollen_low);
        if (cat.contains("moderate")) return getString(R.string.pollen_moderate);
        if (cat.contains("high") && !cat.contains("very")) return getString(R.string.pollen_high);
        if (cat.contains("very high")) return getString(R.string.pollen_very_high);
        if (cat.contains("severe")) return getString(R.string.pollen_severe);

        return getString(R.string.pollen_unknown);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && isAdded()) cargarClimaFavoritos(getView());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null && isAdded()) {
            cargarClimaFavoritos(getView());
        }
    }

    // Cargamos el mapa de radar mediante un iframe en el WebView
    private void cargarRadarRainViewer() {
        WebSettings settings = wvRadarLluvia.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Interceptamos las URLs para evitar que si el usuario clica en un logo o enlace del mapa,
        // se abra el navegador del móvil y lo saque de la aplicación.
        wvRadarLluvia.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                String urlDestino = request.getUrl().toString();
                return !urlDestino.contains("rainviewer.com/map.html");
            }
        });
        wvRadarLluvia.loadUrl("https://www.rainviewer.com/map.html?loc=43.26,-2.93,8&oFa=0&oC=1&oU=0&oCS=1&oF=0&oAP=1&c=1&o=83&lm=0&layer=radar&sm=1&sn=1");
    }

    // Petición a OpenData Euskadi. Como devuelven XML puro en vez de JSON, usamos StringRequest
    private void obtenerPronosticoOpenData() {
        String urlXmlOpenData = "https://opendata.euskadi.eus/contenidos/prevision_tiempo/met_forecast_zone/opendata/met_forecast_zone.xml";

        StringRequest peticion = new StringRequest(Request.Method.GET, urlXmlOpenData,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (isAdded()) procesarXmlOpenData(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && tvPronosticoBilbao != null) tvPronosticoBilbao.setText("Error conectando con Euskalmet.");
                    }
                }
        );
        requestQueue.add(peticion);
    }

    // Parseo manual del documento XML de Euskalmet
    private void procesarXmlOpenData(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = builder.parse(is);

            // Buscamos directamente el areaId 8 que corresponde a la zona Gran Bilbao
            NodeList areas = doc.getElementsByTagName("areaForecast");
            for (int i = 0; i < areas.getLength(); i++) {
                Element area = (Element) areas.item(i);
                if (area.getAttribute("areaId").equals("8")) {
                    NodeList periods = area.getElementsByTagName("periodData");

                    // Nos interesa solo el pronóstico del hoy
                    for (int j = 0; j < periods.getLength(); j++) {
                        Element period = (Element) periods.item(j);
                        if (period.getAttribute("periodDay").equals("today")) {
                            NodeList descriptions = period.getElementsByTagName("forecastDescription");
                            if (descriptions.getLength() > 0) {
                                Element descElement = (Element) descriptions.item(0);
                                String idioma = java.util.Locale.getDefault().getLanguage();

                                // Euskalmet solo provee textos en CAstellano y Euskera.
                                // Si el usuario tiene la app en Inglés, llamamos a ML Kit para traducirlo al vuelo.
                                if (idioma.equals("es") || idioma.equals("eu")) {
                                    String textoNativo = descElement.getElementsByTagName(idioma).item(0).getTextContent().trim();
                                    if (tvPronosticoBilbao != null) tvPronosticoBilbao.setText(textoNativo);
                                } else {
                                    String textoEspanol = descElement.getElementsByTagName("es").item(0).getTextContent().trim();
                                    traducirConIA(textoEspanol, idioma);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isAdded() && tvPronosticoBilbao != null) tvPronosticoBilbao.setText("Error analizando datos.");
        }
    }

    // Utiliza los modelos de Google ML Kit locales para traducir textos offline
    private void traducirConIA(String textoEspanol, String idiomaDestino) {
        if (!isAdded()) return;
        String mlKitLang = TranslateLanguage.fromLanguageTag(idiomaDestino);
        if (mlKitLang == null) {
            if (tvPronosticoBilbao != null) tvPronosticoBilbao.setText(textoEspanol);
            return;
        }
        if (tvPronosticoBilbao != null) tvPronosticoBilbao.setText("Traduciendo pronóstico...");

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.SPANISH)
                .setTargetLanguage(mlKitLang)
                .build();
        final Translator traductor = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        // Si es la primera vez que se usa, descarga el paquete de idioma
        traductor.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        traductor.translate(textoEspanol)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String textoTraducido) {
                                        if (isAdded() && tvPronosticoBilbao != null) tvPronosticoBilbao.setText(textoTraducido);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // si falla la descarga o traducción, mostramos la versión en español original
                        if (isAdded() && tvPronosticoBilbao != null) tvPronosticoBilbao.setText(textoEspanol);
                    }
                });
    }

    private void asignarIconoYTexto(int code, ImageView iv) {
        String desc = getString(R.string.desconocido);
        int resId = R.drawable.ic_clima_nubes;

        if (code == 0) desc = getString(R.string.cielo_despejado);
        else if (code == 1) desc = getString(R.string.mayormente_despejado);
        else if (code == 2) desc = getString(R.string.parcialmente_nublado);
        else if (code == 3) desc = getString(R.string.totalmente_nublado);
        else if (code == 45 || code == 48) desc = getString(R.string.niebla_densa);
        else if (code == 51 || code == 53 || code == 55) desc = getString(R.string.llovizna);
        else if (code == 56 || code == 57) desc = getString(R.string.llovizna_helada);
        else if (code == 61) desc = getString(R.string.lluvia_ligera);
        else if (code == 63) desc = getString(R.string.lluvia_moderada);
        else if (code == 65) desc = getString(R.string.lluvia_fuerte);
        else if (code == 66 || code == 67) desc = getString(R.string.lluvia_helada);
        else if (code == 71) desc = getString(R.string.nieve_ligera);
        else if (code == 73) desc = getString(R.string.nieve_moderada);
        else if (code == 75) desc = getString(R.string.nevada_fuerte);
        else if (code == 77) desc = getString(R.string.granizo_suave);
        else if (code == 80) desc = getString(R.string.chubascos_ligeros);
        else if (code == 81) desc = getString(R.string.chubascos_moderados);
        else if (code == 82) desc = getString(R.string.chubascos_violentos);
        else if (code == 85 || code == 86) desc = getString(R.string.chubascos_de_nieve);
        else if (code == 95) desc = getString(R.string.tormenta_el_ctrica);
        else if (code == 96 || code == 99) desc = getString(R.string.tormenta_con_granizo);

        if (code == 0) {
            resId = R.drawable.ic_clima_sol;
        } else if (code >= 1 && code <= 3) {
            resId = R.drawable.ic_clima_nubes;
        } else if (code >= 45 && code <= 48) {
            resId = R.drawable.ic_clima_niebla;
        } else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            resId = R.drawable.ic_clima_lluvia;
        } else if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            resId = R.drawable.ic_clima_nieve;
        } else if (code >= 95 && code <= 99) {
            resId = R.drawable.ic_clima_tormenta;
        }

        iv.setImageResource(resId);

        // Si es el icono principal le damos el color rojo , sino lo dejamos con su color por defecto
        if (iv == ivIconoClimaPrincipal) {
            if (tvDescripcionClima != null) tvDescripcionClima.setText(desc);
            iv.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            iv.setColorFilter(null);
        }
    }

    // Lee la BD local para saber qué campus están en favoritos e crea las tarjetas
    private void cargarClimaFavoritos(View view) {
        if (!isAdded()) return;
        android.widget.GridLayout contenedor = view.findViewById(R.id.glFavoritosContainer);
        if (contenedor == null) return;

        // Vaciamos el contenedor primero para evitar que se dupliquen las tarjetas si volvemos a esta pantalla
        contenedor.removeAllViews();

        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        int idUsuarioActual = prefs.getInt("user_id", 0);

        List<String> favoritos = dbHelper.obtenerIdsFavoritosUsuario(idUsuarioActual);

        if (favoritos.isEmpty()) {
            TextView tvVacio = new TextView(requireContext());
            tvVacio.setText(R.string.a_n_no_has_guardado_ning_n_campus_en_favoritos);
            tvVacio.setPadding(16, 16, 16, 16);
            contenedor.addView(tvVacio);
            return;
        }

        for (String idCampus : favoritos) {
            Centro centro = dbHelper.obtenerCentroPorId(idCampus);

            if (centro != null) {
                // Inflamos el layout de la tarjeta a mano para cada centro guardado
                View tarjeta = getLayoutInflater().inflate(R.layout.item_clima_favorito, contenedor, false);
                TextView tvNombre = tarjeta.findViewById(R.id.tvNombreCampus);
                TextView tvTemp = tarjeta.findViewById(R.id.tvTempCampus);
                TextView tvUni = tarjeta.findViewById(R.id.tvUniCampus);

                tvNombre.setText(centro.getNombre());
                tvUni.setText(centro.getUniversidad());

                if (centro.getUniversidad().equals("UPV/EHU")) {
                    tvUni.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.university_tag_color_ehu));
                } else if (centro.getUniversidad().equals("Mondragon")) {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#008a96"));
                } else {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                }

                // Llamada individual a la API para cada tarjeta
                obtenerClimaConCola(centro.getLatitud(), centro.getLongitud(), tvTemp);

                android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
                params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1.0f);
                params.width = 0;
                params.setMargins(10, 10, 10, 20);

                tarjeta.setLayoutParams(params);
                contenedor.addView(tarjeta);
            }
        }
    }

    // Petición  a OpenMeteo: pedimos temperatura actual, UV máximo diario y horas de sol todo de golpe
    private void obtenerClimaPorCoordenadas(double latitud, double longitud, final ImageView ivIcono, final TextView tvTemp) {
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + latitud +
                "&longitude=" + longitud +
                "&current_weather=true" +
                "&daily=uv_index_max,sunrise,sunset" +
                "&timezone=auto";

        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!isAdded() || getView() == null) return;
                        try {
                            if (response.has("current_weather")) {
                                JSONObject currentWeather = response.getJSONObject("current_weather");
                                double temperatura = currentWeather.optDouble("temperature", 0.0);
                                int codigoClima = currentWeather.optInt("weathercode", 0);
                                if (tvTemp != null) tvTemp.setText(temperatura + " ºC");
                                if (ivIcono != null) asignarIconoYTexto(codigoClima, ivIcono);
                            }

                            if (response.has("daily")) {
                                JSONObject dailyParams = response.getJSONObject("daily");

                                if (dailyParams.has("uv_index_max")) {
                                    double uvIndex = dailyParams.getJSONArray("uv_index_max").optDouble(0, 0.0);
                                    actualizarTarjetaUV(uvIndex);
                                }

                                if (dailyParams.has("sunrise") && dailyParams.has("sunset")) {
                                    String amanecerIso = dailyParams.getJSONArray("sunrise").optString(0, "");
                                    String anochecerIso = dailyParams.getJSONArray("sunset").optString(0, "");

                                    // Limpiamos la fecha que nos manda la API para quedarnos únicamente con la hora y los minutos
                                    if (amanecerIso.length() >= 16 && anochecerIso.length() >= 16) {
                                        String amanecer = amanecerIso.substring(11, 16);
                                        String anochecer = anochecerIso.substring(11, 16);

                                        TextView tvAmanecer = getView().findViewById(R.id.tvAmanecer);
                                        TextView tvAnochecer = getView().findViewById(R.id.tvAnochecer);

                                        if (tvAmanecer != null) tvAmanecer.setText("🌅 " + amanecer);
                                        if (tvAnochecer != null) tvAnochecer.setText("🌙 " + anochecer);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && tvTemp != null) tvTemp.setText("Err Red");
                    }
                }
        );
        requestQueue.add(peticion);
    }

    private void actualizarTarjetaUV(double uvIndex) {
        if (getView() == null) return;

        TextView tvValorUV = getView().findViewById(R.id.tvValorUV);
        TextView tvDescUV = getView().findViewById(R.id.tvDescUV);

        if (tvValorUV == null || tvDescUV == null) return;

        tvValorUV.setText(String.valueOf(Math.round(uvIndex)));

        String idioma = java.util.Locale.getDefault().getLanguage();
        String descripcion;

        if (uvIndex < 3) {
            descripcion = getString(R.string.bajo);
        } else if (uvIndex < 6) {
            descripcion = getString(R.string.moderado);
        } else if (uvIndex < 8) {
            descripcion = getString(R.string.alto);
            tvValorUV.setTextColor(android.graphics.Color.parseColor("#F57C00"));
        } else if (uvIndex < 11) {
            descripcion = getString(R.string.muy_alto);
            tvValorUV.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            descripcion = getString(R.string.extremo);
            tvValorUV.setTextColor(android.graphics.Color.parseColor("#7B1FA2"));
        }

        tvDescUV.setText(descripcion);
    }

    private void obtenerClimaConCola(double lat, double lon, final TextView tvTemp) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (isAdded() && tvTemp != null) {
                            try {
                                double t = response.getJSONObject("current_weather").getDouble("temperature");
                                tvTemp.setText(t + " ºC");
                            } catch (Exception e) { tvTemp.setText("Error"); }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && tvTemp != null) tvTemp.setText("Err Red");
                    }
                }
        );
        requestQueue.add(request);
    }
}