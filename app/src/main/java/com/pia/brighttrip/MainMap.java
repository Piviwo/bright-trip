package com.pia.brighttrip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MainMap extends Fragment {

    private static final LatLng BERLIN = new LatLng(52.520008, 13.404954);
    private static final float INITIAL_ZOOM_LEVEL = 13.0f;

    private final List<Marker> clickMarkers = new ArrayList<>();

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            setupMapStyle(googleMap);
            setupMapUI(googleMap);
            addInitialMarker(googleMap);
            setupMapClickListener(googleMap);
        }

        private void setupMapStyle(GoogleMap googleMap) {
            try {
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                requireContext(), R.raw.map_style));
                if (!success) {
                    // Log failure to set style (add logging mechanism here if needed)
                }
            } catch (Exception e) {
                e.printStackTrace(); // Log exception
            }
        }

        private void setupMapUI(GoogleMap googleMap) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(true);
        }

        private void addInitialMarker(GoogleMap googleMap) {
            MarkerOptions marker = new MarkerOptions()
                    .position(BERLIN)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .alpha(0.8f);
            googleMap.addMarker(marker);

            // Move and zoom camera to Berlin
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));
        }

        private void setupMapClickListener(GoogleMap googleMap) {
            googleMap.setOnMapClickListener(newPos -> {
                // Remove all markers added on click
                for (Marker marker : clickMarkers) {
                    marker.remove();
                }
                clickMarkers.clear();

                // Add a marker at the clicked position
                Marker newMarker = googleMap.addMarker(new MarkerOptions().position(newPos));
                clickMarkers.add(newMarker);

                // Construct route URL
                String baseUrl = "https://api.openrouteservice.org/v2/directions/foot-walking";
                String start = BERLIN.longitude + "," + BERLIN.latitude;
                String end = newPos.longitude + "," + newPos.latitude;
                String apiKey = "YOUR_API_KEY_HERE";
                String url = baseUrl + "?api_key=" + apiKey + "&start=" + start + "&end=" + end;

                // Enable traffic layer and log or handle the URL for further processing
                googleMap.setTrafficEnabled(true);
                // TODO: Use the URL for network operations
            });
        }
    };

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
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}
