package com.example.unigo_das.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.example.unigo_das.R;

import java.io.InputStream;

public class ProfileHeaderPreference extends Preference {

    public ProfileHeaderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_profile_header);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ImageView profileImage = (ImageView) holder.findViewById(R.id.profile_image);
        if (profileImage != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
            boolean isGuest = prefs.getBoolean("isGuest", true);

            if (isGuest) {
                profileImage.setImageResource(R.drawable.ic_account_circle);
            } else {
                String userEmail = prefs.getString("user_email", "");
                String fotoUrl = prefs.getString("profile_photo_url_" + userEmail, null);

                if (fotoUrl != null && !fotoUrl.isEmpty()) {

                    cargarImagenDesdeUrl(profileImage, fotoUrl);
                } else {
                    profileImage.setImageResource(R.drawable.ic_account_circle);
                }
            }
        }
    }

    private void cargarImagenDesdeUrl(ImageView imageView, String url) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.net.URL urlObj = new java.net.URL(url);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) urlObj.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);


                    ((android.app.Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Bitmap decodificarBase64(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}