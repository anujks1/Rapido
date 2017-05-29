package com.android.rapido;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.rapido.Utils.DirectionParser;
import com.android.rapido.model.TravelDetail;
import com.android.rapido.service.DirectionService;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by anuj on 27/05/17.
 */

public class MapActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    GoogleApiClient mGoogleApiClient;
    GoogleMap mGoogleMap;
    LatLng mPickupLocation;
    LatLng mFinalLocation;

    BitmapDescriptor mPickupLocationIcon;
    BitmapDescriptor mFinalLocationIcon;
    BitmapDescriptor mCurrentLocationIcon;

    DirectionParser mParser;

    Polyline mPreviousPolyLine;

    BottomSheetBehavior mBottomSheetBehavior;

    @BindView(R.id.travel_detail)
    RelativeLayout mTravelDetailView;

    @BindView(R.id.travel_distance)
    AppCompatTextView mDistanceView;

    @BindView(R.id.travel_duration)
    AppCompatTextView mDurationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        mParser = new DirectionParser();

        //get locations
        mPickupLocation = (LatLng) getIntent().getExtras().get(Constants.PICKUP_LOCATION);
        mFinalLocation = (LatLng) getIntent().getExtras().get(Constants.FINAL_LOCATION);

        //setup google api client
        setupGoogleApiClient();

        //setup markers and route
        setMarkers();

        //setup Map
        setupMap();

        //setupBottomSheet
        setBottomSheet();
    }

    private void setMarkers() {
        mFinalLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_destination1);
        mPickupLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_source1);
        mCurrentLocationIcon = BitmapDescriptorFactory.fromResource(R.drawable.marker_current1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(
                R.id.map_fragment));
        if (mapFragment != null) {
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mGoogleMap = googleMap;
                    markLocations();
                    getDirections();
                    getCurrentLocation();
                }
            });
        }
    }

    private void markLocations() {
        mGoogleMap.addMarker(new MarkerOptions().position(mPickupLocation).icon(mPickupLocationIcon));
        mGoogleMap.addMarker(new MarkerOptions().position(mFinalLocation).icon(mFinalLocationIcon));
        updateCamera();
    }

    private void updateCamera() {
        //Update Camera
        LatLngBounds latLngBounds = LatLngBounds.builder()
                .include(mPickupLocation)
                .include(mFinalLocation)
                .build();
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 64));
    }

    private void setupGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    private void getDirections() {

        DirectionService.Creator.getService().getDirection(mPickupLocation.latitude + "," + mPickupLocation.longitude, mFinalLocation.latitude + "," + mFinalLocation.longitude, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultObserver<JsonElement>() {
                    @Override
                    public void onNext(JsonElement value) {
                        JSONObject result = null;

                        try {
                            result = new JSONObject(value.toString());
                        } catch (Exception e) {
                        }

                        drawPaths(mParser.parse(result));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("Direction Fetch Error", e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void drawPaths(List<List<HashMap<String, String>>> result) {

        ArrayList points = null;
        PolylineOptions lineOptions = null;
        String distance = "";
        String duration = "";

        if (result.size() < 1) {
            Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < result.size(); i++) {
            points = new ArrayList();
            lineOptions = new PolylineOptions();
            List<HashMap<String, String>> path = result.get(i);
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                if (j == 0) {
                    distance = point.get("distance");
                    continue;
                } else if (j == 1) {
                    duration = point.get("duration");
                    continue;
                }

                LatLng position = new LatLng(Double.parseDouble(point.get("lat")), Double.parseDouble(point.get("lng")));
                points.add(position);
            }

            //set property of polyline
            lineOptions.addAll(points);
            lineOptions.width(6);
            lineOptions.color(Color.parseColor("#503F51B5"));
            lineOptions.clickable(true);

            //Store Travel Details
            TravelDetail travelDetail = new TravelDetail();
            travelDetail.setDistance(distance);
            travelDetail.setDuration(duration);

            //Draw polyline and tag object
            mGoogleMap.addPolyline(lineOptions).setTag(travelDetail);
        }

        mGoogleMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                polyline.setColor(Color.BLUE);

                //store this polyline
                if (mPreviousPolyLine != null && mPreviousPolyLine.getId() != polyline.getId()) {
                    mPreviousPolyLine.setColor(Color.parseColor("#503F51B5"));
                }
                mPreviousPolyLine = polyline;

                //show travel detail
                TravelDetail travelDetail = (TravelDetail) polyline.getTag();
                updateTravelDetail(travelDetail);
            }
        });
    }

    private void setBottomSheet() {
        mBottomSheetBehavior = BottomSheetBehavior.from(mTravelDetailView);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setPeekHeight(100);

    }

    private void updateTravelDetail(TravelDetail travelDetail) {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mDistanceView.setText(travelDetail.getDistance());
        mDurationView.setText(travelDetail.getDuration());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 69);
            return;
        }
            mGoogleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mGoogleMap.addMarker(new MarkerOptions().position(latLng).icon(mCurrentLocationIcon));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
