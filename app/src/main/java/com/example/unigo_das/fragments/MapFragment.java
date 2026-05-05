package com.example.unigo_das.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.unigo_das.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// Implementamos OnMapReadyCallback para ser notificados cuando el mapa esté listo
public class MapFragment extends Fragment implements OnMapReadyCallback {

    // Variable de dominio para gestionar el mapa del campus
    private GoogleMap campusMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflamos la vista. Asegúrate de que fragment_map.xml tiene un contenedor (ej. FrameLayout) con id "map_container"
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtenemos el SupportMapFragment dinámicamente usando el administrador de fragmentos hijos
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);

        // Verificamos nulos y cargamos el mapa de manera asíncrona para no bloquear la IU
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.campusMap = googleMap;

        // Utilizamos coordenadas extraídas de la cartografía de prueba para el reto UNIGO
        // Latitud y Longitud de la Escuela de Ingeniería de Bilbao (Edificio II)
        LatLng campusLocationEIB = new LatLng(43.2638, -2.9511);

        // Añadimos un marcador usando nomenclatura clara
        campusMap.addMarker(new MarkerOptions()
                .position(campusLocationEIB)
                .title("Escuela de Ingeniería de Bilbao (EIB)"));

        // Movemos y acercamos la cámara a nuestra localización objetivo
        campusMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocationEIB, 16f));

        // TODO: En un futuro, aquí podremos habilitar campusMap.setMyLocationEnabled(true)
        // una vez gestionado el permiso ACCESS_FINE_LOCATION.
    }
}