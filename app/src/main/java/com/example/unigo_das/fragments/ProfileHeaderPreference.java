package com.example.unigo_das.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.example.unigo_das.R;

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
            
            // Lógica: Si es invitado, silueta. Si no, podrías cargar la foto real.
            profileImage.setImageResource(R.drawable.ic_account_circle);
        }
    }
}