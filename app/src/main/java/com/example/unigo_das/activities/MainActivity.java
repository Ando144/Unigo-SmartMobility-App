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

    private Fragment mapFragment, schoolFragment, weatherFragment, settingsFragment;
    private Fragment activeFragment;
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializamos los centros en la BD
        inicializarCampus();

        // NUEVO: Inicializamos las paradas leyendo los JSON en segundo plano
        inicializarParadasTransporte();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        fm = getSupportFragmentManager();

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                FragmentTransaction transaction = fm.beginTransaction();

                if (activeFragment != null) {
                    transaction.hide(activeFragment);
                }

                if (itemId == R.id.nav_map) {
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    insetsController.setAppearanceLightStatusBars(true);

                    if (mapFragment == null) {
                        mapFragment = new MapFragment();
                        transaction.add(R.id.fragment_container, mapFragment, "map");
                    } else {
                        transaction.show(mapFragment);
                    }
                    activeFragment = mapFragment;

                } else {
                    getWindow().setStatusBarColor(Color.parseColor("#333333"));
                    insetsController.setAppearanceLightStatusBars(false);

                    if (itemId == R.id.nav_school) {
                        if (schoolFragment == null) {
                            schoolFragment = new SchoolFragment();
                            transaction.add(R.id.fragment_container, schoolFragment, "school");
                        } else {
                            transaction.show(schoolFragment);
                        }
                        activeFragment = schoolFragment;

                    } else if (itemId == R.id.nav_weather) {
                        if (weatherFragment == null) {
                            weatherFragment = new WeatherFragment();
                            transaction.add(R.id.fragment_container, weatherFragment, "weather");
                        } else {
                            transaction.show(weatherFragment);
                        }
                        activeFragment = weatherFragment;

                    } else if (itemId == R.id.nav_settings) {
                        if (settingsFragment == null) {
                            settingsFragment = new SettingsFragment();
                            transaction.add(R.id.fragment_container, settingsFragment, "settings");
                        } else {
                            transaction.show(settingsFragment);
                        }
                        activeFragment = settingsFragment;
                    }
                }

                transaction.commit();
                return true;
            }
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }

    public void irRutaEnMapa(String nombreCentro, double latDestino, double lngDestino) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_map);

        if (mapFragment != null) {
            ((MapFragment) mapFragment).dibujarLineaHastaDestino(nombreCentro, latDestino, lngDestino);
        }
    }

    private void inicializarCampus() {
        DataBaseHelper dbHelper = new DataBaseHelper(this);

        if (dbHelper.centrosEstaVacia()) {
            // --- UPV/EHU ---
            dbHelper.insertarCentro("EHU_345", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Sede principal con formación técnica avanzada en diversas ramas de la ingeniería.", "San Mamés", 43.2638, -2.9511);
            dbHelper.insertarCentro("EHU_363", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Centro especializado en investigación y desarrollo de proyectos industriales.", "San Mamés", 43.2638, -2.9511);
            dbHelper.insertarCentro("EHU_364", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Instalaciones dedicadas a la formación práctica y laboratorios de ensayo.", "San Mamés", 43.2638, -2.9511);
            dbHelper.insertarCentro("EHU_350", getString(R.string.centro_ehu_ingenieria_bilbao), "UPV/EHU", "Escuela técnica superior especializada en estudios de náutica y máquinas navales.", "Portugalete", 43.3308, -3.0186);
            dbHelper.insertarCentro("EHU_320", getString(R.string.centro_ehu_bellas_artes), "UPV/EHU", "Facultad dedicada a la formación en artes visuales, diseño y restauración.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_310", getString(R.string.centro_ehu_ciencia_tecnologia), "UPV/EHU", "Centro de referencia en investigación científica y estudios tecnológicos.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_323", getString(R.string.centro_ehu_ciencias_sociales_comunicacion), "UPV/EHU", "Campus centrado en el estudio del periodismo, la publicidad y la sociología.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_324", getString(R.string.centro_ehu_derecho), "UPV/EHU", "Facultad especializada en formación jurídica y práctica legal avanzada.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_321", getString(R.string.centro_ehu_economia_empresa), "UPV/EHU", "Principal centro de formación en economía y administración de empresas.", "Sarriko", 43.2721, -2.9566);
            dbHelper.insertarCentro("EHU_351", getString(R.string.centro_ehu_economia_empresa_elcano), "UPV/EHU", "Escuela universitaria de estudios empresariales con un enfoque práctico.", "Bilbao Centro", 43.2642, -2.9355);
            dbHelper.insertarCentro("EHU_354", getString(R.string.centro_ehu_educacion_bilbao), "UPV/EHU", "Centro dedicado a la formación de profesionales en el ámbito de la educación.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_327", getString(R.string.centro_ehu_medicina_enfermeria), "UPV/EHU", "Facultad dedicada a la formación superior en ciencias de la salud y medicina.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_352", getString(R.string.centro_ehu_medicina_enfermeria), "UPV/EHU", "Instalaciones para la formación práctica de enfermería y cuidados médicos.", "Leioa", 43.3301, -2.9678);
            dbHelper.insertarCentro("EHU_332", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Centro hospitalario dedicado a la docencia clínica y formación de residentes.", "Galdakao", 43.2289, -2.8360);
            dbHelper.insertarCentro("EHU_328", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Unidad docente universitaria integrada en un entorno hospitalario de referencia.", "Basurto", 43.2605, -2.9490);
            dbHelper.insertarCentro("EHU_329", getString(R.string.centro_ehu_unidad_docente_medicina), "UPV/EHU", "Formación práctica especializada para estudiantes de medicina de últimos cursos.", "Cruces", 43.2847, -2.9829);
            dbHelper.insertarCentro("EHU_EXP", getString(R.string.centro_ehu_aulas_experiencia), "UPV/EHU", "Espacio universitario enfocado al aprendizaje permanente para mayores de 55 años.", "Casco Viejo", 43.2576, -2.9238);

            // --- MONDRAGON ---
            dbHelper.insertarCentro("MU_BBF_EMP", getString(R.string.centro_mu_bbf_empresariales), "Mondragon", "Ecosistema de formación y emprendimiento en el centro de la ciudad.", "Uribitarte", 43.2665, -2.9304);
            dbHelper.insertarCentro("MU_BBF_LEINN", getString(R.string.centro_mu_bbf_leinn), "Mondragon", "Centro especializado en liderazgo, emprendimiento e innovación.", "Uribitarte", 43.2665, -2.9304);
            dbHelper.insertarCentro("MU_AS_POL", getString(R.string.centro_mu_as_politecnica), "Mondragon", "Escuela politécnica avanzada integrada en un entorno de innovación digital.", "Zorrotzaurre", 43.2764, -2.9642);
            dbHelper.insertarCentro("MU_AS_HUM", getString(R.string.centro_mu_as_humanidades), "Mondragon", "Facultad de humanidades con enfoque en la digitalización y nuevas tendencias.", "Zorrotzaurre", 43.2764, -2.9642);

            // --- DEUSTO ---
            dbHelper.insertarCentro("DEU_DBS", getString(R.string.centro_deusto_business), "Deusto", "Histórica escuela de negocios con excelencia en formación empresarial.", "Deusto", 43.2713, -2.9379);
            dbHelper.insertarCentro("DEU_DER", getString(R.string.centro_deusto_derecho), "Deusto", "Centro de referencia internacional en formación jurídica y leyes.", "Deusto", 43.2713, -2.9379);
            dbHelper.insertarCentro("DEU_CSH", getString(R.string.centro_deusto_sociales_humanas), "Deusto", "Facultad dedicada al estudio de la psicología, lenguas y ciencias sociales.", "Deusto", 43.2713, -2.9379);
            dbHelper.insertarCentro("DEU_ING", getString(R.string.centro_deusto_ingenieria), "Deusto", "Campus tecnológico enfocado en la informática, industria y telecomunicaciones.", "Deusto", 43.2713, -2.9379);
            dbHelper.insertarCentro("DEU_EDU", getString(R.string.centro_deusto_educacion_deporte), "Deusto", "Formación superior en magisterio y ciencias de la actividad física.", "Deusto", 43.2713, -2.9379);
            dbHelper.insertarCentro("DEU_SAL", getString(R.string.centro_deusto_salud), "Deusto", "Nueva facultad orientada a los retos modernos de la medicina y salud.", "Deusto", 43.2713, -2.9379);
            dbHelper.insertarCentro("DEU_CSC", getString(R.string.centro_deusto_sociales_comunicacion), "Deusto", "Especialidad en comunicación corporativa y estudios sociales críticos.", "Deusto", 43.2713, -2.9379);
        }
    }

    // NUEVO: Método para leer e insertar de golpe todas las paradas en SQLite
    private void inicializarParadasTransporte() {
        DataBaseHelper dbHelper = new DataBaseHelper(this);

        if (dbHelper.paradasEstaVacia()) {
            // Usamos un hilo en segundo plano para no congelar la pantalla de carga
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

                            // Accedemos al objeto raíz y luego al array "listaParadas"
                            org.json.JSONObject objetoRaiz = new org.json.JSONObject(jsonString);
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

                    // Insertamos las miles de paradas de golpe en la base de datos
                    dbHelper.insertarParadasMasivas(paradasAInsertar);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}