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
                            showDeleteConfirmation();
                        } else {
                            openGallery();
                        }
                    }
                });
        return builder.create();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.confirm_delete_photo_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.onRemovePhoto();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel7, null)
                .show();
    }

    private void openGallery() {
        if (imageSelectedListener != null) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imageSelectedListener.onImageSelected(intent);
        }
    }
}