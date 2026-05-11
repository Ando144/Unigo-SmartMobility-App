package com.example.unigo_das;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class UnigoApplication extends Application {

    private static UnigoApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        aplicarTemaGuardado();
    }

    public static void aplicarTemaGuardado() {
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences unigoPrefs = getContext().getSharedPreferences("UnigoPrefs", MODE_PRIVATE);
        String userEmail = unigoPrefs.getString("user_email", null);
        boolean isGuest = unigoPrefs.getBoolean("isGuest", true);

        boolean isDarkMode;

        if (!isGuest && userEmail != null && !userEmail.isEmpty()) {
            isDarkMode = unigoPrefs.getBoolean("dark_mode_" + userEmail, false);
        } else {
            isDarkMode = defaultPrefs.getBoolean("modo_oscuro_activado", false);
        }

        int targetMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(targetMode);
    }

    public static Context getContext() {
        return instance;
    }
}