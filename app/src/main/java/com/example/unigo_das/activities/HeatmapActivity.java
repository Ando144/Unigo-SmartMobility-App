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

    // Fix F1: fixed values for the Intent extra. Before, the caller sent the
    // translated button text ("AIR", "LUFT", ...), so in every language except
    // Spanish the comparison with "AIRE" failed and the pollen map was shown.
    public static final String EXTRA_MAP_TYPE = "TIPO_MAPA";
    public static final String MAP_TYPE_AIR = "AIRE";
    public static final String MAP_TYPE_POLLEN = "POLEN";

    private String mapType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap);

        // We receive whether the user wants to see AIR or POLLEN
        mapType = getIntent().getStringExtra(EXTRA_MAP_TYPE);

        // Back button
        findViewById(R.id.btnCloseMap).setOnClickListener(new View.OnClickListener() {            @Override
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
        // Centre on Bilbao by default with zoom 10f, so the heat clouds can be seen
        LatLng bilbao = new LatLng(43.2630, -2.9350);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bilbao, 10f));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Create the provider of the heatmap layer
        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                String apiKey = BuildConfig.DIRECTIONS_API_KEY;
                String url;

                if (MAP_TYPE_AIR.equals(mapType)) {
                    // Google Air Quality API
                    url = "https://airquality.googleapis.com/v1/mapTypes/UAQI_INDIGO_PERSIAN/heatmapTiles/" + zoom + "/" + x + "/" + y + "?key=" + apiKey;
                } else {
                    // Google Pollen API
                    url = "https://pollen.googleapis.com/v1/mapTypes/TREE_UPI/heatmapTiles/" + zoom + "/" + x + "/" + y + "?key=" + apiKey;
                }

                try {
                    return new URL(url);
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        };

        googleMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(tileProvider)
                .fadeIn(false)
                .transparency(0.2f)); // A little transparency keeps the streets below readable
    }
}