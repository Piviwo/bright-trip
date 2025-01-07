package com.pia.brighttrip;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.location.Location;

public class LocationViewModel extends ViewModel {
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();

    public void setLocation(Location location) {
        locationLiveData.setValue(location);
    }

    public LiveData<Location> getLocation() {
        return locationLiveData;
    }
}