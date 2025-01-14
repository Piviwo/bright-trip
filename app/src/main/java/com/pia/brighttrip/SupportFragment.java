package com.pia.brighttrip;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.content.Intent;
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

        //todo: add intents
        // List of emergency contacts and call numbers
        final ArrayList<String[]> contactDetails = new ArrayList<>();
        contactDetails.add(new String[]{"Police", "110"});
        contactDetails.add(new String[]{"Women's Helpline", "(030)610063"});
        contactDetails.add(new String[]{"Mental Health Helpline", "(030)3906300"});

        // Setting up an ArrayAdapter
        ArrayAdapter<String[]> adapter = new ArrayAdapter<String[]>(requireContext(), android.R.layout.simple_list_item_1, contactDetails) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                String[] contact = contactDetails.get(position);
                // displaying the contact and phone number in the list item
                ((TextView) view.findViewById(android.R.id.text1)).setText(contact[0] + ": " + contact[1]);
                return view;
            }
        };

        listView.setAdapter(adapter);

        //setting up OnItemClickListener
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            String[] contact = contactDetails.get(position);
            String phoneNumber = contact[1];

            //intent to call the number
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + phoneNumber));

            //dial activity
            try {
                startActivity(dialIntent); // Start the dial activity
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to open dialer.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

}