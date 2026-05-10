package com.example.unigo_das.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResultCallback;
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
import com.example.unigo_das.item.Parada;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private ImageView btnQuitarRuta;
    private ImageView btnFiltrosTransporte;
    private SearchView searchViewReal;
    private ListView lvSearchResults;
    private View cardTransporte;
    private View cardInfoRutas;
    private LinearLayout llListaInstrucciones;

    // --- NUEVO: Botón flotante para paradas ---
    private FloatingActionButton fabCapasTransporte;
    private List<Marker> marcadoresParadasActivos = new ArrayList<>();
    private int capaSeleccionadaIndex = 0; // 0 = Ocultar

    private List<Centro> listaTodosLosCentros;
    private BuscadorMapaAdapter searchAdapter;

    // Variables de estado
    private String currentTransportMode = "walking";
    private LatLng destinoPendiente = null;
    private String tituloPendiente = null;
    private LatLng destinoActual = null;
    private String tituloDestinoActual = null;

    // Filtros de transporte público
    private long timestampSalidaPersonalizado = 0;
    private boolean usarAutobus = true;
    private boolean usarMetroTren = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> result) {
                        Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                        if ((fineLocationGranted != null && fineLocationGranted) ||
                                (coarseLocationGranted != null && coarseLocationGranted)) {
                            activarUbicacionEnMapa();
                            procesarRutaPendiente();
                        }
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
        View capaClickBuscador = view.findViewById(R.id.capa_click_buscador);

        cardTransporte = view.findViewById(R.id.card_transporte);
        if (cardTransporte != null) {
            cardTransporte.setVisibility(View.GONE);
        }

        cardInfoRutas = view.findViewById(R.id.card_info_rutas);
        llListaInstrucciones = view.findViewById(R.id.ll_lista_instrucciones);
        btnQuitarRuta = view.findViewById(R.id.btn_quitar_ruta);
        btnFiltrosTransporte = view.findViewById(R.id.btn_filtros_transporte);

        btnQuitarRuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                limpiarRutaActiva();
            }
        });

        btnFiltrosTransporte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarFiltrosTransporte();
            }
        });

        // --- FAB de Capas de Paradas ---
        fabCapasTransporte = view.findViewById(R.id.fab_capas_transporte);
        if (fabCapasTransporte != null) {
            fabCapasTransporte.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarMenuCapasParadas();
                }
            });
        }

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

                    if (destinoActual != null && tituloDestinoActual != null) {
                        dibujarLineaHastaDestino(tituloDestinoActual, destinoActual.latitude, destinoActual.longitude, currentTransportMode);
                    }
                }
            });
        }

        searchAdapter = new BuscadorMapaAdapter(requireContext(), listaTodosLosCentros);
        lvSearchResults.setAdapter(searchAdapter);

        capaClickBuscador.setOnClickListener(new View.OnClickListener() {
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
            public void onItemClick(AdapterView<?> parent, View view1, int position, long id) {
                Centro centroSeleccionado = searchAdapter.getItem(position);
                if (centroSeleccionado != null) {
                    cerrarPantallaBusqueda();
                    dibujarLineaHastaDestino(centroSeleccionado.getNombre(), centroSeleccionado.getLatitud(), centroSeleccionado.getLongitud(), currentTransportMode);
                }
            }
        });

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
            destinoPendiente = new LatLng(getArguments().getDouble("destino_lat"), getArguments().getDouble("destino_lng"));
        }
        comprobarPermisosDeUbicacion();
    }

    private void comprobarPermisosDeUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            activarUbicacionEnMapa();
            procesarRutaPendiente();
        } else {
            locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
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

        // Ocultamos el botón de paradas libres cuando estamos ruteando
        if (fabCapasTransporte != null) {
            fabCapasTransporte.setVisibility(View.GONE);
        }
        limpiarMarcadoresParadasLibres(); // Limpiamos cualquier estación suelta que estuviera dibujada

        if ("transit".equals(modoTransporte) && btnFiltrosTransporte != null) {
            btnFiltrosTransporte.setVisibility(View.VISIBLE);
        } else if (btnFiltrosTransporte != null) {
            btnFiltrosTransporte.setVisibility(View.GONE);
        }

        if (cardTransporte != null) {
            cardTransporte.setVisibility(View.VISIBLE);
        }

        destinoActual = new LatLng(latDestino, lngDestino);
        tituloDestinoActual = titulo;
        LatLng destino = new LatLng(latDestino, lngDestino);
        campusMap.clear();

        cardInfoRutas.setVisibility(View.GONE);
        llListaInstrucciones.removeAllViews();

        campusMap.addMarker(new MarkerOptions().position(destino).title(titulo));
        campusMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destino, 14f));

        FusedLocationProviderClient proveedorLocalizacion = LocationServices.getFusedLocationProviderClient(requireActivity());
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            proveedorLocalizacion.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                obtenerRutaRealGoogle(new LatLng(location.getLatitude(), location.getLongitude()), destino, modoTransporte);
                            }
                        }
                    })
                    .addOnFailureListener(requireActivity(), new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("UnigoDAS", e.getMessage());
                        }
                    });
        } catch (SecurityException e) {
            Log.e("UnigoDAS", e.getMessage());
        }
    }

    private void obtenerRutaRealGoogle(final LatLng origen, final LatLng destino, final String modoTransporte) {
        String apiKey = BuildConfig.DIRECTIONS_API_KEY;
        String idiomaActual = java.util.Locale.getDefault().getLanguage();

        Uri.Builder builderUrl = Uri.parse("https://maps.googleapis.com/maps/api/directions/json").buildUpon();
        builderUrl.appendQueryParameter("origin", origen.latitude + "," + origen.longitude);
        builderUrl.appendQueryParameter("destination", destino.latitude + "," + destino.longitude);
        builderUrl.appendQueryParameter("mode", modoTransporte);
        builderUrl.appendQueryParameter("key", apiKey);
        builderUrl.appendQueryParameter("language", idiomaActual);

        if ("transit".equals(modoTransporte)) {
            if (timestampSalidaPersonalizado > 0) {
                builderUrl.appendQueryParameter("departure_time", String.valueOf(timestampSalidaPersonalizado));
            } else {
                builderUrl.appendQueryParameter("departure_time", "now");
            }

            List<String> modosPermitidos = new ArrayList<>();
            if (usarAutobus) modosPermitidos.add("bus");
            if (usarMetroTren) {
                modosPermitidos.add("subway");
                modosPermitidos.add("train");
                modosPermitidos.add("tram");
            }
            if (!modosPermitidos.isEmpty() && modosPermitidos.size() < 4) {
                builderUrl.appendQueryParameter("transit_mode", android.text.TextUtils.join("|", modosPermitidos));
            }
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

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if ("OK".equals(jsonResponse.getString("status"))) {
                            JSONObject route = jsonResponse.getJSONArray("routes").getJSONObject(0);
                            final List<SegmentoRuta> listaSegmentos = new ArrayList<>();
                            final List<LatLng> puntos = new ArrayList<>();

                            if ("transit".equals(modoTransporte)) {
                                JSONArray steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
                                for (int i = 0; i < steps.length(); i++) {
                                    JSONObject step = steps.getJSONObject(i);
                                    List<LatLng> decodificados = decodificarPolyline(step.getJSONObject("polyline").getString("points"));
                                    puntos.addAll(decodificados);

                                    SegmentoRuta segmento = new SegmentoRuta();
                                    segmento.puntos = decodificados;
                                    segmento.esCaminando = "WALKING".equals(step.getString("travel_mode"));
                                    segmento.duracion = step.getJSONObject("duration").getString("text");
                                    segmento.puntoInicio = new LatLng(step.getJSONObject("start_location").getDouble("lat"), step.getJSONObject("start_location").getDouble("lng"));

                                    if (!segmento.esCaminando && step.has("transit_details")) {
                                        JSONObject td = step.getJSONObject("transit_details");
                                        segmento.paradaOrigen = td.getJSONObject("departure_stop").getString("name");
                                        segmento.paradaDestino = td.getJSONObject("arrival_stop").getString("name");
                                        segmento.direccion = td.optString("headsign", "Destino final");
                                        segmento.numParadas = td.optString("num_stops", "0");
                                        if (td.has("departure_time")) segmento.horaSalida = td.getJSONObject("departure_time").getString("text");
                                        if (td.has("arrival_time")) segmento.horaLlegada = td.getJSONObject("arrival_time").getString("text");

                                        JSONObject lineData = td.getJSONObject("line");
                                        segmento.color = lineData.has("color") ? Color.parseColor(lineData.getString("color")) : Color.RED;
                                        String vName = (lineData.has("vehicle") && lineData.getJSONObject("vehicle").has("name")) ? lineData.getJSONObject("vehicle").getString("name") : "";
                                        segmento.tituloInstruccion = vName + " " + (lineData.has("short_name") ? lineData.getString("short_name") : lineData.optString("name", ""));
                                    } else {
                                        segmento.color = Color.GRAY;
                                        segmento.tituloInstruccion = "Andando";
                                    }
                                    listaSegmentos.add(segmento);
                                }
                            } else {
                                List<LatLng> decodificados = decodificarPolyline(route.getJSONObject("overview_polyline").getString("points"));
                                puntos.addAll(decodificados);
                                SegmentoRuta sr = new SegmentoRuta();
                                sr.puntos = decodificados;
                                sr.esCaminando = false;
                                sr.color = "bicycling".equals(modoTransporte) ? Color.GREEN : Color.BLUE;
                                sr.duracion = route.getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
                                listaSegmentos.add(sr);
                            }

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (campusMap != null && isAdded()) {
                                        llListaInstrucciones.removeAllViews();
                                        for (SegmentoRuta seg : listaSegmentos) {
                                            PolylineOptions opt = new PolylineOptions().addAll(seg.puntos).width(12f).color(seg.color).geodesic(true);
                                            TextView tv = new TextView(requireContext());
                                            tv.setTextSize(14f); tv.setPadding(0, 16, 0, 16); tv.setTextColor(Color.DKGRAY);

                                            if (seg.esCaminando && "transit".equals(modoTransporte)) {
                                                opt.pattern(Arrays.asList(new Dot(), new Gap(15f)));
                                                tv.setText("🚶 Caminar (" + seg.duracion + ")");
                                            } else if (!seg.esCaminando && "transit".equals(modoTransporte)) {
                                                campusMap.addMarker(new MarkerOptions().position(seg.puntoInicio).title(seg.tituloInstruccion).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                                String info = "🚆 " + seg.tituloInstruccion + " (" + seg.duracion + ")\n";
                                                if (seg.horaSalida != null) info += "🕒 " + seg.horaSalida + " - " + seg.horaLlegada + "\n";
                                                info += "↳ Dirección: " + seg.direccion + "\n↳ Desde: " + seg.paradaOrigen + "\n↳ Hasta: " + seg.paradaDestino + " (" + seg.numParadas + " paradas)";
                                                tv.setText(info); tv.setTextColor(seg.color); tv.setTypeface(null, Typeface.BOLD);
                                            } else {
                                                tv.setText(("bicycling".equals(modoTransporte) ? "🚲 Bici " : "🚶 Caminar ") + "(" + seg.duracion + ")");
                                            }
                                            llListaInstrucciones.addView(tv);
                                            campusMap.addPolyline(opt);
                                        }
                                        if (!listaSegmentos.isEmpty()) {
                                            cardInfoRutas.setVisibility(View.VISIBLE);
                                        }
                                        if (!puntos.isEmpty()) {
                                            LatLngBounds.Builder b = new LatLngBounds.Builder();
                                            for (LatLng p : puntos) {
                                                b.include(p);
                                            }
                                            campusMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
                                        }
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("UnigoDAS", e.getMessage());
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
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            lat += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            shift = 0; result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            lng += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    private void limpiarRutaActiva() {
        if (campusMap != null) {
            campusMap.clear();
        }
        cardInfoRutas.setVisibility(View.GONE);
        if (cardTransporte != null) {
            cardTransporte.setVisibility(View.GONE);
        }
        llListaInstrucciones.removeAllViews();
        destinoActual = null;
        tituloDestinoActual = null;
        timestampSalidaPersonalizado = 0;

        // Volver a mostrar el botón de paradas libres y redibujar si había alguna seleccionada
        if (fabCapasTransporte != null) {
            fabCapasTransporte.setVisibility(View.VISIBLE);
        }
        if (capaSeleccionadaIndex > 0) {
            redibujarCapaParadas(capaSeleccionadaIndex);
        }
    }

    // =======================================================================
    // CAPAS DE PARADAS DE TRANSPORTE (SIN RUTA ACTIVA)
    // =======================================================================
    private void mostrarMenuCapasParadas() {
        String[] opciones = {"Ocultar paradas", "Bicicleta", "Bilbobus", "Euskotren", "Metro", "Renfe", "Tranvía"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Mostrar paradas en el mapa");
        builder.setSingleChoiceItems(opciones, capaSeleccionadaIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                capaSeleccionadaIndex = which;
                redibujarCapaParadas(which);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void redibujarCapaParadas(int index) {
        limpiarMarcadoresParadasLibres();
        if (index == 0) return; // Ocultar

        String[] tiposDB = {"", "Bicicleta", "Bilbobus", "Euskotren", "Metro", "Renfe", "Tranvía"};
        String tipoSeleccionado = tiposDB[index];

        // Asignar color al pinche según el transporte
        float colorPinche = BitmapDescriptorFactory.HUE_RED;
        switch (tipoSeleccionado) {
            case "Bicicleta": colorPinche = BitmapDescriptorFactory.HUE_GREEN; break;
            case "Bilbobus": colorPinche = BitmapDescriptorFactory.HUE_RED; break;
            case "Euskotren": colorPinche = BitmapDescriptorFactory.HUE_BLUE; break;
            case "Metro": colorPinche = BitmapDescriptorFactory.HUE_ORANGE; break;
            case "Renfe": colorPinche = BitmapDescriptorFactory.HUE_MAGENTA; break;
            case "Tranvía": colorPinche = BitmapDescriptorFactory.HUE_CYAN; break;
        }

        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        List<Parada> paradas = dbHelper.obtenerParadasPorTipo(tipoSeleccionado);

        if (paradas != null && !paradas.isEmpty() && campusMap != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Parada p : paradas) {
                LatLng pos = new LatLng(p.getLatitud(), p.getLongitud());
                Marker m = campusMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(p.getNombre())
                        .snippet(p.getTipo())
                        .icon(BitmapDescriptorFactory.defaultMarker(colorPinche)));
                marcadoresParadasActivos.add(m);
                builder.include(pos);
            }
            // Movemos la cámara para englobar todas las paradas dibujadas
            campusMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        }
    }

    private void limpiarMarcadoresParadasLibres() {
        for (Marker m : marcadoresParadasActivos) {
            m.remove();
        }
        marcadoresParadasActivos.clear();
    }

    // =======================================================================
    // FILTROS DE RUTA (DATEPICKER Y TIMEPICKER)
    // =======================================================================
    private void mostrarFiltrosTransporte() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Opciones de Transporte Público");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        TextView tvTiempo = new TextView(requireContext());
        tvTiempo.setText("Salida (Máx. 14 días):");
        tvTiempo.setTypeface(null, Typeface.BOLD);
        tvTiempo.setTextColor(Color.DKGRAY);
        tvTiempo.setPadding(0, 0, 0, 16);
        layout.addView(tvTiempo);

        LinearLayout layoutFechaHora = new LinearLayout(requireContext());
        layoutFechaHora.setOrientation(LinearLayout.HORIZONTAL);
        layoutFechaHora.setWeightSum(2f);

        final Button btnFecha = new Button(requireContext());
        LinearLayout.LayoutParams pF = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pF.setMarginEnd(8);
        btnFecha.setLayoutParams(pF);

        final Button btnHora = new Button(requireContext());
        LinearLayout.LayoutParams pH = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pH.setMarginStart(8);
        btnHora.setLayoutParams(pH);

        final java.text.SimpleDateFormat formatoFecha = new java.text.SimpleDateFormat("dd MMM", Locale.getDefault());
        final java.text.SimpleDateFormat formatoHora = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        final Calendar cSel = Calendar.getInstance();

        if (timestampSalidaPersonalizado == 0) {
            btnFecha.setText("Hoy");
            btnHora.setText("Ahora");
        } else {
            cSel.setTimeInMillis(timestampSalidaPersonalizado * 1000L);
            btnFecha.setText(formatoFecha.format(cSel.getTime()));
            btnHora.setText(formatoHora.format(cSel.getTime()));
        }

        btnFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dp = new DatePickerDialog(requireContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        cSel.set(year, month, dayOfMonth);
                        btnFecha.setText(formatoFecha.format(cSel.getTime()));
                        if (btnHora.getText().toString().equals("Ahora")) {
                            btnHora.setText(formatoHora.format(cSel.getTime()));
                        }
                    }
                }, cSel.get(Calendar.YEAR), cSel.get(Calendar.MONTH), cSel.get(Calendar.DAY_OF_MONTH));
                dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                dp.getDatePicker().setMaxDate(System.currentTimeMillis() + (14L * 24L * 60L * 60L * 1000L));
                dp.show();
            }
        });

        btnHora.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog tp = new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        cSel.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        cSel.set(Calendar.MINUTE, minute);
                        cSel.set(Calendar.SECOND, 0);
                        Calendar ahora = Calendar.getInstance();
                        if (cSel.get(Calendar.YEAR) == ahora.get(Calendar.YEAR) &&
                                cSel.get(Calendar.DAY_OF_YEAR) == ahora.get(Calendar.DAY_OF_YEAR) &&
                                cSel.before(ahora)) {
                            cSel.add(Calendar.DAY_OF_YEAR, 1);
                            btnFecha.setText(formatoFecha.format(cSel.getTime()));
                        }
                        btnHora.setText(formatoHora.format(cSel.getTime()));
                        if (btnFecha.getText().toString().equals("Hoy")) {
                            btnFecha.setText(formatoFecha.format(cSel.getTime()));
                        }
                    }
                }, cSel.get(Calendar.HOUR_OF_DAY), cSel.get(Calendar.MINUTE), true);
                tp.show();
            }
        });

        layoutFechaHora.addView(btnFecha);
        layoutFechaHora.addView(btnHora);
        layout.addView(layoutFechaHora);

        TextView tvMedios = new TextView(requireContext());
        tvMedios.setText("\nMedios preferidos:");
        tvMedios.setTypeface(null, Typeface.BOLD);
        tvMedios.setTextColor(Color.DKGRAY);
        layout.addView(tvMedios);

        final CheckBox cbBus = new CheckBox(requireContext());
        cbBus.setText("Autobús");
        cbBus.setChecked(usarAutobus);
        layout.addView(cbBus);

        final CheckBox cbMetro = new CheckBox(requireContext());
        cbMetro.setText("Metro / Tren / Tranvía");
        cbMetro.setChecked(usarMetroTren);
        layout.addView(cbMetro);

        builder.setView(layout);
        builder.setPositiveButton("Aplicar y Buscar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                usarAutobus = cbBus.isChecked();
                usarMetroTren = cbMetro.isChecked();
                if (!usarAutobus && !usarMetroTren) {
                    usarAutobus = true;
                    usarMetroTren = true;
                }
                if (!btnFecha.getText().toString().equals("Hoy") || !btnHora.getText().toString().equals("Ahora")) {
                    timestampSalidaPersonalizado = cSel.getTimeInMillis() / 1000L;
                } else {
                    timestampSalidaPersonalizado = 0;
                }
                if (destinoActual != null && "transit".equals(currentTransportMode)) {
                    dibujarLineaHastaDestino(tituloDestinoActual, destinoActual.latitude, destinoActual.longitude, currentTransportMode);
                }
            }
        });

        builder.setNeutralButton("Restablecer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                timestampSalidaPersonalizado = 0;
                usarAutobus = true;
                usarMetroTren = true;
                if (destinoActual != null && "transit".equals(currentTransportMode)) {
                    dibujarLineaHastaDestino(tituloDestinoActual, destinoActual.latitude, destinoActual.longitude, currentTransportMode);
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
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
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(getItem(position).getNombre());
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
        String horaSalida;
        String horaLlegada;
    }
}