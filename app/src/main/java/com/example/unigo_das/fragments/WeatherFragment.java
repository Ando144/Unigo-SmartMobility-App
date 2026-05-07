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
import androidx.fragment.app.Fragment; // IMPORTANTE: Importamos Fragment, no Activity

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.unigo_das.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


// Ahora extendemos de Fragment
public class WeatherFragment extends Fragment {

    private ImageView ivIconoClimaPrincipal;
    private TextView tvTemperaturaPrincipal;
    private WebView wvRadarLluvia;

    // En los Fragments usamos onCreateView en lugar de onCreate
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // 1. Inflamos (cargamos) el archivo XML
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // 2. Buscamos los elementos DENTRO de esa vista que hemos inflado
        ivIconoClimaPrincipal = view.findViewById(R.id.ivIconoClimaPrincipal);
        tvTemperaturaPrincipal = view.findViewById(R.id.tvTemperaturaPrincipal);
        wvRadarLluvia = view.findViewById(R.id.wvRadarLluvia);

        wvRadarLluvia.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 40f);
            }
        });
        wvRadarLluvia.setClipToOutline(true);

        // 3. Cargar el Radar de Lluvia
        cargarRadarRainViewer();

        // 4. Pedir el clima de Bilbao (Estación de Deusto por defecto: C039)
        String urlEstacionDeusto = "https://www.euskadi.eus/contenidos/estacion_meteo/station_c039/es_station/data/es_r01dtpd017ddc1d60bce292a6764656c87e9202fcc";
        obtenerClimaEstacion(urlEstacionDeusto, ivIconoClimaPrincipal, tvTemperaturaPrincipal);

        // Devolvemos la vista completamente montada
        return view;
    }

    private void cargarRadarRainViewer() {
        WebSettings settings = wvRadarLluvia.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // Necesario para que RainViewer no se quede cargando
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        settings.setSupportZoom(true); // Permite que el WebView soporte zoom
        settings.setBuiltInZoomControls(true); // Activa el gesto de "pellizcar" para hacer zoom
        settings.setDisplayZoomControls(false); // Oculta los feos botones (+ / -) antiguos de Android

        // Creamos un Cliente Personalizado para "enjaular" la navegación
        wvRadarLluvia.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                String urlDestino = request.getUrl().toString();

                // Si la URL que intenta cargar es la del mapa, le damos luz verde
                if (urlDestino.contains("rainviewer.com/map.html")) {
                    return false;
                }

                // Si es cualquier otro enlace (el link de arriba a la izquierda)
                // devolvemos true
                return true;
            }
        });

        // Cargamos el widget incrustado de RainViewer (Opción B: El Radar Real)
        String urlWidget = "https://www.rainviewer.com/map.html?loc=43.26,-2.93,8&oFa=0&oC=1&oU=0&oCS=1&oF=0&oAP=1&c=1&o=83&lm=0&layer=radar&sm=1&sn=1";
        wvRadarLluvia.loadUrl(urlWidget);
    }
    private void obtenerClimaEstacion(String urlXmlDatos, final ImageView ivIcono, final TextView tvTemp) {
        StringRequest peticion = new StringRequest(Request.Method.GET, urlXmlDatos,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        procesarXmlEuskalmet(response, ivIcono, tvTemp);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvTemp.setText("Error");
                    }
                }
        );
        // IMPORTANTE: En un fragment usamos requireContext() en lugar de "this" para Volley
        Volley.newRequestQueue(requireContext()).add(peticion);
    }

    private void procesarXmlEuskalmet(String xml, ImageView ivIcono, TextView tvTemp) {
        try {
            // Analizador nativo de XML en Android
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

            double temperatura = 0.0;
            double precipitacion = 0.0;
            boolean datosEncontrados = false;

            NodeList nodos = doc.getElementsByTagName("*");
            for (int i = 0; i < nodos.getLength(); i++) {
                Element elemento = (Element) nodos.item(i);
                String nombreEtiqueta = elemento.getNodeName().toLowerCase();
                String contenido = elemento.getTextContent().trim();

                if (nombreEtiqueta.contains("temp") && contenido.matches("[-+]?\\d*\\.?\\d+")) {
                    temperatura = Double.parseDouble(contenido);
                    datosEncontrados = true;
                }
                if (nombreEtiqueta.contains("precip") && contenido.matches("[-+]?\\d*\\.?\\d+")) {
                    precipitacion = Double.parseDouble(contenido);
                }
            }

            if (datosEncontrados) {
                tvTemp.setText(temperatura + " ºC");

                if (precipitacion > 0.0) {
                    ivIcono.setImageResource(android.R.drawable.ic_menu_sort_by_size);
                } else if (temperatura < 12.0) {
                    ivIcono.setImageResource(android.R.drawable.ic_menu_gallery);
                } else {
                    ivIcono.setImageResource(android.R.drawable.ic_menu_view);
                }
            } else {
                tvTemp.setText("No Data");
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvTemp.setText("Error XML");
        }
    }
}