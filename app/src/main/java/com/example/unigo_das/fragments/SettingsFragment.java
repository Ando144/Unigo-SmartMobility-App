package com.example.unigo_das.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.unigo_das.R;
import com.example.unigo_das.activities.LoginActivity;
import com.example.unigo_das.network.NetworkWorker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.app.ProgressDialog;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsFragment extends PreferenceFragmentCompat implements LanguageDialogFragment.LanguageChangeListener {

    private SharedPreferences customPrefs;
    private SwitchPreferenceCompat darkModePref;
    private ActivityResultLauncher<Intent> galleryLauncher;

    // Launcher to request the notification permission.
    // If the user accepts, the alarm is scheduled; if not, the switch is turned off automatically
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), new androidx.activity.result.ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        androidx.preference.SwitchPreferenceCompat switchNotif = findPreference("pref_notificaciones_clima");
                        if (switchNotif != null && switchNotif.isChecked()) {
                            android.content.SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                            String currentTime = prefs.getString("pref_hora_notificacion", "07:30");
                            int h = Integer.parseInt(currentTime.split(":")[0]);
                            int m = Integer.parseInt(currentTime.split(":")[1]);
                            scheduleDailyAlarm(requireContext(), h, m);
                        }
                    } else {
                        androidx.preference.SwitchPreferenceCompat switchNotif = findPreference("pref_notificaciones_clima");
                        if (switchNotif != null) {
                            switchNotif.setChecked(false);
                        }
                    }
                }
            });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, new androidx.core.view.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, insets.top, 0, 0);
                return windowInsets;
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // The hierarchy is inflated from XML, but all the logic is handled here.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        customPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        String userEmail = customPrefs.getString("user_email", "");

        // ===================================================================
        // NOTIFICATION TIME AND NOTIFICATIONS SWITCH
        // ===================================================================
        final androidx.preference.Preference timePref = findPreference("pref_hora_notificacion");
        final androidx.preference.SwitchPreferenceCompat switchNotif = findPreference("pref_notificaciones_clima");

        if (timePref != null && switchNotif != null) {
            final android.content.SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            timePref.setSummary(prefs.getString("pref_hora_notificacion", "07:30"));

            boolean notificationsOn = false;
            if (!userEmail.isEmpty()) {
                notificationsOn = customPrefs.getBoolean("pref_notificaciones_" + userEmail, false);
            }
            switchNotif.setChecked(notificationsOn);

            timePref.setOnPreferenceClickListener(preference -> {
                String currentTime = prefs.getString("pref_hora_notificacion", "07:30");
                int h = Integer.parseInt(currentTime.split(":")[0]);
                int m = Integer.parseInt(currentTime.split(":")[1]);

                android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(requireContext(),
                        (view, hourOfDay, minute) -> {
                            String formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                            prefs.edit().putString("pref_hora_notificacion", formattedTime).apply();
                            timePref.setSummary(formattedTime);

                            // If the time changes and the switch is on, the alarm is rescheduled with the new time.
                            if (switchNotif.isChecked()) {
                                scheduleDailyAlarm(requireContext(), hourOfDay, minute);
                            }
                        }, h, m, true);
                timePicker.show();
                return true;
            });

            switchNotif.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean notificationsEnabled = (Boolean) newValue;

                if (!userEmail.isEmpty()) {
                    customPrefs.edit().putBoolean("pref_notificaciones_" + userEmail, notificationsEnabled).apply();
                }

                if (notificationsEnabled) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                            return true;
                        }
                    }
                    String currentTime = prefs.getString("pref_hora_notificacion", "07:30");
                    int h = Integer.parseInt(currentTime.split(":")[0]);
                    int m = Integer.parseInt(currentTime.split(":")[1]);
                    scheduleDailyAlarm(requireContext(), h, m);
                } else {
                    cancelAlarm(requireContext());
                }
                return true;
            });
        }

        // ===================================================================
        // FAVOURITE TRANSPORT
        // ===================================================================
        final androidx.preference.Preference transportPref = findPreference("pref_transporte_favorito");
        if (transportPref != null) {
            final android.content.SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            final String[] transportNames = getResources().getStringArray(R.array.transport_names);
            final String[] transportValues = getResources().getStringArray(R.array.transport_values);

            String currentTransport;
            if (!userEmail.isEmpty()) {
                currentTransport = customPrefs.getString("transporte_favorito_" + userEmail, "publico");
            } else {
                currentTransport = prefs.getString("pref_transporte_favorito", "publico");
            }
            for (int i = 0; i < transportValues.length; i++) {
                if (transportValues[i].equals(currentTransport)) transportPref.setSummary(transportNames[i]);
            }

            transportPref.setOnPreferenceClickListener(preference -> {
                int selectedIndex = 0;
                String saved;
                if (!userEmail.isEmpty()) {
                    saved = customPrefs.getString("transporte_favorito_" + userEmail, "publico");
                } else {
                    saved = prefs.getString("pref_transporte_favorito", "publico");
                }
                for (int i = 0; i < transportValues.length; i++) {
                    if (transportValues[i].equals(saved)) selectedIndex = i;
                }

                com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
                builder.setTitle(R.string.favorite_transport);
                builder.setSingleChoiceItems(transportNames, selectedIndex, (dialog, which) -> {
                    String chosenValue = transportValues[which];

                    if (!userEmail.isEmpty()) {
                        customPrefs.edit().putString("transporte_favorito_" + userEmail, chosenValue).apply();
                    } else {
                        prefs.edit().putString("pref_transporte_favorito", chosenValue).apply();
                    }
                    transportPref.setSummary(transportNames[which]);

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> dialog.dismiss(), 150);
                });
                builder.setNegativeButton(R.string.cancel3, null);
                builder.show();
                return true;
            });
        }

        // ===================================================================
        // MAP STYLE
        // ===================================================================
        final androidx.preference.Preference mapPref = findPreference("pref_tipo_mapa");
        if (mapPref != null) {
            final android.content.SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            final String[] mapTypeNames = getResources().getStringArray(R.array.map_type_names);
            final String[] mapTypeValues = getResources().getStringArray(R.array.map_type_values);

            String currentMapType;
            if (!userEmail.isEmpty()) {
                currentMapType = customPrefs.getString("tipo_mapa_" + userEmail, "1");
            } else {
                currentMapType = prefs.getString("pref_tipo_mapa", "1");
            }
            for (int i = 0; i < mapTypeValues.length; i++) {
                if (mapTypeValues[i].equals(currentMapType)) mapPref.setSummary(mapTypeNames[i]);
            }

            mapPref.setOnPreferenceClickListener(preference -> {
                int selectedIndex = 0;
                String saved;
                if (!userEmail.isEmpty()) {
                    saved = customPrefs.getString("tipo_mapa_" + userEmail, "1");
                } else {
                    saved = prefs.getString("pref_tipo_mapa", "1");
                }
                for (int i = 0; i < mapTypeValues.length; i++) {
                    if (mapTypeValues[i].equals(saved)) selectedIndex = i;
                }

                com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
                builder.setTitle(R.string.map_style);
                builder.setSingleChoiceItems(mapTypeNames, selectedIndex, (dialog, which) -> {
                    String chosenValue = mapTypeValues[which];

                    if (!userEmail.isEmpty()) {
                        customPrefs.edit().putString("tipo_mapa_" + userEmail, chosenValue).apply();
                    } else {
                        prefs.edit().putString("pref_tipo_mapa", chosenValue).apply();
                    }
                    mapPref.setSummary(mapTypeNames[which]);

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> dialog.dismiss(), 150);
                });
                builder.setNegativeButton(R.string.cancel4, null);
                builder.show();
                return true;
            });
        }

        // ===================================================================
        // ELECTRIC CAR - saved per user
        // ===================================================================
        SwitchPreferenceCompat electricCarPref = findPreference("pref_coche_electrico");
        if (electricCarPref != null) {
            boolean electricCarState = false;
            if (!userEmail.isEmpty()) {
                electricCarState = customPrefs.getBoolean("pref_coche_electrico_" + userEmail, false);
            }
            electricCarPref.setChecked(electricCarState);

            electricCarPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = (boolean) newValue;
                if (!userEmail.isEmpty()) {
                    customPrefs.edit().putBoolean("pref_coche_electrico_" + userEmail, checked).apply();
                } else {
                    getPreferenceManager().getSharedPreferences().edit().putBoolean("pref_coche_electrico", checked).apply();
                }
                return true;
            });
        }

        // ===================================================================
        // OPEN DATA -- CREDITS
        // ===================================================================
        androidx.preference.Preference openDataPref = findPreference("pref_open_data");
        if (openDataPref != null) {
            openDataPref.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.data_sources)
                        .setMessage(R.string.data_sources_acknowledgement)
                        .setPositiveButton(R.string.accept, null)
                        .show();
                return true;
            });
        }

        // ===================================================================
        // FINAL INITIALISATION
        // ===================================================================
        boolean isGuest = customPrefs.getBoolean("isGuest", true);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        android.net.Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            showPhotoConfirmation(imageUri);
                        }
                    }
                }
        );

        setupProfileHeader();
        setupAccountSection(isGuest);
        setupGeneralConfig();
    }

    // AlarmManager is used so the operating system runs our notification at an exact time.
    private void scheduleDailyAlarm(Context context, int alarmHour, int alarmMinute) {
        // Fix F2: the scheduling code moved to WeatherAlarmScheduler, so the receiver
        // can schedule the next day's notification too.
        com.example.unigo_das.receivers.WeatherAlarmScheduler.scheduleDailyAlarm(context, alarmHour, alarmMinute);

        String alertTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", alarmHour, alarmMinute);
        Toast.makeText(context, getString(R.string.alert_scheduled_for) + alertTime, Toast.LENGTH_SHORT).show();
    }

    private void cancelAlarm(Context context) {
        com.example.unigo_das.receivers.WeatherAlarmScheduler.cancelAlarm(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncDarkModeSwitch();
    }

    private void syncDarkModeSwitch() {
        if (darkModePref != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean isDark = prefs.getBoolean("modo_oscuro_activado", false);
            darkModePref.setChecked(isDark);
        }
    }

    private void setupProfileHeader() {
        Preference profileHeader = findPreference("pref_profile_header");
        if (profileHeader != null) {
            boolean isGuest = customPrefs.getBoolean("isGuest", true);

            if (isGuest) {
                profileHeader.setEnabled(false);
            } else {
                profileHeader.setEnabled(true);
                profileHeader.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ProfileDialogFragment dialog = new ProfileDialogFragment();
                        dialog.setListener(new ProfileDialogFragment.ProfileAction() {
                            @Override
                            public void onRemovePhoto() {
                                deleteProfilePhoto();
                            }
                        });
                        dialog.setImageSelectedListener(new ProfileDialogFragment.OnImageSelectedListener() {
                            @Override
                            public void onImageSelected(Intent intent) {
                                galleryLauncher.launch(intent);
                            }
                        });
                        dialog.show(getParentFragmentManager(), "ProfileDialog");
                        return true;
                    }
                });
            }
        }
    }

    // Dynamically sets which parts of the menu are shown for a guest or for a user with an account.
    private void setupAccountSection(boolean isGuest) {
        Preference myAccount = findPreference("pref_my_account");
        Preference logout = findPreference("pref_logout");
        Preference login = findPreference("pref_login");

        Preference emailInfo = findPreference("pref_email_info");
        Preference nameInfo = findPreference("pref_name_info");
        Preference changePassword = findPreference("pref_change_password");

        if (isGuest) {
            if (myAccount != null) myAccount.setVisible(false);
            if (logout != null) logout.setVisible(false);
            if (emailInfo != null) emailInfo.setVisible(false);
            if (nameInfo != null) nameInfo.setVisible(false);
            if (changePassword != null) changePassword.setVisible(false);

            if (login != null) {
                login.setVisible(true);
                login.setOnPreferenceClickListener(p -> {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    return true;
                });
            }
        } else {
            if (login != null) login.setVisible(false);
            if (myAccount != null) myAccount.setVisible(false);

            String userEmail = customPrefs.getString("user_email", "");
            String userName = customPrefs.getString("user_name", getString(R.string.name_label));

            if (emailInfo != null) {
                emailInfo.setVisible(true);
                emailInfo.setSummary(userEmail);
            }
            if (nameInfo != null) {
                nameInfo.setVisible(true);
                nameInfo.setSummary(userName);
            }
            if (changePassword != null) {
                changePassword.setVisible(true);
                changePassword.setOnPreferenceClickListener(p -> {
                    ChangePasswordDialogFragment dialog = new ChangePasswordDialogFragment();
                    dialog.show(getParentFragmentManager(), "ChangePasswordDialog");
                    return true;
                });
            }

            if (logout != null) {
                logout.setVisible(true);
                logout.setOnPreferenceClickListener(p -> {
                    customPrefs.edit()
                            .putBoolean("isGuest", true)
                            .putString("user_email", null)
                            .remove("user_id")
                            .apply();
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    requireActivity().recreate();
                    return true;
                });
            }
        }
    }

    private void setupGeneralConfig() {
        final Preference languagePref = findPreference("pref_language");
        if (languagePref != null) {
            updateLanguageSummary(languagePref);

            languagePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final String[] languageNames = {"Castellano", "Euskara", "English", "Français", "Deutsch", "Italiano"};
                    final String[] codes = {"es", "eu", "en", "fr", "de", "it"};

                    LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
                    String activeLang = currentLocales.isEmpty() ? "es" : currentLocales.get(0).getLanguage();
                    int selectedIndex = 0;
                    for (int i = 0; i < codes.length; i++) {
                        if (codes[i].equals(activeLang)) selectedIndex = i;
                    }

                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.choose_language))
                            .setSingleChoiceItems(languageNames, selectedIndex, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, int which) {
                                    final String newCode = codes[which];

                                    // Close the dialog before applying the language change.
                                    // AppCompatDelegate restarts the Activity when the Locale changes, to redraw the texts.
                                    // If the dialog is still open during that restart, the app crashes with
                                    // a 'WindowLeaked' exception because the window reference is lost.
                                    dialog.dismiss();

                                    String userEmail = customPrefs.getString("user_email", "");
                                    if (!userEmail.isEmpty()) {
                                        customPrefs.edit().putString("language_" + userEmail, newCode).apply();
                                    }

                                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newCode));
                                        }
                                    }, 150);
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                    return true;
                }
            });
        }

        darkModePref = findPreference("modo_oscuro_activado");
        if (darkModePref != null) {
            syncDarkModeSwitch();
            darkModePref.setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(androidx.preference.Preference preference, Object newValue) {
                    boolean checked = (boolean) newValue;

                    SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    defaultPrefs.edit().putBoolean("modo_oscuro_activado", checked).apply();

                    SharedPreferences unigoPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
                    boolean isGuest = unigoPrefs.getBoolean("isGuest", true);

                    if (!isGuest) {
                        String userEmail = unigoPrefs.getString("user_email", "");
                        if (!userEmail.isEmpty()) {
                            unigoPrefs.edit().putBoolean("dark_mode_" + userEmail, checked).apply();
                        }
                    }

                    AppCompatDelegate.setDefaultNightMode(checked ?
                            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                    return true;
                }
            });
        }
    }

    private void updateLanguageSummary(androidx.preference.Preference pref) {
        androidx.core.os.LocaleListCompat currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
        String lang = currentLocales.isEmpty() ? "es" : currentLocales.get(0).getLanguage();
        switch (lang) {
            case "eu": pref.setSummary("Euskara"); break;
            case "en": pref.setSummary("English"); break;
            case "fr": pref.setSummary("Français"); break;
            case "de": pref.setSummary("Deutsch"); break;
            case "it": pref.setSummary("Italiano"); break;
            default: pref.setSummary("Castellano"); break;
        }
    }

    @Override
    public void onLanguageChanged() {
        // The restart is now handled automatically by AppCompatDelegate.
    }

    private void showPhotoConfirmation(final Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(R.string.confirm_profile_photo);

                ImageView imageView = new ImageView(requireContext());
                imageView.setImageBitmap(bitmap);

                int size = (int) (200 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.gravity = Gravity.CENTER;
                params.setMargins(16, 16, 16, 16);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                imageView.setClipToOutline(true);
                imageView.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });

                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setGravity(Gravity.CENTER);
                layout.addView(imageView);

                builder.setView(layout);
                builder.setPositiveButton(R.string.accept3, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        uploadProfilePhoto(bitmap);
                    }
                });
                builder.setNegativeButton(R.string.cancel6, null);
                builder.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Image upload done manually with the native HttpURLConnection and multipart/form-data encoding.
    // Volley often has problems with large binary files, so the HTTP body is built by hand.
    private void uploadProfilePhoto(final Bitmap bitmap) {
        final String userEmail = customPrefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            final ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.uploading_photo));
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Fix F7: the context and the Activity are kept before starting the thread. Before,
            // requireContext()/requireActivity() were called on the background thread and crashed
            // the app (IllegalStateException) if the user left the screen during the upload.
            final Context appContext = requireContext().getApplicationContext();
            final Activity activity = requireActivity();

            // Network calls block the UI, so the work runs on a background Thread.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File tempFile = new File(appContext.getCacheDir(), "temp_profile.jpg");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos); // Compress the image so the server is not overloaded
                        fos.close();

                        // The boundary is the "separator" between the parts of the form (text and image)
                        String boundary = "*****" + System.currentTimeMillis() + "*****";
                        String url = "http://35.233.9.137/subir_foto.php";

                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

                        // The user's email
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"email\"\r\n\r\n");
                        dos.writeBytes(userEmail + "\r\n");

                        // The image file
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"foto\";filename=\"profile.jpg\"\r\n");
                        dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                        // Write the image bytes directly to the output stream
                        FileInputStream fis = new FileInputStream(tempFile);
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                        fis.close();

                        dos.writeBytes("\r\n");
                        dos.writeBytes("--" + boundary + "--\r\n");
                        dos.flush();
                        dos.close();

                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }

                        final String jsonReply = response.toString();

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();

                                try {
                                    JSONObject json = new JSONObject(jsonReply);
                                    if (json.getBoolean("success")) {
                                        String photoUrl = json.getString("foto_url");
                                        customPrefs.edit()
                                                .putString("profile_photo_url_" + userEmail, photoUrl)
                                                .apply();
                                        // Fix F7: the UI is only updated if the screen is still open
                                        if (!isAdded()) return;
                                        requireActivity().recreate();
                                        Toast.makeText(requireContext(), R.string.photo_updated, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    if (!isAdded()) return; // Fix F7
                                    Toast.makeText(requireContext(), R.string.photo_upload_error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                if (!isAdded()) return; // Fix F7: the screen was closed during the upload
                                Toast.makeText(requireContext(), R.string.connection_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }


    private void deleteProfilePhoto() {
        String userEmail = customPrefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            Data inputData = new Data.Builder()
                    .putString("script", "eliminar_foto.php")
                    .putString("email", userEmail)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NetworkWorker.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(request);

            customPrefs.edit().remove("profile_photo_url_" + userEmail).apply();
            requireActivity().recreate();
        }
    }
}