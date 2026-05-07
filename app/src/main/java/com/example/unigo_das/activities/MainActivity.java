package com.example.unigo_das.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.unigo_das.R;
import com.example.unigo_das.fragments.MapFragment;
import com.example.unigo_das.fragments.SchoolFragment;
import com.example.unigo_das.fragments.SettingsFragment;
import com.example.unigo_das.fragments.WeatherFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Configuramos la app para que siempre dibuje detrás de las barras (Edge-to-Edge)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // 2. Controlador para cambiar el color de los iconos de la hora y batería
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_map) {
                    // --- MODO MAPA (Pantalla Completa) ---
                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                    insetsController.setAppearanceLightStatusBars(true);

                    // 1. ANULAMOS el listener para evitar que Android recalcule el padding
                    ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, null);

                    // 2. Quitamos el padding para que el mapa ocupe todo el espacio
                    fragmentContainer.setPadding(0, 0, 0, 0);

                    // 3. Forzamos la actualización visual
                    fragmentContainer.requestApplyInsets();

                    selectedFragment = new MapFragment();
                } else {
                    // --- MODO NORMAL (Otros Fragments) ---
                    getWindow().setStatusBarColor(Color.parseColor("#333333")); // Tu color principal
                    insetsController.setAppearanceLightStatusBars(false);

                    // VOLVIMOS A CONECTAR el listener para proteger la UI de la barra de estado
                    ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
                        int topInset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                        v.setPadding(0, topInset, 0, 0);
                        return windowInsets;
                    });
                    fragmentContainer.requestApplyInsets();

                    // Asignamos el fragmento correspondiente
                    if (itemId == R.id.nav_school) {
                        selectedFragment = new SchoolFragment();
                    } else if (itemId == R.id.nav_weather) {
                        selectedFragment = new WeatherFragment();
                    } else if (itemId == R.id.nav_settings) {
                        selectedFragment = new SettingsFragment();
                    }
                }

                // Reemplazamos el fragmento
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            }
        });

        // Seleccionar el mapa por defecto al abrir la aplicación
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
        }
    }
}