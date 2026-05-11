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
    private ActivityResultLauncher<Intent> galeriaLauncher;

    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), new androidx.activity.result.ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        androidx.preference.SwitchPreferenceCompat switchNotif = findPreference("pref_notificaciones_clima");
                        if (switchNotif != null && switchNotif.isChecked()) {
                            android.content.SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                            String horaActual = prefs.getString("pref_hora_notificacion", "07:30");
                            int h = Integer.parseInt(horaActual.split(":")[0]);
                            int m = Integer.parseInt(horaActual.split(":")[1]);
                            programarAlarmaDiaria(requireContext(), h, m);
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

        // 1. Añadimos el listener de Insets en onViewCreated para arreglar la superposición visual
        ViewCompat.setOnApplyWindowInsetsListener(view, new androidx.core.view.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                // Le damos el padding top al contenedor para que las preferencias bajen
                v.setPadding(0, insets.top, 0, 0);

                return windowInsets;
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        final androidx.preference.Preference horaPref = findPreference("pref_hora_notificacion");
        final androidx.preference.SwitchPreferenceCompat switchNotif = findPreference("pref_notificaciones_clima");

        if (horaPref != null && switchNotif != null) {
            final android.content.SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            horaPref.setSummary(prefs.getString("pref_hora_notificacion", "07:30"));

            horaPref.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    String horaActual = prefs.getString("pref_hora_notificacion", "07:30");
                    int h = Integer.parseInt(horaActual.split(":")[0]);
                    int m = Integer.parseInt(horaActual.split(":")[1]);

                    android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(requireContext(),
                            new android.app.TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                                    String horaFormateada = String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                                    prefs.edit().putString("pref_hora_notificacion", horaFormateada).apply();
                                    horaPref.setSummary(horaFormateada);

                                    if (switchNotif.isChecked()) {
                                        programarAlarmaDiaria(requireContext(), hourOfDay, minute);
                                    }
                                }
                            }, h, m, true);
                    timePicker.show();
                    return true;
                }
            });

            switchNotif.setOnPreferenceChangeListener(new androidx.preference.Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(androidx.preference.Preference preference, Object newValue) {
                    boolean activado = (Boolean) newValue;
                    if (activado) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                                return true;
                            }
                        }

                        String horaActual = prefs.getString("pref_hora_notificacion", "07:30");
                        int h = Integer.parseInt(horaActual.split(":")[0]);
                        int m = Integer.parseInt(horaActual.split(":")[1]);
                        programarAlarmaDiaria(requireContext(), h, m);
                    } else {
                        cancelarAlarma(requireContext());
                    }
                    return true;
                }
            });
        }

        androidx.preference.Preference openDataPref = findPreference("pref_open_data");
        if (openDataPref != null) {
            openDataPref.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Fuentes de Datos")
                            .setMessage("Esta aplicación utiliza datos abiertos proporcionados por:\n\n• Euskalmet\n• Open Data Euskadi\n• Bizkaibus\n• Euskotren y Metro Bilbao\n• GeoBilbao\n\nAgradecemos su labor en la apertura de datos para el Reto UNIGO.")
                            .setPositiveButton("Aceptar", null)
                            .show();
                    return true;
                }
            });
        }

        customPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        boolean isGuest = customPrefs.getBoolean("isGuest", true);

        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<androidx.activity.result.ActivityResult>() {
                    @Override
                    public void onActivityResult(androidx.activity.result.ActivityResult result) {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                            android.net.Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                mostrarConfirmacionFoto(imageUri);
                            }
                        }
                    }
                }
        );

        setupProfileHeader();
        setupAccountSection(isGuest);
        setupGeneralConfig();
    }

    private void programarAlarmaDiaria(Context context, int hora, int minuto) {
        java.util.Calendar calendario = java.util.Calendar.getInstance();
        calendario.set(java.util.Calendar.HOUR_OF_DAY, hora);
        calendario.set(java.util.Calendar.MINUTE, minuto);
        calendario.set(java.util.Calendar.SECOND, 0);
        calendario.set(java.util.Calendar.MILLISECOND, 0);

        if (calendario.getTimeInMillis() <= System.currentTimeMillis()) {
            calendario.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }

        android.app.AlarmManager gestor = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        android.content.Intent intentBC = new android.content.Intent(context, com.example.unigo_das.receivers.NotificacionClimaReceiver.class);

        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        android.app.PendingIntent ibc = android.app.PendingIntent.getBroadcast(context, 1, intentBC, flags);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            gestor.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendario.getTimeInMillis(), ibc);
        } else {
            gestor.setRepeating(android.app.AlarmManager.RTC_WAKEUP, calendario.getTimeInMillis(), android.app.AlarmManager.INTERVAL_DAY, ibc);
        }

        String horaAviso = String.format(java.util.Locale.getDefault(), "%02d:%02d", hora, minuto);
        android.widget.Toast.makeText(context, "Aviso programado para las " + horaAviso, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void cancelarAlarma(Context context) {
        android.app.AlarmManager gestor = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        android.content.Intent intentBC = new android.content.Intent(context, com.example.unigo_das.receivers.NotificacionClimaReceiver.class);

        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        android.app.PendingIntent ibc = android.app.PendingIntent.getBroadcast(context, 1, intentBC, flags);

        gestor.cancel(ibc);
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
                                eliminarFotoPerfil();
                            }
                        });
                        dialog.setImageSelectedListener(new ProfileDialogFragment.OnImageSelectedListener() {
                            @Override
                            public void onImageSelected(Intent intent) {
                                galeriaLauncher.launch(intent);
                            }
                        });
                        dialog.show(getParentFragmentManager(), "ProfileDialog");
                        return true;
                    }
                });
            }
        }
    }

    private void setupAccountSection(boolean isGuest) {
        Preference myAccount = findPreference("pref_my_account");
        Preference logout = findPreference("pref_logout");
        Preference login = findPreference("pref_login");

        if (isGuest) {
            if (myAccount != null) myAccount.setVisible(false);
            if (logout != null) logout.setVisible(false);
            if (login != null) {
                login.setVisible(true);
                login.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(androidx.preference.Preference p) {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                        return true;
                    }
                });
            }
        } else {
            if (myAccount != null) {
                myAccount.setVisible(true);
                myAccount.setSummary(customPrefs.getString("user_email", "usuario@example.com"));
                myAccount.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(androidx.preference.Preference p) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new AccountDetailFragment())
                                .addToBackStack(null)
                                .commit();
                        return true;
                    }
                });
            }
            if (logout != null) {
                logout.setVisible(true);
                logout.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(androidx.preference.Preference p) {
                        customPrefs.edit()
                                .putBoolean("isGuest", true)
                                .putString("user_email", null)
                                .remove("user_id")
                                .apply();

                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        requireActivity().recreate();
                        return true;
                    }
                });
            }
            if (login != null) login.setVisible(false);
        }
    }

    private void setupGeneralConfig() {
        Preference languagePref = findPreference("pref_language");
        if (languagePref != null) {
            languagePref.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    LanguageDialogFragment dialog = new LanguageDialogFragment();
                    dialog.show(getChildFragmentManager(), "LanguageDialog");
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

    @Override
    public void onLanguageChanged() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
            String currentLang = currentLocales.isEmpty() ? "es" : currentLocales.get(0).getLanguage();
            prefs.edit().putString("language_" + userEmail, currentLang).apply();
        }

        if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
            getActivity().recreate();
        }
    }

    private void mostrarConfirmacionFoto(final Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Confirmar foto de perfil");

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
                builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        guardarFotoPerfil(bitmap);
                    }
                });
                builder.setNegativeButton("Cancelar", null);
                builder.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void guardarFotoPerfil(final Bitmap bitmap) {
        final String userEmail = customPrefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            final ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Subiendo foto...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File tempFile = new File(requireContext().getCacheDir(), "temp_profile.jpg");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                        fos.close();

                        String boundary = "*****" + System.currentTimeMillis() + "*****";
                        String url = "http://35.233.9.137/subir_foto.php";

                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"email\"\r\n\r\n");
                        dos.writeBytes(userEmail + "\r\n");

                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"foto\";filename=\"profile.jpg\"\r\n");
                        dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

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

                        final String respuestaJson = response.toString();

                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();

                                try {
                                    JSONObject json = new JSONObject(respuestaJson);
                                    if (json.getBoolean("success")) {
                                        String fotoUrl = json.getString("foto_url");
                                        customPrefs.edit()
                                                .putString("profile_photo_url_" + userEmail, fotoUrl)
                                                .apply();
                                        requireActivity().recreate();
                                        Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(requireContext(), "Error al subir foto", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void eliminarFotoPerfil() {
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