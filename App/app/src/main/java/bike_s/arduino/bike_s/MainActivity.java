package bike_s.arduino.bike_s;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Session session;
    private GoogleMap mMap;
    private ProgressDialog pDialog;
    private Handler customHandler = new Handler();
    private TextView timerValue;
    private ImageButton startTimer;
    //lock vars
    private ImageButton saveCode;

    //vars for timer
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private long startTime = 0L;
    private boolean timerRunning = false;
    // URL to get contacts JSON
    private static String url = "https://api.citybik.es/v2/networks/bike_s-srm-szczecin";
    private ArrayList<HashMap<String, String>> stationList;
    private String TAG = MainActivity.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1252;
    private static final int SCAN_STATION_DELAY_15MIN = 900000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(this);
        session = new Session(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);

        if (!session.loggedIn()) {
            logout();
        }
        handleLock(header);
        handleTimer();
        scanStations();

        /*
         * Fragment mapy w MainActivity
         * Obtain the SupportMapFragment and get notified when the map is ready to be used.
         */
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        navigationView.setNavigationItemSelectedListener(this);
        TextView userNameTextView = (TextView) header.findViewById(R.id.userName);
        userNameTextView.setText(session.getUserName());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_stations: {
                Intent intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.btnLogout: {
                logout();
                break;
            }
            default:
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setCurrentLocation();
            } else {
                Log.e("Permission: ", "Permission not granted focus on default place");
                setDefaultLocation();
            }
        }
    }

    private void handleTimer() {
        timerValue = (TextView) findViewById(R.id.timerValue);
        startTimer = (ImageButton) findViewById(R.id.timerStart);
        timerValue.setVisibility(View.INVISIBLE);

        startTimer.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if (timerRunning == false) {
                    startTime = SystemClock.uptimeMillis();
                    customHandler.postDelayed(updateTimerThread, 0);
                    timerValue.setVisibility(View.VISIBLE);
                    timerRunning = true;
                } else {
                    timerValue.setVisibility(View.INVISIBLE);
                    timerRunning = false;
                }
            }
        });
    }

    private void handleLock(View header) {
        final TextView codeRemind = (TextView) header.findViewById(R.id.codeRemind);
        saveCode = (ImageButton) findViewById(R.id.saveCode);

        if (session.getLockCombination() != 0) {
            codeRemind.setText("Twój obecny kod: " + session.getLockCombination());
        } else {
            codeRemind.setText("Brak ustawionego kodu zamka.");
        }
        saveCode.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                final Context context = getApplicationContext();
                final int duration = Toast.LENGTH_SHORT;
                View view2 = (LayoutInflater.from(MainActivity.this)).inflate(R.layout.code_input, null);

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                alertBuilder.setView(view2);
                final EditText userInput = (EditText) view2.findViewById(R.id.userInput);
                TextView currentCode = (TextView) view2.findViewById(R.id.currentCode);
                if (session.getLockCombination() == 0) {
                    currentCode.setText("Brak ustawionego kodu zamka.");
                } else {
                    currentCode.setText("Twój obecny kod: " + session.getLockCombination());
                }

                alertBuilder.setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String aa = userInput.getText().toString();
                                Toast toast;
                                String text;
                                if (!aa.isEmpty()) {
                                    session.saveLockCombination(Integer.valueOf(aa));
                                    text = "Twój kod został ustawiony: " + session.getLockCombination();
                                    toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                } else {
                                    if (session.getLockCombination() == 0) {
                                        text = "Pole było puste. Brak ustawionego kodu zamka.";
                                    } else {
                                        text = "Pole było puste. Twój kod to: " + session.getLockCombination();
                                    }
                                    toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
                                codeRemind.setText("Twój obecny kod: " + session.getLockCombination());
                            }
                        });
                Dialog dialog = alertBuilder.create();
                dialog.show();
                userInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });
    }

    public void scanStations() {
        stationList = new ArrayList<>();
        new GetStations().execute();

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new GetStations().execute();
                handler.postDelayed(this, SCAN_STATION_DELAY_15MIN);
            }
        }, SCAN_STATION_DELAY_15MIN);
    }

    //Funkcja opowiedzialna za nadanie punktu na mapie
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else { // no need to ask for permission
            setCurrentLocation();
        }
    }

    private void setDefaultLocation() {
        LatLng zut = new LatLng(53.4475413, 14.4919891);
        mMap.addMarker(new MarkerOptions().position(zut).title("Marker ZUTu"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zut, 18));
    }

    @SuppressLint("MissingPermission")
    private void setCurrentLocation() {
        mMap.setMyLocationEnabled(true);
        /*
         * Button może być przesunięty w inne miejsce w layoucie
         * wykorzystac przez:
         * View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
         */
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        Location location = task.getResult();
                        LatLng currentLatLng = new LatLng(location.getLatitude(),
                                location.getLongitude());
                        CameraUpdate update = CameraUpdateFactory
                                .newLatLngZoom(currentLatLng, 18);
                        mMap.moveCamera(update);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void logout() {
        session.setLoggedIn(false, null);
        finish();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }

    private class GetStations extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading stations...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            JsonParser sh = new JsonParser();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    // Getting JSON Array node
                    JSONObject network = jsonObj.getJSONObject("network");
                    JSONArray stations = network.getJSONArray("stations");

                    // looping through All Stations
                    for (int i = 0; i < stations.length(); i++) {
                        JSONObject c = stations.getJSONObject(i);
                        getData(c);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server.",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
            return null;
        }

        protected void getData(JSONObject c) throws JSONException {
            String emptySlots = c.getString("empty_slots");
            String freeBikes = c.getString("free_bikes");
            String lat = c.getString("latitude");
            String lng = c.getString("longitude");
            String name = c.getString("name");

            //JSONObject extra = c.getJSONObject("extra");
            // String number = extra.getString("number");
            //String slots = extra.getString("slots");
            //String uid = extra.getString("uid");

            // tmp hash map for single station
            HashMap<String, String> station = new HashMap<>();

            // adding each child node to HashMap key => value
            //station.put("uid", uid);
            station.put("name", name);
            station.put("emptySlots", emptySlots);
            station.put("freeBikes", freeBikes);
            station.put("lat", lat);
            station.put("lng", lng);
            //station.put("number", number);
            //station.put("slots", slots);

            stationList.add(station);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            for (HashMap<String, String> map : stationList) {
                double lat = Double.parseDouble(map.get("lat"));
                double lng = Double.parseDouble(map.get("lng"));
                int freeBikes = Integer.parseInt(map.get("freeBikes"));
                int emptySlots = Integer.parseInt(map.get("emptySlots"));
                String name = map.get("name");

                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lng))
                        .title(name)
                        .snippet("Free bikes: " + freeBikes + " Empty slots: " + emptySlots));
            }
        }
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            if (mins >= 20) {
                timerValue.setTextColor(0xFFFF0000);
            } else if (mins >= 15) {
                timerValue.setTextColor(0xFFE37718);
            } else {
                timerValue.setTextColor(0xFF000000);
            }
            timerValue.setText("" + mins + ":"
                    + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
        }
    };
}