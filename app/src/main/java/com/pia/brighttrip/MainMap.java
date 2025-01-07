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

import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.List;

public class MainMap extends Fragment {

    // Constants for default map settings
    private static final LatLng BERLIN = new LatLng(52.520008, 13.404954);
    private static final float INITIAL_ZOOM_LEVEL = 13.0f;
    String apiKey = BuildConfig.GOOGLE_API_KEY;
    private PlacesClient placesClient;

    // List to track markers added by user clicks
    private final List<Marker> clickMarkers = new ArrayList<>();

    @Nullable
    @Override
    /*part of the Android Fragment lifecycle and responsible for
     * creating and returning the view hierarchy associated with the Fragment.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_map, container, false);
    }

    /*called when the view associated with the Fragment has been created
     * used to set up UI elements or perform operations that require the fragment's view hierarchy to be fully created
     * configures a SupportMapFragment to display a Google Map within the Fragment*/
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get the MapFragment and set up the callback
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    /*  callback function for map setup
    * ensures  UI thread remains responsive, notifies when map is ready
    * decouples  map's lifecycle from own logic*/
    private final OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            setupMapStyle(googleMap);   // Apply custom map styling
            setupMapUI(googleMap);      // Configure UI settings for the map
            addInitialMarker(googleMap); // Place an initial marker on the map
            setupMapClickListener(googleMap); // Handle user clicks on the map
        }

        /**
         * Sets up custom styling for the map using a JSON style resource.
         * @param googleMap The GoogleMap instance to style.
         */
        private void setupMapStyle(GoogleMap googleMap) {
            try {
                boolean success = googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                requireContext(), R.raw.map_style));
                if (!success) {
                    // Log failure to apply style
                    // tbc

                }
            } catch (Exception e) {
                e.printStackTrace(); // Log exceptions for debugging
            }
        }

        /**
         * Configures basic UI settings for the Google Map.
         * @param googleMap The GoogleMap instance to configure.
         */
        private void setupMapUI(GoogleMap googleMap) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); // Set map type to normal
            googleMap.getUiSettings().setZoomControlsEnabled(true); // Enable zoom controls
            googleMap.getUiSettings().setMapToolbarEnabled(false); // Enable map toolbar
        }

        /**
         * Adds an initial marker at a predefined location (Berlin).
         * @param googleMap The GoogleMap instance to add the marker to.
         */
        private void addInitialMarker(GoogleMap googleMap) {
            MarkerOptions marker = new MarkerOptions()
                    .position(BERLIN)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .alpha(0.8f); // Set transparency
            googleMap.addMarker(marker); // Add the marker to the map

            // Move and zoom the camera to the marker location
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));
        }

        /**
         * Sets up a listener to handle clicks on the map.
         * Adds a marker at the clicked location and enables traffic view.
         * @param googleMap The GoogleMap instance to add the listener to.
         */
        private void setupMapClickListener(GoogleMap googleMap) {
            googleMap.setOnMapClickListener(newPos -> {
                // Clear existing markers added by clicks
                for (Marker marker : clickMarkers) {
                    marker.remove();
                }
                clickMarkers.clear();

                // Add a new marker at the clicked position
                Marker newMarker = googleMap.addMarker(new MarkerOptions()
                        .position(newPos)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                if (newMarker != null) {
                    clickMarkers.add(newMarker);
                }

                // Construct the API URL for route calculation
                String baseUrl = "https://api.openrouteservice.org/v2/directions/foot-walking";
                String start = BERLIN.longitude + "," + BERLIN.latitude;
                String end = newPos.longitude + "," + newPos.latitude;
                String url = baseUrl + "?api_key=" + apiKey + "&start=" + start + "&end=" + end;
                //todo new drawLine(googleMap).execute(url);

                //Enable traffic layer for additional map features
                //googleMap.setTrafficEnabled(true);
            });
        }

        //TODO: find places

    };
}
