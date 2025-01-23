package com.pia.brighttrip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
import android.location.Location;


public class MainMap extends Fragment {
    private static final LatLng BERLIN = new LatLng(52.5308, 13.3472);
    private static final float INITIAL_ZOOM_LEVEL = 15.0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap googleMap;
    private Marker clickMarker;
    private GeoJsonLayer currentGeoJsonLayer;
    private Polyline currentPolyline;
    private Double xCoord;
    private Double yCoord;
    private boolean areCoordinatesInitialized = false;

    private final String googleApiKey = BuildConfig.GOOGLE_API_KEY;
    private final String routingApiKey = BuildConfig.OPEN_ROUTE_API_KEY;

    private FusedLocationProviderClient fusedLocationClient;

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
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Check permissions and request location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request permission if not granted
            ActivityCompat.requestPermissions(requireActivity(), new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Get the last known location and move the camera
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            } else {
                Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this::onMapReady);
        }

        setUpFindPlaces();

        // Retrieve the arguments passed from ExploreFragment
        Bundle args = getArguments();
        if (args != null) {
            xCoord = args.getDouble("xcoord", 0);
            yCoord = args.getDouble("ycoord", 0);
            areCoordinatesInitialized = true;
        } else {
            //Toast.makeText(getContext(), "No data received!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the map being ready for interaction.
     * @param googleMap The GoogleMap instance.
     */
    private void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        setupMapUI();

        setupMapClickListener();
        if (areCoordinatesInitialized) {
            showLocationOnMap();
        }

        addLampLayer();
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
            ImageButton clearButton = fragmentView.findViewById(
                    com.google.android.libraries.places.R.id.places_autocomplete_clear_button);
            clearButton.setColorFilter(getResources().getColor(R.color.white_grey));

            // Overrides the old on click function of the search input
            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(requireContext(), "Search button clicked!", Toast.LENGTH_SHORT).show();
                    EditText searchField = fragmentView.findViewById(
                            com.google.android.libraries.places.R.id.places_autocomplete_search_input);
                    searchField.setText("");
                    if (currentPolyline != null) {
                        currentPolyline.remove();
                    }
                    if (currentGeoJsonLayer != null) {
                        currentGeoJsonLayer.removeLayerFromMap();
                    }
                    if(clickMarker != null){
                        clickMarker.remove();
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));

                }
            });
        }

        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                LatLng selectedLocation = place.getLatLng();

                if (selectedLocation != null) {
                    // Remove the previous marker if it exists
                    if (clickMarker != null) {
                        clickMarker.setPosition(selectedLocation);
                    } else {
                        clickMarker = googleMap.addMarker(new MarkerOptions()
                                .position(selectedLocation)
                                .zIndex(1.0f)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
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
     * Shows the location from the Explore Page on map
     */
    private void showLocationOnMap() {
        if (googleMap == null) {
            Log.e("showLocationOnMap", "googleMap is not initialized yet!");
            return;
        }
        LatLng position = new LatLng(yCoord,xCoord);
        googleMap.addMarker(new MarkerOptions()
            .position(position)
            .zIndex(1.0f)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        Log.d("showLocationOnMap", "position" + position.toString());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
    }

    /**
     * Adds the Geojson file with street lamps
     */
    private void addLampLayer(){
        // Load the street lamps
        try {
            GeoJsonLayer layer = new GeoJsonLayer(googleMap, R.raw.moabit_lamps, requireContext());
            BitmapDescriptor customIcon = BitmapDescriptorFactory.fromResource(R.drawable.lamp);

            // Iterate through each feature/point in the geojson layer
            for (GeoJsonFeature feature : layer.getFeatures()) {
                if (feature.hasGeometry() && feature.getGeometry() instanceof GeoJsonPoint) {
                    //add our custom lamp icon
                    GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                    pointStyle.setIcon(customIcon);
                    feature.setPointStyle(pointStyle);
                    pointStyle.setAnchor(0.5f, 0.5f);
                }
            }
            layer.addLayerToMap();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e("GeoJsonError", "Error loading GeoJSON file: " + e.getMessage());
        }
    }

    /**
     * Sets up map UI settings
     */
    private void setupMapUI() {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BERLIN, INITIAL_ZOOM_LEVEL));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);

        // Enable location features
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            //onRequestPermissionsResult();
        }

        //Custom map buttons
        ImageButton btnZoomIn = getView().findViewById(R.id.btn_zoom_in);
        ImageButton btnZoomOut = getView().findViewById(R.id.btn_zoom_out);
        ImageButton btnCurrentLocation = getView().findViewById(R.id.btn_current_location);

        btnZoomIn.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        btnCurrentLocation.setOnClickListener(v -> {
            // Get the last known location from FusedLocationProviderClient
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    // Get the latitude and longitude from the Location object
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng userLocation = new LatLng(latitude, longitude);

                    // Create a CameraPosition with the user's location
                    CameraPosition camPos = new CameraPosition.Builder()
                            .target(userLocation)
                            .zoom(15)
                            .bearing(25)
                            .build();

                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));

                } else {
                    Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable the current location button functionality
                setupMapUI(); // Ensure the UI gets updated to show location
            } else {
                // Permission denied, handle the case
                Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
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
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

            drawRouteTo(newPos);
        });
    }

    /**
     * Draws a polyline between the current location and the specified position.
     */
    private void drawRouteTo(LatLng destination) {
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(requireActivity(), location -> {
                if (location != null && googleMap != null) {
                    // Build the Directions API request URL
                    String url =
                        "https://api.openrouteservice.org/v2/directions/"
                                + "driving-car"
                                + "?api_key=" + routingApiKey
                                + "&start="
                                + location.getLongitude() + ","
                                + location.getLatitude() + ","
                                + "&end="
                                + clickMarker.getPosition().longitude + ","
                                + clickMarker.getPosition().latitude;
                    new DownloadGeoJsonFile(location).execute(url);
                } else {
                    Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * Downloads the geojson file of the route between to locations
     */
    private class DownloadGeoJsonFile extends AsyncTask<String, Void, GeoJsonLayer> {

        private Location location;
        public DownloadGeoJsonFile(Location location) {
            this.location = location;
        }

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
                lineStringStyle.setColor(ContextCompat.getColor(requireContext(), R.color.route_color));
                lineStringStyle.setWidth(12f);

                // Add the new layer to the map
                layer.addLayerToMap();

                // Update the reference to the current layer
                currentGeoJsonLayer = layer;
            } else {
                currentPolyline = googleMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(location.getLatitude(), location.getLongitude()), clickMarker.getPosition())
                        .width(8)
                        .color(ContextCompat.getColor(requireContext(), R.color.route_color))
                        .zIndex(1.0f)
                        .geodesic(true));
            }

            // Create LatLngBounds.Builder to include both marker positions
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
            builder.include(clickMarker.getPosition());
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }
}
