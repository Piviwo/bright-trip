package com.pia.brighttrip;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Html;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
            dbCursor = database.rawQuery("SELECT * FROM pois_moabit WHERE name IS NOT NULL;", null);
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
        int index_fclass = cursor.getColumnIndex("fclass");
        int index_name = cursor.getColumnIndex("name");
        int index_xcoord = cursor.getColumnIndex("xcoord");
        int index_ycoord = cursor.getColumnIndex("ycoord");
        //int index_address = cursor.getColumnIndex("address");
        int index_opens = cursor.getColumnIndex("opens");
        int index_closes = cursor.getColumnIndex("closes");


        for (int i = 0; i < length; i++) {
            String name = cursor.getString(index_name).toUpperCase();
            String fclass = cursor.getString(index_fclass).toUpperCase();
            String opens = cursor.getString(index_opens);
            String closes = cursor.getString(index_closes);
            String status = "status not available";
            //String address = cursor.getString(index_address).toUpperCase();

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

            //todo: try table
            String color = "FBD437";
            html_array[i] = Html.fromHtml(
                    "<b><span>" + name + "</span></b><br><br>" +
                            "<i><span>" + fclass + "</span></i><br>" +
                            "<span>" + "address" + "</span><br><br>" +
                            "<span>" + status + "</span><br>" +
                            "<span style='color:" + color + ";'> open from: " + opens.substring(0, 5) + " to " + closes.substring(0, 5) + "</span>"
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
