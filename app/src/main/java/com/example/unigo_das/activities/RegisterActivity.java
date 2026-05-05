package com.example.unigo_das.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo_das.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNombreReg, etEmailReg, etPasswordReg;
    private MaterialButton btnDoRegister, btnBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Enlazamos las vistas
        etNombreReg = findViewById(R.id.etNombreReg);
        etEmailReg = findViewById(R.id.etEmailReg);
        etPasswordReg = findViewById(R.id.etPasswordReg);
        btnDoRegister = findViewById(R.id.btnDoRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        // Acción de Registrarse
        btnDoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = etNombreReg.getText().toString().trim();
                String email = etEmailReg.getText().toString().trim();
                String pass = etPasswordReg.getText().toString().trim();

                if (!nombre.isEmpty() && !email.isEmpty() && !pass.isEmpty()) {
                    // TODO: Futura llamada a WorkManager para conectar con registro.php

                    Toast.makeText(RegisterActivity.this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show();

                    // Guardamos el estado: Ha entrado con cuenta (no es invitado)
                    marcarEstadoUsuario(false);

                    // Redirigimos al mapa principal
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    // Limpiamos la pila para que no pueda volver atrás al registro/login
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Acción de volver al Login
        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Como el Login nos llamó, simplemente cerramos esta actividad y el Login aparecerá
                finish();
            }
        });
    }

    // Método auxiliar igual que en el Login
    private void marcarEstadoUsuario(boolean esInvitado) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", esInvitado);
        editor.apply();
    }
}
