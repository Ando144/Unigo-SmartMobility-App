package com.example.unigo_das.fragments;

import android.Manifest;
import android.content.Context;
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

        wvRadarLluvia.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 40f);
            }
        });
        wvRadarLluvia.setClipToOutline(true);

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

    private void obtenerUbicacionYClima() {
        if (!isAdded() || getContext() == null) return;
        
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
        if (!isAdded() || getContext() == null) return "Tu ubicación";
        
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

    private void actualizarClimaConNombre(double lat, double lon, String nombre) {
        if (tvUbicacionPrincipal != null) {
            tvUbicacionPrincipal.setText(nombre);
        }
        obtenerClimaPorCoordenadas(lat, lon, ivIconoClimaPrincipal, tvTemperaturaPrincipal);
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
                return !urlDestino.contains("rainviewer.com/map.html");
            }
        });
        wvRadarLluvia.loadUrl("https://www.rainviewer.com/map.html?loc=43.26,-2.93,8&oFa=0&oC=1&oU=0&oCS=1&oF=0&oAP=1&c=1&o=83&lm=0&layer=radar&sm=1&sn=1");
    }

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
                        if (isAdded() && tvPronosticoBilbao != null) tvPronosticoBilbao.setText(textoEspanol);
                    }
                });
    }

    private void obtenerClimaPorCoordenadas(double latitud, double longitud, final ImageView ivIcono, final TextView tvTemp) {
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + latitud + "&longitude=" + longitud + "&current_weather=true";
        JsonObjectRequest peticion = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!isAdded()) return;
                        try {
                            JSONObject current = response.getJSONObject("current_weather");
                            double temperatura = current.getDouble("temperature");
                            int codigoClima = current.getInt("weathercode");
                            if (tvTemp != null) tvTemp.setText(temperatura + " ºC");

                            if (ivIcono != null) {
                                asignarIconoYTexto(codigoClima, ivIcono);
                            }
                        } catch (Exception e) {
                            if (tvTemp != null) tvTemp.setText("Error");
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

    private void asignarIconoYTexto(int code, ImageView iv) {
        String desc = "Desconocido";
        int resId = R.drawable.ic_clima_nubes;

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

        if (iv == ivIconoClimaPrincipal) {
            if (tvDescripcionClima != null) tvDescripcionClima.setText(desc);
            iv.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            iv.setColorFilter(null);
        }
    }

    private void cargarClimaFavoritos(View view) {
        if (!isAdded()) return;
        android.widget.GridLayout contenedor = view.findViewById(R.id.glFavoritosContainer);
        if (contenedor == null) return;
        contenedor.removeAllViews();

        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        int idUsuarioActual = 1;

        List<String> favoritos = dbHelper.obtenerIdsFavoritosUsuario(idUsuarioActual);

        if (favoritos.isEmpty()) {
            TextView tvVacio = new TextView(requireContext());
            tvVacio.setText("Aún no has guardado ningún campus en favoritos.");
            tvVacio.setPadding(16, 16, 16, 16);
            contenedor.addView(tvVacio);
            return;
        }

        for (String idCampus : favoritos) {
            Centro centro = dbHelper.obtenerCentroPorId(idCampus);

            if (centro != null) {
                View tarjeta = getLayoutInflater().inflate(R.layout.item_clima_favorito, contenedor, false);
                TextView tvNombre = tarjeta.findViewById(R.id.tvNombreCampus);
                TextView tvTemp = tarjeta.findViewById(R.id.tvTempCampus);
                TextView tvUni = tarjeta.findViewById(R.id.tvUniCampus);

                tvNombre.setText(centro.getNombre());
                tvUni.setText(centro.getUniversidad());

                if (centro.getUniversidad().equals("UPV/EHU")) {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                } else if (centro.getUniversidad().equals("Mondragon")) {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#557755"));
                } else {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                }

                obtenerClimaConCola(centro.getLatitud(), centro.getLongitud(), tvTemp);
                contenedor.addView(tarjeta);
            }
        }
    }

    private void obtenerClimaConCola(double lat, double lon, final TextView tvTemp) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (isAdded() && tvTemp != null) {
                        try {
                            double t = response.getJSONObject("current_weather").getDouble("temperature");
                            tvTemp.setText(t + " ºC");
                        } catch (Exception e) { tvTemp.setText("Error"); }
                    }
                },
                error -> {
                    if (isAdded() && tvTemp != null) tvTemp.setText("Err Red");
                }
        );
        requestQueue.add(request);
    }
}