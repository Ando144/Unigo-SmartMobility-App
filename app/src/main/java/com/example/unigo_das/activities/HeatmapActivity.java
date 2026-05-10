package com.example.unigo_das.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unigo_das.R;
import com.example.unigo_das.BuildConfig;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

public class HeatmapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String tipoMapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap);

        // Recibimos si el usuario quiere ver AIRE o POLEN
        tipoMapa = getIntent().getStringExtra("TIPO_MAPA");

        // Botón de volver atrás
        findViewById(R.id.btnCerrarMapa).setOnClickListener(new View.OnClickListener() {            @Override
            public void onClick(View v) {
                finish();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_heatmap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Centramos en Bilbao por defecto con un zoom de 10f para apreciar las nubes de calor
        LatLng bilbao = new LatLng(43.2630, -2.9350);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bilbao, 10f));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Creamos el proveedor de la capa térmica (TileOverlay)
        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                String apiKey = BuildConfig.DIRECTIONS_API_KEY;
                String url;

                if ("AIRE".equals(tipoMapa)) {
                    // API de Calidad del Aire de Google (Índice UAQI)
                    url = "https://airquality.googleapis.com/v1/mapTypes/UAQI_INDIGO_PERSIAN/heatmapTiles/" + zoom + "/" + x + "/" + y + "?key=" + apiKey;
                } else {
                    // API de Polen de Google (Árboles - TREE_UPI)
                    url = "https://pollen.googleapis.com/v1/mapTypes/TREE_UPI/heatmapTiles/" + zoom + "/" + x + "/" + y + "?key=" + apiKey;
                }

                try {
                    return new URL(url);
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        };

        // Superponemos la capa térmica eliminando el "Fade In" para que la carga se perciba instantánea
        googleMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(tileProvider)
                .fadeIn(false)
                .transparency(0.2f)); // Un poco de transparencia mejora la legibilidad de las calles debajo
    }
}