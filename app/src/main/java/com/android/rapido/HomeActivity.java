package com.android.rapido;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class HomeActivity extends AppCompatActivity{

    LatLng mPickupLocation;
    LatLng mFinalLocation;

    boolean isDestinationSelected;
    boolean isSourceSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        //Pickup Location
        setupPickupLocation();

        //Destination Location
        setupDestinationLocation();

        //Permission
        getPermission();
    }

    private void setupPickupLocation() {

        PlaceAutocompleteFragment placeAutocompleteFragment=(PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.pick_up_autocomplete_fragment);

        placeAutocompleteFragment.setHint("Enter Pickup Location");

        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPickupLocation=place.getLatLng();
                isSourceSelected=true;
            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    private void setupDestinationLocation() {
        PlaceAutocompleteFragment placeAutocompleteFragment=(PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.destination_autocomplete_fragment);

        placeAutocompleteFragment.setHint("Enter Destination");

        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mFinalLocation=place.getLatLng();
                isDestinationSelected=true;
            }

            @Override
            public void onError(Status status) {

            }
        });
    }

    public void getPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 69);
            return;
        }
    }

    @OnClick(R.id.route_btn)
    public void onViewRouteClicked(){
        if(isDestinationSelected && isSourceSelected ) {
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra(Constants.PICKUP_LOCATION, mPickupLocation);
            mapIntent.putExtra(Constants.FINAL_LOCATION, mFinalLocation);
            startActivity(mapIntent);
        }else{
            Toast.makeText(this,"Please select both the Locations",Toast.LENGTH_LONG).show();
        }
    }
}
