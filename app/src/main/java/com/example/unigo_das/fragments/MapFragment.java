package com.example.unigo_das.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.unigo_das.R;
import com.example.unigo_das.BuildConfig;
import com.example.unigo_das.db.DataBaseHelper;
import com.example.unigo_das.item.Campus;
import com.example.unigo_das.item.TransitStop;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.data.kml.KmlLayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap campusMap;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    // UI views
    private View cardFakeSearchBar;
    private View fullSearchScreen;
    private View searchHeader;
    private ImageView btnCloseSearch;
    private ImageView btnClearRoute;
    private ImageView btnTransitFilters;
    private SearchView searchViewReal;
    private ListView lvSearchResults;
    private View cardTransport;
    private View cardRouteInfo;
    private LinearLayout llInstructionList;

    // UI to minimise/expand
    private ImageView btnMinimizeRoute;
    private View scrollInstructions;
    private View routesHeader;
    private boolean isCardExpanded = true;

    private FloatingActionButton fabTransitLayers;
    private List<Marker> activeStopMarkers = new ArrayList<>();
    private int selectedLayerIndex = 0;

    private List<Campus> allCampuses;
    private MapSearchAdapter searchAdapter;

    // State variables
    private String currentTransportMode = "walking";
    private LatLng pendingDestination = null;
    private String pendingTitle = null;
    private LatLng currentDestination = null;
    private String currentDestinationTitle = null;

    // Transport filters and preferences
    private long customDepartureTimestamp = 0;
    private boolean useBus = true;
    private boolean useMetro = true;
    private boolean useTram = true;
    private boolean useTrain = true;
    private boolean isElectricCar = false;
    private KmlLayer bikeLaneLayer;

    // Fix F9: one background thread for all route requests, closed in onDestroy()
    private final ExecutorService routeExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android mechanism to request several permissions at once, cleanly.
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> result) {
                        Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                        if ((fineLocationGranted != null && fineLocationGranted) ||
                                (coarseLocationGranted != null && coarseLocationGranted)) {
                            enableMyLocationOnMap();
                            // If the user came from "SchoolFragment" and has just granted the permission,
                            // draw the route they asked for.
                            processPendingRoute();
                        }
                    }
                }
        );

        loadSearchData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // "hidden" is false when the fragment becomes visible on screen again.
        // If the user changes the map style in Settings and comes back here, refresh it.
        if (!hidden && isAdded() && campusMap != null) {
            loadMapSettings();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardFakeSearchBar = view.findViewById(R.id.card_fake_search_bar);
        fullSearchScreen = view.findViewById(R.id.full_search_screen);
        searchHeader = view.findViewById(R.id.search_header);
        btnCloseSearch = view.findViewById(R.id.btn_close_search);
        searchViewReal = view.findViewById(R.id.searchViewReal);
        lvSearchResults = view.findViewById(R.id.lv_search_results);
        View searchClickLayer = view.findViewById(R.id.search_click_layer);

        cardTransport = view.findViewById(R.id.card_transport);
        if (cardTransport != null) {
            cardTransport.setVisibility(View.GONE);
        }

        cardRouteInfo = view.findViewById(R.id.card_route_info);
        llInstructionList = view.findViewById(R.id.ll_instruction_list);
        btnClearRoute = view.findViewById(R.id.btn_clear_route);
        btnTransitFilters = view.findViewById(R.id.btn_transit_filters);

        btnMinimizeRoute = view.findViewById(R.id.btn_minimize_route);
        scrollInstructions = view.findViewById(R.id.scroll_instructions);
        routesHeader = view.findViewById(R.id.routes_header);

        btnClearRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearActiveRoute();
            }
        });

        btnTransitFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTransitFilters();
            }
        });

        btnMinimizeRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scrollInstructions.getVisibility() == View.VISIBLE) {
                    scrollInstructions.setVisibility(View.GONE);
                    // Points down when collapsed
                    btnMinimizeRoute.animate().rotation(0).setDuration(300).start();
                } else {
                    scrollInstructions.setVisibility(View.VISIBLE);
                    // Points up when expanded
                    btnMinimizeRoute.animate().rotation(180).setDuration(300).start();
                }
            }
        });

        routesHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCardExpanded) {
                    toggleCardState();
                }
            }
        });

        fabTransitLayers = view.findViewById(R.id.fab_transit_layers);
        if (fabTransitLayers != null) {
            fabTransitLayers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showStopLayersMenu();
                }
            });
        }

        RadioGroup rgTransportMode = view.findViewById(R.id.rg_transport_mode);
        if (rgTransportMode != null) {
            rgTransportMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.rb_bike) {
                        currentTransportMode = "bicycling";
                    } else if (checkedId == R.id.rb_public_transport) {
                        currentTransportMode = "transit";
                    } else {
                        currentTransportMode = "walking";
                    }

                    // If the tab changes and a route is already loaded, recalculate it.
                    if (currentDestination != null && currentDestinationTitle != null) {
                        drawRouteToDestination(currentDestinationTitle, currentDestination.latitude, currentDestination.longitude, currentTransportMode);
                    }
                }
            });
        }

        searchAdapter = new MapSearchAdapter(requireContext(), allCampuses);
        lvSearchResults.setAdapter(searchAdapter);

        // When the search bar is tapped, show the full white search screen
        // and open the keyboard automatically.
        searchClickLayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullSearchScreen.setVisibility(View.VISIBLE);
                searchViewReal.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchViewReal.findFocus(), InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        btnCloseSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSearchScreen();
            }
        });

        searchViewReal.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchViewReal.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchAdapter.getFilter().filter(newText);
                return false;
            }
        });

        lvSearchResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view1, int position, long id) {
                Campus selectedCampus = searchAdapter.getItem(position);
                if (selectedCampus != null) {
                    closeSearchScreen();

                    // RESET THE FILTERS WHEN SEARCHING
                    useBus = true; useMetro = true; useTram = true; useTrain = true;
                    // isElectricCar is not reset because it is a persistent preference
                    customDepartureTimestamp = 0;

                    // Avoid double API calls and restore the favourite
                    currentDestination = null;
                    loadMapSettings();

                    drawRouteToDestination(selectedCampus.getName(), selectedCampus.getLatitude(), selectedCampus.getLongitude(), currentTransportMode);
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Places the search bar just below the camera notch,
        // without covering the notification bar.
        ViewCompat.setOnApplyWindowInsetsListener(view, new androidx.core.view.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) cardFakeSearchBar.getLayoutParams();
                mlp.topMargin = insets.top + (int) (16 * getResources().getDisplayMetrics().density);
                cardFakeSearchBar.setLayoutParams(mlp);
                searchHeader.setPadding(0, insets.top, 0, 0);
                return windowInsets;
            }
        });
    }

    // This method combines the local data (SharedPreferences of the active session)
    // with the map to personalise the experience.
    private void loadMapSettings() {
        if (getContext() == null) return;

        SharedPreferences unigoPrefs = requireContext().getSharedPreferences("UnigoPrefs", Context.MODE_PRIVATE);
        SharedPreferences defaultPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
        String userEmail = unigoPrefs.getString("user_email", "");
        boolean isGuest = unigoPrefs.getBoolean("isGuest", true);

        // Apply the map style
        String mapTypeStr;
        if (!isGuest && !userEmail.isEmpty()) {
            mapTypeStr = unigoPrefs.getString("tipo_mapa_" + userEmail, defaultPrefs.getString("pref_tipo_mapa", "1"));
        } else {
            mapTypeStr = defaultPrefs.getString("pref_tipo_mapa", "1");
        }
        int mapType;
        try {
            mapType = Integer.parseInt(mapTypeStr);
        } catch (NumberFormatException e) {
            mapType = GoogleMap.MAP_TYPE_NORMAL;
        }
        if (campusMap != null) {
            campusMap.setMapType(mapType);
        }

        // Electric car
        if (!isGuest && !userEmail.isEmpty()) {
            this.isElectricCar = unigoPrefs.getBoolean("pref_coche_electrico_" + userEmail,
                    defaultPrefs.getBoolean("pref_coche_electrico", false));
        } else {
            this.isElectricCar = defaultPrefs.getBoolean("pref_coche_electrico", false);
        }

        // Favourite transport
        if (currentDestination == null) {
            String favoriteTransport;
            if (!isGuest && !userEmail.isEmpty()) {
                favoriteTransport = unigoPrefs.getString("transporte_favorito_" + userEmail,
                        defaultPrefs.getString("pref_transporte_favorito", "publico"));
            } else {
                favoriteTransport = defaultPrefs.getString("pref_transporte_favorito", "publico");
            }

            RadioGroup rgTransportMode = getView() != null ? getView().findViewById(R.id.rg_transport_mode) : null;

            switch (favoriteTransport) {
                case "publico":
                    currentTransportMode = "transit";
                    if (rgTransportMode != null)
                        rgTransportMode.check(R.id.rb_public_transport);
                    break;
                case "bici":
                    currentTransportMode = "bicycling";
                    if (rgTransportMode != null) rgTransportMode.check(R.id.rb_bike);
                    break;
                case "pie":
                    currentTransportMode = "walking";
                    if (rgTransportMode != null) rgTransportMode.check(R.id.rb_walking);
                    break;
            }
        }
    }

    private int getCardTextColor() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            return Color.parseColor("#EEEEEE");
        } else {
            return Color.parseColor("#1A1A1A");
        }
    }

    private int getSecondaryTextColor() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            return Color.parseColor("#B0B0B0");
        } else {
            return Color.parseColor("#666666");
        }
    }

    private int getSurfaceColor() {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
        return typedValue.data;
    }

    private void closeSearchScreen() {
        fullSearchScreen.setVisibility(View.GONE);
        searchViewReal.setQuery("", false);
        searchViewReal.clearFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchViewReal.getWindowToken(), 0);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.campusMap = googleMap;
        // Default initial view (Bilbao)
        LatLng campusLocationEIB = new LatLng(43.265842, -2.940452);
        campusMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusLocationEIB, 15f));

        campusMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // So the user does not feel trapped by the long list of route steps:
                // a tap on the map minimises the instructions.
                if (cardRouteInfo.getVisibility() == View.VISIBLE && isCardExpanded) {
                    toggleCardState();
                }
            }
        });

        loadMapSettings();

        // If MainActivity asked for a route as soon as the fragment was created (for example, from Favourites in the campus list).
        if (getArguments() != null && getArguments().containsKey("destino_nombre")) {
            pendingTitle = getArguments().getString("destino_nombre");
            pendingDestination = new LatLng(getArguments().getDouble("destino_lat"), getArguments().getDouble("destino_lng"));
        }
        checkLocationPermissions();
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationOnMap();
            processPendingRoute();
        } else {
            locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void processPendingRoute() {
        if (pendingDestination != null && pendingTitle != null) {
            loadMapSettings();
            drawRouteToDestination(pendingTitle, pendingDestination.latitude, pendingDestination.longitude, currentTransportMode);
            pendingDestination = null;
            pendingTitle = null;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocationOnMap() {
        if (campusMap != null) {
            campusMap.setMyLocationEnabled(true);
            campusMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    // Single entry point to request routes from the API
    public void drawRouteToDestination(String title, double destLat, double destLng, String transportMode) {
        if (campusMap == null) return;

        // Hide the static stops/bike lanes while a real route is drawn, so the map stays clean
        if (fabTransitLayers != null) {
            fabTransitLayers.setVisibility(View.GONE);
        }
        clearStopMarkers();

        // The filters are only shown if the user chooses public transport.
        if ("transit".equals(transportMode) && btnTransitFilters != null) {
            btnTransitFilters.setVisibility(View.VISIBLE);
        } else if (btnTransitFilters != null) {
            btnTransitFilters.setVisibility(View.GONE);
        }

        if (cardTransport != null) {
            cardTransport.setVisibility(View.VISIBLE);
        }

        currentDestination = new LatLng(destLat, destLng);
        currentDestinationTitle = title;
        final LatLng destination = new LatLng(destLat, destLng);
        campusMap.clear();

        cardRouteInfo.setVisibility(View.GONE);
        llInstructionList.removeAllViews();

        campusMap.addMarker(new MarkerOptions().position(destination).title(title));
        campusMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 14f));

        FusedLocationProviderClient locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity());
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                fetchGoogleRoute(new LatLng(location.getLatitude(), location.getLongitude()), destination, transportMode);
                            }
                        }
                    })
                    .addOnFailureListener(requireActivity(), new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("UnigoDAS", "Location request failed", e); // Fix F7: Log.e with a null message throws a NullPointerException (it crashed the app on the background thread)
                        }
                    });
        } catch (SecurityException e) {
            Log.e("UnigoDAS", "Location permission error", e); // Fix F7: Log.e with a null message throws a NullPointerException (it crashed the app on the background thread)
        }
    }

    // Plain HTTP call to the Google Directions API.
    // It runs on a SingleThreadExecutor so the UI is not blocked while waiting for the network response.
    private void fetchGoogleRoute(final LatLng origin, final LatLng destination, final String transportMode) {
        String apiKey = BuildConfig.DIRECTIONS_API_KEY;
        String currentLanguage = Locale.getDefault().getLanguage();

        Uri.Builder builderUrl = Uri.parse("https://maps.googleapis.com/maps/api/directions/json").buildUpon();
        builderUrl.appendQueryParameter("origin", origin.latitude + "," + origin.longitude);
        builderUrl.appendQueryParameter("destination", destination.latitude + "," + destination.longitude);
        builderUrl.appendQueryParameter("mode", transportMode);
        builderUrl.appendQueryParameter("key", apiKey);
        builderUrl.appendQueryParameter("language", currentLanguage);

        if ("transit".equals(transportMode)) {
            // Ask Google for all the possible alternatives,
            // so we can later discard the ones that use unchecked transport modes.
            builderUrl.appendQueryParameter("alternatives", "true");

            if (customDepartureTimestamp > 0) {
                builderUrl.appendQueryParameter("departure_time", String.valueOf(customDepartureTimestamp));
            } else {
                builderUrl.appendQueryParameter("departure_time", "now");
            }

            List<String> allowedModes = new ArrayList<>();
            if (useBus) allowedModes.add("bus");
            if (useMetro) allowedModes.add("subway");
            if (useTram) allowedModes.add("tram");
            if (useTrain) allowedModes.add("train");

            if (!allowedModes.isEmpty() && allowedModes.size() < 4) {
                builderUrl.appendQueryParameter("transit_mode", android.text.TextUtils.join("|", allowedModes));
            }
        } else {
            builderUrl.appendQueryParameter("alternatives", "false");
        }

        final String directionsApiUrl = builderUrl.build().toString();
        // Fix F9: the route requests reuse one background thread (routeExecutor). Before, a new
        // thread was created for every request and never shut down.
        ExecutorService executor = routeExecutor;
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(directionsApiUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    if (connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if ("OK".equals(jsonResponse.getString("status"))) {
                            JSONArray routesArray = jsonResponse.getJSONArray("routes");

                            boolean validRouteFound = false;
                            final List<RouteSegment> finalSegments = new ArrayList<>();
                            final List<LatLng> finalPoints = new ArrayList<>();
                            String finalTotalPrice = getString(R.string.variable_price);

                            double finalTotalKm = 0.0;
                            int finalRouteCo2 = 0;

                            // Iterate over all the alternative routes returned by Google
                            for (int r = 0; r < routesArray.length(); r++) {
                                JSONObject route = routesArray.getJSONObject(r);
                                List<RouteSegment> tempSegments = new ArrayList<>();
                                List<LatLng> tempPoints = new ArrayList<>();
                                boolean routeMatchesFilters = true;
                                boolean usesPublicVehicle = false;
                                double tempTotalKm = 0.0;
                                int tempRouteCo2 = 0;

                                String tempTotalPrice = getString(R.string.variable_price);
                                if (route.has("fare")) {
                                    tempTotalPrice = route.getJSONObject("fare").getString("text") + " ("+ getString(R.string.single_ticket) + ")";
                                }

                                if ("transit".equals(transportMode)) {
                                    JSONArray steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
                                    // Now split the route step by step (walk to the stop, take the bus, walk to the destination)
                                    for (int i = 0; i < steps.length(); i++) {
                                        JSONObject step = steps.getJSONObject(i);
                                        List<LatLng> decodedPoints = decodePolyline(step.getJSONObject("polyline").getString("points"));
                                        tempPoints.addAll(decodedPoints);

                                        RouteSegment segment = new RouteSegment();
                                        segment.points = decodedPoints;
                                        segment.isWalking = "WALKING".equals(step.getString("travel_mode"));
                                        segment.duration = step.getJSONObject("duration").getString("text");
                                        segment.startPoint = new LatLng(step.getJSONObject("start_location").getDouble("lat"), step.getJSONObject("start_location").getDouble("lng"));

                                        int stepMeters = step.getJSONObject("distance").getInt("value");
                                        double stepKm = stepMeters / 1000.0;
                                        tempTotalKm += stepKm;

                                        // If it is a real vehicle (bus, metro...) read the details (line colour, stops, etc.)
                                        if (!segment.isWalking && step.has("transit_details")) {
                                            usesPublicVehicle = true;
                                            JSONObject td = step.getJSONObject("transit_details");
                                            segment.departureStop = td.getJSONObject("departure_stop").getString("name");
                                            segment.arrivalStop = td.getJSONObject("arrival_stop").getString("name");
                                            segment.headsign = td.optString("headsign", getString(R.string.final_destination));
                                            segment.numStops = td.optString("num_stops", "0");
                                            if (td.has("departure_time")) segment.departureTime = td.getJSONObject("departure_time").getString("text");
                                            if (td.has("arrival_time")) segment.arrivalTime = td.getJSONObject("arrival_time").getString("text");

                                            JSONObject lineData = td.getJSONObject("line");
                                            segment.color = lineData.has("color") ? Color.parseColor(lineData.getString("color")) : Color.RED;
                                            if (!isDarkModeOnDevice()) {
                                                if (isColorTooLight(segment.color)) {
                                                    segment.color = ContextCompat.getColor(requireContext(), R.color.bilbao_grey_dark);
                                                }
                                            }

                                            String agency = "";
                                            if (lineData.has("agencies")) {
                                                agency = lineData.getJSONArray("agencies").getJSONObject(0).getString("name");
                                            }

                                            String vName = (lineData.has("vehicle") && lineData.getJSONObject("vehicle").has("name")) ? lineData.getJSONObject("vehicle").getString("name") : "";
                                            String lineName = lineData.has("short_name") ? lineData.getString("short_name") : lineData.optString("name", "");

                                            if (!agency.isEmpty()) {
                                                segment.instructionTitle = vName + " " + lineName + " (" + agency + ")";
                                            } else {
                                                segment.instructionTitle = vName + " " + lineName;
                                            }

                                            String vType = lineData.has("vehicle") && lineData.getJSONObject("vehicle").has("type") ? lineData.getJSONObject("vehicle").getString("type") : "";

                                            // Extra safety check, because Google sometimes returns bus routes even when they are excluded.
                                            if (vType.contains("BUS") && !useBus) routeMatchesFilters = false;
                                            else if (vType.contains("SUBWAY") && !useMetro) routeMatchesFilters = false;
                                            else if (vType.contains("TRAM") && !useTram) routeMatchesFilters = false;
                                            else if ((vType.contains("TRAIN") || vType.contains("RAIL")) && !useTrain) routeMatchesFilters = false;

                                            // Estimated carbon footprint depending on the transport mode
                                            int stepCo2 = 0;
                                            if (vType.contains("BUS")) {
                                                stepCo2 = (int)(stepKm * 80);
                                                segment.colorCO2 = Color.parseColor("#1976D2");
                                            } else {
                                                stepCo2 = (int)(stepKm * 40);
                                                segment.colorCO2 = Color.parseColor("#00897B");
                                            }
                                            segment.co2Info = stepCo2 + "g CO2";
                                            tempRouteCo2 += stepCo2;

                                        } else {
                                            // Walking step
                                            segment.color = Color.GRAY;
                                            segment.instructionTitle = "Andando";
                                            segment.co2Info = "0g CO2";
                                            segment.colorCO2 = Color.parseColor("#2E7D32");
                                        }
                                        tempSegments.add(segment);
                                    }
                                } else {
                                    // Simple bike or walking mode: there is a single long line.
                                    List<LatLng> decodedPoints = decodePolyline(route.getJSONObject("overview_polyline").getString("points"));
                                    tempPoints.addAll(decodedPoints);
                                    RouteSegment sr = new RouteSegment();
                                    sr.points = decodedPoints;
                                    sr.isWalking = false;
                                    sr.color = "bicycling".equals(transportMode) ? Color.GREEN : Color.BLUE;
                                    sr.duration = route.getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
                                    sr.co2Info = "0g CO2";
                                    sr.colorCO2 = Color.parseColor("#2E7D32");
                                    int totalMeters = route.getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getInt("value");
                                    tempTotalKm = totalMeters / 1000.0;
                                    tempRouteCo2 = 0;
                                    tempSegments.add(sr);
                                }

                                // If the route passed our filters, stop the loop and keep it (usually the fastest one).
                                if (routeMatchesFilters) {
                                    validRouteFound = true;
                                    finalSegments.addAll(tempSegments);
                                    finalPoints.addAll(tempPoints);
                                    finalTotalPrice = tempTotalPrice;
                                    finalTotalKm = tempTotalKm;
                                    finalRouteCo2 = tempRouteCo2;
                                    break;
                                }
                            }

                            final boolean isRouteFullyValid = validRouteFound;
                            final String finalPrice = finalTotalPrice;

                            // Calculation for the summary card
                            float[] distanceResults = new float[1];
                            Location.distanceBetween(origin.latitude, origin.longitude, destination.latitude, destination.longitude, distanceResults);
                            double straightLineKm = distanceResults[0] / 1000.0;
                            double carKm = straightLineKm * 1.3;

                            int estimatedCarCo2;
                            if (isElectricCar) {
                                estimatedCarCo2 = 0;
                            } else {
                                estimatedCarCo2 = (int) (carKm * 143);
                            }

                            final int finalCo2Saving = Math.max(0, estimatedCarCo2 - finalRouteCo2);
                            final boolean carWasElectric = isElectricCar;

                            // Back to the main thread to update the views
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (campusMap != null && isAdded()) {

                                        // If all the filters were unchecked, show an alert
                                        if (!isRouteFullyValid) {
                                            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(requireContext());
                                            errorBuilder.setTitle(getString(R.string.invalid_route_title));
                                            errorBuilder.setMessage(getString(R.string.invalid_route_message));
                                            errorBuilder.setPositiveButton(getString(R.string.change_filters), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    showTransitFilters();
                                                }
                                            });
                                            errorBuilder.setNegativeButton(getString(R.string.cancel), null);
                                            errorBuilder.show();

                                            campusMap.clear();
                                            cardRouteInfo.setVisibility(View.GONE);
                                            return;
                                        }

                                        // Draw the line and create the TextView
                                        llInstructionList.removeAllViews();
                                        for (RouteSegment seg : finalSegments) {
                                            PolylineOptions opt = new PolylineOptions().addAll(seg.points).width(12f).color(seg.color).geodesic(true);
                                            TextView tv = new TextView(requireContext());
                                            tv.setTextSize(14f); tv.setPadding(0, 16, 0, 16); tv.setTextColor(getCardTextColor());

                                            if (seg.isWalking && "transit".equals(transportMode)) {
                                                // Walking path = dots
                                                opt.pattern(Arrays.asList(new Dot(), new Gap(15f)));
                                                String base = getString(R.string.walk) + " (" + seg.duration + ") • ";
                                                SpannableStringBuilder ssb = new SpannableStringBuilder(base + seg.co2Info);
                                                ssb.setSpan(new ForegroundColorSpan(seg.colorCO2), base.length(), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                ssb.setSpan(new StyleSpan(Typeface.BOLD), base.length(), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                tv.setText(ssb);
                                            } else if (!seg.isWalking && "transit".equals(transportMode)) {
                                                // Public transport = thick line in the line colour
                                                campusMap.addMarker(new MarkerOptions().position(seg.startPoint).title(seg.instructionTitle).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                                String base = seg.instructionTitle + " (" + seg.duration + ") • ";
                                                SpannableStringBuilder ssb = new SpannableStringBuilder(base + seg.co2Info);
                                                ssb.setSpan(new ForegroundColorSpan(seg.colorCO2), base.length(), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                String extraInfo = "\n";
                                                if (seg.departureTime != null) extraInfo += seg.departureTime + " - " + seg.arrivalTime + "\n";
                                                extraInfo += "↳ " + getString(R.string.direction) + ": " + seg.headsign + "\n↳ " + getString(R.string.from) + ": " + seg.departureStop + "\n↳ " + getString(R.string.to) + ": " + seg.arrivalStop + " (" + seg.numStops + " "+ getString(R.string.stops) + ")";
                                                ssb.append(extraInfo);
                                                tv.setText(ssb);
                                                tv.setTextColor(seg.color);
                                                tv.setTypeface(null, Typeface.BOLD);
                                            } else {
                                                // Main bike or walking route
                                                String base = ("bicycling".equals(transportMode) ? getString(R.string.bike) + " " : getString(R.string.walk) + " ") + "(" + seg.duration + ") • ";
                                                SpannableStringBuilder ssb = new SpannableStringBuilder(base + seg.co2Info);
                                                ssb.setSpan(new ForegroundColorSpan(seg.colorCO2), base.length(), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                ssb.setSpan(new StyleSpan(Typeface.BOLD), base.length(), ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                tv.setText(ssb);
                                            }
                                            llInstructionList.addView(tv);
                                            campusMap.addPolyline(opt);
                                        }

                                        // Summary card with money and CO2
                                        if (!finalSegments.isEmpty()) {
                                            View savingSeparator = new View(requireContext());
                                            savingSeparator.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
                                            savingSeparator.setBackgroundColor(Color.LTGRAY);
                                            TextView tvCo2Saving = new TextView(requireContext());
                                            if (carWasElectric) {
                                                tvCo2Saving.setText(getString(R.string.electric_car_emissions));
                                                tvCo2Saving.setTextColor(Color.parseColor("#2E7D32"));
                                            } else {
                                                tvCo2Saving.setText(getString(R.string.you_avoided) + " " + finalCo2Saving + getString(R.string.co2_vs_combustion_car));
                                                tvCo2Saving.setTextColor(Color.parseColor("#2E7D32"));
                                            }
                                            tvCo2Saving.setTextSize(15f);
                                            tvCo2Saving.setPadding(0, 30, 0, 16);
                                            tvCo2Saving.setTypeface(null, Typeface.BOLD);
                                            llInstructionList.addView(savingSeparator);
                                            llInstructionList.addView(tvCo2Saving);

                                            if ("transit".equals(transportMode)) {
                                                TextView tvPrice = new TextView(requireContext());
                                                tvPrice.setText(getString(R.string.estimated_cost) + ": " + finalPrice);
                                                tvPrice.setTextSize(16f);
                                                tvPrice.setPadding(0, 8, 0, 8);
                                                tvPrice.setTextColor(Color.parseColor("#3F51B5"));
                                                tvPrice.setTypeface(null, Typeface.BOLD);
                                                TextView tvBarikNotice = new TextView(requireContext());
                                                tvBarikNotice.setText("*" + getString(R.string.estimated_cost_notice));
                                                tvBarikNotice.setTextSize(12f);
                                                tvBarikNotice.setTextColor(Color.GRAY);
                                                tvBarikNotice.setPadding(0, 0, 0, 16);
                                                llInstructionList.addView(tvPrice);
                                                llInstructionList.addView(tvBarikNotice);
                                            }
                                            cardRouteInfo.setVisibility(View.VISIBLE);
                                        }
                                        if (!finalPoints.isEmpty()) {
                                            // The camera is adjusted so the whole route fits on screen
                                            LatLngBounds.Builder b = new LatLngBounds.Builder();
                                            for (LatLng p : finalPoints) { b.include(p); }
                                            campusMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
                                        }
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) { Log.e("UnigoDAS", "Route request failed", e); } // Fix F7: Log.e with a null message throws a NullPointerException (it crashed the app on the background thread)
                finally { if (connection != null) connection.disconnect(); }
            }
        });
    }

    // Google sends the route lines compressed in a single string of special characters.
    // This standard algorithm decodes them into a list of lat/long points to draw.
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            lat += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            shift = 0; result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            lng += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    private void toggleCardState() {
        if (isCardExpanded) {
            scrollInstructions.setVisibility(View.GONE);
            btnMinimizeRoute.setImageResource(android.R.drawable.arrow_up_float);
            isCardExpanded = false;
        } else {
            scrollInstructions.setVisibility(View.VISIBLE);
            btnMinimizeRoute.setImageResource(android.R.drawable.arrow_down_float);
            isCardExpanded = true;
        }
    }

    private void clearActiveRoute() {
        if (bikeLaneLayer != null && bikeLaneLayer.isLayerOnMap()) {
            bikeLaneLayer.removeLayerFromMap();
        }
        if (campusMap != null) {
            campusMap.clear();
        }
        cardRouteInfo.setVisibility(View.GONE);
        if (cardTransport != null) {
            cardTransport.setVisibility(View.GONE);
        }
        llInstructionList.removeAllViews();
        currentDestination = null; currentDestinationTitle = null;
        customDepartureTimestamp = 0;

        // When the screen is cleared, the buttons go back to the favourite transport
        loadMapSettings();

        // If the stop layer was open, show it again
        if (fabTransitLayers != null) {
            fabTransitLayers.setVisibility(View.VISIBLE);
        }
        if (selectedLayerIndex > 0) {
            redrawStopLayer(selectedLayerIndex);
        }
        if (!isCardExpanded) {
            toggleCardState();
        }
    }

    private void showStopLayersMenu() {
        final String[] layerOptions = {getString(R.string.hide_stops), "Bilbaobizi", "Bilbobus", "Bizkaibus", "Euskotren", getString(R.string.metro), "Renfe", getString(R.string.tram_option)};

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());

        builder.setTitle(getString(R.string.show_stops_title));
        builder.setIcon(R.drawable.ic_menu_map);

        builder.setSingleChoiceItems(layerOptions, selectedLayerIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedLayerIndex = which;
                redrawStopLayer(which);

                // Delay so the user sees what was tapped before the dialog closes
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                }, 50);
            }
        });

        builder.setNegativeButton(getString(R.string.close), null);
        builder.show();
    }

    // Draws all the station markers on the map
    private void redrawStopLayer(int index) {
        clearStopMarkers();

        if (bikeLaneLayer != null && bikeLaneLayer.isLayerOnMap()) {
            bikeLaneLayer.removeLayerFromMap();
        }

        if (index == 0) return;

        String[] dbTypes = {"", "Bilbaobizi", "Bilbobus", "Bizkaibus", "Euskotren", "Metro", "Renfe", "Tranvía"};
        String selectedType = dbTypes[index];

        // Bilbaobizi is special: it loads a KML file from raw instead of reading SQLite
        if ("Bilbaobizi".equals(selectedType)) {
            if (campusMap != null) {
                try {
                    if (bikeLaneLayer == null) {
                        bikeLaneLayer = new com.google.maps.android.data.kml.KmlLayer(campusMap, R.raw.bike_lanes, requireContext());
                    }
                    bikeLaneLayer.addLayerToMap();
                } catch (Exception e) {
                    Log.e("UnigoDAS", "Error cargando el KML", e);
                }
            }
        }

        float markerHue = BitmapDescriptorFactory.HUE_RED;
        String dbType = selectedType;

        switch (selectedType) {
            case "Bilbaobizi":
                markerHue = BitmapDescriptorFactory.HUE_ROSE;
                dbType = "Bicicleta";
                break;
            case "Bilbobus": markerHue = BitmapDescriptorFactory.HUE_RED; break;
            case "Bizkaibus": markerHue = BitmapDescriptorFactory.HUE_GREEN; break;
            case "Euskotren": markerHue = BitmapDescriptorFactory.HUE_BLUE; break;
            case "Metro": markerHue = BitmapDescriptorFactory.HUE_ORANGE; break;
            case "Renfe": markerHue = BitmapDescriptorFactory.HUE_MAGENTA; break;
            case "Tranvía": markerHue = BitmapDescriptorFactory.HUE_CYAN; break;
        }

        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        List<TransitStop> stops = dbHelper.getStopsByType(dbType);

        if (stops != null && !stops.isEmpty() && campusMap != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (TransitStop p : stops) {
                LatLng pos = new LatLng(p.getLatitude(), p.getLongitude());
                Marker m = campusMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(p.getName())
                        .snippet(p.getType())
                        .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));
                activeStopMarkers.add(m);
                builder.include(pos);
            }

            if ("Bilbaobizi".equals(selectedType)) {
                LatLng bizkaiaCenter = new LatLng(43.263, -2.935);
                campusMap.animateCamera(CameraUpdateFactory.newLatLngZoom(bizkaiaCenter, 11f));
            } else {
                campusMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
            }
        }
    }

    private void clearStopMarkers() {
        for (Marker m : activeStopMarkers) { m.remove(); }
        activeStopMarkers.clear();
    }

    private void showTransitFilters() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());

        builder.setTitle(getString(R.string.route_filters_title));
        builder.setIcon(R.drawable.ic_transport);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);

        TextView tvTime = new TextView(requireContext());
        tvTime.setText(getString(R.string.departure_max_days) + ":");
        tvTime.setTypeface(null, Typeface.BOLD);
        tvTime.setTextColor(getSecondaryTextColor());
        layout.addView(tvTime);

        LinearLayout dateTimeLayout = new LinearLayout(requireContext());
        dateTimeLayout.setOrientation(LinearLayout.HORIZONTAL);
        dateTimeLayout.setWeightSum(2f);
        dateTimeLayout.setPadding(0, 16, 0, 16);

        final com.google.android.material.button.MaterialButton btnDate = new com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dateParams.setMarginEnd(8); btnDate.setLayoutParams(dateParams);

        final com.google.android.material.button.MaterialButton btnTime = new com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        timeParams.setMarginStart(8); btnTime.setLayoutParams(timeParams);

        final java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd MMM", Locale.getDefault());
        final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        final Calendar selectedCal = Calendar.getInstance();
        // Fix F3: remember whether the user picked a date or a time in this dialog.
        // Before, only a changed date was saved, so a new time for "today" was ignored.
        final boolean[] departureChanged = {false};

        if (customDepartureTimestamp == 0) {
            btnDate.setText(getString(R.string.today)); btnTime.setText(getString(R.string.now));
        } else {
            selectedCal.setTimeInMillis(customDepartureTimestamp * 1000L);
            btnDate.setText(dateFormat.format(selectedCal.getTime()));
            btnTime.setText(timeFormat.format(selectedCal.getTime()));
        }

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                DatePickerDialog dp = new DatePickerDialog(requireContext(), new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedCal.set(year, month, dayOfMonth); btnDate.setText(dateFormat.format(selectedCal.getTime()));
                        departureChanged[0] = true;
                    }
                }, selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH));
                dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); dp.show();
            }
        });

        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                TimePickerDialog tp = new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selectedCal.set(Calendar.HOUR_OF_DAY, hourOfDay); selectedCal.set(Calendar.MINUTE, minute);
                        btnTime.setText(timeFormat.format(selectedCal.getTime()));
                        departureChanged[0] = true;
                    }
                }, selectedCal.get(Calendar.HOUR_OF_DAY), selectedCal.get(Calendar.MINUTE), true); tp.show();
            }
        });

        dateTimeLayout.addView(btnDate); dateTimeLayout.addView(btnTime);
        layout.addView(dateTimeLayout);

        TextView tvModes = new TextView(requireContext());
        tvModes.setText("\n" + getString(R.string.preferred_modes) + ":");
        tvModes.setTypeface(null, Typeface.BOLD);
        tvModes.setTextColor(getSecondaryTextColor());
        layout.addView(tvModes);

        final CheckBox cbBus = new CheckBox(requireContext()); cbBus.setText(getString(R.string.bus_option)); cbBus.setChecked(useBus);
        layout.addView(cbBus);
        final CheckBox cbMetro = new CheckBox(requireContext()); cbMetro.setText(getString(R.string.metro)); cbMetro.setChecked(useMetro);
        layout.addView(cbMetro);
        final CheckBox cbTram = new CheckBox(requireContext()); cbTram.setText(getString(R.string.tram_option)); cbTram.setChecked(useTram);
        layout.addView(cbTram);
        final CheckBox cbTrain = new CheckBox(requireContext()); cbTrain.setText(getString(R.string.train)); cbTrain.setChecked(useTrain);
        layout.addView(cbTrain);

        View separator = new View(requireContext());
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        separatorParams.setMargins(0, 16, 0, 16); separator.setLayoutParams(separatorParams);
        separator.setBackgroundColor(Color.LTGRAY); layout.addView(separator);

        final CheckBox cbElectricCar = new CheckBox(requireContext());
        cbElectricCar.setText(getString(R.string.my_car_is_electric)); cbElectricCar.setChecked(isElectricCar);
        layout.addView(cbElectricCar);

        builder.setView(layout);
        builder.setPositiveButton(getString(R.string.apply), new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                useBus = cbBus.isChecked(); useMetro = cbMetro.isChecked();
                useTram = cbTram.isChecked(); useTrain = cbTrain.isChecked();
                isElectricCar = cbElectricCar.isChecked();
                if (departureChanged[0] || !btnDate.getText().toString().equals(getString(R.string.today))) customDepartureTimestamp = selectedCal.getTimeInMillis() / 1000L;
                if (currentDestination != null) drawRouteToDestination(currentDestinationTitle, currentDestination.latitude, currentDestination.longitude, currentTransportMode);
            }
        });
        builder.setNeutralButton(getString(R.string.reset), new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                useBus = true; useMetro = true; useTram = true; useTrain = true; isElectricCar = false;
                customDepartureTimestamp = 0;
                if (currentDestination != null) drawRouteToDestination(currentDestinationTitle, currentDestination.latitude, currentDestination.longitude, currentTransportMode);
            }
        });
        builder.show();
    }

    private void loadSearchData() {
        DataBaseHelper dbHelper = new DataBaseHelper(requireContext());
        allCampuses = dbHelper.getAllCampuses();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        routeExecutor.shutdown(); // Fix F9
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchViewReal = null; campusMap = null;
    }

    private class MapSearchAdapter extends BaseAdapter implements Filterable {
        private Context context;
        private List<Campus> originalList;
        private List<Campus> filteredCampuses;
        public MapSearchAdapter(Context context, List<Campus> list) {
            this.context = context; this.originalList = new ArrayList<>(list); this.filteredCampuses = new ArrayList<>(list);
        }
        @Override public int getCount() { return filteredCampuses.size(); }
        @Override public Campus getItem(int position) { return filteredCampuses.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) { convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false); }
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(getItem(position).getName());
            return convertView;
        }
        @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Campus> filtered = new ArrayList<>();
                    if (constraint == null || constraint.length() == 0) { filtered.addAll(originalList); }
                    else {
                        String query = constraint.toString().toLowerCase().trim();
                        for (Campus c : originalList) { if (c.getName().toLowerCase().contains(query)) filtered.add(c); }
                    }
                    results.values = filtered; results.count = filtered.size();
                    return results;
                }
                @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredCampuses.clear(); if (results.values != null) { filteredCampuses.addAll((List<Campus>) results.values); }
                    notifyDataSetChanged();
                }
            };
        }
    }

    private static class RouteSegment {
        List<LatLng> points; int color; boolean isWalking; String instructionTitle; LatLng startPoint;
        String duration; String departureStop; String arrivalStop; String headsign; String numStops;
        String departureTime; String arrivalTime; String co2Info; int colorCO2;
    }


     // Detects whether the device is in dark mode

    private boolean isDarkModeOnDevice() {
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }


    private boolean isColorTooLight(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

        // If the luminance is greater than 0.75, the colour is very light (close to white)
        // Adjust this threshold as needed: 0.7 = stricter, 0.8 = more permissive
        return luminance > 0.75;
    }

    public void startRouteToDestination(String title, double destLat, double destLng) {
        // First, apply the preferences
        loadMapSettings();

        // Then start the route calculation with the transport mode just read.
        drawRouteToDestination(title, destLat, destLng, currentTransportMode);
    }
}