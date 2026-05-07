package com.example.unigo_das.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.unigo_das.R;
import com.example.unigo_das.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    // Variables de dominio
    private GoogleMap campusMap;
    private EditText campusSearchInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enlazamos la vista del buscador
        campusSearchInput = view.findViewById(R.id.campus_search_input);


        // TODO: En el futuro, aquí se implementará el TextWatcher (RxJava/Debounce) para la búsqueda geolocalizada.

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        View campusSearchCard = view.findViewById(R.id.campus_search_card);

        // Protegemos el buscador flotante de la barra de notificaciones
        ViewCompat.setOnApplyWindowInsetsListener(campusSearchCard, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            // Sumamos la altura de la barra de estado (insets.top) al margen original (16dp)
            mlp.topMargin = insets.top + (int) (16 * getResources().getDisplayMetrics().density);
            v.setLayoutParams(mlp);

            return windowInsets;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.campusMap = googleMap;

        // Latitud y Longitud de la Escuela de Ingeniería de Bilbao (Edificio II)
        LatLng campusLocationEIB = new LatLng(43.2638, -2.9511);

        campusMap.addMarker(new MarkerOptions()
                .position(campusLocationEIB)
                .title("Escuela de Ingeniería de Bilbao (EIB)"));

        campusMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocationEIB, 16f));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevención de fugas de memoria (Memory Leaks): anulamos las referencias a las vistas
        campusSearchInput = null;
        campusMap = null;
    }
}