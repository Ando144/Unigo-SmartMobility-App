package com.example.unigo_das.fragments;

import android.app.Dialog;
import android.os.Bundle;

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
        // Intentar obtener el listener del fragmento padre o de la actividad
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
        
        String[] languageCodes = {"es", "eu", "en"};

        // Obtener el idioma actual desde AppCompatDelegate (API persistente)
        String currentLang = "es";
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (!currentLocales.isEmpty()) {
            currentLang = currentLocales.get(0).getLanguage();
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
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selectedLang = languageCodes[which];
                    updateLanguage(selectedLang);
                    dismiss();
                });

        return builder.create();
    }

    private void updateLanguage(String langCode) {
        // AppCompatDelegate.setApplicationLocales gestiona la persistencia global del idioma.
        // Esto garantiza que el idioma no se pierda al cambiar de Modo Claro a Oscuro.
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);

        if (listener != null) {
            listener.onLanguageChanged();
        }
    }
}