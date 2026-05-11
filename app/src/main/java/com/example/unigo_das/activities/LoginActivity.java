package com.example.unigo_das.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo_das.R;
import com.example.unigo_das.network.NetworkWorker;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister, btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        TextView btnLangEs = findViewById(R.id.btnLangEs);
        TextView btnLangEu = findViewById(R.id.btnLangEu);
        TextView btnLangEn = findViewById(R.id.btnLangEn);
        TextView btnLangFr = findViewById(R.id.btnLangFr);
        TextView btnLangDe = findViewById(R.id.btnLangDe);
        TextView btnLangIt = findViewById(R.id.btnLangIt);
        btnLangEs.setOnClickListener(v -> cambiarIdiomaLogin("es", btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangDe, btnLangIt));
        btnLangEu.setOnClickListener(v -> cambiarIdiomaLogin("eu", btnLangEu, btnLangEs, btnLangEn, btnLangFr, btnLangDe, btnLangIt));
        btnLangEn.setOnClickListener(v -> cambiarIdiomaLogin("en", btnLangEn, btnLangEs, btnLangEu, btnLangFr, btnLangDe, btnLangIt));
        btnLangFr.setOnClickListener(v -> cambiarIdiomaLogin("fr", btnLangFr, btnLangEs, btnLangEu, btnLangEn, btnLangDe, btnLangIt));
        btnLangDe.setOnClickListener(v -> cambiarIdiomaLogin("de", btnLangDe, btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangIt));
        btnLangIt.setOnClickListener(v -> cambiarIdiomaLogin("it", btnLangIt, btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangDe));

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (!email.isEmpty() && !pass.isEmpty()) {
                    ejecutarLogin(email, pass);
                } else {
                    Toast.makeText(LoginActivity.this, "Por favor, llena los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marcarEstadoUsuario(true);
                irAMainActivity();
            }
        });
    }

    private void ejecutarLogin(String email, String password) {
        Data inputData = new Data.Builder()
                .putString("script", "login.php")
                .putString("email", email)
                .putString("password", password)
                .build();

        OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(NetworkWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(loginRequest);

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(loginRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            String response = workInfo.getOutputData().getString("response");
                            procesarRespuestaLogin(response);
                        } else {
                            String errorResponse = workInfo.getOutputData().getString("response");
                            procesarError(errorResponse);
                        }
                    }
                });
    }

    private void procesarRespuestaLogin(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);

            if (json.containsKey("success") && (Boolean) json.get("success")) {
                String nombre = (String) json.get("nombre");
                String email = etEmail.getText().toString().trim();
                String fotoUrl = json.containsKey("foto") ? (String) json.get("foto") : null;

                SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_email", email);
                editor.putString("user_name", nombre);
                editor.putBoolean("isGuest", false);

                editor.remove("user_id");
                if (json.containsKey("id")) {
                    editor.putInt("user_id", ((Long) json.get("id")).intValue());
                }

                // Guardar URL de la foto si existe
                if (fotoUrl != null && !fotoUrl.isEmpty()) {
                    editor.putString("profile_photo_url_" + email, fotoUrl);
                } else {
                    editor.remove("profile_photo_url_" + email);
                }

                String savedLang = prefs.getString("language_" + email, "es");
                LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(savedLang);
                AppCompatDelegate.setApplicationLocales(appLocales);

                editor.apply();

                aplicarTemaUsuario(email);
                irAMainActivity();
            } else {
                String error = (String) json.get("error");
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Error al procesar respuesta del servidor", Toast.LENGTH_SHORT).show();
        }
    }

    private void aplicarTemaUsuario(String email) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Obtener preferencia guardada para este usuario
        boolean isDarkMode = prefs.getBoolean("dark_mode_" + email,
                defaultPrefs.getBoolean("modo_oscuro_activado", false));

        // También actualizamos la preferencia general para mantener sincronización
        defaultPrefs.edit().putBoolean("modo_oscuro_activado", isDarkMode).apply();

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void procesarError(String errorResponse) {
        if (errorResponse != null) {
            try {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(errorResponse);
                String error = (String) json.get("error");
                Toast.makeText(this, error != null ? error : "Error en la conexión", Toast.LENGTH_SHORT).show();
            } catch (ParseException e) {
                Toast.makeText(this, "Error de red o credenciales incorrectas", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show();
        }
    }

    private void irAMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void marcarEstadoUsuario(boolean esInvitado) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", esInvitado);
        editor.apply();
    }

    private void cambiarIdiomaLogin(String langCode, TextView activo, TextView... inactivos) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
        marcarIdiomaActivo(activo, inactivos);
    }

    private void marcarIdiomaActivo(TextView activo, TextView... inactivos) {
        activo.setBackgroundColor(ContextCompat.getColor(this, R.color.bilbao_red));
        activo.setTextColor(ContextCompat.getColor(this, R.color.white));
        for (TextView tv : inactivos) {
            tv.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }
}
