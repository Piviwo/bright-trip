package com.pia.brighttrip;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Bottom Navigation View
        bottomNavigationView = findViewById(R.id.btm_nav);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return handleNavigationSelection(item);
            }
        });

        getSupportFragmentManager().beginTransaction().replace(R.id.rel_layout, new MainMap()).commit();

        // Initialize Location Manager and Listener
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    Toast.makeText(MainActivity.this, "Location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Request Permissions and Handle Location Updates
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Request updates from GPS
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                10000, // Minimum time interval (ms)
                                10,    // Minimum distance interval (meters)
                                locationListener,
                                Looper.getMainLooper()
                        );
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Request updates from Network Provider
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                0, // Minimum time interval (ms)
                                0, // Minimum distance interval (meters)
                                locationListener,
                                Looper.getMainLooper()
                        );
                    } else {
                        // Permission denied
                        Toast.makeText(this, "Location permissions denied. Cannot fetch location.", Toast.LENGTH_LONG).show();
                    }
                });

        // Launch the permission request
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        locationPermissionRequest.launch(PERMISSIONS);
    }

    private boolean handleNavigationSelection(@NonNull MenuItem item) {
        Fragment fragment = null;

        if (item.getItemId() == R.id.mainmap)
            fragment = new MainMap();

        if (item.getItemId() == R.id.explore)
            fragment = new MainMap();

        if (item.getItemId() == R.id.support)
            fragment = new SupportFragment();

        if (item.getItemId() == R.id.settings)
            fragment = new SettingsFragment();

        if (fragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.rel_layout, fragment).commit();

        return true;
    }
}
