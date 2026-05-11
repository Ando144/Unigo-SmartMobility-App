package com.example.unigo_das.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.unigo_das.R;

public class ProfileDialogFragment extends DialogFragment {

    public interface ProfileAction {
        void onRemovePhoto();
    }

    // Nuevo callback para pasar la URI de la imagen seleccionada
    public interface OnImageSelectedListener {
        void onImageSelected(Intent data);
    }

    private ProfileAction listener;
    private OnImageSelectedListener imageSelectedListener;

    public void setListener(ProfileAction listener) {
        this.listener = listener;
    }

    public void setImageSelectedListener(OnImageSelectedListener imageSelectedListener) {
        this.imageSelectedListener = imageSelectedListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.profile_image_options)
                .setItems(new CharSequence[]{
                        getString(R.string.remove_photo),
                        getString(R.string.change_photo)
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            mostrarConfirmacionEliminar();
                        } else {
                            abrirGaleria();
                        }
                    }
                });
        return builder.create();
    }

    private void mostrarConfirmacionEliminar() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.eliminar_foto)
                .setMessage(R.string.est_s_seguro_de_que_quieres_eliminar_tu_foto_de_perfil)
                .setPositiveButton(R.string.eliminar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.onRemovePhoto();
                        }
                    }
                })
                .setNegativeButton(R.string.cancelar7, null)
                .show();
    }

    private void abrirGaleria() {
        // Usar el imageSelectedListener para comunicarse con SettingsFragment
        if (imageSelectedListener != null) {
            // Crear un launcher temporal (esto se manejará en SettingsFragment)
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // Devolver el intent a SettingsFragment para que use su propio launcher
            imageSelectedListener.onImageSelected(intent);
        }
    }
}