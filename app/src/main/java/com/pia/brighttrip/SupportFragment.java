package com.pia.brighttrip;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

        // List of emergency contacts
        ArrayList<String> contactNames = new ArrayList<>();
        contactNames.add("Police: 110");
        contactNames.add("Women's Helpline: (030) 610063");
        contactNames.add("Mental Health Helpline: (030) 3906300");

        // Setting up an ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.list_item_calls,
                contactNames
        );

        listView.setAdapter(adapter);

        // Setting up an item click listener to make the call when the number is clicked
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedContact = contactNames.get(position);
            //get the phone numnber
            String phoneNumber = selectedContact.split(":")[1].trim();

            // Create an intent to dial the phone number
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber)); // Set the phone number for the intent

            try {
                startActivity(callIntent); // Start the dialer activity
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to make the call", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

}