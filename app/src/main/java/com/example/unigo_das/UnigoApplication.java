package com.example.unigo_das;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class UnigoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Aplicar el tema guardado al iniciar la aplicación
        aplicarTemaGuardado();
    }

    public static void aplicarTemaGuardado() {
        // Usamos las preferencias generales como fallback
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Intentamos obtener el email del usuario actual
        SharedPreferences unigoPrefs = getContext().getSharedPreferences("UnigoPrefs", MODE_PRIVATE);
        String userEmail = unigoPrefs.getString("user_email", null);
        boolean isGuest = unigoPrefs.getBoolean("isGuest", true);

        boolean isDarkMode;

        if (!isGuest && userEmail != null && !userEmail.isEmpty()) {
            // Usuario logueado: usar su preferencia personal
            isDarkMode = unigoPrefs.getBoolean("dark_mode_" + userEmail, false);
        } else {
            // Invitado o sin email: usar preferencia general
            isDarkMode = defaultPrefs.getBoolean("modo_oscuro_activado", false);
        }

        int targetMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(targetMode);
    }

    private static Context getContext() {
        // Necesitamos una forma de obtener el contexto
        // Como es una clase Application, podemos usar una instancia estática
        return instance;
    }

    private static UnigoApplication instance;

    public UnigoApplication() {
        instance = this;
    }
}