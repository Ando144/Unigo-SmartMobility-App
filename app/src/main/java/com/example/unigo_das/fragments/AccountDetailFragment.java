package com.example.unigo_das.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.unigo_das.R;

public class AccountDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_detail, container, false);

        TextView tvEmail = view.findViewById(R.id.tvAccountEmail);
        TextView tvName = view.findViewById(R.id.tvAccountName);

        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        tvEmail.setText(prefs.getString("user_email", "usuario@example.com"));
        tvName.setText(prefs.getString("user_name", "Juan Pérez"));

        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            // Sin lógica funcional por ahora
        });

        return view;
    }
}