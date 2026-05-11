package com.example.unigo_das.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.unigo_das.R;
import com.example.unigo_das.network.NetworkWorker;
import com.google.android.material.textfield.TextInputLayout;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ChangePasswordDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_password, null);

        TextInputLayout tilOld = view.findViewById(R.id.tilOldPassword);
        TextInputLayout tilNew = view.findViewById(R.id.tilNewPassword);
        EditText etOld = view.findViewById(R.id.etOldPassword);
        EditText etNew = view.findViewById(R.id.etNewPassword);

        builder.setView(view)
                .setTitle(R.string.change_password)
                .setPositiveButton(R.string.aplicar, null)
                .setNegativeButton(R.string.cancelar7, null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String oldPass = etOld.getText().toString().trim();
                String newPass = etNew.getText().toString().trim();

                boolean isValid = true;
                if (oldPass.isEmpty()) {
                    tilOld.setError(getString(R.string.required_field));
                    isValid = false;
                } else {
                    tilOld.setError(null);
                }
                if (newPass.length() < 6) {
                    tilNew.setError(getString(R.string.password_length_error));
                    isValid = false;
                } else {
                    tilNew.setError(null);
                }

                if (isValid) {
                    // Deshabilitar botones para evitar doble click
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                    cambiarContrasena(oldPass, newPass, dialog);
                }
            });
        });

        return dialog;
    }

    private void cambiarContrasena(String oldPass, String newPass, AlertDialog dialog) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", "");

        Data inputData = new Data.Builder()
                .putString("script", "cambiar_password.php")
                .putString("email", email)
                .putString("old_password", oldPass)
                .putString("new_password", newPass)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NetworkWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(workRequest);

        // Usamos getWorkInfoByIdLiveData con observeForever para que funcione aunque el diálogo se cierre
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(workRequest.getId())
                .observeForever(new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState().isFinished()) {
                            // Quitamos el observer para evitar fugas de memoria
                            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(workRequest.getId())
                                    .removeObserver(this);

                            String response = workInfo.getOutputData().getString("response");

                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                procesarRespuestaCambio(response, dialog);
                            } else {
                                // Si el worker falló (timeout, etc.)
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), R.string.network_error, Toast.LENGTH_LONG).show();
                                }
                                // Re-habilitar botones si el diálogo sigue abierto
                                if (dialog.isShowing()) {
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                                }
                            }
                        }
                    }
                });
    }

    private void procesarRespuestaCambio(String jsonResponse, AlertDialog dialog) {
        if (jsonResponse == null || getContext() == null) {
            if (dialog.isShowing()) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
            }
            return;
        }

        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonResponse);

            if (json.containsKey("success") && (Boolean) json.get("success")) {
                // Cerrar el diálogo inmediatamente
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                // Mostrar el Toast después de cerrar el diálogo
                Toast.makeText(getContext(), R.string.password_changed_success, Toast.LENGTH_LONG).show();
            } else {
                String error = json.containsKey("error") ? (String) json.get("error") : getString(R.string.error_generic);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                // Re-habilitar botones para que el usuario pueda corregir
                if (dialog.isShowing()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.server_error, Toast.LENGTH_LONG).show();
            if (dialog.isShowing()) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
            }
        }
    }
}