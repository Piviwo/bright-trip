package com.pia.brighttrip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


public class MainMap extends Fragment {
    private static final LatLng BERLIN = new LatLng(52.520008, 13.404954);
    private static final float INITIAL_ZOOM_LEVEL = 13.0f;

    private GoogleMap googleMap;
    private Marker currentLocationMarker;
    private Marker clickMarker;
    private Polyline currentRoute;

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

    private void addInitialMarker() {
        MarkerOptions marker = new MarkerOptions()
                .position(BERLIN)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .alpha(0.8f);
        googleMap.addMarker(marker);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));
    }

    private void setupMapClickListener() {
        googleMap.setOnMapClickListener(newPos -> {
            if (clickMarker != null){
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
     * @param destination The LatLng position of the new marker.
     */
    //todo: draw route with GOOGLE API
    private void drawRouteTo(LatLng destination) {
        if (currentLocationMarker != null && googleMap != null) {
            // Get the current location marker's position
            LatLng currentLatLng = currentLocationMarker.getPosition();

            // Remove the previous polyline, if it exists
            if (currentRoute != null) {
                currentRoute.remove();
            }

            // Draw a polyline between the current location and the destination
            currentRoute = googleMap.addPolyline(new PolylineOptions()
                    .add(currentLatLng, destination)
                    .width(8)
                    .color(Color.WHITE)
                    .geodesic(true));
        }
    }
}
