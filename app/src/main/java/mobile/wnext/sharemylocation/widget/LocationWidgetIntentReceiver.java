package mobile.wnext.sharemylocation.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import mobile.wnext.sharemylocation.AppConstants;
import mobile.wnext.sharemylocation.R;

/**
 * Created by Wery7 on 18/4/2015.
 */
public class LocationWidgetIntentReceiver extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient mLocationClient;
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(2000)         // 10 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    private void initLocationClient(Context context) {
        if(mLocationClient==null) {
            Log.i(AppConstants.TAG, "Build location client");
            mLocationClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mLocationClient.connect();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(AppConstants.INTENT_ACTION_SEND_MESSAGE)){
            doActionOnIntentReceive(context);
        }else if(intent.getAction().equals(AppConstants.INTENT_ACTION_REFRESH_LOCATION)) {
            refreshLocation(context);
        }
    }

    private void refreshLocation(Context context) {

    }

    private void doActionOnIntentReceive(Context context){
        Log.i(AppConstants.TAG,"Share button touched");

        // searching for the current location
        Log.i(AppConstants.TAG,"Searching the current location");

        // display a list of contact to select from
        Log.i(AppConstants.TAG,"Share button touched");

        // send the message
        Log.i(AppConstants.TAG,"Share button touched");

        // get the current location and update the text view
        initLocationClient(context);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(AppConstants.TAG, "onConnected "+bundle);
        if(mLocationClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mLocationClient,
                    REQUEST,
                    this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(AppConstants.TAG,"Connection is suspended in the widget");
    }

    RemoteViews remoteViews;

    @Override
    public void onLocationChanged(Location location) {
        Log.i(AppConstants.TAG,"Location change: "+location);
        if(remoteViews!=null && location!=null ) {
            remoteViews.setTextViewText(R.id.tvWidgetLat, "LAT: "+String.valueOf(location.getLatitude()));
            remoteViews.setTextViewText(R.id.tvWidgetLng, "LON: " + String.valueOf(location.getLongitude()));
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(AppConstants.TAG,"Connection is failed");
    }
}
