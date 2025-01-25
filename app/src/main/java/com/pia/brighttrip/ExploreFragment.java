package com.pia.brighttrip;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ExploreFragment extends Fragment {

    private ListView list_view;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Cursor dbCursor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the ListView using the view passed in onViewCreated
        list_view = view.findViewById(R.id.list);

        // Initialize dbHelper and handle database setup
        try {
            dbHelper = new DatabaseHelper(requireContext());
            dbHelper.createDataBase();
            database = dbHelper.getDataBase();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Query the database
        try {
            dbCursor = database.rawQuery(
                    "SELECT * FROM pois_moabit WHERE name IS NOT NULL AND fclass " +
                            "IN ('cafe', 'restaurant', 'bar', 'fire_station', 'hotel', 'police', 'pub', 'kiosk', 'fast_food');", null);
            ArrayAdapter<CharSequence> adapter = createAdapterHtml(dbCursor);
            list_view.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainMap mainMapFragment = new MainMap();
                Bundle bundle = new Bundle();

                if (dbCursor != null && dbCursor.moveToPosition(position)) {
                    @SuppressLint("Range") String name = dbCursor.getString(dbCursor.getColumnIndex("name"));
                    @SuppressLint("Range") Double xcoord = dbCursor.getDouble(dbCursor.getColumnIndex("xcoord"));
                    @SuppressLint("Range") Double ycoord = dbCursor.getDouble(dbCursor.getColumnIndex("ycoord"));
                    bundle.putString("name", name);
                    bundle.putDouble("xcoord", xcoord);
                    bundle.putDouble("ycoord", ycoord);
                    mainMapFragment.setArguments(bundle);
                } else {
                    Log.e("ExploreFragment", "Cursor is null or unable to move to position: " + position);
                }

                // Navigate to MainMapFragment
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.rel_layout, mainMapFragment)
                        .commit();

                // Change bottom navigation color
                BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.btm_nav);
                bottomNavigationView.getMenu().findItem(R.id.mainmap).setChecked(true);
            }
        });

    }

    private ArrayAdapter<CharSequence> createAdapterHtml(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            return new ArrayAdapter<>(requireContext(),
                    R.layout.list_item, new Spanned[]{});
        }

        int length = cursor.getCount();
        cursor.moveToFirst();
        Spanned[] html_array = new Spanned[length];
        Geocoder geocoder = new Geocoder(requireContext());

        int index_fclass = cursor.getColumnIndex("fclass");
        int index_name = cursor.getColumnIndex("name");
        int index_xcoord = cursor.getColumnIndex("xcoord");
        int index_ycoord = cursor.getColumnIndex("ycoord");
        int index_opens = cursor.getColumnIndex("opens");
        int index_closes = cursor.getColumnIndex("closes");


        for (int i = 0; i < length; i++) {
            String name = cursor.getString(index_name).toUpperCase();
            String fclass = cursor.getString(index_fclass).toLowerCase().replace("_", " ");
            String opens = cursor.getString(index_opens);
            String closes = cursor.getString(index_closes);
            String status = "status not available";
            double xcoord = cursor.getDouble(index_xcoord);
            double ycoord = cursor.getDouble(index_ycoord);

            // Use the Geocoder to fetch the address for the coordinates
            String address = "";
            if (i < 10) { // Geocode only the first 10 items
                try {
                    List<Address> addresses = geocoder.getFromLocation(ycoord, xcoord, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);

                        // Extract only specific components of the address
                        String street = addr.getThoroughfare();
                        String streetNumber = addr.getSubThoroughfare();
                        String postalCode = addr.getPostalCode();
                        String city = addr.getLocality();

                        // Build address string
                        address = (streetNumber != null ? streetNumber + " " : "") +
                                (street != null ? street + ", " : "") +
                                (postalCode != null ? postalCode + " " : "") +
                                (city != null ? city : "");
                    } else {
                        address = "Address not found";
                    }
                } catch (IOException e) {
                    address = "Geocoding error";
                }
            } else {
                address = "Geocoding skipped";
            }

            try {
                // Define input format for opens and closes times
                DateFormat inputFormat = new SimpleDateFormat("HH:mm:ss");

                // Parse the "opens" and "closes" times
                Date openTime = inputFormat.parse(opens);
                Date closeTime = inputFormat.parse(closes);

                // Get the current time of the device
                Date currentTime = new Date();
                DateFormat currentTimeFormat = new SimpleDateFormat("HH:mm:ss");
                String currentTimeString = currentTimeFormat.format(currentTime);
                currentTime = inputFormat.parse(currentTimeString);

                // Determine if the amenity is open or closed
                if (currentTime.after(openTime) && currentTime.before(closeTime)) {
                    status = "open";
                } else {
                    status = "closed";
                }

            } catch (ParseException e) {
                Log.e("AMENITY_STATUS", "Error parsing time", e);
            }

            String color = status.equals("open") ? "#FBD437" : "#F94124";
            String openText = status.equals("open") ? " - from: " : " - open from: ";
                    html_array[i] = HtmlCompat.fromHtml(
                    "<big><span style='color: #FFD800;'>" + name + "</span></big><small><br></small>" +
                            "<span>" + address + "</span><br><br>" +
                            //"<small><span>" + fclass + "</span><br><br></small>" +
                            "<b><span style='color:" + color + ";'>" + status + "</span></b>" +
                            "<span>" + openText + opens.substring(0, 5) + " to " + closes.substring(0, 5) + "</span>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            );
            cursor.moveToNext();
        }

        return new ArrayAdapter<>(requireContext(), R.layout.list_item, html_array);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Close the cursor to prevent memory leaks
        if (dbCursor != null && !dbCursor.isClosed()) {
            dbCursor.close();
        }
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
}
