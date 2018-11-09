package com.medina.juanantonio.farey;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    /* For 1 second text change listener */

    long delay = 1000; // 1 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();

    // Containers

    RelativeLayout transitionsContainer;
    RelativeLayout input_con;
    ListView result_con;
    LinearLayout taxi_icon_con;

    // Inputs and Buttons

    EditText origin_input;
    EditText destination_input;
    Button compute_butt;
    ImageView whitebg;
    ImageButton get_location;

    // Result Container Status

    Boolean result_on = false;

    // ListView Items

    ArrayList<Map<String, String>> items;

    // For Listview Item Details

    String[] from = {"location_name", "location_address", "location_placeid"};
    int[] to = {R.id.loc_name, R.id.loc_add, R.id.loc_place_id};

    SimpleAdapter adapter;

    // Google Maps API

    String search_url = "https://maps.googleapis.com/maps/api/place/details/json";
    String autocomp_url = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
    String dist_url = "https://maps.googleapis.com/maps/api/distancematrix/json";
    String geocode_url = "https://maps.googleapis.com/maps/api/geocode/json";

    // Google Maps API Key

    String API_KEY = "AIzaSyApUoTRdW6dG7k-eoaBW9azQPJ_JGaLU8s";

    // For Random String

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    String SESSION_TOKEN;

    Boolean origin_chosen = true;

    String final_origin_name;
    String final_destination_name;

    String final_origin_placeid;
    String final_destination_placeid;

    // Map LatLng

    LatLng origin_latlng;
    LatLng destination_latlng;

    float origin_lat = 0.0f;
    float origin_long = 0.0f;

    float destination_lat;
    float destination_long;

    float user_loc_lat = 0.0f;
    float user_loc_long = 0.0f;

    String user_loc_name;
    String user_loc_placeid;

    // Map Markers

    Marker origin_marker;
    Marker destination_marker;

    // Map Bounds

    LatLngBounds.Builder builder;

    LocationManager locationManager;
    LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        transitionsContainer = (RelativeLayout) findViewById(R.id.transitions_container);
        input_con = (RelativeLayout) findViewById(R.id.input_con);
        result_con = (ListView) findViewById(R.id.result_con);
        whitebg = (ImageView) findViewById(R.id.whitebg);
        taxi_icon_con = (LinearLayout) findViewById(R.id.taxi_icon_con);

        origin_input = (EditText) findViewById(R.id.origin_input);
        destination_input = (EditText) findViewById(R.id.destination_input);
        compute_butt = (Button) findViewById(R.id.compute_butt);
        get_location = (ImageButton) findViewById(R.id.get_location);

        SESSION_TOKEN = randomAlphaNumeric(15);

        Log.d("HTTP", "SESSION_TOKEN: " + SESSION_TOKEN);

        items = new ArrayList<Map<String, String>>();

        builder = new LatLngBounds.Builder();


        /** --------------------- ORIGIN AND DESTINATION LISTENERS ---------------------------- */

        origin_input.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                openSearchResults();
            }

        });

        destination_input.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                openSearchResults();
            }

        });

        origin_input.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                openSearchResults();
                origin_chosen = true;
            }

        });


        destination_input.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                openSearchResults();
                origin_chosen = false;
            }

        });

        compute_butt.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.KITKAT)
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {

                if (origin_marker != null && destination_marker != null) {
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

                    builder.include(origin_marker.getPosition());
                    builder.include(destination_marker.getPosition());

                    LatLngBounds bounds = builder.build();

                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.animateCamera(cu);

                    TransitionManager.beginDelayedTransition(transitionsContainer);
                    taxi_icon_con.setVisibility(View.GONE);
                    compute_butt.setVisibility(View.GONE);

                    Log.d("HTTP", "Compute Button Clicked");

                    Log.d("HTTP", "Origin Name: " + final_origin_name);
                    Log.d("HTTP", "Origin Place ID: " + final_origin_placeid);
                    Log.d("HTTP", "Origin Latitude: " + origin_lat);
                    Log.d("HTTP", "Origin Longitude: " + origin_long);

                    Log.d("HTTP", "Destination Name: " + final_destination_name);
                    Log.d("HTTP", "Destination Place ID: " + final_destination_placeid);
                    Log.d("HTTP", "Destination Latitude: " + destination_lat);
                    Log.d("HTTP", "Destination Longitude: " + destination_long);

                    String query = "origins=place_id:" + final_origin_placeid + "&destinations=place_id:" +
                            final_destination_placeid + "&key=" + API_KEY;


                } else {
                    Log.d("HTTP", "Origin and Destination are Required.");
                }

            }
        });

        get_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("HTTP", "Setting User Location to Current Location");

                origin_lat = user_loc_lat;
                origin_long = user_loc_long;
                final_origin_name = user_loc_name;
                final_origin_placeid = user_loc_placeid;

                origin_latlng = new LatLng(origin_lat, origin_long);

                origin_marker = mMap.addMarker(new MarkerOptions().position(origin_latlng).title(final_origin_name));

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(origin_latlng)      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom\
                        .bearing(90)                // Sets the orientation of the camera to east
                        .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                origin_input.setText(final_origin_name);
            }
        });

        /** --------------------------------------------------------------------------------- */

        /** -------------------------- LISTVIEW ADAPTER SETTINGS ---------------------------- */



        /** ---------------------------------------------------------------------------------- */

        /** ---------------------- 1 SECOND TEXT CHANGE LISTENER ----------------------------- */

        /** For 1 second text change listener for ORIGIN INPUT */



        /** ------------------------------------------------------------------------------- */

        /** ---------------------- GET USERS CURRENT LOCATION ----------------------------- */


        /** -------------------------------------------------------------------------------- */
    }

    /**
     * ------------------ RUNNABLES FOR 1 SECOND TEXT CHANGE LISTENER ----------------------
     */





    /** ----------------------------------------------------------------------------------------------------- */

    /** ----------------------------------- OPEN AND CLOSE SEARCH RESULTS ----------------------------------- */

    /**
     * Call Function to open Search Results
     */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void openSearchResults() {
        TransitionManager.beginDelayedTransition(transitionsContainer);

        RelativeLayout.LayoutParams input_con_params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        input_con_params.setMargins(0, 0, 0, 0);
        input_con_params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        input_con.setBackgroundResource(R.drawable.white_bg);
        input_con.setPaddingRelative(30, 30, 30, 30);

        result_on = true; // Change variable value for next use
        compute_butt.setVisibility(View.GONE); // Hide Compute Button
        taxi_icon_con.setVisibility(View.GONE);
        whitebg.setVisibility(View.VISIBLE);

        ViewCompat.setElevation(input_con, 8); // Remove Elevation

        input_con.setLayoutParams(input_con_params);
    }

    /**
     * Call Function to close Search Results
     */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void closeSearchResults() {
        TransitionManager.beginDelayedTransition(transitionsContainer);

        closeKeyboard();

        RelativeLayout.LayoutParams input_con_params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        int margins = dpToPx(10);

        input_con_params.setMargins(margins, 0, margins, margins);
        input_con_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        input_con.setBackgroundResource(R.drawable.round_borders);

        result_on = false; // Change variable value for next use
        compute_butt.setVisibility(View.VISIBLE); // Show Compute Button
        taxi_icon_con.setVisibility(View.VISIBLE);
        whitebg.setVisibility(View.GONE);

        input_con.setLayoutParams(input_con_params);

        ViewCompat.setElevation(input_con, 8); // Remove Elevation
    }

    /** ---------------------------------------------------------------------------------------------- */

    /**
     * ---------------------------------- IMPLEMENT GOOGLE MAPS -------------------------------------
     */

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Log.d("HTTP", "onMapReady running...");
    }

    /** ---------------------------------------------------------------------------------------------- */

    /** ------------------------------------ HANDLE BACK BUTTON ---------------------------------------- */

    /**
     * Handle back button press to close search results.
     */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBackPressed() { // for API level 5 and greater
        if (result_on) closeSearchResults();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { // older than API 5
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (result_on) closeSearchResults();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /** -------------------------------------------------------------------------------------------------- */

    /**
     * ---------------------------------------- FUNCTION TOOLS ------------------------------------------
     */

    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }


    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private HashMap<String, String> putData(String loc, String address, String placeid) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("location_name", loc);
        item.put("location_address", address);
        item.put("location_placeid", placeid);
        return item;
    }
}

    /** ---------------------------------------------------------------------------------------------- */
