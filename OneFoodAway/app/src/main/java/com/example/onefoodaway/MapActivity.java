package com.example.onefoodaway;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private final String GOOGLE_PLACES_API_KEY = "AIzaSyAH008n41rXGsO2oYtJgZduebNYwN127_I";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        else {
            map.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location != null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.getLatitude(), location.getLongitude()))
                        .zoom(12)
                        .bearing(270)
                        .tilt(20)
                        .build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            String foodUrl = createFoodUrl(location.getLatitude(), location.getLongitude());
            new FoodCallBackTask().execute(foodUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Write Logic
                } else {
                    // STUB
                }
                return;
            }
        }
    }

    private String createFoodUrl(double lat, double lng) {
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        stringBuilder.append("location=").append(lat).append(",").append(lng);
        stringBuilder.append("&radius=").append(5000);
        stringBuilder.append("&types=").append("cafe|meal_delivery|meal_takeaway|restaurant");
        stringBuilder.append("&sensor=true");
        stringBuilder.append("&key=").append(GOOGLE_PLACES_API_KEY);
        return stringBuilder.toString();
    }

    private class FoodCallBackTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection
                        .getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                // Show Error Message
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String name = null;
            double lat, lng;
            try {
                JSONObject data = new JSONObject(result);
                JSONArray jsonArray = data.getJSONArray("results");
                if (data.getString("status").equalsIgnoreCase("OK")) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject place = jsonArray.getJSONObject(i);
                        if (!place.isNull("name")) {
                            name = place.getString("name");
                        }
                        lat = place.getJSONObject("geometry").getJSONObject("location")
                                .getDouble("lat");
                        lng = place.getJSONObject("geometry").getJSONObject("location")
                                .getDouble("lng");
                        map.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(name));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                // Show Error Message
            }
        }
    }
}
