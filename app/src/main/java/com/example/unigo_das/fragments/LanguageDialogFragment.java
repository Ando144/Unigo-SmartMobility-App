package com.example.unigo_das.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.DialogFragment;

import com.example.unigo_das.R;

public class LanguageDialogFragment extends DialogFragment {

    public interface LanguageChangeListener {
        void onLanguageChanged();
    }

    private LanguageChangeListener listener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() instanceof LanguageChangeListener) {
            listener = (LanguageChangeListener) getParentFragment();
        } else if (getActivity() instanceof LanguageChangeListener) {
            listener = (LanguageChangeListener) getActivity();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String[] languages = {
                getString(R.string.lang_es),
                getString(R.string.lang_eu),
                getString(R.string.lang_en)
        };

        final String[] languageCodes = {"es", "eu", "en"};

        final String currentLang;
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (!currentLocales.isEmpty()) {
            currentLang = currentLocales.get(0).getLanguage();
        } else {
            currentLang = "es";
        }

        int checkedItem = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLang)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.select_language)
                .setSingleChoiceItems(languages, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedLang = languageCodes[which];
                        dismiss();

                        // Solo hacer algo si el idioma es diferente
                        if (!selectedLang.equals(currentLang)) {
                            updateLanguage(selectedLang);
                        }
                    }
                });

        return builder.create();
    }

    private void updateLanguage(final String langCode) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);

        // Guardar preferencia por usuario
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        if (!userEmail.isEmpty()) {
            prefs.edit().putString("language_" + userEmail, langCode).apply();
        }

        // Notificar el cambio después de un delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && listener != null) {
                listener.onLanguageChanged();
            }
        }, 300);
    }
}