package mobile.wnext.sharemylocation;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMapClickListener,
        LocationListener {

    public static final String TAG = "ShareMyLocation";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ShareActionProvider mShareActionProvider;
    private LocationClient mLocationClient;
    private Resources mResource;
    Marker mCurrentLocationMarker;

    LocationMessage mMessage;

    // Acquire a reference to the system Location Manager
    LocationManager mlocationManager;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(10000)         // 10 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    Button btnSetting;
    TextView tvAddress, tvNearBy, tvGmLink, tvLatlng;

    private boolean initializedLocation = false;
    private LatLng userSelectedLocation = null;
    private LatLng lastKnownLatLng = null;

    private GetAddressTask mGetAddressTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreated");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mResource = getResources();

        tvAddress = (TextView) findViewById(R.id.tvAddress);
        tvNearBy = (TextView) findViewById(R.id.tvLocationNearBy);
        tvGmLink = (TextView) findViewById(R.id.tvGmapLink);
        tvLatlng = (TextView) findViewById(R.id.tvLocation);
        btnSetting = (Button) findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchSettingIntent();
            }
        });

        mGetAddressTask = new GetAddressTask(this);

        mLocationClient = new LocationClient(this, this, this);
        findLastKnownLocation();
        setUpMapIfNeeded();
    }

    private void findLastKnownLocation() {
        mlocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = mlocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(lastKnownLocation!=null )Log.i(TAG, "Last known location from GPS provider: "+lastKnownLocation);

        if(lastKnownLocation==null) {
            lastKnownLocation = mlocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Log.i(TAG, "Last known location from Network: "+lastKnownLocation);
        }
        if(lastKnownLocation == null) {
            lastKnownLocation = mlocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            Log.i(TAG, "Last known location from Network: "+lastKnownLocation);
        }

        if(lastKnownLocation != null) {
            lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }
        else {
            Log.i(TAG, "Not found Last known location");
        }
    }

    private void dispatchSettingIntent() {
        Intent settingIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settingIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        // Connect the client.
        if(mLocationClient!=null)
            mLocationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        // Disconnecting the client invalidates it.
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
        super.onStop();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Log.i(TAG, "setupMap");
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapClickListener(this);
        if(lastKnownLatLng!=null) {
            MarkerOptions options = new MarkerOptions()
                    .position(lastKnownLatLng)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            mCurrentLocationMarker = mMap.addMarker(options);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 16);
            mMap.moveCamera(cameraUpdate);
        }
        Log.i(TAG, "" + mMap.getMyLocation());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.mn_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        return true;
    }

    private Intent createShareIntent() {
        String message = getMessage();
        //Log.i(TAG, "Create share intent message: " + message);
        Intent messageIntent = new Intent(Intent.ACTION_SEND);
        messageIntent.setType("text/plain");
        messageIntent.putExtra(Intent.EXTRA_TEXT, message);
        return messageIntent;
    }

    private String getMessage() {
        String message = "";
        String newLine = "\n";
        message += tvLatlng.getText().toString()+newLine+newLine;
        message += tvAddress.getText().toString()+newLine+newLine;
        if(tvNearBy.getText().toString()!=null && !tvNearBy.getText().toString().equals("")) {
            message += tvNearBy.getText().toString();
        }
        message += tvGmLink.getText().toString();
        return message;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected "+bundle);
        mLocationClient.requestLocationUpdates(
                REQUEST,
                this);  // LocationListener

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG,"Device location is changed to: "+location);
        // update message
        if(userSelectedLocation==null) {
            mGetAddressTask.execute(location);
        }
    }

    private void changeMarkerLocationAndUpdateShareMessage(Address address) {
        Log.i(TAG, "Address is changed to "+address+". User selected location: "+userSelectedLocation);
        LatLng latLng = parseLatLng(address);

        if (mCurrentLocationMarker == null && mMap != null) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            mCurrentLocationMarker = mMap.addMarker(options);
        }

        String addressText = getAddressLine(address);

        tvAddress.setText(addressText);
        if(address.getPremises()!=null) {
            tvNearBy.setText(String.format("%s", address.getPremises()));
            tvNearBy.setVisibility(View.VISIBLE);
        }
        else {
            tvNearBy.setText("");
            tvNearBy.setVisibility(View.GONE);
        }

        if(userSelectedLocation==null) {
            mCurrentLocationMarker.setPosition(latLng);

            setLatLngInformation(latLng);

            // move camera to the current location marker
            if(initializedLocation==false) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
                mMap.animateCamera(cameraUpdate);
                initializedLocation = true;
            }
        }
        else {
            mCurrentLocationMarker.setPosition(userSelectedLocation);

            setLatLngInformation(userSelectedLocation);
            // move camera to the current location marker
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(userSelectedLocation);
            mMap.animateCamera(cameraUpdate);
        }

        // update share intent message
        mShareActionProvider.setShareIntent(createShareIntent());
    }

    private void setLatLngInformation(LatLng latLng)     {
        tvLatlng.setText(String.format(mResource.getString(R.string.str_my_location_is),
                String.format("%s,%s", latLng.latitude, latLng.longitude)));
        tvGmLink.setText(String.format("http://maps.google.com/maps?&z=16&q=%s+%s&ll=%s+%s",
                        latLng.latitude, latLng.longitude,
                        latLng.latitude, latLng.longitude));

    }

    @Override
    public void onDisconnected() {
        Log.i(TAG,"onDisconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG,"onConnectionFailed" + connectionResult.toString());
    }

    @Override
    public boolean onMyLocationButtonClick() {
        userSelectedLocation = null;
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.i(TAG,"User click to latlng: "+latLng);
        userSelectedLocation = latLng;
        mGetAddressTask.execute(parseLocation(latLng));
    }

    private class GetAddressTask extends AsyncTask<Location, Void, List<Address>> {
        Context mContext;
        Resources resources;
        public GetAddressTask(Context context) {
            super();
            mContext = context;
            resources = context.getResources();
        }

        @Override
        protected List<Address> doInBackground(Location... locations) {
            Geocoder geocoder =
                    new Geocoder(mContext, Locale.getDefault());
            // Get the current location from the input parameter list
            Location loc = locations[0];
            // Create a list to contain the result address
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
            } catch (IOException e1) {
                Log.e(TAG,
                        "IO Exception in getFromLocation()");
                e1.printStackTrace();
                return null;
            } catch (IllegalArgumentException e2) {
                // Error message to post in the log
                String errorString = "Illegal arguments " +
                        Double.toString(loc.getLatitude()) +
                        " , " +
                        Double.toString(loc.getLongitude()) +
                        " passed to address service";
                Log.e(TAG, errorString);
                e2.printStackTrace();
                return null;
            }

            return addresses;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            if(addresses!=null && addresses.size()>0) {
                changeMarkerLocationAndUpdateShareMessage(addresses.get(0));
            }
            else if(userSelectedLocation!=null) {
                Toast.makeText(mContext,
                        "Cannot get address information for the selected location",
                        Toast.LENGTH_SHORT)
                        .show();
            }

        }
    }

    private Location parseLocation(LatLng latLng) {
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        return location;
    }

    private LatLng parseLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    private LatLng parseLatLng(Address address) {
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

    private String getAddressLine(Address address) {
        String result = "";
        for (int i=0;i<address.getMaxAddressLineIndex();i++) {
            result += address.getAddressLine(i)+", ";
        }
        result = result.substring(0,result.length()-2);
        return result;
    }
}
