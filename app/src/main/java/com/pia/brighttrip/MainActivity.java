package com.pia.brighttrip;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.btm_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        getSupportFragmentManager().beginTransaction().replace(R.id.rel_layout, new MainMap()).commit();

    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        if (item.getItemId() == R.id.mainmap)
                fragment = new MainMap();

        if (item.getItemId() ==  R.id.explore)
                fragment = new MainMap();

        if (item.getItemId() ==  R.id.support)
                fragment = new SupportFragment();

        if (item.getItemId() ==  R.id.settings)
                fragment = new SettingsFragment();

        if (fragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.rel_layout, fragment).commit();

        return true;
    }
}