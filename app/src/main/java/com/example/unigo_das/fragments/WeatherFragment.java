package com.example.unigo_das.fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.unigo_das.R;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.item.Campus;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;
import java.io.StringReader;
import java.util.List;
import java.util.Locale;

public class WeatherFragment extends Fragment {

    private ImageView ivMainWeatherIcon;
    private TextView tvMainTemperature;
    private TextView tvBilbaoForecast;
    private TextView tvMainLocation;
    private TextView tvWeatherDescription;
    private WebView wvRainRadar;
    private FusedLocationProviderClient fusedLocationClient;

    private com.android.volley.RequestQueue requestQueue;

    // Fix F9: a single background thread for the Geocoder, closed in onDestroy()
    private final java.util.concurrent.ExecutorService geocoderExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        ivMainWeatherIcon = view.findViewById(R.id.ivMainWeatherIcon);
        tvMainTemperature = view.findViewById(R.id.tvMainTemperature);
        tvBilbaoForecast = view.findViewById(R.id.tvBilbaoForecast);
        tvMainLocation = view.findViewById(R.id.tvMainLocation);
        tvWeatherDescription = view.findViewById(R.id.tvWeatherDescription);
        wvRainRadar = view.findViewById(R.id.wvRainRadar);

        requestQueue = Volley.newRequestQueue(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        androidx.cardview.widget.CardView cvAirMap = view.findViewById(R.id.cvAirMap);
        androidx.cardview.widget.CardView cvPollenMap = view.findViewById(R.id.cvPollenMap);

        cvAirMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHeatmapScreen(com.example.unigo_das.activities.HeatmapActivity.MAP_TYPE_AIR);
            }
        });

        cvPollenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHeatmapScreen(com.example.unigo_das.activities.HeatmapActivity.MAP_TYPE_POLLEN);
            }
        });

        // Round the radar corners in code so it matches the card design
        wvRainRadar.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 40f);
            }
        });
        wvRainRadar.setClipToOutline(true);

        // Fixes the scroll problem: when the WebView is touched, the parent (the ScrollView)
        // is told not to intercept the touch, so the map can be moved without scrolling the screen.
        wvRainRadar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        loadRainViewerRadar();
        fetchLocationAndWeather();
        fetchOpenDataForecast();

        return view;
    }

    private void openHeatmapScreen(String mapType) {
        android.content.Intent intent = new android.content.Intent(requireContext(), com.example.unigo_das.activities.HeatmapActivity.class);
        intent.putExtra(com.example.unigo_das.activities.HeatmapActivity.EXTRA_MAP_TYPE, mapType);
        startActivity(intent);
    }

    // Main method that coordinates the weather data
    private void fetchLocationAndWeather() {
        if (!isAdded() || getContext() == null) return;

        // Without location permission, Bilbao is loaded by default
        // so the screen is not left empty.
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            updateWeatherWithName(43.26, -2.94, "Bilbao");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (!isAdded() || getContext() == null) return;
                if (location != null) {
                    // Fix F9: Geocoder.getFromLocation() blocks (it can use the network), so it now runs
                    // on a background thread instead of the main thread (risk of "App not responding").
                    final double lat = location.getLatitude();
                    final double lon = location.getLongitude();
                    final Context appContext = requireContext().getApplicationContext();
                    geocoderExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            final String city = lookUpCityName(appContext, lat, lon);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isAdded() || getContext() == null) return;
                                    updateWeatherWithName(lat, lon, city != null ? city : getString(R.string.your_location2));
                                }
                            });
                        }
                    });
                } else {
                    updateWeatherWithName(43.26, -2.94, "Bilbao");
                }
            }
        });
    }

    // Returns the city of the coordinates, or null if it cannot be found.
    // It runs on a background thread, so it only uses the application context (fix F9).
    private static String lookUpCityName(Context appContext, double lat, double lon) {
        Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getLocality();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateWeatherWithName(double lat, double lon, String name) {
        if (tvMainLocation != null) {
            tvMainLocation.setText(name);
        }
        fetchWeatherByCoordinates(lat, lon, ivMainWeatherIcon, tvMainTemperature);

        fetchAirQuality(lat, lon);
        fetchPollenLevel(lat, lon);
    }

    // Unlike OpenMeteo, the Google API requires the coordinates
    // as JSON inside a POST request, which is why the 'body' is prepared first.
    private void fetchAirQuality(double lat, double lon) {
        String url = "https://airquality.googleapis.com/v1/currentConditions:lookup?key=" + com.example.unigo_das.BuildConfig.DIRECTIONS_API_KEY;

        try {
            JSONObject body = new JSONObject();
            JSONObject location = new JSONObject();
            location.put("latitude", lat);
            location.put("longitude", lon);
            body.put("location", location);
            body.put("languageCode", "en");

            JsonObjectRequest apiRequest = new JsonObjectRequest(Request.Method.POST, url, body,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // It is very important to check isAdded() in the callbacks so the app
                            // does not crash if the user has already changed tab when the response arrives.
                            if (!isAdded() || getView() == null) return;
                            try {
                                String categoryEnglish = response.getJSONArray("indexes")
                                        .getJSONObject(0)
                                        .getString("category");

                                String translatedCategory = translateAirQuality(categoryEnglish);

                                TextView tvAirIndex = getView().findViewById(R.id.tvAirIndex);
                                if (tvAirIndex != null) {
                                    tvAirIndex.setText(translatedCategory);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                TextView tv = getView().findViewById(R.id.tvAirIndex);
                                if (tv != null) tv.setText(getString(R.string.error_api));
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (isAdded() && getView() != null) {
                                TextView tvAirIndex = getView().findViewById(R.id.tvAirIndex);
                                if (tvAirIndex != null) tvAirIndex.setText(getString(R.string.no_data_short));
                            }
                        }
                    }
            );
            requestQueue.add(apiRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GET request to the Google Pollen API
    private void fetchPollenLevel(double lat, double lon) {
        String url = "https://pollen.googleapis.com/v1/forecast:lookup?key=" + com.example.unigo_das.BuildConfig.DIRECTIONS_API_KEY +
                "&location.latitude=" + lat +
                "&location.longitude=" + lon +
                "&days=1&languageCode=en";

        JsonObjectRequest apiRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!isAdded() || getView() == null) return;
                        try {
                            if (!response.has("dailyInfo") || response.getJSONArray("dailyInfo").length() == 0) {
                                updatePollenText(getString(R.string.no_data_long));
                                return;
                            }

                            // Google returns several pollen types (grass, trees, weeds).
                            // Iterate over the array and keep the highest value to warn the user.
                            JSONObject daily = response.getJSONArray("dailyInfo").getJSONObject(0);
                            String maxLevel = "None";

                            if (daily.has("pollenTypeInfo")) {
                                org.json.JSONArray types = daily.getJSONArray("pollenTypeInfo");
                                int maxVal = -1;

                                for (int i = 0; i < types.length(); i++) {
                                    JSONObject item = types.getJSONObject(i);
                                    if (item.has("indexInfo")) {
                                        JSONObject indexInfo = item.getJSONObject("indexInfo");
                                        int val = indexInfo.optInt("value", 0);
                                        if (val >= maxVal) {
                                            maxVal = val;
                                            maxLevel = indexInfo.optString("category", "None");
                                        }
                                    }
                                }
                            }

                            updatePollenText(translatePollen(maxLevel));

                        } catch (Exception e) {
                            e.printStackTrace();
                            updatePollenText(getString(R.string.no_data_short));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded()) {
                            updatePollenText(getString(R.string.no_data_short));
                        }
                    }
                }
        );
        requestQueue.add(apiRequest);
    }

    private void updatePollenText(String text) {
        if (getView() != null) {
            TextView tv = getView().findViewById(R.id.tvPollenIndex);
            if (tv != null) tv.setText(text);
        }
    }

    private String translateAirQuality(String categoryEn) {
        if (categoryEn == null || !isAdded()) return getString(R.string.no_data_short);

        String cat = categoryEn.toLowerCase().trim();

        if (cat.contains("excellent")) return getString(R.string.air_excellent);
        if (cat.contains("good")) return getString(R.string.air_good);
        if (cat.contains("moderate")) return getString(R.string.air_moderate);
        // Fix F6: Google's Universal AQI also returns "Low air quality", which showed as "unknown"
        if (cat.contains("low")) return getString(R.string.air_low);
        if (cat.contains("poor") && !cat.contains("very")) return getString(R.string.air_poor);
        if (cat.contains("very poor")) return getString(R.string.air_very_poor);
        if (cat.contains("severe")) return getString(R.string.air_severe);

        return getString(R.string.air_unknown);
    }

    private String translatePollen(String categoryEn) {
        if (categoryEn == null || !isAdded()) return getString(R.string.no_data_short);

        String cat = categoryEn.toLowerCase().trim();

        if (cat.contains("none")) return getString(R.string.pollen_none);
        if (cat.contains("very low")) return getString(R.string.pollen_very_low);
        if (cat.contains("low") && !cat.contains("very")) return getString(R.string.pollen_low);
        if (cat.contains("moderate")) return getString(R.string.pollen_moderate);
        if (cat.contains("high") && !cat.contains("very")) return getString(R.string.pollen_high);
        if (cat.contains("very high")) return getString(R.string.pollen_very_high);
        if (cat.contains("severe")) return getString(R.string.pollen_severe);

        return getString(R.string.pollen_unknown);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null && isAdded()) loadFavoritesWeather(getView());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        geocoderExecutor.shutdown(); // Fix F9
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null && isAdded()) {
            loadFavoritesWeather(getView());
        }
    }

    // Load the radar map in the WebView
    private void loadRainViewerRadar() {
        WebSettings settings = wvRainRadar.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Intercept URLs so that, if the user taps a logo or link on the map,
        // the phone browser does not open and take them out of the app.
        wvRainRadar.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                String targetUrl = request.getUrl().toString();
                return !targetUrl.contains("rainviewer.com/map.html");
            }
        });
        wvRainRadar.loadUrl("https://www.rainviewer.com/map.html?loc=43.26,-2.93,8&oFa=0&oC=1&oU=0&oCS=1&oF=0&oAP=1&c=1&o=83&lm=0&layer=radar&sm=1&sn=1");
    }

    // Request to OpenData Euskadi. It returns plain XML instead of JSON, so a StringRequest is used
    private void fetchOpenDataForecast() {
        String urlXmlOpenData = "https://opendata.euskadi.eus/contenidos/prevision_tiempo/met_forecast_zone/opendata/met_forecast_zone.xml";

        StringRequest apiRequest = new StringRequest(Request.Method.GET, urlXmlOpenData,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (isAdded()) parseOpenDataXml(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && tvBilbaoForecast != null) tvBilbaoForecast.setText(R.string.euskalmet_connection_error); // Fix F5: text moved to strings.xml so it is translated to the 6 languages
                    }
                }
        );
        requestQueue.add(apiRequest);
    }

    // Manual parsing of the Euskalmet XML document
    private void parseOpenDataXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document doc = builder.parse(is);

            // Look directly for areaId 8, which is the Greater Bilbao zone
            NodeList areas = doc.getElementsByTagName("areaForecast");
            for (int i = 0; i < areas.getLength(); i++) {
                Element area = (Element) areas.item(i);
                if (area.getAttribute("areaId").equals("8")) {
                    NodeList periods = area.getElementsByTagName("periodData");

                    // Only today's forecast is needed
                    for (int j = 0; j < periods.getLength(); j++) {
                        Element period = (Element) periods.item(j);
                        if (period.getAttribute("periodDay").equals("today")) {
                            NodeList descriptions = period.getElementsByTagName("forecastDescription");
                            if (descriptions.getLength() > 0) {
                                Element descElement = (Element) descriptions.item(0);
                                String language = java.util.Locale.getDefault().getLanguage();

                                // Euskalmet only provides texts in Spanish and Basque.
                                // If the app is in another language, ML Kit is used to translate it on the fly.
                                if (language.equals("es") || language.equals("eu")) {
                                    String nativeText = descElement.getElementsByTagName(language).item(0).getTextContent().trim();
                                    if (tvBilbaoForecast != null) tvBilbaoForecast.setText(nativeText);
                                } else {
                                    String spanishText = descElement.getElementsByTagName("es").item(0).getTextContent().trim();
                                    translateWithMlKit(spanishText, language);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isAdded() && tvBilbaoForecast != null) tvBilbaoForecast.setText(R.string.data_parse_error); // Fix F5: text moved to strings.xml so it is translated to the 6 languages
        }
    }

    // Uses the local Google ML Kit models to translate texts offline
    private void translateWithMlKit(String spanishText, String targetLanguage) {
        if (!isAdded()) return;
        String mlKitLang = TranslateLanguage.fromLanguageTag(targetLanguage);
        if (mlKitLang == null) {
            if (tvBilbaoForecast != null) tvBilbaoForecast.setText(spanishText);
            return;
        }
        if (tvBilbaoForecast != null) tvBilbaoForecast.setText(R.string.translating_forecast); // Fix F5: text moved to strings.xml so it is translated to the 6 languages

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.SPANISH)
                .setTargetLanguage(mlKitLang)
                .build();
        final Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        // The first time it is used, the language package is downloaded
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        translator.translate(spanishText)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String translatedText) {
                                        if (isAdded() && tvBilbaoForecast != null) tvBilbaoForecast.setText(translatedText);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // If the download or translation fails, show the original Spanish version
                        if (isAdded() && tvBilbaoForecast != null) tvBilbaoForecast.setText(spanishText);
                    }
                });
    }

    private void setIconAndText(int code, ImageView iv) {
        String desc = getString(R.string.unknown);
        int resId = R.drawable.ic_weather_clouds;

        if (code == 0) desc = getString(R.string.clear_sky);
        else if (code == 1) desc = getString(R.string.mostly_clear);
        else if (code == 2) desc = getString(R.string.partly_cloudy);
        else if (code == 3) desc = getString(R.string.overcast);
        else if (code == 45 || code == 48) desc = getString(R.string.dense_fog);
        else if (code == 51 || code == 53 || code == 55) desc = getString(R.string.drizzle);
        else if (code == 56 || code == 57) desc = getString(R.string.freezing_drizzle);
        else if (code == 61) desc = getString(R.string.light_rain);
        else if (code == 63) desc = getString(R.string.moderate_rain);
        else if (code == 65) desc = getString(R.string.heavy_rain);
        else if (code == 66 || code == 67) desc = getString(R.string.freezing_rain);
        else if (code == 71) desc = getString(R.string.light_snow);
        else if (code == 73) desc = getString(R.string.moderate_snow);
        else if (code == 75) desc = getString(R.string.heavy_snow);
        else if (code == 77) desc = getString(R.string.light_hail);
        else if (code == 80) desc = getString(R.string.light_showers);
        else if (code == 81) desc = getString(R.string.moderate_showers);
        else if (code == 82) desc = getString(R.string.violent_showers);
        else if (code == 85 || code == 86) desc = getString(R.string.snow_showers);
        else if (code == 95) desc = getString(R.string.thunderstorm);
        else if (code == 96 || code == 99) desc = getString(R.string.thunderstorm_with_hail);

        if (code == 0) {
            resId = R.drawable.ic_weather_sun;
        } else if (code >= 1 && code <= 3) {
            resId = R.drawable.ic_weather_clouds;
        } else if (code >= 45 && code <= 48) {
            resId = R.drawable.ic_weather_fog;
        } else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            resId = R.drawable.ic_weather_rain;
        } else if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            resId = R.drawable.ic_weather_snow;
        } else if (code >= 95 && code <= 99) {
            resId = R.drawable.ic_weather_storm;
        }

        iv.setImageResource(resId);

        // The main icon is tinted red; the others keep their default colour
        if (iv == ivMainWeatherIcon) {
            if (tvWeatherDescription != null) tvWeatherDescription.setText(desc);
            iv.setColorFilter(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            iv.setColorFilter(null);
        }
    }

    // Reads the local DB to know which campuses are favourites and creates the cards
    private void loadFavoritesWeather(View view) {
        if (!isAdded()) return;
        android.widget.GridLayout favoritesContainer = view.findViewById(R.id.glFavoritesContainer);
        if (favoritesContainer == null) return;

        // Empty the container first so the cards are not duplicated when returning to this screen
        favoritesContainer.removeAllViews();

        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("user_id", 0);

        List<String> favorites = dbHelper.getUserFavoriteIds(currentUserId);

        if (favorites.isEmpty()) {
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText(R.string.no_favorite_campuses_yet);
            tvEmpty.setPadding(16, 16, 16, 16);
            favoritesContainer.addView(tvEmpty);
            return;
        }

        for (String idCampus : favorites) {
            Campus campus = dbHelper.getCampusById(idCampus);

            if (campus != null) {
                // Inflate the card layout manually for each saved campus
                View card = getLayoutInflater().inflate(R.layout.item_favorite_weather, favoritesContainer, false);
                TextView tvCampusName = card.findViewById(R.id.tvCampusName);
                TextView tvTemp = card.findViewById(R.id.tvTempCampus);
                TextView tvUni = card.findViewById(R.id.tvUniCampus);

                tvCampusName.setText(campus.getName());
                tvUni.setText(campus.getUniversity());

                if (campus.getUniversity().equals("UPV/EHU")) {
                    tvUni.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.university_tag_color_ehu));
                } else if (campus.getUniversity().equals("Mondragon")) {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#008a96"));
                } else {
                    tvUni.setTextColor(android.graphics.Color.parseColor("#1976D2"));
                }

                // One API call per card
                fetchTemperatureQueued(campus.getLatitude(), campus.getLongitude(), tvTemp);

                android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
                params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1.0f);
                params.width = 0;
                params.setMargins(10, 10, 10, 20);

                card.setLayoutParams(params);
                favoritesContainer.addView(card);
            }
        }
    }

    // Request to OpenMeteo: current temperature, daily max UV and sunrise/sunset in a single call
    private void fetchWeatherByCoordinates(double latitude, double longitude, final ImageView ivIcon, final TextView tvTemp) {
        String urlOpenMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                "&longitude=" + longitude +
                "&current_weather=true" +
                "&daily=uv_index_max,sunrise,sunset" +
                "&timezone=auto";

        JsonObjectRequest apiRequest = new JsonObjectRequest(Request.Method.GET, urlOpenMeteo, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!isAdded() || getView() == null) return;
                        try {
                            if (response.has("current_weather")) {
                                JSONObject currentWeather = response.getJSONObject("current_weather");
                                double temperature = currentWeather.optDouble("temperature", 0.0);
                                int weatherCode = currentWeather.optInt("weathercode", 0);
                                if (tvTemp != null) tvTemp.setText(temperature + " ºC");
                                if (ivIcon != null) setIconAndText(weatherCode, ivIcon);
                            }

                            if (response.has("daily")) {
                                JSONObject dailyParams = response.getJSONObject("daily");

                                if (dailyParams.has("uv_index_max")) {
                                    double uvIndex = dailyParams.getJSONArray("uv_index_max").optDouble(0, 0.0);
                                    updateUvCard(uvIndex);
                                }

                                if (dailyParams.has("sunrise") && dailyParams.has("sunset")) {
                                    String sunriseIso = dailyParams.getJSONArray("sunrise").optString(0, "");
                                    String sunsetIso = dailyParams.getJSONArray("sunset").optString(0, "");

                                    // Strip the date sent by the API and keep only the hour and minutes
                                    if (sunriseIso.length() >= 16 && sunsetIso.length() >= 16) {
                                        String sunrise = sunriseIso.substring(11, 16);
                                        String sunset = sunsetIso.substring(11, 16);

                                        TextView tvSunrise = getView().findViewById(R.id.tvSunrise);
                                        TextView tvSunset = getView().findViewById(R.id.tvSunset);

                                        if (tvSunrise != null) tvSunrise.setText("🌅 " + sunrise);
                                        if (tvSunset != null) tvSunset.setText("🌙 " + sunset);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && tvTemp != null) tvTemp.setText(R.string.network_error_short);
                    }
                }
        );
        requestQueue.add(apiRequest);
    }

    private void updateUvCard(double uvIndex) {
        if (getView() == null) return;

        TextView tvUvValue = getView().findViewById(R.id.tvUvValue);
        TextView tvDescUV = getView().findViewById(R.id.tvDescUV);

        if (tvUvValue == null || tvDescUV == null) return;

        tvUvValue.setText(String.valueOf(Math.round(uvIndex)));

        String language = java.util.Locale.getDefault().getLanguage();
        String description;

        if (uvIndex < 3) {
            description = getString(R.string.uv_low);
        } else if (uvIndex < 6) {
            description = getString(R.string.uv_moderate);
        } else if (uvIndex < 8) {
            description = getString(R.string.uv_high);
            tvUvValue.setTextColor(android.graphics.Color.parseColor("#F57C00"));
        } else if (uvIndex < 11) {
            description = getString(R.string.uv_very_high);
            tvUvValue.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
        } else {
            description = getString(R.string.uv_extreme);
            tvUvValue.setTextColor(android.graphics.Color.parseColor("#7B1FA2"));
        }

        tvDescUV.setText(description);
    }

    private void fetchTemperatureQueued(double lat, double lon, final TextView tvTemp) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (isAdded() && tvTemp != null) {
                            try {
                                double t = response.getJSONObject("current_weather").getDouble("temperature");
                                tvTemp.setText(t + " ºC");
                            } catch (Exception e) { tvTemp.setText(R.string.temperature_error); }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && tvTemp != null) tvTemp.setText(R.string.network_error_short);
                    }
                }
        );
        requestQueue.add(request);
    }
}