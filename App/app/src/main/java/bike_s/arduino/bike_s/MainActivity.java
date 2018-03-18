package bike_s.arduino.bike_s;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Session session;
    private TextView timerValue;
    private ImageButton startTimer;
    private ImageButton navigateButton;
    private ImageButton saveCode;
    private Handler customHandler = new Handler();
    private ArrayList<Station> stationList;
    private ProgressDialog pDialog;
    private Polyline routePolyLine;

    private String TAG = MainActivity.class.getSimpleName();
    private static String url = "https://api.citybik.es/v2/networks/bike_s-srm-szczecin";
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private long startTime = 0L;
    private boolean timerRunning = false;

    private LatLng lastSeen;
    private LatLng lastMarker;

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
        navigationView.setNavigationItemSelectedListener(this);
        View header = navigationView.getHeaderView(0);
        TextView userNameTextView = (TextView) header.findViewById(R.id.userName);
        userNameTextView.setText(session.getUserName());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (!session.loggedIn()) {
            logout();
        }

        scanStations();
        saveLockCode(header);
        startTimer();
        handleNavigationButton();
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
        /*
         * Handle action bar item clicks here. The action bar will
         * automatically handle clicks on the Home/Up button, so long
         * as you specify a parent activity in AndroidManifest.xml.
         */
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
                final View view2 = (LayoutInflater.from(MainActivity.this)).inflate(R.layout.stations, null);
                final ArrayAdapter<Station> stationsAdapter;
                ArrayList<Station> stationsInfo = new ArrayList<>();
                final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                ListView stationsListView = view2.findViewById(R.id.stationsList);
                EditText stationSearch = view2.findViewById(R.id.stationSearch);

                alertBuilder.setView(view2);
                alertBuilder.setCancelable(true)
                        .setPositiveButton("Zamknij", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                final Dialog dialog = alertBuilder.create();
                dialog.show();

                for (Station station : stationList) {
                   stationsInfo.add(station);
                }
                stationsAdapter = new ArrayAdapter<Station>(this, R.layout.station_row, stationsInfo);
                stationsListView.setAdapter(stationsAdapter);
                stationSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        stationsAdapter.getFilter().filter(charSequence);
                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                stationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Station s = (Station)adapterView.getAdapter().getItem(i);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(s.getLatitude(), s.getLongitude()), 18));
                        dialog.dismiss();
                    }
                });
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

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

    public void saveLockCode(View header) {
        saveCode = (ImageButton) findViewById(R.id.saveCode);
        final TextView codeRemind = (TextView) header.findViewById(R.id.codeRemind);

        if (session.GetLockCombination() != 0) {
            codeRemind.setText("Twój obecny kod: " + session.GetLockCombination());
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
                if (session.GetLockCombination() == 0) {
                    currentCode.setText("Brak ustawionego kodu zamka.");
                } else {
                    currentCode.setText("Twój obecny kod: " + session.GetLockCombination());
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
                                    text = "Twój kod został ustawiony: " + session.GetLockCombination();
                                    toast = Toast.makeText(context, text, duration);
                                    toast.show();

                                } else {
                                    if (session.GetLockCombination() == 0) {
                                        text = "Pole było puste. Brak ustawionego kodu zamka.";
                                    } else {
                                        text = "Pole było puste. Twój kod to: " + session.GetLockCombination();
                                    }
                                    toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }

                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
                                codeRemind.setText("Twój obecny kod: " + session.GetLockCombination());

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

    public void startTimer() {
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

    public void handleNavigationButton() {
        navigateButton = (ImageButton) findViewById(R.id.navigateButton);

        navigateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (routePolyLine != null) {
                    routePolyLine.remove();
                }
                final Context context = getApplicationContext();
                final int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, "Wyznaczono trasę...", duration);
                toast.show();
                LatLng origin = lastSeen;
                LatLng dest = lastMarker;
                // Getting URL to the Google Directions API
                String url = getDirectionsUrl(origin, dest);
                DownloadTask downloadTask = new DownloadTask();
                // Start downloading json data from Google Directions API
                downloadTask.execute(url);
                navigateButton.setVisibility(View.INVISIBLE);
            }
        });
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
                        lastSeen = currentLatLng;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));
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
            String name = c.getString("name");
            Integer emptySlots = c.getInt("empty_slots");
            Integer freeBikes = c.getInt("free_bikes");
            Double lat = c.getDouble("latitude");
            Double lng = c.getDouble("longitude");
            Station st = new Station(name, lat, lng, emptySlots, freeBikes);

            stationList.add(st);
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
            for (Station station : stationList) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(station.getLatitude(), station.getLongitude()))
                        .title(station.getName())
                        .snippet("Wolne rowery: " + station.getFreeBikes() +
                                " Wolne miejsca: " + station.getEmptySlots()));
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        lastMarker = marker.getPosition();
                        navigateButton.setVisibility(View.VISIBLE);
                        marker.showInfoWindow();
                        return true;
                    }
                });
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

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);
        }
    }


    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points;
            PolylineOptions lineOptions = new PolylineOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);
            }
            // Drawing polyline in the Google Map for the i-th route
            routePolyLine = mMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=bicycling";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}

