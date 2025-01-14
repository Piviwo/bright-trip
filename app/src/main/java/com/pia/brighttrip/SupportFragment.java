package com.pia.brighttrip;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class SupportFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support, container, false);

        // Referencing the ListView in the fragment layout
        ListView listView = view.findViewById(R.id.call_list);

        //todo: add intents
        // List of emergency contacts
        ArrayList<String> contactNames = new ArrayList<>();
        contactNames.add("Police: 110");
        contactNames.add("Women's Helpline: (030) 610063");
        contactNames.add("Mental Health Helpline: (030) 3906300");

        // Setting up an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                contactNames
        );

        listView.setAdapter(adapter);

        return view;
    }

}