package com.example.unigo_das.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.unigo_das.R;

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

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import java.io.StringReader;


public class WeatherFragment extends Fragment {

    private ImageView ivIconoClimaPrincipal;
    private TextView tvTemperaturaPrincipal;
    private TextView tvPronosticoBilbao;
    private WebView wvRadarLluvia;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        ivIconoClimaPrincipal = view.findViewById(R.id.ivIconoClimaPrincipal);
        tvTemperaturaPrincipal = view.findViewById(R.id.tvTemperaturaPrincipal);
        tvPronosticoBilbao = view.findViewById(R.id.tvPronosticoBilbao);
        wvRadarLluvia = view.findViewById(R.id.wvRadarLluvia);

        wvRadarLluvia.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 40f);
            }
        });
        wvRadarLluvia.setClipToOutline(true);

        cargarRadarRainViewer();
        obtenerClimaPorCoordenadas(43.26, -2.94, ivIconoClimaPrincipal, tvTemperaturaPrincipal);
        obtenerPronosticoOpenData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) cargarClimaFavoritos(getView());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null) {
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
        Volley.newRequestQueue(requireContext()).add(peticion);
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
                                    tvPronosticoBilbao.setText(textoNativo);
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
            tvPronosticoBilbao.setText("Error analizando datos.");
        }
    }

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
                                if (codigoClima >= 51) {
                                    ivIcono.setImageResource(android.R.drawable.ic_menu_sort_by_size);
                                } else if (codigoClima >= 1 && codigoClima <= 3) {
                                    ivIcono.setImageResource(android.R.drawable.ic_menu_gallery);
                                } else {
                                    ivIcono.setImageResource(android.R.drawable.ic_menu_view);
                                }
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
        Volley.newRequestQueue(requireContext()).add(peticion);
    }

    private void cargarClimaFavoritos(View view) {
        android.widget.LinearLayout contenedor = view.findViewById(R.id.llFavoritosContainer);
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
            TextView tvUni = tarjeta.findViewById(R.id.tvUniCampus); // Referencia a la etiqueta de universidad

            String nombreMostrar = "";
            String uniNombre = "";
            int uniColor = android.graphics.Color.GRAY; // Color por defecto
            double lat = 0.0;
            double lon = 0.0;

            // Determinar Universidad y Color por el prefijo del ID
            if (idCampus.startsWith("EHU_")) {
                uniNombre = "UPV/EHU";
                uniColor = android.graphics.Color.parseColor("#D32F2F"); // Rojo
            } else if (idCampus.startsWith("MU_")) {
                uniNombre = "Mondragon";
                uniColor = android.graphics.Color.parseColor("#557755"); // Verde apagado
            } else if (idCampus.startsWith("DEU_")) {
                uniNombre = "Deusto";
                uniColor = android.graphics.Color.parseColor("#1976D2"); // Azul
            }

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
                obtenerClimaPorCoordenadas(lat, lon, null, tvTemp);
                contenedor.addView(tarjeta);
            }
        }
    }
}