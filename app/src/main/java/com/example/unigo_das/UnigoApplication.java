package com.example.unigo_das;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class UnigoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Aplicar el tema guardado al iniciar la aplicación de forma inmediata
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("modo_oscuro_activado", false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}