package com.example.unigo_das.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.unigo_das.R;

public class ProfileDialogFragment extends DialogFragment {

    public interface ProfileAction {
        void onRemovePhoto();
    }

    private ProfileAction listener;

    public void setListener(ProfileAction listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.profile_image_options)
                .setItems(new CharSequence[]{getString(R.string.remove_photo), getString(R.string.change_photo)}, (dialog, which) -> {
                    if (which == 0) {
                        if (listener != null) listener.onRemovePhoto();
                    } else {
                        openGallery();
                    }
                });
        return builder.create();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getActivity().startActivityForResult(intent, 1001); // Simplified for this context
    }
}