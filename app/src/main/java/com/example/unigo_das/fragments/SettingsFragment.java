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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        customPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        boolean isGuest = customPrefs.getBoolean("isGuest", true);

        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
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
                        // AÑADIR ESTE LISTENER:
                        dialog.setImageSelectedListener(new ProfileDialogFragment.OnImageSelectedListener() {
                            @Override
                            public void onImageSelected(Intent intent) {
                                // Lanzar la galería usando el launcher del fragmento
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
                login.setOnPreferenceClickListener(p -> {
                    // Eliminamos el forzado a modo claro aquí, ya que LoginActivity ya tiene su propio tema en el Manifest.
                    // Mantener el modo noche actual para que se aplique al volver.
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    return true;
                });
            }
        } else {
            if (myAccount != null) {
                myAccount.setVisible(true);
                myAccount.setSummary(customPrefs.getString("user_email", "usuario@example.com"));
                myAccount.setOnPreferenceClickListener(p -> {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new AccountDetailFragment())
                            .addToBackStack(null)
                            .commit();
                    return true;
                });
            }
            if (logout != null) {
                logout.setVisible(true);
                logout.setOnPreferenceClickListener(p -> {
                    // Limpiar datos de usuario pero mantener su preferencia guardada
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
            if (login != null) login.setVisible(false);
        }
    }

    private void setupGeneralConfig() {
        Preference languagePref = findPreference("pref_language");
        if (languagePref != null) {
            languagePref.setOnPreferenceClickListener(preference -> {
                LanguageDialogFragment dialog = new LanguageDialogFragment();
                dialog.show(getChildFragmentManager(), "LanguageDialog");
                return true;
            });
        }

        darkModePref = findPreference("modo_oscuro_activado");
        if (darkModePref != null) {
            syncDarkModeSwitch();
            darkModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = (boolean) newValue;

                // Guardar en preferencias generales
                SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                defaultPrefs.edit().putBoolean("modo_oscuro_activado", checked).apply();

                // Guardar preferencia personal del usuario
                SharedPreferences unigoPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
                boolean isGuest = unigoPrefs.getBoolean("isGuest", true);

                if (!isGuest) {
                    String userEmail = unigoPrefs.getString("user_email", "");
                    if (!userEmail.isEmpty()) {
                        // Guardar preferencia específica para este usuario
                        unigoPrefs.edit().putBoolean("dark_mode_" + userEmail, checked).apply();
                    }
                }

                AppCompatDelegate.setDefaultNightMode(checked ?
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                return true;
            });
        }
    }

    @Override
    public void onLanguageChanged() {
        // Guardar preferencia de idioma para este usuario
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
            String currentLang = currentLocales.isEmpty() ? "es" : currentLocales.get(0).getLanguage();
            prefs.edit().putString("language_" + userEmail, currentLang).apply();
        }

        // Recrear la activity solo si el fragmento sigue añadido
        if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
            getActivity().recreate();
        }
    }

    private void mostrarConfirmacionFoto(final Uri imageUri) {
        try {
            // Cargar la imagen seleccionada
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                // Crear un diálogo de confirmación con vista previa
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Confirmar foto de perfil");

                // Crear un ImageView para la vista previa
                ImageView imageView = new ImageView(requireContext());
                imageView.setImageBitmap(bitmap);

                // Redimensionar para vista previa
                int size = (int) (200 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.gravity = Gravity.CENTER;
                params.setMargins(16, 16, 16, 16);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Añadir borde circular
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

    private void guardarFotoPerfil(Bitmap bitmap) {
        String userEmail = customPrefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            // Mostrar progreso
            final ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Subiendo foto...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Subir en hilo secundario
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Convertir bitmap a archivo temporal
                        File tempFile = new File(requireContext().getCacheDir(), "temp_profile.jpg");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                        fos.close();

                        // Preparar la petición multipart
                        String boundary = "*****" + System.currentTimeMillis() + "*****";
                        String url = "http://35.233.9.137/subir_foto.php";

                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

                        // Añadir campo email
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"email\"\r\n\r\n");
                        dos.writeBytes(userEmail + "\r\n");

                        // Añadir imagen
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

                        // Leer respuesta
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }

                        final String respuestaJson = response.toString();

                        // Volver al hilo principal
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
            // Enviar petición al servidor
            Data inputData = new Data.Builder()
                    .putString("script", "eliminar_foto.php")
                    .putString("email", userEmail)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NetworkWorker.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(requireContext()).enqueue(request);

            // Eliminar localmente
            customPrefs.edit().remove("profile_photo_url_" + userEmail).apply();
            requireActivity().recreate();
        }
    }
}