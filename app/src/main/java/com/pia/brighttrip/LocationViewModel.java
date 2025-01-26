package com.pia.brighttrip;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.location.Location;

public class LocationViewModel extends ViewModel {
    // Initialize global variables
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();

    /**
     * Function to set the current location
     * @param location
     */
    public void setLocation(Location location) {
        locationLiveData.setValue(location);
    }

    /**
     * Function to get the current location
     */
    public LiveData<Location> getLocation() {
        return locationLiveData;
    }
}