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
            // Caso 1: Inicio limpio (ir al mapa por defecto)
            bottomNav.setSelectedItemId(R.id.nav_map);
            // navegarA ya es llamado por el listener de setSelectedItemId
        } else {
            // Caso 2: Recreación (por cambio de idioma, tema, etc.)
            // Recuperamos el ID de la pestaña que guardamos en onSaveInstanceState
            int selectedId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_map);
            
            // Forzamos la selección visual en la barra
            bottomNav.setSelectedItemId(selectedId);
            
            // Refrescamos el fragmento manualmente para asegurar que 
            // se carguen los recursos del idioma actual (strings.xml)
            syncStatusBar(selectedId);
            navegarA(selectedId);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // Guardamos el ID de la pestaña actual para recuperarlo tras la recreación
            outState.putInt(KEY_SELECTED_TAB, bottomNav.getSelectedItemId());
        }
    }

    private void navegarA(int itemId) {
        Fragment fragment = null;
        String tag = "";

        if (itemId == R.id.nav_map) {
            fragment = new MapFragment();
            tag = "map";
        } else if (itemId == R.id.nav_school) {
            fragment = new SchoolFragment();
            tag = "school";
        } else if (itemId == R.id.nav_weather) {
            fragment = new WeatherFragment();
            tag = "weather";
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            tag = "settings";
        }

        if (fragment != null) {
            syncStatusBar(itemId);
            // Al usar REPLACE, el FragmentManager destruye el fragmento anterior (si existe)
            // y crea uno nuevo, lo que garantiza que el método onCreateView use los recursos
            // del nuevo idioma configurado.
            fm.beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .commit();
        }
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
        bottomNav.setSelectedItemId(R.id.nav_map);
    }

    private void inicializarCampus() {
        DataBaseHelper dbHelper = new DataBaseHelper(this);
        dbHelper.insertarCentro("EHU_345", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Sede principal...", "San Mamés", 43.2638, -2.9511);
        dbHelper.insertarCentro("EHU_363", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Centro especializado...", "San Mamés", 43.2638, -2.9511);
        dbHelper.insertarCentro("EHU_364", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Instalaciones...", "San Mamés", 43.2638, -2.9511);
        dbHelper.insertarCentro("EHU_350", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Escuela técnica...", "Portugalete", 43.3308, -3.0186);
        dbHelper.insertarCentro("EHU_320", getString(R.string.centro_ehu_bellas_artes), "UPV/EHU", "Facultad...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_310", getString(R.string.centro_ehu_ciencia_tecnologia), "UPV/EHU", "Centro de referencia...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_323", getString(R.string.centro_ehu_ciencias_sociales_comunicacion), "UPV/EHU", "Campus centrado...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_324", getString(R.string.centro_ehu_derecho), "UPV/EHU", "Facultad especializada...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_321", getString(R.string.centro_ehu_economia_empresa), "UPV/EHU", "Principal centro...", "Sarriko", 43.2721, -2.9566);
        dbHelper.insertarCentro("EHU_351", getString(R.string.centro_ehu_economia_empresa_elcano), "UPV/EHU", "Escuela universitaria...", "Bilbao Centro", 43.2642, -2.9355);
        dbHelper.insertarCentro("EHU_354", getString(R.string.centro_ehu_educacion_bilbao), "UPV/EHU", "Centro dedicado...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_327", getString(R.string.centro_ehu_medicina_enfermeria), "UPV/EHU", "Facultad dedicada...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_352", getString(R.string.centro_ehu_medicina_enfermeria), "UPV/EHU", "Instalaciones...", "Leioa", 43.3301, -2.9678);
        dbHelper.insertarCentro("EHU_332", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Centro hospitalario...", "Galdakao", 43.2289, -2.8360);
        dbHelper.insertarCentro("EHU_328", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Unidad docente...", "Basurto", 43.2605, -2.9490);
        dbHelper.insertarCentro("EHU_329", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Formación práctica...", "Cruces", 43.2847, -2.9829);
        dbHelper.insertarCentro("EHU_EXP", getString(R.string.centro_ehu_aulas_experiencia), "UPV/EHU", "Espacio universitario...", "Casco Viejo", 43.2576, -2.9238);

        dbHelper.insertarCentro("MU_BBF_EMP", getString(R.string.centro_mu_bbf_empresariales), "Mondragon", "Ecosistema...", "Uribitarte", 43.2665, -2.9304);
        dbHelper.insertarCentro("MU_BBF_LEINN", getString(R.string.centro_mu_bbf_leinn), "Mondragon", "Centro especializado...", "Uribitarte", 43.2665, -2.9304);
        dbHelper.insertarCentro("MU_AS_POL", getString(R.string.centro_mu_as_politecnica), "Mondragon", "Escuela politécnica...", "Zorrotzaurre", 43.2764, -2.9642);
        dbHelper.insertarCentro("MU_AS_HUM", getString(R.string.centro_mu_as_humanidades), "Mondragon", "Facultad de humanidades...", "Zorrotzaurre", 43.2764, -2.9642);

        dbHelper.insertarCentro("DEU_DBS", getString(R.string.centro_deusto_business), "Deusto", "Histórica escuela...", "Deusto", 43.2713, -2.9379);
        dbHelper.insertarCentro("DEU_DER", getString(R.string.centro_deusto_derecho), "Deusto", "Centro de referencia...", "Deusto", 43.2713, -2.9379);
        dbHelper.insertarCentro("DEU_CSH", getString(R.string.centro_deusto_sociales_humanas), "Deusto", "Facultad dedicada...", "Deusto", 43.2713, -2.9379);
        dbHelper.insertarCentro("DEU_ING", getString(R.string.centro_deusto_ingenieria), "Deusto", "Campus tecnológico...", "Deusto", 43.2713, -2.9379);
        dbHelper.insertarCentro("DEU_EDU", getString(R.string.centro_deusto_educacion_deporte), "Deusto", "Formación superior...", "Deusto", 43.2713, -2.9379);
        dbHelper.insertarCentro("DEU_SAL", getString(R.string.centro_deusto_salud), "Deusto", "Nueva facultad...", "Deusto", 43.2713, -2.9379);
        dbHelper.insertarCentro("DEU_CSC", getString(R.string.centro_deusto_sociales_comunicacion), "Deusto", "Especialidad en comunicación...", "Deusto", 43.2713, -2.9379);
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