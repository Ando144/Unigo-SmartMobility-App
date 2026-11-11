/*
 * =============================================================================
 *  UNIGO - Smart urban mobility for university students in Bilbao (Bizkaia)
 * =============================================================================
 *  What it does : Main screen of the app. Hosts the four tabs (Map, Campuses,
 *                 Weather, Settings) with a BottomNavigationView, applies the
 *                 user's theme, and loads the reference data into SQLite on
 *                 start-up (28 campuses and 3,895 public transport stops from
 *                 the JSON files in assets/).
 *  Platform     : Android 7.0+ (minSdk 24, targetSdk 36). Needs Google Play
 *                 services (Maps, Location) and an Internet connection.
 *  Tools        : Java 11, Android Gradle Plugin 9, Gradle 9.2, AndroidX,
 *                 Material Components, Google Maps SDK + Maps Utils, Volley,
 *                 WorkManager, ML Kit Translate, SQLite. Back end: PHP + MySQL
 *                 server (not part of this repository).
 *  Authors      : Jon Ander Duque, Asier Faraig, Andoni Ortiz de Zarate
 *  Course       : DAS course project, academic year 2025-26 - Bachelor's
 *                 Degree in Management and Information Systems Engineering,
 *                 Bilbao School of Engineering (UPV/EHU).
 * =============================================================================
 */
package com.example.unigo_das.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

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
        // Apply dark mode BEFORE super.onCreate
        applyDarkMode();

        // Update the database with the new language BEFORE
        // super.onCreate restores the fragments and they read the old database.
        initializeCampuses();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fix F9: initializeCampuses() was called a second time here; once (before super.onCreate) is enough.
        initializeTransitStops();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        fm = getSupportFragmentManager();

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                navigateTo(item.getItemId());
                return true;
            }
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        } else {
            int selectedId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_map);
            bottomNav.setSelectedItemId(selectedId);
        }
    }

    private void applyDarkMode() {
        SharedPreferences unigoPrefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isGuest = unigoPrefs.getBoolean("isGuest", true);
        boolean isDarkMode;

        if (!isGuest) {
            String userEmail = unigoPrefs.getString("user_email", "");
            if (!userEmail.isEmpty()) {
                isDarkMode = unigoPrefs.getBoolean("dark_mode_" + userEmail, false);
                defaultPrefs.edit().putBoolean("modo_oscuro_activado", isDarkMode).apply();
            } else {
                isDarkMode = defaultPrefs.getBoolean("modo_oscuro_activado", false);
            }
        } else {
            isDarkMode = defaultPrefs.getBoolean("modo_oscuro_activado", false);
        }

        int targetMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
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

    private void navigateTo(int itemId) {
        FragmentTransaction transaction = fm.beginTransaction();
        String tag = "";
        Fragment targetFragment = null;

        if (itemId == R.id.nav_map) tag = "map";
        else if (itemId == R.id.nav_school) tag = "school";
        else if (itemId == R.id.nav_weather) tag = "weather";
        else if (itemId == R.id.nav_settings) tag = "settings";

        targetFragment = fm.findFragmentByTag(tag);

        for (Fragment frag : fm.getFragments()) {
            if (frag.isVisible()) {
                transaction.hide(frag);
            }
        }

        if (targetFragment == null) {
            if (itemId == R.id.nav_map) {
                targetFragment = new MapFragment();
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
        else {
            transaction.show(targetFragment);
            if (itemId == R.id.nav_map && mapArgs != null) {
                ((MapFragment) targetFragment).startRouteToDestination(
                        mapArgs.getString("destino_nombre"),
                        mapArgs.getDouble("destino_lat"),
                        mapArgs.getDouble("destino_lng")
                );
                mapArgs = null;
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
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
            int color = typedValue.data;
            
            getWindow().setStatusBarColor(color);
            
            if (insetsController != null) {
                boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                                 == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                insetsController.setAppearanceLightStatusBars(!isDark);
            }
        }
    }

    public void showRouteOnMap(String campusName, double destLat, double destLng) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        mapArgs = new Bundle();
        mapArgs.putString("destino_nombre", campusName);
        mapArgs.putDouble("destino_lat", destLat);
        mapArgs.putDouble("destino_lng", destLng);

        if (bottomNav.getSelectedItemId() == R.id.nav_map) {
            MapFragment mapFragment = (MapFragment) fm.findFragmentByTag("map");
            if (mapFragment != null) {
                mapFragment.startRouteToDestination(campusName, destLat, destLng);
                mapArgs = null;
            }
        } else {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }

    private void initializeCampuses() {
        // Fix F9: the 28 campuses are written in a single transaction (before: 28 separate
        // open/insert/close cycles on the main thread, twice per start-up).
        java.util.List<com.example.unigo_das.item.Campus> campuses = new java.util.ArrayList<>();
        campuses.add(new com.example.unigo_das.item.Campus("EHU_345", getString(R.string.campus_ehu_engineering_bilbao), "UPV/EHU", "Sede principal...", "San Mamés", 43.26233, -2.94840));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_363", getString(R.string.campus_ehu_engineering_bilbao1), "UPV/EHU", "Centro especializado...", "San Mamés", 43.26332, -2.95035));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_364", getString(R.string.campus_ehu_engineering_bilbao2), "UPV/EHU", "Instalaciones...", "San Mamés", 43.2638, -2.9511));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_350", getString(R.string.campus_ehu_engineering_bilbao3), "UPV/EHU", "Escuela técnica...", "Portugalete", 43.32682, -3.02284));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_320", getString(R.string.campus_ehu_fine_arts), "UPV/EHU", "Facultad...", "Leioa", 43.33142, -2.97310));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_310", getString(R.string.campus_ehu_science_technology), "UPV/EHU", "Centro de referencia...", "Leioa", 43.33082, -2.97000));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_323", getString(R.string.campus_ehu_social_communication_sciences), "UPV/EHU", "Campus centrado...", "Leioa", 43.33102, -2.96741));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_324", getString(R.string.campus_ehu_law), "UPV/EHU", "Facultad especializada...", "Leioa", 43.33097983602023, -2.965655349095869));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_321", getString(R.string.campus_ehu_economics_business), "UPV/EHU", "Principal centro...", "Sarriko", 43.27331543973917, -2.9583611657702416));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_351", getString(R.string.campus_ehu_economics_business_elcano), "UPV/EHU", "Escuela universitaria...", "Bilbao Centro", 43.26010899657177, -2.933159441081518));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_354", getString(R.string.campus_ehu_education_bilbao), "UPV/EHU", "Centro dedicado...", "Leioa", 43.3330960679207, -2.9727070307354544));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_327", getString(R.string.campus_ehu_medicine_nursing), "UPV/EHU", "Facultad dedicada...", "Leioa", 43.32975437145838, -2.9655047077553744));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_352", getString(R.string.campus_ehu_medicine_nursing), "UPV/EHU", "Instalaciones...", "Leioa", 43.32975437145838, -2.9655047077553744));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_332", getString(R.string.campus_ehu_medical_teaching_unit), "UPV/EHU", "Centro hospitalario...", "Galdakao", 43.223510878386605, -2.817928390528635));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_328", getString(R.string.campus_ehu_medical_teaching_unit), "UPV/EHU", "Unidad docente...", "Basurto", 43.26158155692848, -2.952932217865744));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_329", getString(R.string.campus_ehu_medical_teaching_unit), "UPV/EHU", "Formación práctica...", "Cruces", 43.28217570183873, -2.9841267313810045));
        campuses.add(new com.example.unigo_das.item.Campus("EHU_EXP", getString(R.string.campus_ehu_experience_classrooms), "UPV/EHU", "Espacio universitario...", "Casco Viejo", 43.25780220598689, -2.923145862870646));

        campuses.add(new com.example.unigo_das.item.Campus("MU_BBF_EMP", getString(R.string.campus_mu_bbf_business), "Mondragon", "Ecosistema...", "Uribitarte", 43.26444212526109, -2.9271480629194784));
        campuses.add(new com.example.unigo_das.item.Campus("MU_BBF_LEINN", getString(R.string.campus_mu_bbf_leinn), "Mondragon", "Centro especializado...", "Uribitarte", 43.26444212526109, -2.9271480629194784));
        campuses.add(new com.example.unigo_das.item.Campus("MU_AS_POL", getString(R.string.campus_mu_as_polytechnic), "Mondragon", "Escuela politécnica...", "Zorrotzaurre", 43.27214942309993, -2.9638728246668524));
        campuses.add(new com.example.unigo_das.item.Campus("MU_AS_HUM", getString(R.string.campus_mu_as_humanities), "Mondragon", "Facultad de humanidades...", "Zorrotzaurre", 43.27214942309993, -2.9638728246668524));

        campuses.add(new com.example.unigo_das.item.Campus("DEU_DBS", getString(R.string.campus_deusto_business), "Deusto", "Histórica escuela...", "Deusto", 43.270489177414404, -2.93751635987864));
        campuses.add(new com.example.unigo_das.item.Campus("DEU_DER", getString(R.string.campus_deusto_law), "Deusto", "Centro de referencia...", "Deusto", 43.270489177414404, -2.93751635987864));
        campuses.add(new com.example.unigo_das.item.Campus("DEU_CSH", getString(R.string.campus_deusto_social_human_sciences), "Deusto", "Facultad dedicada...", "Deusto", 43.270489177414404, -2.93751635987864));
        campuses.add(new com.example.unigo_das.item.Campus("DEU_ING", getString(R.string.campus_deusto_engineering), "Deusto", "Campus tecnológico...", "Deusto", 43.270489177414404, -2.93751635987864));
        campuses.add(new com.example.unigo_das.item.Campus("DEU_EDU", getString(R.string.campus_deusto_education_sport), "Deusto", "Formación superior...", "Deusto", 43.270489177414404, -2.93751635987864));
        campuses.add(new com.example.unigo_das.item.Campus("DEU_SAL", getString(R.string.campus_deusto_health), "Deusto", "Nueva facultad...", "Deusto", 43.270489177414404, -2.93751635987864));
        campuses.add(new com.example.unigo_das.item.Campus("DEU_CSC", getString(R.string.campus_deusto_social_communication), "Deusto", "Especialidad en comunicación...", "Deusto", 43.270489177414404, -2.93751635987864));
        new DataBaseHelper(this).insertCampusesBulk(campuses);
    }

    private void initializeTransitStops() {
        DataBaseHelper dbHelper = new DataBaseHelper(this);
        if (dbHelper.isStopTableEmpty()) {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    String[] assetFiles = getAssets().list("");
                    java.util.List<com.example.unigo_das.item.TransitStop> stopsToInsert = new java.util.ArrayList<>();
                    for (String fileName : assetFiles) {
                        if (fileName != null && fileName.endsWith(".json")) {
                            java.io.InputStream is = getAssets().open(fileName);
                            byte[] buffer = new byte[is.available()];
                            is.read(buffer);
                            is.close();
                            String jsonString = new String(buffer, "UTF-8");
                            org.json.JSONObject rootObject = new org.json.JSONObject(jsonString);
                            if (rootObject.has("listaParadas")) {
                                org.json.JSONArray jsonArray = rootObject.getJSONArray("listaParadas");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                                    String id = obj.optString("id", fileName + "_" + i);
                                    String name = obj.optString("nombre", "Desconocida");
                                    String type = obj.optString("tipo", "Transporte");
                                    double lat = obj.optDouble("latitud", 0.0);
                                    double lon = obj.optDouble("longitud", 0.0);
                                    stopsToInsert.add(new com.example.unigo_das.item.TransitStop(id, name, type, lat, lon));
                                }
                            }
                        }
                    }
                    dbHelper.insertStopsBulk(stopsToInsert);
                } catch (Exception e) { e.printStackTrace(); }
            });
            executor.shutdown(); // Fix F9: the thread ends when the task finishes
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Make sure the context uses the correct language configuration
        super.attachBaseContext(newBase);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // This makes sure the resources are updated with the new configuration
        getResources().updateConfiguration(newConfig, getResources().getDisplayMetrics());
    }

}