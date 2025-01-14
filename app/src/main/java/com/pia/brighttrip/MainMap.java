package com.pia.brighttrip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;

import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;


public class MainMap extends Fragment {
    private static final LatLng BERLIN = new LatLng(52.5308, 13.3472);
    private static final float INITIAL_ZOOM_LEVEL = 15.0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap googleMap;
    private Marker currentLocationMarker;
    private Marker clickMarker;
    private GeoJsonLayer currentGeoJsonLayer;
    private Polyline currentPolyline;

    private String googleApiKey = BuildConfig.GOOGLE_API_KEY;
    private String routingApiKey = BuildConfig.OPEN_ROUTE_API_KEY;

    private LocationViewModel locationViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the LocationViewModel
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);

        // Set up the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this::onMapReady);
        }

        setUpFindPlaces();

        // Observe location updates
        locationViewModel.getLocation().observe(getViewLifecycleOwner(), this::updateCurrentLocation);
    }

    /**
     * Sets up the search input for finding places with google places API
     */
    private void setUpFindPlaces(){

        // Initialize the Places API and AutocompleteSupportFragment.
        Places.initialize(requireContext().getApplicationContext(), googleApiKey);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);


        // Specify data of a place to return at request
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        // Change the default google style
        if (autocompleteFragment != null) {
            // Access the root view of the AutocompleteSupportFragment
            View fragmentView = autocompleteFragment.getView();
            if (fragmentView != null) {

                fragmentView.setBackgroundColor(getResources().getColor(R.color.dark_blue));

                // Find the EditText within the fragment's view hierarchy
                EditText editText = fragmentView.findViewById(
                        com.google.android.libraries.places.R.id.places_autocomplete_search_input);

                if (editText != null) {
                    editText.setTextColor(getResources().getColor(R.color.white));
                    editText.setHintTextColor(getResources().getColor(R.color.white_grey));
                }

                ImageView searchIcon = fragmentView.findViewById(
                        com.google.android.libraries.places.R.id.places_autocomplete_search_button);

                if (searchIcon != null) {
                    // Change the tint of the magnifier icon
                    searchIcon.setColorFilter(getResources().getColor(R.color.white_grey),
                            PorterDuff.Mode.SRC_IN);
                }
            }
        }

        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                // Get the LatLng of the selected place
                LatLng selectedLocation = place.getLatLng();

                // Check if LatLng is not null
                if (selectedLocation != null) {
                    // Remove the previous marker if it exists
                    if (clickMarker != null) {
                        clickMarker.setPosition(selectedLocation);
                    } else {
                        clickMarker = googleMap.addMarker(new MarkerOptions()
                                .position(selectedLocation)
                                .zIndex(1.0f)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                    }

                    // Move and animate the camera to the selected place
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                    drawRouteTo(selectedLocation);
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                // Handle any errors
                Log.e("PlaceSelectionError", "An error occurred: " + status);
            }
        });
    }

    /**
     * Handles the map being ready for interaction.
     * @param googleMap The GoogleMap instance.
     */
    private void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setupMapUI();
        setupMapClickListener();
        addLampLayer();
    }
    /**
     * Adds the Geojson file with street lamps
     */
    private void addLampLayer(){
        // Load the street lamps
        try {
            GeoJsonLayer layer = new GeoJsonLayer(googleMap, R.raw.moabit_lamps, requireContext());
            //todo: refactor?
            BitmapDescriptor customIcon = BitmapDescriptorFactory.fromResource(R.drawable.lamp);

            // Iterate through each feature/point in the geojson layer
            for (GeoJsonFeature feature : layer.getFeatures()) {
                if (feature.hasGeometry() && feature.getGeometry() instanceof GeoJsonPoint) {
                    //add our custom lamp icon
                    GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                    pointStyle.setIcon(customIcon);
                    feature.setPointStyle(pointStyle);
                }
            }
            layer.addLayerToMap();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e("GeoJsonError", "Error loading GeoJSON file: " + e.getMessage());
        }
    }

    /**
     * Updates the current location marker on the map.
     * @param location The new location.
     */
    private void updateCurrentLocation(Location location) {
        if (location != null && googleMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            if (currentLocationMarker == null) {
                // Add a new marker for the current location
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(currentLatLng)
                        .zIndex(1.0f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title("Current Location");
                currentLocationMarker = googleMap.addMarker(markerOptions);
            } else {
                // Update the marker's position
            }
        }
    }

    private void setupMapUI() {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Enable location features
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            requestLocationPermission();
        }

        // Set listeners for location button and location click events
        googleMap.setOnMyLocationButtonClickListener(() -> {
            Toast.makeText(getContext(), "My Location button clicked", Toast.LENGTH_SHORT).show();
            return false;
        });

        googleMap.setOnMyLocationClickListener(location -> {
            Toast.makeText(getContext(), "Location: " + location.toString(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Function to request location permission if not set
     */
    private void requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show an explanation to the user asynchronously
            Toast.makeText(getContext(), "Location permission is required to use this feature.", Toast.LENGTH_SHORT).show();
        }
        // Request the permission
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * Click listener for the map to select a location
     */
    private void setupMapClickListener() {
        googleMap.setOnMapClickListener(newPos -> {
            if (clickMarker != null){
                //remove previous markers
                clickMarker.remove();
            }
            clickMarker = googleMap.addMarker(new MarkerOptions()
                    .zIndex(1.0f)
                    .position(newPos)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

            drawRouteTo(newPos);
        });
    }

    /**
     * Draws a polyline between the current location and the specified position.
     */
    private void drawRouteTo(LatLng destination) {
        if (currentLocationMarker != null && googleMap != null) {
            // Get the current location marker's position
            LatLng currentLatLng = currentLocationMarker.getPosition();

            // Build the Directions API request URL
            String url =
                    "https://api.openrouteservice.org/v2/directions/"
                            + "foot-walking"
                            + "?api_key=" + routingApiKey
                            + "&start="
                            + currentLatLng.longitude + ","
                            + currentLatLng.latitude
                            + "&end="
                            + clickMarker.getPosition().longitude + ","
                            + clickMarker.getPosition().latitude;
            new DownloadGeoJsonFile().execute(url);
        }
    }

    /**
     * Downloads the geojson file of the route between to locations
     */
    private class DownloadGeoJsonFile extends AsyncTask<String, Void, GeoJsonLayer> {

        @Override
        protected GeoJsonLayer doInBackground(String... params) {
            try {
                InputStream stream = new URL(params[0]).openStream();

                String line;
                StringBuilder result = new StringBuilder();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream));

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                stream.close();

                return new GeoJsonLayer(googleMap, new JSONObject(result.toString()));
            } catch (IOException e) {
                Log.e("mLogTag", "GeoJSON file could not be read", e);
            } catch (JSONException e) {
                Log.e("mLogTag",
                        "GeoJSON file could not be converted to a JSONObject");
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoJsonLayer layer) {
            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            if (layer != null) {
                // Remove the old layer if it exists
                if (currentGeoJsonLayer != null) {
                    currentGeoJsonLayer.removeLayerFromMap();
                }

                // Style the new layer
                GeoJsonLineStringStyle lineStringStyle = layer.getDefaultLineStringStyle();
                lineStringStyle.setColor(Color.WHITE);
                lineStringStyle.setWidth(10f);

                // Add the new layer to the map
                layer.addLayerToMap();

                // Update the reference to the current layer
                currentGeoJsonLayer = layer;
            } else {
                currentPolyline = googleMap.addPolyline(new PolylineOptions()
                        .add(currentLocationMarker.getPosition(), clickMarker.getPosition())
                        .width(8)
                        .color(Color.WHITE)
                        .zIndex(1.0f)
                        .geodesic(true));
            }

            // Create LatLngBounds.Builder to include both marker positions
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(currentLocationMarker.getPosition());
            builder.include(clickMarker.getPosition());
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }
}
