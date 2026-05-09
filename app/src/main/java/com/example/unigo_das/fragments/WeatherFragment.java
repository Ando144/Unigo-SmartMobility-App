package com.example.unigo_das.fragments;

import android.Manifest;
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

        // Enlace de las variables con los elementos visuales del XML
        ivIconoClimaPrincipal = view.findViewById(R.id.ivIconoClimaPrincipal);
        tvTemperaturaPrincipal = view.findViewById(R.id.tvTemperaturaPrincipal);
        tvPronosticoBilbao = view.findViewById(R.id.tvPronosticoBilbao);
        tvUbicacionPrincipal = view.findViewById(R.id.tvUbicacionPrincipal);
        tvDescripcionClima = view.findViewById(R.id.tvDescripcionClima);
        wvRadarLluvia = view.findViewById(R.id.wvRadarLluvia);

        requestQueue = Volley.newRequestQueue(requireContext());

        // Inicialización del servicio de localización de Google
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Redondeo de las esquinas del WebView
        wvRadarLluvia.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 40f);
            }
        });
        wvRadarLluvia.setClipToOutline(true);

        // Evita conflictos al tocar el mapa, bloquea el scroll de la pantalla principal
        wvRadarLluvia.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        // Llamadas iniciales para cargar los datos de la pantalla
        cargarRadarRainViewer();
        obtenerUbicacionYClima();
        obtenerPronosticoOpenData();

        return view;
    }

    // Comprueba los permisos y obtiene la posición GPS actual.
    // Si falla o no hay permisos, establece Bilbao como ubicación por defecto.
    private void obtenerUbicacionYClima() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            actualizarClimaConNombre(43.26, -2.94, "Bilbao");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    String nombreCiudad = obtenerNombreCiudad(location.getLatitude(), location.getLongitude());
                    actualizarClimaConNombre(location.getLatitude(), location.getLongitude(), nombreCiudad);
                } else {
                    actualizarClimaConNombre(43.26, -2.94, "Bilbao");
                }
            }
        });
    }

    // Utiliza el Geocoder de Android para transformar coordenadas (Lat/Lon) en un nombre de ciudad/pueblo
    private String obtenerNombreCiudad(double lat, double lon) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                return (city != null) ? city : "Tu ubicación";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Tu ubicación";
    }

    // Metodo auxiliar que actualiza el texto de la cabecera y lanza la petición a la API del clima.
    private void actualizarClimaConNombre(double lat, double lon, String nombre) {
        if (tvUbicacionPrincipal != null) {
            tvUbicacionPrincipal.setText(nombre);
        }
        obtenerClimaPorCoordenadas(lat, lon, ivIconoClimaPrincipal, tvTemperaturaPrincipal);
    }

    //Se dispara al volver a la pestaña (por ejemplo, tras abrir la app desde segundo plano).
    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) cargarClimaFavoritos(getView());
    }

    // Se dispara al navegar entre pestañas del menú inferior
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) {
            cargarClimaFavoritos(getView());
        }
    }

    // Configura e inyecta el mapa interactivo de precipitaciones (RainViewer) en el WebView.
    private void cargarRadarRainViewer() {
        WebSettings settings = wvRadarLluvia.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        wvRadarLluvia.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                String urlDestino = request.getUrl().toString();
                // Bloquea enlaces externos para evitar que el usuario salga del radar
                return !urlDestino.contains("rainviewer.com/map.html");
            }
        });
        wvRadarLluvia.loadUrl("https://www.rainviewer.com/map.html?loc=43.26,-2.93,8&oFa=0&oC=1&oU=0&oCS=1&oF=0&oAP=1&c=1&o=83&lm=0&layer=radar&sm=1&sn=1");
    }

    // Descarga el XML de pronóstico oficial de Euskalmet usando la RequestQueue global en OpenData.
    private void obtenerPronosticoOpenData() {
        String urlXmlOpenData = "https://opendata.euskadi.eus/contenidos/prevision_tiempo/met_forecast_zone/opendata/met_forecast_zone.xml";

        StringRequest peticion = new StringRequest(Request.Method.GET, urlXmlOpenData,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        procesarXmlOpenData(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (tvPronosticoBilbao != null) tvPronosticoBilbao.setText("Error conectando con Euskalmet.");
                    }
                }
        );
        requestQueue.add(peticion);
    }

    // Analiza el XML recibido, busca la región del Gran Bilbao (areaId=8) y extrae el texto del pronóstico
    // según el idioma actual del teléfono.
    private void procesarXmlOpenData(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = builder.parse(is);

            NodeList areas = doc.getElementsByTagName("areaForecast");
            for (int i = 0; i < areas.getLength(); i++) {
                Element area = (Element) areas.item(i);
                if (area.getAttribute("areaId").equals("8")) {
                    NodeList periods = area.getElementsByTagName("periodData");
                    for (int j = 0; j < periods.getLength(); j++) {
                        Element period = (Element) periods.item(j);
                        if (period.getAttribute("periodDay").equals("today")) {
                            NodeList descriptions = period.getElementsByTagName("forecastDescription");
                            if (descriptions.getLength() > 0) {
                                Element descElement = (Element) descriptions.item(0);
                                String idioma = java.util.Locale.getDefault().getLanguage();

                                // Si el móvil está en Español o Euskera, usa el texto oficial
                                if (idioma.equals("es") || idioma.equals("eu")) {
                                    String textoNativo = descElement.getElementsByTagName(idioma).item(0).getTextContent().trim();
                                    tvPronosticoBilbao.setText(textoNativo);
                                } else {
                                    // Si es otro idioma, traduce el texto en español usando la IA de Google ML kit
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
            tvPronosticoBilbao.setText("Error analizando datos.");
        }
    }

    // Utiliza el modelo On-Device de Google ML Kit para traducir texto localmente sin APIs de pago.
    private void traducirConIA(String textoEspanol, String idiomaDestino) {
        String mlKitLang = TranslateLanguage.fromLanguageTag(idiomaDestino);
        if (mlKitLang == null) {
            tvPronosticoBilbao.setText(textoEspanol);
            return;
        }
        tvPronosticoBilbao.setText("Traduciendo pronóstico...");
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.SPANISH)
                .setTargetLanguage(mlKitLang)
                .build();
        final Translator traductor = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        traductor.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        traductor.translate(textoEspanol)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String textoTraducido) {
                                        tvPronosticoBilbao.setText(textoTraducido);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        tvPronosticoBilbao.setText(textoEspanol);
                    }
                });
    }

    // Realiza la petición a la API de Open-Meteo usando la RequestQueue global para obtener clima.
    private void obtenerClimaPorCoordenadas(double latitud, double longitud, final ImageView ivIcono, final TextView tvTemp) {
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + latitud + "&longitude=" + longitud + "&current_weather=true";
        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject current = response.getJSONObject("current_weather");
                            double temperatura = current.getDouble("temperature");
                            int codigoClima = current.getInt("weathercode");
                            tvTemp.setText(temperatura + " ºC");

                            if (ivIcono != null) {
                                asignarIconoYTexto(codigoClima, ivIcono);
                            }
                        } catch (Exception e) {
                            tvTemp.setText("Error");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvTemp.setText("Err Red");
                    }
                }
        );
        requestQueue.add(peticion);
    }

    // Convierte los códigos numéricos WMO internacionales en descripciones legibles y asocia el icono vectorial correspondiente.
    private void asignarIconoYTexto(int code, ImageView iv) {
        String desc = "Desconocido";
        int resId = R.drawable.ic_clima_nubes;

        // 1. Traducción del código WMO a texto
        if (code == 0) desc = "Cielo despejado";
        else if (code == 1) desc = "Mayormente despejado";
        else if (code == 2) desc = "Parcialmente nublado";
        else if (code == 3) desc = "Totalmente nublado";
        else if (code == 45 || code == 48) desc = "Niebla densa";
        else if (code == 51 || code == 53 || code == 55) desc = "Llovizna";
        else if (code == 56 || code == 57) desc = "Llovizna helada";
        else if (code == 61) desc = "Lluvia ligera";
        else if (code == 63) desc = "Lluvia moderada";
        else if (code == 65) desc = "Lluvia fuerte";
        else if (code == 66 || code == 67) desc = "Lluvia helada";
        else if (code == 71) desc = "Nieve ligera";
        else if (code == 73) desc = "Nieve moderada";
        else if (code == 75) desc = "Nevada fuerte";
        else if (code == 77) desc = "Granizo suave";
        else if (code == 80) desc = "Chubascos ligeros";
        else if (code == 81) desc = "Chubascos moderados";
        else if (code == 82) desc = "Chubascos violentos";
        else if (code == 85 || code == 86) desc = "Chubascos de nieve";
        else if (code == 95) desc = "Tormenta eléctrica";
        else if (code == 96 || code == 99) desc = "Tormenta con granizo";

        // 2. Agrupación visual por tipo de clima
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

        // Si es el clima principal de la pantalla, actualiza el texto y colorea el icono de rojo
        if (iv == ivIconoClimaPrincipal) {
            if (tvDescripcionClima != null) tvDescripcionClima.setText(desc);
            iv.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            iv.setColorFilter(null);
        }
    }

    // Lee los IDs guardados en SharedPreferences y crea dinámicamente las tarjetas en el GridLayout.
    private void cargarClimaFavoritos(View view) {
        android.widget.GridLayout contenedor = view.findViewById(R.id.glFavoritosContainer);
        contenedor.removeAllViews();

        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", android.content.Context.MODE_PRIVATE);
        java.util.Set<String> favoritos = prefs.getStringSet("centros_favoritos", new java.util.HashSet<String>());

        if (favoritos.isEmpty()) {
            TextView tvVacio = new TextView(requireContext());
            tvVacio.setText("Aún no has guardado ningún campus en favoritos.");
            tvVacio.setPadding(16, 16, 16, 16);
            contenedor.addView(tvVacio);
            return;
        }

        for (String idCampus : favoritos) {
            View tarjeta = getLayoutInflater().inflate(R.layout.item_clima_favorito, contenedor, false);
            TextView tvNombre = tarjeta.findViewById(R.id.tvNombreCampus);
            TextView tvTemp = tarjeta.findViewById(R.id.tvTempCampus);
            TextView tvUni = tarjeta.findViewById(R.id.tvUniCampus);

            String nombreMostrar = "";
            String uniNombre = "";
            int uniColor = android.graphics.Color.GRAY;
            double lat = 0.0;
            double lon = 0.0;

            // Detección de la universidad a la que pertenece el campus mediante su prefijo y poniendo el color correspondiente a cada una
            if (idCampus.startsWith("EHU_")) {
                uniNombre = "UPV/EHU";
                uniColor = android.graphics.Color.parseColor("#D32F2F");
            } else if (idCampus.startsWith("MU_")) {
                uniNombre = "Mondragon";
                uniColor = android.graphics.Color.parseColor("#557755");
            } else if (idCampus.startsWith("DEU_")) {
                uniNombre = "Deusto";
                uniColor = android.graphics.Color.parseColor("#1976D2");
            }

            // Mapeo manual de IDs a nombres y coordenadas (Hardcoded y habra que meterlo en la BD)
            switch (idCampus) {
                case "EHU_345": case "EHU_363": case "EHU_364":
                    nombreMostrar = getString(R.string.centro_ehu_ingenieria_bilbao); lat = 43.2638; lon = -2.9511; break;
                case "EHU_350":
                    nombreMostrar = getString(R.string.centro_ehu_ingenieria_bilbao); lat = 43.3308; lon = -3.0186; break;
                case "EHU_320":
                    nombreMostrar = getString(R.string.centro_ehu_bellas_artes); lat = 43.3301; lon = -2.9678; break;
                case "EHU_310":
                    nombreMostrar = getString(R.string.centro_ehu_ciencia_tecnologia); lat = 43.3301; lon = -2.9678; break;
                case "EHU_323":
                    nombreMostrar = getString(R.string.centro_ehu_ciencias_sociales_comunicacion); lat = 43.3301; lon = -2.9678; break;
                case "EHU_324":
                    nombreMostrar = getString(R.string.centro_ehu_derecho); lat = 43.3301; lon = -2.9678; break;
                case "EHU_321":
                    nombreMostrar = getString(R.string.centro_ehu_economia_empresa); lat = 43.2721; lon = -2.9566; break;
                case "EHU_351":
                    nombreMostrar = getString(R.string.centro_ehu_economia_empresa_elcano); lat = 43.2642; lon = -2.9355; break;
                case "EHU_354":
                    nombreMostrar = getString(R.string.centro_ehu_educacion_bilbao); lat = 43.3301; lon = -2.9678; break;
                case "EHU_327": case "EHU_352":
                    nombreMostrar = getString(R.string.centro_ehu_medicina_enfermeria); lat = 43.3301; lon = -2.9678; break;
                case "EHU_332":
                    nombreMostrar = getString(R.string.centro_ehu_unidad_docente_medicina); lat = 43.2289; lon = -2.8360; break;
                case "EHU_328":
                    nombreMostrar = getString(R.string.centro_ehu_unidad_docente_medicina); lat = 43.2605; lon = -2.9490; break;
                case "EHU_329":
                    nombreMostrar = getString(R.string.centro_ehu_unidad_docente_medicina); lat = 43.2847; lon = -2.9829; break;
                case "EHU_EXP":
                    nombreMostrar = getString(R.string.centro_ehu_aulas_experiencia); lat = 43.2576; lon = -2.9238; break;
                case "MU_BBF_EMP":
                    nombreMostrar = getString(R.string.centro_mu_bbf_empresariales); lat = 43.2665; lon = -2.9304; break;
                case "MU_BBF_LEINN":
                    nombreMostrar = getString(R.string.centro_mu_bbf_leinn); lat = 43.2665; lon = -2.9304; break;
                case "MU_AS_POL":
                    nombreMostrar = getString(R.string.centro_mu_as_politecnica); lat = 43.2764; lon = -2.9642; break;
                case "MU_AS_HUM":
                    nombreMostrar = getString(R.string.centro_mu_as_humanidades); lat = 43.2764; lon = -2.9642; break;
                case "DEU_DBS":
                    nombreMostrar = getString(R.string.centro_deusto_business); lat = 43.2713; lon = -2.9379; break;
                case "DEU_DER":
                    nombreMostrar = getString(R.string.centro_deusto_derecho); lat = 43.2713; lon = -2.9379; break;
                case "DEU_CSH":
                    nombreMostrar = getString(R.string.centro_deusto_sociales_humanas); lat = 43.2713; lon = -2.9379; break;
                case "DEU_ING":
                    nombreMostrar = getString(R.string.centro_deusto_ingenieria); lat = 43.2713; lon = -2.9379; break;
                case "DEU_EDU":
                    nombreMostrar = getString(R.string.centro_deusto_educacion_deporte); lat = 43.2713; lon = -2.9379; break;
                case "DEU_SAL":
                    nombreMostrar = getString(R.string.centro_deusto_salud); lat = 43.2713; lon = -2.9379; break;
                case "DEU_CSC":
                    nombreMostrar = getString(R.string.centro_deusto_sociales_comunicacion); lat = 43.2713; lon = -2.9379; break;
            }

            if (lat != 0.0 && lon != 0.0) {
                tvNombre.setText(nombreMostrar);
                tvUni.setText(uniNombre);
                tvUni.setTextColor(uniColor);

                obtenerClimaConCola(lat, lon, tvTemp);
                contenedor.addView(tarjeta);
            }
        }
    }

    // Petición de red asíncrona  para cargar la temperatura de los campus favoritos usando la RequestQueue global.
    private void obtenerClimaConCola(double lat, double lon, final TextView tvTemp) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double t = response.getJSONObject("current_weather").getDouble("temperature");
                        tvTemp.setText(t + " ºC");
                    } catch (Exception e) { tvTemp.setText("Error"); }
                },
                error -> tvTemp.setText("Err Red")
        );
        requestQueue.add(request);
    }
}