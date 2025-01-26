package com.pia.brighttrip;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Toast;

// This Activity contains the main activity which is called when starting the app
public class MainActivity extends AppCompatActivity {

    // Initialize global variables
    BottomNavigationView bottomNavigationView;
    LocationManager locationManager;
    LocationListener locationListener;
    private LocationViewModel locationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Bottom Navigation View and set Listener
        bottomNavigationView = findViewById(R.id.btm_nav);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return handleNavigationSelection(item);
            }
        });

        // Initialize Location Manager and Listener
        getSupportFragmentManager().beginTransaction().replace(R.id.rel_layout, new MainMap()).commit();
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    locationViewModel.setLocation(location);
                }
            }
        };

        // Request Permissions and Handle Location Updates
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    }
                    Boolean coarseLocationGranted = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    }

                    if (fineLocationGranted != null && fineLocationGranted) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "Location permissions are required to use this feature.", Toast.LENGTH_SHORT).show();
                            return;
                        }
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

    /**
     * Handles the navigation selection of the bottom navigation bar
     * @param item
     */
    private boolean handleNavigationSelection(@NonNull MenuItem item) {
        Fragment fragment = null;

        if (item.getItemId() == R.id.mainmap)
            fragment = new MainMap();

        if (item.getItemId() == R.id.explore)
            fragment = new ExploreFragment();

        if (item.getItemId() == R.id.support)
            fragment = new SupportFragment();

        if (item.getItemId() == R.id.about)
            fragment = new AboutFragment();

        if (fragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.rel_layout, fragment).commit();

        return true;
    }
}
