package com.pia.brighttrip;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class SupportFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_support, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnPolice = view.findViewById(R.id.btn_police);
        Button btnFire = view.findViewById(R.id.btn_fire);
        Button btnWomen = view.findViewById(R.id.btn_women);
        Button btnMental = view.findViewById(R.id.btn_mental);

        btnPolice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callPolice();
            }
        });

        btnFire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callFire();
            }
        });

        btnWomen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callWomen();
            }
        });

        btnMental.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callMental();
            }
        });
    }

    public void callPolice() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:110"));
        startActivity(intent);
    }

    public void callFire() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:112"));
        startActivity(intent);
    }

    public void callWomen() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:030610063"));
        startActivity(intent);
    }

    public void callMental() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:0303906300"));
        startActivity(intent);
    }

}