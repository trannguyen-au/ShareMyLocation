package mobile.wnext.sharemylocation;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ShareActionProvider;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.LocationListener;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnIndoorStateChangeListener,
        LocationListener {

    public static final String TAG = "ShareMyLocation";
    public static final String PREF_SELECTED_INTENT = "SELECTED_INTENT";
    public static final int REQUEST_CODE_SETTING=0;
    public static final int REQUEST_CODE_ADS=0;

    public static final int MAX_TRY_GET_ADDRESS_COUNT = 15;
    private int currentTryCount;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ShareActionProvider mShareActionProvider;
    private LocationClient mLocationClient;
    private SharedPreferences sharedPreferences;
    private SettingsActivity.AppSettings appSettings;
    Location mFoundLocation;
    Marker mCurrentLocationMarker;

    // the location message object which hold the logic to generate the final message
    LocationMessage mMessage;

    // Acquire a reference to the system Location Manager
    LocationManager mlocationManager;

    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(10000)         // 10 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


    private boolean initializedLocation = false;

    /**
     * This value will be set when the user selected a location by touch on the map.
     */
    private LatLng userSelectedLocation = null;

    // this value will be used when the map is setup to reduce the time of loading world map.
    private LatLng lastKnownLatLng = null;

    // display ads before exit
    private InterstitialAd interstitial;

    TextView lblPreviewText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreated");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        currentTryCount = 0;
        lblPreviewText = (TextView) findViewById(R.id.lblPreviewText);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        appSettings = new SettingsActivity.AppSettings(this);
        mMessage = new LocationMessage(this, (TextView) findViewById(R.id.tvLocation), appSettings);

        mLocationClient = new LocationClient(this, this, this);

        findLastKnownLocation();
        setUpMapIfNeeded();

        initializeActionBar();

        loadAdRequest();
    }

    private void loadAdRequest() {
        // Create the interstitial.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getString(R.string.ad_unit_id));
        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                exitApp();
            }
        });

        // Create ad request.
        AdRequest adRequest = new AdRequest.Builder()
                .setLocation(parseLocation(lastKnownLatLng))
                .build();

        // Begin loading your interstitial.
        interstitial.loadAd(adRequest);
    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public boolean displayInterstitial() {
        if (interstitial.isLoaded()) {
            interstitial.show();
            return true;
        } else {
            return false;
        }
    }

    private void exitApp() {
        finish();
    }

    private void initializeActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        SpinnerAdapter adapter = ArrayAdapter.createFromResource(this,
                R.array.map_type_list, android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int index, long l) {
                if(mMap==null) {
                    Toast.makeText(MapsActivity.this,
                            getResources().getString(R.string.str_wait_for_ready_map),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if(index==0) {  // map
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    lblPreviewText.setTextColor(getResources().getColor(R.color.light_blue_text));
                    return true;
                }
                else if(index == 1) { // earth
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    lblPreviewText.setTextColor(getResources().getColor(R.color.white_text));
                    return true;
                }
                return false;
            }
        });
    }

    private void findLastKnownLocation() {
        mlocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(mlocationManager!=null) {
            Location lastKnownLocation = mlocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null)
                Log.i(TAG, "Last known location from GPS provider: " + lastKnownLocation);

            if (lastKnownLocation == null) {
                lastKnownLocation = mlocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Log.i(TAG, "Last known location from Network: " + lastKnownLocation);
            }
            if (lastKnownLocation == null) {
                lastKnownLocation = mlocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                Log.i(TAG, "Last known location from Network: " + lastKnownLocation);
            }

            if (lastKnownLocation != null) {
                lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            } else {
                Log.i(TAG, "Not found Last known location");
            }
        }
    }

    private void dispatchSettingIntent() {
        Intent settingIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivityForResult(settingIntent,REQUEST_CODE_SETTING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.i(TAG, "Activity return from code: "+requestCode);
        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE_SETTING) {
            // rebind the text message
            mMessage.bindData();
        }
    }

    @Override
    public void onBackPressed() {
        if(!displayInterstitial()){
            super.onBackPressed();
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

    @Override
    protected  void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected  void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        setUpMapIfNeeded();
        Log.i(TAG, appSettings.toString());
    }

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

    private void setUpMap() {
        Log.i(TAG, "setupMap");

        // map settings
        mMap.setMyLocationEnabled(true);
        mMap.setIndoorEnabled(true);
        mMap.setBuildingsEnabled(true);
        mMap.setTrafficEnabled(false);

        // map events
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapClickListener(this);
        if(lastKnownLatLng!=null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 16);
            mMap.moveCamera(cameraUpdate);
        }
        mMap.setOnIndoorStateChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.mn_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
                // save the intent for later use
                // TODO: write code to use this saved intent
                SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_SELECTED_INTENT,intent.toUri(Intent.URI_INTENT_SCHEME));
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case R.id.mn_clipboard:
                // copy the message to the clipboard
                copyMessageToClipboard();
                return true;
            case R.id.mn_setting:
                // start the setting intent
                dispatchSettingIntent();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void copyMessageToClipboard() {
        ClipboardHelper ch = new ClipboardHelper(this);
        ch.copyToClipboard(mMessage.toString());
        Toast.makeText(this,
                getString(R.string.str_message_copied_to_clipboard),
                Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected "+bundle);
        mLocationClient.requestLocationUpdates(
                REQUEST,
                this);  // LocationListener
    }

    // Location information come from the device
    @Override
    public void onLocationChanged(Location location) {
        if(mMessage==null || mLocationClient==null || mShareActionProvider==null || appSettings==null)
            return; // wait until the screen is ready

        Log.i(TAG,"Try Num# "+currentTryCount+++":Device location is changed to: "+location);
        if(currentTryCount > MAX_TRY_GET_ADDRESS_COUNT || mMessage.isFoundUserAddress()) {
            // only try a few times to get the user address then stop
            mLocationClient.disconnect();
        }

        // update message
        if(userSelectedLocation == null &&
                (mFoundLocation==null ||
                        mFoundLocation.getAccuracy() > location.getAccuracy())) {
            // first time found a location or the new location has improved accuracy
            mFoundLocation = location;

            mMessage.setLatLng(parseLatLng(mFoundLocation));
            // update share intent message
            mShareActionProvider.setShareIntent(mMessage.getShareIntent());
            setMarkerLocation(mMessage.getLatLng());

            // only search address for location with high accuracy
            if(appSettings.enableAddress()) {
                (new GetAddressTask(this)).execute(location);
            }
        }
    }

    private void setMarkerLocation(LatLng latLng) {
        // add a marker if it does not exist
        if (mCurrentLocationMarker == null && mMap != null) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            mCurrentLocationMarker = mMap.addMarker(options);
        }
        mCurrentLocationMarker.setPosition(latLng);

        // move camera to the current device location only once
        if(initializedLocation==false) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            mMap.animateCamera(cameraUpdate);
            mMessage.setFoundUserAddress(true);
            initializedLocation = true;
        }
    }

    private void setAddressInformation(Address address) {
        Log.i(TAG, "Address is changed to "+address+".\n User selected location: "+userSelectedLocation);
        mMessage.setAddress(address);

        // update share intent message
        mShareActionProvider.setShareIntent(mMessage.getShareIntent());
    }

    private void setLatLngInformation(LatLng latLng) {
        mMessage.setLatLng(latLng);

        // update share intent message
        mShareActionProvider.setShareIntent(mMessage.getShareIntent());
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
        mFoundLocation = null;

        // reset the count
        currentTryCount = 0;

        final MapsActivity theThis = this;

        mMessage.setAddress(null);

        if(mlocationManager!=null) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            mlocationManager.requestSingleUpdate(
                    mlocationManager.getBestProvider(criteria, true),
                    new android.location.LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            theThis.onLocationChanged(location);
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
                    },
                    Looper.myLooper());
        }
        return false;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.i(TAG,"User click to latlng: "+latLng);
        userSelectedLocation = latLng;
        mMessage.setLatLng(latLng);
        mMessage.setAddress(null);

        // update shared intent message for latlng update
        mShareActionProvider.setShareIntent(mMessage.getShareIntent());

        setMarkerLocation(userSelectedLocation);

        // move camera to the current location marker
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(userSelectedLocation);
        mMap.animateCamera(cameraUpdate);

        // request revert geocode to find address
        if(appSettings.enableAddress()) {
            (new GetAddressTask(this)).execute(parseLocation(latLng));
        }
    }

    @Override
    public void onIndoorBuildingFocused() {
        Log.i(TAG, "onIndoorBuildingFocused");
        mMessage.setIndoorBuilding(mMap.getFocusedBuilding());
    }

    @Override
    public void onIndoorLevelActivated(IndoorBuilding indoorBuilding) {
        mMessage.setIndoorBuilding(indoorBuilding);
    }

    private final AndroidHttpClient ANDROID_HTTP_CLIENT = AndroidHttpClient.newInstance(GeocoderHelper.class.getName());

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

        private Address fetchCityNameUsingGoogleMap(Location location)
        {
            String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + location.getLatitude() + ","
                    + location.getLongitude() + "&sensor=false&language="+Locale.getDefault().getISO3Language();
            try
            {
                Address address = new Address(Locale.getDefault());
                JSONObject googleMapResponse = new JSONObject(ANDROID_HTTP_CLIENT.execute(new HttpGet(googleMapUrl),
                        new BasicResponseHandler()));

                // many nested loops.. not great -> use expression instead
                // loop among all results
                JSONArray results = (JSONArray) googleMapResponse.get("results");
                if(results.length()>0) {
                    // loop among all addresses within this result
                    JSONObject result = results.getJSONObject(0);
                    if (result.has("formatted_address"))
                    {
                        String formattedAddress = result.getString("formatted_address");

                        JSONArray addressComponents = result.getJSONArray("address_components");
                        // loop among all address component to find a 'locality' or 'sublocality'
                        for (int j = 0; j < addressComponents.length(); j++)
                        {
                            JSONObject addressComponent = addressComponents.getJSONObject(j);
                            if (result.has("types"))
                            {
                                JSONArray types = addressComponent.getJSONArray("types");

                                // search for locality and sublocality
                                String cityName = null;

                                StringBuilder addressLine = new StringBuilder();
                                for (int k = 0; k < types.length(); k++)
                                {
                                    if ("street_number".equals(types.getString(k)) && cityName == null)
                                    {
                                        if (addressComponent.has("long_name"))
                                        {
                                            cityName = addressComponent.getString("long_name");
                                        }
                                        else if (addressComponent.has("short_name"))
                                        {
                                            cityName = addressComponent.getString("short_name");
                                        }
                                    }
                                    if ("locality".equals(types.getString(k)) && cityName == null)
                                    {
                                        if (addressComponent.has("long_name"))
                                        {
                                            cityName = addressComponent.getString("long_name");
                                        }
                                        else if (addressComponent.has("short_name"))
                                        {
                                            cityName = addressComponent.getString("short_name");
                                        }
                                    }
                                    if ("sublocality".equals(types.getString(k)))
                                    {
                                        if (addressComponent.has("long_name"))
                                        {
                                            cityName = addressComponent.getString("long_name");
                                        }
                                        else if (addressComponent.has("short_name"))
                                        {
                                            cityName = addressComponent.getString("short_name");
                                        }
                                    }
                                }
                                if (cityName != null)
                                {
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
                ignored.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            if(addresses!=null && addresses.size()>0) {
                setAddressInformation(addresses.get(0));
            }
            /*else if(userSelectedLocation!=null) {
                Toast.makeText(mContext,
                        "Cannot get address information for the selected location",
                        Toast.LENGTH_SHORT)
                        .show();
            }*/
        }
    }

    private Location parseLocation(final LatLng latLng) {
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        return location;
    }

    private LatLng parseLatLng(final Address address) {
        return new LatLng(address.getLatitude(), address.getLongitude());
    }

    private LatLng parseLatLng(final Location location) {
        return new LatLng(location.getLatitude(),location.getLongitude());
    }

}
