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

    private TextInputEditText etNameReg, etEmailReg, etPasswordReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etNameReg = findViewById(R.id.etNameReg);
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

        // Manual language selector for the registration screen
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

        btnDoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etNameReg.getText() != null ? etNameReg.getText().toString().trim() : "";
                String email = etEmailReg.getText() != null ? etEmailReg.getText().toString().trim() : "";
                String pass = etPasswordReg.getText() != null ? etPasswordReg.getText().toString().trim() : "";

                if (!name.isEmpty() && !email.isEmpty() && !pass.isEmpty()) {
                    performRegistration(name, email, pass);
                } else {
                    Toast.makeText(RegisterActivity.this, R.string.register_fill_fields, Toast.LENGTH_SHORT).show(); // Fix F5: text moved to strings.xml so it is translated to the 6 languages
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


    private void performRegistration(String username, String email, String password) {
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

        // Attach an Observer to the WorkManager LiveData to know the exact moment
        // when the Worker finishes its background work and returns the server data.
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            String response = workInfo.getOutputData().getString("response");
                            if (response != null && !response.isEmpty()) {
                                handleServerResponse(response);
                            } else {
                                Toast.makeText(RegisterActivity.this, R.string.empty_server_response_error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    // Manual parsing of the PHP script response.
    // json-simple is used to parse the string and check whether the MySQL insert succeeded.
    private void handleServerResponse(String jsonResponse) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);

            // If the backend returns success = true, we assume the user now exists in the DB.
            if (json.containsKey("success") && (Boolean) json.get("success")) {
                String email = etEmailReg.getText() != null ? etEmailReg.getText().toString().trim() : "";
                String name = etNameReg.getText() != null ? etNameReg.getText().toString().trim() : "";

                Toast.makeText(this, R.string.user_registered, Toast.LENGTH_SHORT).show(); // Fix F5: text moved to strings.xml so it is translated to the 6 languages

                // Persist the session locally so the user does not have to log in again when reopening the app
                saveUserState(email, name);
                if (json.containsKey("id")) {
                    SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putInt("user_id", ((Long) json.get("id")).intValue()).apply();
                }

                goToMainActivity();
            } else {
                // Show the exact error returned by PHP (e.g. "The email is already in use")
                String error = (String) json.get("error");
                Toast.makeText(this, "Error: " + (error != null ? error : getString(R.string.generic_failure)), Toast.LENGTH_LONG).show(); // Fix F5: text moved to strings.xml so it is translated to the 6 languages
            }
        } catch (ParseException e) {
            Toast.makeText(this, getString(R.string.server_error_with_response, jsonResponse), Toast.LENGTH_LONG).show(); // Fix F5: text moved to strings.xml so it is translated to the 6 languages
        }
    }

    // Centralised writing to SharedPreferences.
    // Clear the isGuest flag so the app knows it must show the options of a registered user.
    private void saveUserState(String email, String name) {
        SharedPreferences prefs = getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isGuest", false);
        editor.putString("user_email", email);
        editor.putString("user_name", name);
        editor.apply();
    }

    // When going to the main screen, the whole navigation stack is destroyed.
    // Otherwise the user could press Android's "Back" button on the main menu
    // and return to this registration screen while already logged in.
    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Delegates the language change to AppCompatDelegate, which recreates the Activity
    // and reloads the strings.xml of the newly selected Locale.
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