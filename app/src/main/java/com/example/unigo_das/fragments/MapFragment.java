package com.example.unigo_das.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.example.unigo_das.db.DataBaseHelper; // NUEVO IMPORT
import com.example.unigo_das.item.Centro;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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

    private List<Centro> listaTodosLosCentros;
    private BuscadorMapaAdapter searchAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new androidx.activity.result.ActivityResultCallback<java.util.Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(java.util.Map<String, Boolean> result) {
                        Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                        if ((fineLocationGranted != null && fineLocationGranted) ||
                                (coarseLocationGranted != null && coarseLocationGranted)) {
                            activarUbicacionEnMapa();
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

        searchAdapter = new BuscadorMapaAdapter(requireContext(), listaTodosLosCentros);
        lvSearchResults.setAdapter(searchAdapter);

        // --- LÓGICA DE ABRIR/CERRAR LA PANTALLA TIPO GOOGLE MAPS ---

        // 1. Al tocar el botón del mapa, abrimos la pantalla completa
        cardBuscadorFalso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pantallaBusquedaCompleta.setVisibility(View.VISIBLE);
                searchViewReal.requestFocus(); // Foco al buscador real

                // Forzamos que se abra el teclado
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchViewReal.findFocus(), InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        // 2. Al tocar la flecha de volver, cerramos la pantalla
        btnCerrarBusqueda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cerrarPantallaBusqueda();
            }
        });

        // 3. El buscador filtra la lista en tiempo real
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

        // 4. Al tocar un centro de la lista, cerramos pantalla y hacemos la ruta
        lvSearchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Centro centroSeleccionado = searchAdapter.getItem(position);

                if (centroSeleccionado != null) {
                    cerrarPantallaBusqueda();
                    dibujarLineaHastaDestino(centroSeleccionado.getNombre(), centroSeleccionado.getLatitud(), centroSeleccionado.getLongitud());
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

                // Margen del botón flotante para que no lo tape la barra de estado
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) cardBuscadorFalso.getLayoutParams();
                mlp.topMargin = insets.top + (int) (16 * getResources().getDisplayMetrics().density);
                cardBuscadorFalso.setLayoutParams(mlp);

                // Padding de la pantalla blanca para que el buscador no se meta debajo de la hora/batería
                headerBusqueda.setPadding(0, insets.top, 0, 0);

                return windowInsets;
            }
        });
    }

    private void cerrarPantallaBusqueda() {
        pantallaBusquedaCompleta.setVisibility(View.GONE);
        searchViewReal.setQuery("", false); // Limpiamos el texto
        searchViewReal.clearFocus();

        // Escondemos el teclado
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchViewReal.getWindowToken(), 0);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.campusMap = googleMap;

        LatLng campusLocationEIB = new LatLng(43.2638, -2.9511);
        campusMap.addMarker(new MarkerOptions().position(campusLocationEIB).title("Escuela de Ingeniería de Bilbao (EIB)"));
        campusMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocationEIB, 16f));

        comprobarPermisosDeUbicacion();
    }

    private void comprobarPermisosDeUbicacion() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            activarUbicacionEnMapa();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressWarnings("MissingPermission")
    private void activarUbicacionEnMapa() {
        if (campusMap != null) {
            campusMap.setMyLocationEnabled(true);
            campusMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    public void dibujarLineaHastaDestino(String titulo, double latDestino, double lngDestino) {
        if (campusMap == null) return;

        LatLng destino = new LatLng(latDestino, lngDestino);
        campusMap.clear();

        campusMap.addMarker(new MarkerOptions().position(destino).title(titulo));

        @SuppressWarnings("MissingPermission")
        Location miUbicacion = campusMap.getMyLocation();

        if (miUbicacion != null) {
            LatLng origen = new LatLng(miUbicacion.getLatitude(), miUbicacion.getLongitude());
            obtenerRutaRealGoogle(origen, destino);
        }

        campusMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destino, 14f));
    }

    private void obtenerRutaRealGoogle(LatLng origen, LatLng destino) {
        String apiKey = "TU_CLAVE_API_DE_GOOGLE_AQUI"; // <-- RECUERDA PONER TU CLAVE
        String urlDirecciones = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=walking" +
                "&key=" + apiKey;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlDirecciones);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray routes = jsonResponse.getJSONArray("routes");

                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String polylineCodificada = overviewPolyline.getString("points");

                        List<LatLng> puntosRuta = decodificarPolyline(polylineCodificada);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                PolylineOptions rutaGoogle = new PolylineOptions()
                                        .addAll(puntosRuta)
                                        .width(12f)
                                        .color(Color.BLUE)
                                        .geodesic(true);
                                campusMap.addPolyline(rutaGoogle);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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

    // NUEVO MÉTODO: Ahora extraemos la información dinámicamente de la base de datos
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
            textView.setText(getItem(position).getNombre()); // Mostrar el nombre del centro en la lista
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
                        // ACTUALIZADO: Filtramos usando el nuevo campo "descripcion"
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
}