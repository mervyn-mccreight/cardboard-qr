package de.fhwedel.google.cardboardprojekt;

import android.os.Bundle;

import com.google.vrtoolkit.cardboard.CardboardActivity;


public class MainActivity extends CardboardActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
