package bike_s.arduino.bike_s;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private String TAG = MainActivity.class.getSimpleName();
    private Session session;
    GoogleMap mMap;
    // URL to get contacts JSON
    private static String url = "https://api.citybik.es/v2/networks/bike_s-srm-szczecin";
    ArrayList<HashMap<String, String>> stationList;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new Session(this);
        if (!session.loggedIn()) {
            logout();
        }

        /*
         * Fragment mapy w MainActivity
         * Obtain the SupportMapFragment and get notified when the map is ready to be used.
         */
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        /*
         *Aby wstawić cokolwiek do bocznego menu trzeba wykorzystać poniższy header w celu
         *wykorzystania findViewById
        */
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(this);


        TextView userNameTextView = (TextView) header.findViewById(R.id.userName);
        userNameTextView.setText(session.getUserName());

        stationList = new ArrayList<>();
        new GetStations().execute();
        /*
         * =============JSON z API
         * Jak na ten moment Fatal Error na API :|
         * Oryginalny link:
         * https://api.citybik.es/v2/networks/bike_s-srm-szczecin
         * testuje na przykłądowym api
         */
       /* RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.androidhive.info/contacts/";
        StringRequest stringRequest = new StringRequest( Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObj = new JSONObject(response);
                            JSONArray contacts = jsonObj.getJSONArray("contacts");

                            for (int i = 0; i < contacts.length(); i++) {
                                JSONObject c = contacts.getJSONObject( i );
                                String email = c.getString( "email" );
                                Log.e(TAG, "Pole email["+i+"]: " + email);//odpowiedź z API pola email
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Context context = getApplicationContext();
                CharSequence text = "Błąd pobierania danych!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
        //=============KONIEC JSONA z API
        */
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
                Intent intent = new Intent(this, MapsActivity.class);
                //EditText editText = (EditText) findViewById(R.id.editText);
                //String message = editText.getText().toString();
                //intent.putExtra(EXTRA_MESSAGE, message);
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


    //Funkcja opowiedzialna za nadanie punktu na mapie
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng zut = new LatLng(53.4475413, 14.4919891);

        mMap.addMarker(new MarkerOptions().position(zut).title("Marker ZUTu"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zut, 18));
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

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
}

