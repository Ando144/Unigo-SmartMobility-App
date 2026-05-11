package com.example.unigo_das.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.example.unigo_das.R;
import com.example.unigo_das.activities.LoginActivity;

public class SettingsFragment extends PreferenceFragmentCompat implements LanguageDialogFragment.LanguageChangeListener {

    private SharedPreferences customPrefs;
    private SwitchPreferenceCompat darkModePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        customPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        boolean isGuest = customPrefs.getBoolean("isGuest", true);

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
            profileHeader.setOnPreferenceClickListener(preference -> {
                ProfileDialogFragment dialog = new ProfileDialogFragment();
                dialog.setListener(() -> {
                    // Lógica para eliminar foto
                });
                dialog.show(getParentFragmentManager(), "ProfileDialog");
                return true;
            });
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
                            .putString("user_email", null) // Limpiar email
                            .apply();

                    // Volver al modo claro por defecto al cerrar sesión
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
        if (isAdded() && getActivity() != null) {
            // Forzamos la recreación de la actividad para aplicar los cambios de idioma a toda la UI
            getActivity().recreate();
        }
    }
}