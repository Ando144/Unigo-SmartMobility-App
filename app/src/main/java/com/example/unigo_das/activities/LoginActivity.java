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
import androidx.lifecycle.Observer;
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

        final TextView btnLangEs = findViewById(R.id.btnLangEs);
        final TextView btnLangEu = findViewById(R.id.btnLangEu);
        final TextView btnLangEn = findViewById(R.id.btnLangEn);
        final TextView btnLangFr = findViewById(R.id.btnLangFr);
        final TextView btnLangDe = findViewById(R.id.btnLangDe);
        final TextView btnLangIt = findViewById(R.id.btnLangIt);


        btnLangEs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLoginLanguage("es", btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangDe, btnLangIt);
            }
        });
        btnLangEu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLoginLanguage("eu", btnLangEu, btnLangEs, btnLangEn, btnLangFr, btnLangDe, btnLangIt);
            }
        });
        btnLangEn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLoginLanguage("en", btnLangEn, btnLangEs, btnLangEu, btnLangFr, btnLangDe, btnLangIt);
            }
        });
        btnLangFr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLoginLanguage("fr", btnLangFr, btnLangEs, btnLangEu, btnLangEn, btnLangDe, btnLangIt);
            }
        });
        btnLangDe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLoginLanguage("de", btnLangDe, btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangIt);
            }
        });
        btnLangIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLoginLanguage("it", btnLangIt, btnLangEs, btnLangEu, btnLangEn, btnLangFr, btnLangDe);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (!email.isEmpty() && !pass.isEmpty()) {
                    performLogin(email, pass);
                } else {
                    Toast.makeText(LoginActivity.this, R.string.login_fill_fields, Toast.LENGTH_SHORT).show(); // Fix F5: text moved to strings.xml so it is translated to the 6 languages
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
                saveUserState(true);
                goToMainActivity();
            }
        });
    }


    private void performLogin(String email, String password) {
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
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                String response = workInfo.getOutputData().getString("response");
                                handleLoginResponse(response);
                            } else {
                                String errorResponse = workInfo.getOutputData().getString("response");
                                handleError(errorResponse);
                            }
                        }
                    }
                });
    }

    // Parses the response and stores all the user information in SharedPreferences
    // to keep the session open. It also restores the visual settings of that specific user.
    private void handleLoginResponse(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);

            if (json.containsKey("success") && (Boolean) json.get("success")) {
                String name = (String) json.get("nombre");
                String email = etEmail.getText().toString().trim();
                String photoUrl = json.containsKey("foto") ? (String) json.get("foto") : null;

                SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                // Leave guest mode and store the key session data
                editor.putString("user_email", email);
                editor.putString("user_name", name);
                editor.putBoolean("isGuest", false);

                editor.remove("user_id");
                if (json.containsKey("id")) {
                    editor.putInt("user_id", ((Long) json.get("id")).intValue());
                }

                // Sync the URL of the user's profile photo on the server with local storage
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    editor.putString("profile_photo_url_" + email, photoUrl);
                } else {
                    editor.remove("profile_photo_url_" + email);
                }

                // Restore the language in which the user last left the app
                String savedLang = prefs.getString("language_" + email, "es");
                LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(savedLang);
                AppCompatDelegate.setApplicationLocales(appLocales);

                editor.apply();

                // Restore and apply the user's preferred dark mode before going to the main screen
                applyUserTheme(email);
                goToMainActivity();
            } else {
                String error = (String) json.get("error");
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            Toast.makeText(this, R.string.server_response_parse_error, Toast.LENGTH_SHORT).show();
        }
    }

    // Combines the account preferences of this email with the global preferences of the device
    // so that each user gets the dark or light theme they last chose.
    private void applyUserTheme(String email) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isDarkMode = prefs.getBoolean("dark_mode_" + email,
                defaultPrefs.getBoolean("modo_oscuro_activado", false));

        defaultPrefs.edit().putBoolean("modo_oscuro_activado", isDarkMode).apply();

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void handleError(String errorResponse) {
        if (errorResponse != null) {
            try {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(errorResponse);
                String error = (String) json.get("error");
                Toast.makeText(this, error != null ? error : getString(R.string.connection_failed), Toast.LENGTH_SHORT).show();
            } catch (ParseException e) {
                Toast.makeText(this, R.string.network_or_credentials_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.server_connection_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        // Finish the login Activity so that pressing 'Back' does not show the form again
        finish();
    }

    private void saveUserState(boolean isGuestUser) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", isGuestUser);
        editor.apply();
    }

    // Logic to redraw the view immediately when a new Locale is selected
    private void changeLoginLanguage(String langCode, TextView activeButton, TextView... inactiveButtons) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
        highlightActiveLanguage(activeButton, inactiveButtons);
    }

    private void highlightActiveLanguage(TextView activeButton, TextView... inactiveButtons) {
        activeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.bilbao_red));
        activeButton.setTextColor(ContextCompat.getColor(this, R.color.white));
        for (TextView tv : inactiveButtons) {
            tv.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }
}