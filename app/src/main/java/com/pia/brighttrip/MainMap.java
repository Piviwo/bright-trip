package com.pia.brighttrip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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


public class MainMap extends Fragment {
    private static final LatLng BERLIN = new LatLng(52.5308, 13.3472);
    private static final float INITIAL_ZOOM_LEVEL = 13.0f;

    private GoogleMap googleMap;
    private Marker currentLocationMarker;
    private Marker clickMarker;
    private GeoJsonLayer currentGeoJsonLayer;

    private final String apiKey = BuildConfig.OPEN_ROUTE_API_KEY;
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

        // Observe location updates
        locationViewModel.getLocation().observe(getViewLifecycleOwner(), this::updateCurrentLocation);
    }

    /**
     * Handles the map being ready for interaction.
     * @param googleMap The GoogleMap instance.
     */
    private void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        setupMapStyle();
        setupMapUI();
        addInitialMarker();
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
            BitmapDescriptor customIcon = BitmapDescriptorFactory.fromResource(R.drawable.moon);

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
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title("Current Location");
                currentLocationMarker = googleMap.addMarker(markerOptions);
            } else {
                // Update the marker's position
                currentLocationMarker.setPosition(currentLatLng);
            }

            // Move the camera to the current location
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, INITIAL_ZOOM_LEVEL));
        }
    }

    /**
    * Sets up styles for the basemap
    */
    private void setupMapStyle() {
        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
            if (!success) {
                // Log failure to apply style
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log exceptions for debugging
        }
    }

    private void setupMapUI() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
    }

    /**
     * Adds an initial marker to the map and moves camera to Berlin-Moabit
     */
    private void addInitialMarker() {
        MarkerOptions marker = new MarkerOptions()
                .position(BERLIN)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .alpha(0.8f);
        googleMap.addMarker(marker);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));
    }

    /**
     * Click listener for the map (on click selects a location)
     */
    private void setupMapClickListener() {
        googleMap.setOnMapClickListener(newPos -> {
            if (clickMarker != null){
                //remove previous markers
                clickMarker.remove();
            }
            clickMarker = googleMap.addMarker(new MarkerOptions()
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
                            + "?api_key=" + apiKey
                            + "&start="
                            + currentLatLng.longitude + ","
                            + currentLatLng.latitude
                            + "&end="
                            + clickMarker.getPosition().longitude + ","
                            + clickMarker.getPosition().latitude;
            new DownloadGeoJsonFile().execute(url);

            /*
            // Remove the previous polyline, if it exists
            if (currentRoute != null) {
                currentRoute.remove();
            }

            // Draw a polyline between the current location and the destination
            currentRoute = googleMap.addPolyline(new PolylineOptions()
                    .add(currentLatLng, destination)
                    .width(8)
                    .color(Color.WHITE)
                    .geodesic(true));*/
        }
    }

    /**
     * Downloads the geojson file of the route
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
            }
        }
    }
}
