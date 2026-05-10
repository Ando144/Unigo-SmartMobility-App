package com.example.unigo_das.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.unigo_das.R;
import com.example.unigo_das.BuildConfig;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.item.Centro;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap campusMap;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    // Vistas de UI
    private View cardBuscadorFalso;
    private View pantallaBusquedaCompleta;
    private View headerBusqueda;
    private ImageView btnCerrarBusqueda;
    private SearchView searchViewReal;
    private ListView lvSearchResults;

    // Nuevas Vistas para Tarjeta de Rutas
    private View cardInfoRutas;
    private LinearLayout llListaInstrucciones;

    private List<Centro> listaTodosLosCentros;
    private BuscadorMapaAdapter searchAdapter;

    // Variables de estado
    private String currentTransportMode = "walking";
    private LatLng destinoPendiente = null;
    private String tituloPendiente = null;
    private LatLng destinoActual = null;
    private String tituloDestinoActual = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                    if ((fineLocationGranted != null && fineLocationGranted) ||
                            (coarseLocationGranted != null && coarseLocationGranted)) {
                        activarUbicacionEnMapa();
                        procesarRutaPendiente();
                    }
                }
        );

        cargarDatosBuscador();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enlazamos las vistas
        cardBuscadorFalso = view.findViewById(R.id.card_buscador_falso);
        pantallaBusquedaCompleta = view.findViewById(R.id.pantalla_busqueda_completa);
        headerBusqueda = view.findViewById(R.id.header_busqueda);
        btnCerrarBusqueda = view.findViewById(R.id.btn_cerrar_busqueda);
        searchViewReal = view.findViewById(R.id.searchViewReal);
        lvSearchResults = view.findViewById(R.id.lv_search_results);

        // Enlazamos tarjeta de rutas
        cardInfoRutas = view.findViewById(R.id.card_info_rutas);
        llListaInstrucciones = view.findViewById(R.id.ll_lista_instrucciones);

        // Selector de transporte
        RadioGroup rgModoTransporte = view.findViewById(R.id.rg_modo_transporte);
        if (rgModoTransporte != null) {
            rgModoTransporte.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.rb_bici) {
                        currentTransportMode = "bicycling";
                    } else if (checkedId == R.id.rb_transporte_publico) {
                        currentTransportMode = "transit";
                    } else {
                        currentTransportMode = "walking";
                    }

                    // Si ya hay una ruta activa, la recalculamos
                    if (destinoActual != null && tituloDestinoActual != null) {
                        dibujarLineaHastaDestino(
                                tituloDestinoActual,
                                destinoActual.latitude,
                                destinoActual.longitude,
                                currentTransportMode
                        );
                    }
                }
            });
        }

        searchAdapter = new BuscadorMapaAdapter(requireContext(), listaTodosLosCentros);
        lvSearchResults.setAdapter(searchAdapter);

        // --- LÓGICA DEL BUSCADOR ---
        cardBuscadorFalso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pantallaBusquedaCompleta.setVisibility(View.VISIBLE);
                searchViewReal.requestFocus();

                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchViewReal.findFocus(), InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        btnCerrarBusqueda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cerrarPantallaBusqueda();
            }
        });

        searchViewReal.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchViewReal.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchAdapter.getFilter().filter(newText);
                return false;
            }
        });

        lvSearchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Centro centroSeleccionado = searchAdapter.getItem(position);

                if (centroSeleccionado != null) {
                    cerrarPantallaBusqueda();
                    dibujarLineaHastaDestino(
                            centroSeleccionado.getNombre(),
                            centroSeleccionado.getLatitud(),
                            centroSeleccionado.getLongitud(),
                            currentTransportMode
                    );
                }
            }
        });

        // --- MAPA E INSETS ---
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ViewCompat.setOnApplyWindowInsetsListener(view, new androidx.core.view.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) cardBuscadorFalso.getLayoutParams();
                mlp.topMargin = insets.top + (int) (16 * getResources().getDisplayMetrics().density);
                cardBuscadorFalso.setLayoutParams(mlp);

                headerBusqueda.setPadding(0, insets.top, 0, 0);

                return windowInsets;
            }
        });
    }

    private void cerrarPantallaBusqueda() {
        pantallaBusquedaCompleta.setVisibility(View.GONE);
        searchViewReal.setQuery("", false);
        searchViewReal.clearFocus();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchViewReal.getWindowToken(), 0);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.campusMap = googleMap;

        LatLng campusLocationEIB = new LatLng(43.265842, -2.940452);
        campusMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocationEIB, 15f));

        if (getArguments() != null && getArguments().containsKey("destino_nombre")) {
            tituloPendiente = getArguments().getString("destino_nombre");
            destinoPendiente = new LatLng(
                    getArguments().getDouble("destino_lat"),
                    getArguments().getDouble("destino_lng")
            );
        }

        comprobarPermisosDeUbicacion();
    }

    private void comprobarPermisosDeUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            activarUbicacionEnMapa();
            procesarRutaPendiente();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void procesarRutaPendiente() {
        if (destinoPendiente != null && tituloPendiente != null) {
            dibujarLineaHastaDestino(tituloPendiente, destinoPendiente.latitude, destinoPendiente.longitude, currentTransportMode);
            destinoPendiente = null;
            tituloPendiente = null;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void activarUbicacionEnMapa() {
        if (campusMap != null) {
            campusMap.setMyLocationEnabled(true);
            campusMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    public void dibujarLineaHastaDestino(String titulo, double latDestino, double lngDestino, String modoTransporte) {
        if (campusMap == null) return;

        // Guardamos el destino para posibles recálculos al cambiar de transporte
        destinoActual = new LatLng(latDestino, lngDestino);
        tituloDestinoActual = titulo;

        LatLng destino = new LatLng(latDestino, lngDestino);
        campusMap.clear();

        // Ocultar tarjeta hasta cargar nuevos datos
        cardInfoRutas.setVisibility(View.GONE);
        llListaInstrucciones.removeAllViews();

        campusMap.addMarker(new MarkerOptions().position(destino).title(titulo));
        campusMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destino, 14f));

        com.google.android.gms.location.FusedLocationProviderClient proveedorLocalizacion =
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity());

        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("UnigoDAS_Location", "Permisos denegados.");
                return;
            }

            proveedorLocalizacion.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(requireActivity(), new com.google.android.gms.tasks.OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng origen = new LatLng(location.getLatitude(), location.getLongitude());
                                obtenerRutaRealGoogle(origen, destino, modoTransporte);
                            } else {
                                Log.w("UnigoDAS_Location", "Ubicación null. Asegúrate de tener GPS encendido.");
                            }
                        }
                    })
                    .addOnFailureListener(requireActivity(), new com.google.android.gms.tasks.OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("UnigoDAS_Location", "Error obteniendo ubicación: " + e.getMessage());
                        }
                    });
        } catch (SecurityException e) {
            Log.e("UnigoDAS_Location", "Sin permisos: " + e.getMessage());
        }
    }

    private void obtenerRutaRealGoogle(final LatLng origen, final LatLng destino, final String modoTransporte) {
        String apiKey = BuildConfig.DIRECTIONS_API_KEY;

        Uri.Builder builderUrl = Uri.parse("https://maps.googleapis.com/maps/api/directions/json").buildUpon();
        builderUrl.appendQueryParameter("origin", origen.latitude + "," + origen.longitude);
        builderUrl.appendQueryParameter("destination", destino.latitude + "," + destino.longitude);
        builderUrl.appendQueryParameter("mode", modoTransporte);
        builderUrl.appendQueryParameter("key", apiKey);

        if ("transit".equals(modoTransporte)) {
            builderUrl.appendQueryParameter("departure_time", "now");
        }

        final String directionsApiUrl = builderUrl.build().toString();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(directionsApiUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int statusCode = connection.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder campusRouteResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            campusRouteResponse.append(line);
                        }
                        reader.close();

                        final JSONObject jsonResponse = new JSONObject(campusRouteResponse.toString());
                        final String apiStatus = jsonResponse.getString("status");

                        if ("OK".equals(apiStatus)) {
                            JSONArray routes = jsonResponse.getJSONArray("routes");
                            JSONObject route = routes.getJSONObject(0);

                            final List<SegmentoRuta> listaSegmentos = new ArrayList<>();
                            final List<LatLng> puntosTotalesParaCamara = new ArrayList<>();

                            if ("transit".equals(modoTransporte)) {
                                JSONArray legs = route.getJSONArray("legs");
                                JSONObject leg = legs.getJSONObject(0);
                                JSONArray steps = leg.getJSONArray("steps");

                                for (int i = 0; i < steps.length(); i++) {
                                    JSONObject step = steps.getJSONObject(i);
                                    String travelMode = step.getString("travel_mode");
                                    String points = step.getJSONObject("polyline").getString("points");
                                    List<LatLng> decodificados = decodificarPolyline(points);
                                    puntosTotalesParaCamara.addAll(decodificados);

                                    SegmentoRuta segmento = new SegmentoRuta();
                                    segmento.puntos = decodificados;
                                    segmento.esCaminando = "WALKING".equals(travelMode);
                                    segmento.duracion = step.getJSONObject("duration").getString("text");

                                    JSONObject startLoc = step.getJSONObject("start_location");
                                    segmento.puntoInicio = new LatLng(startLoc.getDouble("lat"), startLoc.getDouble("lng"));

                                    if (!segmento.esCaminando && step.has("transit_details")) {
                                        JSONObject transitDetails = step.getJSONObject("transit_details");
                                        JSONObject lineData = transitDetails.getJSONObject("line");

                                        segmento.paradaOrigen = transitDetails.getJSONObject("departure_stop").getString("name");
                                        segmento.paradaDestino = transitDetails.getJSONObject("arrival_stop").getString("name");
                                        segmento.direccion = transitDetails.optString("headsign", "Destino final");
                                        segmento.numParadas = transitDetails.optString("num_stops", "0");

                                        if (lineData.has("color")) {
                                            try {
                                                segmento.color = Color.parseColor(lineData.getString("color"));
                                            } catch (Exception e) { segmento.color = Color.RED; }
                                        } else {
                                            segmento.color = Color.RED;
                                        }

                                        String nombreVehiculo = "";
                                        if (lineData.has("vehicle") && lineData.getJSONObject("vehicle").has("name")) {
                                            nombreVehiculo = lineData.getJSONObject("vehicle").getString("name");
                                        }
                                        String nombreLinea = lineData.has("short_name") ? lineData.getString("short_name") : lineData.optString("name", "");
                                        segmento.tituloInstruccion = nombreVehiculo + " " + nombreLinea;
                                    } else {
                                        segmento.color = Color.GRAY;
                                        segmento.tituloInstruccion = "Andando";
                                    }
                                    listaSegmentos.add(segmento);
                                }
                            } else {
                                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                                String points = overviewPolyline.getString("points");
                                List<LatLng> decodificados = decodificarPolyline(points);
                                puntosTotalesParaCamara.addAll(decodificados);

                                SegmentoRuta segmentoUnico = new SegmentoRuta();
                                segmentoUnico.puntos = decodificados;
                                segmentoUnico.esCaminando = false;
                                segmentoUnico.color = "bicycling".equals(modoTransporte) ? Color.GREEN : Color.BLUE;
                                segmentoUnico.duracion = route.getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
                                listaSegmentos.add(segmentoUnico);
                            }

                            // --- VOLVEMOS AL HILO PRINCIPAL ---
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (campusMap != null && isAdded()) {
                                        llListaInstrucciones.removeAllViews();

                                        for (SegmentoRuta seg : listaSegmentos) {
                                            PolylineOptions opcionesLinea = new PolylineOptions()
                                                    .addAll(seg.puntos)
                                                    .width(12f)
                                                    .color(seg.color)
                                                    .geodesic(true);

                                            TextView tvInstruccion = new TextView(requireContext());
                                            tvInstruccion.setTextSize(14f);
                                            tvInstruccion.setPadding(0, 16, 0, 16);
                                            tvInstruccion.setTextColor(Color.DKGRAY);

                                            if (seg.esCaminando && "transit".equals(modoTransporte)) {
                                                List<PatternItem> patron = Arrays.asList(new Dot(), new Gap(15f));
                                                opcionesLinea.pattern(patron);
                                                tvInstruccion.setText("🚶 Caminar (" + seg.duracion + ")");

                                            } else if (!seg.esCaminando && "transit".equals(modoTransporte)) {
                                                campusMap.addMarker(new MarkerOptions()
                                                        .position(seg.puntoInicio)
                                                        .title(seg.tituloInstruccion)
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                                                String infoTransporte = "🚆 " + seg.tituloInstruccion + " (" + seg.duracion + ")\n" +
                                                        "↳ Dirección: " + seg.direccion + "\n" +
                                                        "↳ Desde: " + seg.paradaOrigen + "\n" +
                                                        "↳ Hasta: " + seg.paradaDestino + " (" + seg.numParadas + " paradas)";

                                                tvInstruccion.setText(infoTransporte);
                                                tvInstruccion.setTextColor(seg.color);
                                                tvInstruccion.setTypeface(null, Typeface.BOLD);
                                            } else {
                                                String emoji = "bicycling".equals(modoTransporte) ? "🚲 Bici " : "🚶 Caminar ";
                                                tvInstruccion.setText(emoji + "(" + seg.duracion + ")");
                                            }

                                            llListaInstrucciones.addView(tvInstruccion);
                                            campusMap.addPolyline(opcionesLinea);
                                        }

                                        if (!listaSegmentos.isEmpty()) {
                                            cardInfoRutas.setVisibility(View.VISIBLE);
                                        }

                                        if (!puntosTotalesParaCamara.isEmpty()) {
                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (LatLng point : puntosTotalesParaCamara) {
                                                builder.include(point);
                                            }
                                            campusMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                                        }
                                    }
                                }
                            });
                        } else {
                            Log.e("UnigoDAS_Net", "Error API Google (" + apiStatus + ")");
                        }
                    }
                } catch (Exception e) {
                    Log.e("UnigoDAS_Net", "Excepción en red: " + e.getMessage());
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        });
    }

    private List<LatLng> decodificarPolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }

    private void cargarDatosBuscador() {
        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        listaTodosLosCentros = dbHelper.obtenerTodosLosCentros();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchViewReal = null;
        campusMap = null;
    }

    // =======================================================================
    // Adaptador Personalizado (Buscador Inteligente)
    // =======================================================================
    private class BuscadorMapaAdapter extends BaseAdapter implements Filterable {
        private Context context;
        private List<Centro> listaOriginal;
        private List<Centro> listaFiltrada;
        private CentroFilter filtro;

        public BuscadorMapaAdapter(Context context, List<Centro> lista) {
            this.context = context;
            this.listaOriginal = new ArrayList<>(lista);
            this.listaFiltrada = new ArrayList<>(lista);
        }

        @Override
        public int getCount() {
            return listaFiltrada.size();
        }

        @Override
        public Centro getItem(int position) {
            return listaFiltrada.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(getItem(position).getNombre());
            return convertView;
        }

        @Override
        public Filter getFilter() {
            if (filtro == null) {
                filtro = new CentroFilter();
            }
            return filtro;
        }

        private class CentroFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Centro> filtrados = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filtrados.addAll(listaOriginal);
                } else {
                    String query = constraint.toString().toLowerCase().trim();
                    for (Centro c : listaOriginal) {
                        if (c.getNombre().toLowerCase().contains(query) ||
                                (c.getDescripcion() != null && c.getDescripcion().toLowerCase().contains(query)) ||
                                c.getUniversidad().toLowerCase().contains(query)) {
                            filtrados.add(c);
                        }
                    }
                }
                results.values = filtrados;
                results.count = filtrados.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                listaFiltrada.clear();
                if (results.values != null) {
                    listaFiltrada.addAll((List<Centro>) results.values);
                }
                notifyDataSetChanged();
            }
        }
    }

    // =======================================================================
    // Clase auxiliar para guardar los trozos de la ruta
    // =======================================================================
    private static class SegmentoRuta {
        List<LatLng> puntos;
        int color;
        boolean esCaminando;
        String tituloInstruccion;
        LatLng puntoInicio;

        String duracion;
        String paradaOrigen;
        String paradaDestino;
        String direccion;
        String numParadas;
    }
}