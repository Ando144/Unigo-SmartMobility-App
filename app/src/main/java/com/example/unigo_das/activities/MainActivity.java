package com.example.unigo_das.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.unigo_das.R;
import com.example.unigo_das.fragments.MapFragment;
import com.example.unigo_das.fragments.SchoolFragment;
import com.example.unigo_das.fragments.SettingsFragment;
import com.example.unigo_das.fragments.WeatherFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    // 1. Guardamos los fragmentos en memoria para no destruirlos
    private Fragment mapFragment, schoolFragment, weatherFragment, settingsFragment;
    private Fragment activeFragment; // Para saber cuál estamos viendo ahora
    private FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        fm = getSupportFragmentManager();

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                // Iniciamos la transacción para cambiar de pantalla
                FragmentTransaction transaction = fm.beginTransaction();

                // Opcional: Si hiciste las animaciones rápidas, ponlas aquí
                // transaction.setCustomAnimations(R.anim.fade_in_fast, R.anim.fade_out_fast);

                // Ocultamos el fragmento que esté activo actualmente
                if (activeFragment != null) {
                    transaction.hide(activeFragment);
                }

                if (itemId == R.id.nav_map) {
                    // --- MODO MAPA ---
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    insetsController.setAppearanceLightStatusBars(true);

                    // Si el mapa no existe, lo creamos y lo añadimos. Si ya existe, solo lo mostramos.
                    if (mapFragment == null) {
                        mapFragment = new MapFragment();
                        transaction.add(R.id.fragment_container, mapFragment, "map");
                    } else {
                        transaction.show(mapFragment);
                    }
                    activeFragment = mapFragment;

                } else {
                    // --- MODO NORMAL ---
                    getWindow().setStatusBarColor(Color.parseColor("#333333")); // Tu color oscuro
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

                // Ejecutamos los cambios
                transaction.commit();
                return true;
            }
        });

        // Al abrir la app, forzamos que se seleccione el mapa la primera vez
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }

    // --- NUEVO MÉTODO PUENTE PARA DIBUJAR LA RUTA ---
    public void irRutaEnMapa(String nombreCentro, double latDestino, double lngDestino) {
        // 1. Cambiamos visualmente a la pestaña del mapa
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_map);

        // 2. Le pasamos los datos al MapFragment para que dibuje la línea
        if (mapFragment != null) {
            ((MapFragment) mapFragment).dibujarLineaHastaDestino(nombreCentro, latDestino, lngDestino);
        }
    }
}