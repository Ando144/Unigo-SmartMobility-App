package com.example.unigo_das.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

        btnBackToLogin.setOnClickListener(v -> finish());
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

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        String response = workInfo.getOutputData().getString("response");
                        if (response != null && !response.isEmpty()) {
                            procesarRespuestaServidor(response);
                        } else {
                            Toast.makeText(this, "Fallo crítico: Respuesta vacía del servidor (PHP Crash)", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void procesarRespuestaServidor(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);

            if (json.containsKey("success") && (Boolean) json.get("success")) {
                Toast.makeText(this, "¡Usuario registrado!", Toast.LENGTH_SHORT).show();
                marcarEstadoUsuario();
                irAMainActivity();
            } else {
                String error = (String) json.get("error");
                Toast.makeText(this, "Error: " + (error != null ? error : "Fallo"), Toast.LENGTH_LONG).show();
            }
        } catch (ParseException e) {
            // Si el servidor escupe un error de PHP (HTML), lo verás aquí
            Toast.makeText(this, "ERROR SERVIDOR: " + jsonResponse, Toast.LENGTH_LONG).show();
        }
    }

    private void irAMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void marcarEstadoUsuario() {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", false);
        editor.apply();
    }
}
