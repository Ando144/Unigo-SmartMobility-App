package com.example.unigo_das.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo_das.R;

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

        // 1. Camino: Iniciar Sesión (Mockeado por ahora)
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String pass = etPassword.getText().toString();

                if (!email.isEmpty() && !pass.isEmpty()) {
                    // TODO: Aquí en el futuro llamaremos a nuestro WorkManager para el PHP
                    marcarEstadoUsuario(false); // No es invitado
                    irAMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Por favor, llena los campos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 2. Camino: Ir a Registro
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // 3. Camino: Entrar como Invitado
        btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marcarEstadoUsuario(true); // Sí es invitado
                irAMainActivity();
            }
        });
    }

    // Método auxiliar para ir al Main y cerrar el Login para que no se pueda volver atrás con el botón "Back"
    private void irAMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Guardamos en SharedPreferences si es invitado o no
    private void marcarEstadoUsuario(boolean esInvitado) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", esInvitado);
        editor.apply();
    }
}