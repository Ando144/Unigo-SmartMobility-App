package com.example.unigo_das.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo_das.R;
import com.example.unigo_das.network.NetworkWorker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNombreReg, etEmailReg, etPasswordReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNombreReg = findViewById(R.id.etNombreReg);
        etEmailReg = findViewById(R.id.etEmailReg);
        etPasswordReg = findViewById(R.id.etPasswordReg);
        MaterialButton btnDoRegister = findViewById(R.id.btnDoRegister);
        MaterialButton btnBackToLogin = findViewById(R.id.btnBackToLogin);

        final TextView btnLangEs = findViewById(R.id.btnLangEs);
        final TextView btnLangEu = findViewById(R.id.btnLangEu);
        final TextView btnLangEn = findViewById(R.id.btnLangEn);
        final TextView btnLangFr = findViewById(R.id.btnLangFr);
        final TextView btnLangDe = findViewById(R.id.btnLangDe);
        final TextView btnLangIt = findViewById(R.id.btnLangIt);

        // Selector de idiomas manual para la pantalla de registro
        btnLangEs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarIdiomaLogin("es", btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangDe, btnLangIt);
            }
        });
        btnLangEu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarIdiomaLogin("eu", btnLangEu, btnLangEs, btnLangEn, btnLangFr, btnLangDe, btnLangIt);
            }
        });
        btnLangEn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarIdiomaLogin("en", btnLangEn, btnLangEs, btnLangEu, btnLangFr, btnLangDe, btnLangIt);
            }
        });
        btnLangFr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarIdiomaLogin("fr", btnLangFr, btnLangEs, btnLangEu, btnLangEn, btnLangDe, btnLangIt);
            }
        });
        btnLangDe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarIdiomaLogin("de", btnLangDe, btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangIt);
            }
        });
        btnLangIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cambiarIdiomaLogin("it", btnLangIt, btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangDe);
            }
        });

        btnDoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = etNombreReg.getText() != null ? etNombreReg.getText().toString().trim() : "";
                String email = etEmailReg.getText() != null ? etEmailReg.getText().toString().trim() : "";
                String pass = etPasswordReg.getText() != null ? etPasswordReg.getText().toString().trim() : "";

                if (!nombre.isEmpty() && !email.isEmpty() && !pass.isEmpty()) {
                    ejecutarRegistro(nombre, email, pass);
                } else {
                    Toast.makeText(RegisterActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    private void ejecutarRegistro(String username, String email, String password) {
        Data inputData = new Data.Builder()
                .putString("script", "registro.php")
                .putString("username", username)
                .putString("email", email)
                .putString("password", password)
                .build();

        OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(NetworkWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(registerRequest);

        // Enganchamos un Observer al LiveData del WorkManager para enterarnos en el momento exacto
        // en que el Worker termina su trabajo en segundo plano y devuelve los datos del servidor.
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String response = workInfo.getOutputData().getString("response");
                            if (response != null && !response.isEmpty()) {
                                procesarRespuestaServidor(response);
                            } else {
                                Toast.makeText(RegisterActivity.this, R.string.fallo_cr_tico_respuesta_vac_a_del_servidor_php_crash, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    // Parseo manual de la respuesta del script PHP.
    // Usamos json-simple para trocear el string y comprobar si la inserción en MySQL fue exitosa.
    private void procesarRespuestaServidor(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);

            // Si el backend devuelve success = true, asumimos que el usuario ya existe en la BD.
            if (json.containsKey("success") && (Boolean) json.get("success")) {
                String email = etEmailReg.getText() != null ? etEmailReg.getText().toString().trim() : "";
                String nombre = etNombreReg.getText() != null ? etNombreReg.getText().toString().trim() : "";

                Toast.makeText(this, "¡Usuario registrado!", Toast.LENGTH_SHORT).show();

                // Persistimos la sesión en local para que el usuario no tenga que loguearse al reabrir la app
                marcarEstadoUsuario(email, nombre);
                if (json.containsKey("id")) {
                    SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("user_id", ((Long) json.get("id")).intValue()).apply();
                }

                irAMainActivity();
            } else {
                // Capturamos el error exacto que escupe PHP (ej: "El correo ya está en uso")
                String error = (String) json.get("error");
                Toast.makeText(this, "Error: " + (error != null ? error : "Fallo"), Toast.LENGTH_LONG).show();
            }
        } catch (ParseException e) {
            Toast.makeText(this, "ERROR SERVIDOR: " + jsonResponse, Toast.LENGTH_LONG).show();
        }
    }

    // Centralizamos la escritura en SharedPreferences.
    // Quitamos la flag de isGuest para que la app sepa que tiene que renderizar las opciones de usuario registrado.
    private void marcarEstadoUsuario(String email, String nombre) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", false);
        editor.putString("user_email", email);
        editor.putString("user_name", nombre);
        editor.apply();
    }

    //  Al ir a la pantalla principal, destruimos toda la pila de navegación .
    // Si no hacemos esto, el usuario podría darle al botón "Atrás" de Android en el menú principal
    // y volvería absurdamente a esta pantalla de registro estando ya logueado.
    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Delega el cambio de idioma al AppCompatDelegate, lo cual forzará a la Activity a recrearse
    // y recargar los strings.xml que tocan al nuevo Locale seleccionado.
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