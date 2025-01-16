package com.pia.brighttrip;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;

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
            // Handle initialization failure gracefully
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
                Object clickedItem = parent.getItemAtPosition(position);
                Toast.makeText(getContext(), "Clicked: " + clickedItem.toString(), Toast.LENGTH_SHORT).show();
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
        double xcoord = cursor.getColumnIndex("xcoord");
        double ycoord = cursor.getColumnIndex("ycoord");


        for (int i = 0; i < length; i++) {
            html_array[i] = Html.fromHtml(cursor.getString(index_name) + "<br><i>"
                    + cursor.getString(index_fclass) + "</i><br>" + "{AdressPlaceholder}") ;
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
