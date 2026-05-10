package com.example.unigo_das.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.unigo_das.R;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.fragments.MapFragment;
import com.example.unigo_das.fragments.SchoolFragment;
import com.example.unigo_das.fragments.SettingsFragment;
import com.example.unigo_das.fragments.WeatherFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private FragmentManager fm;
    private WindowInsetsControllerCompat insetsController;
    private static final String KEY_SELECTED_TAB = "selected_tab_id";

    private Bundle mapArgs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Forzar actualización de textos en BD según el idioma actual
        inicializarCampus();
        inicializarParadasTransporte();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        fm = getSupportFragmentManager();

        // 1. Establecer el listener PRIMERO
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                navegarA(item.getItemId());
                return true;
            }
        });

        // 2. Manejar el estado inicial o la recreación
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        } else {
            int selectedId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_map);
            // Esto dispara el listener que llama a navegarA() automáticamente
            bottomNav.setSelectedItemId(selectedId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            outState.putInt(KEY_SELECTED_TAB, bottomNav.getSelectedItemId());
        }
    }

    private void navegarA(int itemId) {
        FragmentTransaction transaction = fm.beginTransaction();
        String tag = "";
        Fragment targetFragment = null;

        // Identificamos el tag según la opción seleccionada
        if (itemId == R.id.nav_map) tag = "map";
        else if (itemId == R.id.nav_school) tag = "school";
        else if (itemId == R.id.nav_weather) tag = "weather";
        else if (itemId == R.id.nav_settings) tag = "settings";

        // Buscamos si el fragmento ya existe en la memoria
        targetFragment = fm.findFragmentByTag(tag);

        // Ocultamos todos los fragmentos activos
        for (Fragment frag : fm.getFragments()) {
            if (frag.isVisible()) {
                transaction.hide(frag);
            }
        }

        // Si NO existe, lo instanciamos y lo AGREGAMOS (add en vez de replace)
        if (targetFragment == null) {
            if (itemId == R.id.nav_map) {
                targetFragment = new MapFragment();
                // Si es la primera vez que creamos el mapa y hay una ruta pendiente:
                if (mapArgs != null) {
                    targetFragment.setArguments(mapArgs);
                    mapArgs = null;
                }
            } else if (itemId == R.id.nav_school) {
                targetFragment = new SchoolFragment();
            } else if (itemId == R.id.nav_weather) {
                targetFragment = new WeatherFragment();
            } else if (itemId == R.id.nav_settings) {
                targetFragment = new SettingsFragment();
            }
            transaction.add(R.id.fragment_container, targetFragment, tag);
        }
        // Si YA EXISTE, simplemente lo MOSTRAMOS
        else {
            transaction.show(targetFragment);

            // Si volvemos al mapa existente y hay una petición de ruta pendiente:
            if (itemId == R.id.nav_map && mapArgs != null) {
                ((MapFragment) targetFragment).dibujarLineaHastaDestino(
                        mapArgs.getString("destino_nombre"),
                        mapArgs.getDouble("destino_lat"),
                        mapArgs.getDouble("destino_lng"),
                        "walking" // Por defecto en walking
                );
                mapArgs = null; // Limpiamos tras consumir
            }
        }

        syncStatusBar(itemId);
        transaction.commit();
    }

    private void syncStatusBar(int itemId) {
        if (itemId == R.id.nav_map) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            if (insetsController != null) insetsController.setAppearanceLightStatusBars(true);
        } else {
            getWindow().setStatusBarColor(Color.parseColor("#333333"));
            if (insetsController != null) insetsController.setAppearanceLightStatusBars(false);
        }
    }

    public void irRutaEnMapa(String nombreCentro, double latDestino, double lngDestino) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 1. Guardamos la petición de ruta
        mapArgs = new Bundle();
        mapArgs.putString("destino_nombre", nombreCentro);
        mapArgs.putDouble("destino_lat", latDestino);
        mapArgs.putDouble("destino_lng", lngDestino);

        // 2. Si ya estamos en la pestaña del mapa, lo dibujamos directamente
        if (bottomNav.getSelectedItemId() == R.id.nav_map) {
            MapFragment mapFragment = (MapFragment) fm.findFragmentByTag("map");
            if (mapFragment != null) {
                mapFragment.dibujarLineaHastaDestino(nombreCentro, latDestino, lngDestino, "walking");
                mapArgs = null; // Lo limpiamos para que no lo repita
            }
        } else {
            // 3. Si estamos en otra pestaña, forzamos el cambio al mapa.
            // Esto dispara el Listener -> navegarA() -> Muestra el mapa y detecta el mapArgs
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }

    private void inicializarCampus() {
        DataBaseHelper dbHelper = new DataBaseHelper(this);
        dbHelper.insertarCentro("EHU_345", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Sede principal...", "San Mamés", 43.26233, -2.94840);
        dbHelper.insertarCentro("EHU_363", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Centro especializado...", "San Mamés", 43.26332, -2.95035);
        dbHelper.insertarCentro("EHU_364", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Instalaciones...", "San Mamés", 43.2638, -2.9511);
        dbHelper.insertarCentro("EHU_350", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Escuela técnica...", "Portugalete", 43.32682, -3.02284);
        dbHelper.insertarCentro("EHU_320", getString(R.string.centro_ehu_bellas_artes), "UPV/EHU", "Facultad...", "Leioa", 43.33142, -2.97310);
        dbHelper.insertarCentro("EHU_310", getString(R.string.centro_ehu_ciencia_tecnologia), "UPV/EHU", "Centro de referencia...", "Leioa", 43.33082, -2.97000);
        dbHelper.insertarCentro("EHU_323", getString(R.string.centro_ehu_ciencias_sociales_comunicacion), "UPV/EHU", "Campus centrado...", "Leioa", 43.33102, -2.96741);
        dbHelper.insertarCentro("EHU_324", getString(R.string.centro_ehu_derecho), "UPV/EHU", "Facultad especializada...", "Leioa", 43.33097983602023, -2.965655349095869);
        dbHelper.insertarCentro("EHU_321", getString(R.string.centro_ehu_economia_empresa), "UPV/EHU", "Principal centro...", "Sarriko", 43.27331543973917, -2.9583611657702416);
        dbHelper.insertarCentro("EHU_351", getString(R.string.centro_ehu_economia_empresa_elcano), "UPV/EHU", "Escuela universitaria...", "Bilbao Centro", 43.26010899657177, -2.933159441081518);
        dbHelper.insertarCentro("EHU_354", getString(R.string.centro_ehu_educacion_bilbao), "UPV/EHU", "Centro dedicado...", "Leioa", 43.3330960679207, -2.9727070307354544);
        dbHelper.insertarCentro("EHU_327", getString(R.string.centro_ehu_medicina_enfermeria), "UPV/EHU", "Facultad dedicada...", "Leioa", 43.32975437145838, -2.9655047077553744);
        dbHelper.insertarCentro("EHU_352", getString(R.string.centro_ehu_medicina_enfermeria), "UPV/EHU", "Instalaciones...", "Leioa", 43.32975437145838, -2.9655047077553744);
        dbHelper.insertarCentro("EHU_332", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Centro hospitalario...", "Galdakao", 43.223510878386605, -2.817928390528635);
        dbHelper.insertarCentro("EHU_328", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Unidad docente...", "Basurto", 43.26158155692848, -2.952932217865744);
        dbHelper.insertarCentro("EHU_329", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Formación práctica...", "Cruces", 43.28217570183873, -2.9841267313810045);
        dbHelper.insertarCentro("EHU_EXP", getString(R.string.centro_ehu_aulas_experiencia), "UPV/EHU", "Espacio universitario...", "Casco Viejo", 43.25780220598689, -2.923145862870646);

        dbHelper.insertarCentro("MU_BBF_EMP", getString(R.string.centro_mu_bbf_empresariales), "Mondragon", "Ecosistema...", "Uribitarte", 43.26444212526109, -2.9271480629194784);
        dbHelper.insertarCentro("MU_BBF_LEINN", getString(R.string.centro_mu_bbf_leinn), "Mondragon", "Centro especializado...", "Uribitarte", 43.26444212526109, -2.9271480629194784);
        dbHelper.insertarCentro("MU_AS_POL", getString(R.string.centro_mu_as_politecnica), "Mondragon", "Escuela politécnica...", "Zorrotzaurre", 43.27214942309993, -2.9638728246668524);
        dbHelper.insertarCentro("MU_AS_HUM", getString(R.string.centro_mu_as_humanidades), "Mondragon", "Facultad de humanidades...", "Zorrotzaurre", 43.27214942309993, -2.9638728246668524);

        dbHelper.insertarCentro("DEU_DBS", getString(R.string.centro_deusto_business), "Deusto", "Histórica escuela...", "Deusto", 43.270489177414404, -2.93751635987864);
        dbHelper.insertarCentro("DEU_DER", getString(R.string.centro_deusto_derecho), "Deusto", "Centro de referencia...", "Deusto", 43.270489177414404, -2.93751635987864);
        dbHelper.insertarCentro("DEU_CSH", getString(R.string.centro_deusto_sociales_humanas), "Deusto", "Facultad dedicada...", "Deusto", 43.270489177414404, -2.93751635987864);
        dbHelper.insertarCentro("DEU_ING", getString(R.string.centro_deusto_ingenieria), "Deusto", "Campus tecnológico...", "Deusto", 43.270489177414404, -2.93751635987864);
        dbHelper.insertarCentro("DEU_EDU", getString(R.string.centro_deusto_educacion_deporte), "Deusto", "Formación superior...", "Deusto", 43.270489177414404, -2.93751635987864);
        dbHelper.insertarCentro("DEU_SAL", getString(R.string.centro_deusto_salud), "Deusto", "Nueva facultad...", "Deusto", 43.270489177414404, -2.93751635987864);
        dbHelper.insertarCentro("DEU_CSC", getString(R.string.centro_deusto_sociales_comunicacion), "Deusto", "Especialidad en comunicación...", "Deusto", 43.270489177414404, -2.93751635987864);
    }

    private void inicializarParadasTransporte() {
        DataBaseHelper dbHelper = new DataBaseHelper(this);
        if (dbHelper.paradasEstaVacia()) {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    String[] archivosAssets = getAssets().list("");
                    java.util.List<com.example.unigo_das.item.Parada> paradasAInsertar = new java.util.ArrayList<>();
                    for (String nombreArchivo : archivosAssets) {
                        if (nombreArchivo != null && nombreArchivo.endsWith(".json")) {
                            java.io.InputStream is = getAssets().open(nombreArchivo);
                            byte[] buffer = new byte[is.available()];
                            is.read(buffer);
                            is.close();
                            String jsonString = new String(buffer, "UTF-8");
                            org.json.JSONObject objetoRaiz = new org.json.JSONObject(jsonString);
                            if (objetoRaiz.has("listaParadas")) {
                                org.json.JSONArray jsonArray = objetoRaiz.getJSONArray("listaParadas");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                                    String id = obj.optString("id", nombreArchivo + "_" + i);
                                    String nombre = obj.optString("nombre", "Desconocida");
                                    String tipo = obj.optString("tipo", "Transporte");
                                    double lat = obj.optDouble("latitud", 0.0);
                                    double lon = obj.optDouble("longitud", 0.0);
                                    paradasAInsertar.add(new com.example.unigo_das.item.Parada(id, nombre, tipo, lat, lon));
                                }
                            }
                        }
                    }
                    dbHelper.insertarParadasMasivas(paradasAInsertar);
                } catch (Exception e) { e.printStackTrace(); }
            });
        }
    }

}