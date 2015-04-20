package mobile.wnext.sharemylocation.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import mobile.wnext.sharemylocation.AppConstants;
import mobile.wnext.sharemylocation.LocationMessage;
import mobile.wnext.sharemylocation.MapsActivity;
import mobile.wnext.sharemylocation.R;
import mobile.wnext.sharemylocation.SettingsActivity;

/**
 * Created by Wery7 on 18/4/2015.
 */
public class LocationWidgetIntentReceiver extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static int currentStatus = 0;
    public static final int SEARCHING = 1;
    public static final int STANDBY = 0;

    private Context mContext;
    private int mMaxSampling = 5; // number of location samples
    private int mCurrentSampleIndex = 0;
    private static Location mLastKnownLocation;

    private GoogleApiClient mLocationClient;
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(100)         // 10 each seconds
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

            // start request for location update
            mLocationClient.connect();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

        if(currentStatus == STANDBY) {
            if (intent.getAction().equals(AppConstants.INTENT_ACTION_SEND_MESSAGE)) {
                sendMessageAction(context);
            } else if (intent.getAction().equals(AppConstants.INTENT_ACTION_REFRESH_LOCATION)) {
                currentStatus = SEARCHING;
                refreshLocation(context);
                updateWidgetView();
            }
        }
        else {
            if (intent.getAction().equals(AppConstants.INTENT_ACTION_STOP_REFRESHING)) {
                stopListeningToLocationUpdate();
                currentStatus = STANDBY;
                updateWidgetView();
            }
            else if(intent.getAction().equals(AppConstants.INTENT_ACTION_SEND_MESSAGE)) {
                Toast.makeText(context, "Currently detecting location.. Please try again after the location is set", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshLocation(Context context) {
        Log.i(AppConstants.TAG, "Refresh location touched");
        mCurrentSampleIndex = 0;
        mLastKnownLocation = null;
        initLocationClient(context);
        Toast.makeText(context, context.getString(R.string.detecting_your_location), Toast.LENGTH_SHORT).show();
    }

    private void sendMessageAction(Context context){
        Log.i(AppConstants.TAG,"Share button touched");
        if(mLastKnownLocation != null) {
            // prepare the intent with message
            SettingsActivity.AppSettings appSetting = new SettingsActivity.AppSettings(context);

            // prepare message
            LocationMessage locationMessage = new LocationMessage(context, appSetting);
            locationMessage.setLatLng(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
            if(appSetting.enableAddress()) {
                // try to get address using google geocode
                //locationMessage.setAddress(new Address(Locale.getDefault()));
            }

            // send the message
            Intent shareIntent = locationMessage.getShareIntent();
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String title = context.getString(R.string.app_name);
            Intent chooser = Intent.createChooser(shareIntent, title);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Log.i(AppConstants.TAG, "Current setting: "+appSetting.toString());
            if(shareIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooser);
            }

            Log.i(AppConstants.TAG, "Share button touched");
        }
        else {
            Toast.makeText(context, context.getString(R.string.refresh_the_location_first),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(AppConstants.TAG, "onConnected " + bundle);
        if(mLocationClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mLocationClient,
                    REQUEST,
                    this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(AppConstants.TAG, "Connection is suspended in the widget");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(AppConstants.TAG, "Location change in widget: " + location);
        Log.i(AppConstants.TAG, "Widget status: " + currentStatus);
        if(currentStatus == SEARCHING) {
            // sampling the location to get the most accurate location within a number of sample
            if (mLastKnownLocation == null) {
                mLastKnownLocation = location;
                //Log.i(AppConstants.TAG, "Last location not found, location is updated to "+mLastKnownLocation);
            }
            else if (mLastKnownLocation.hasAccuracy() &&
                    location.hasAccuracy() &&
                    mLastKnownLocation.getAccuracy() > location.getAccuracy()) {
               //Log.i(AppConstants.TAG, "Location is updated. Old Acc: "+mLastKnownLocation.getAccuracy()+" newAcc:"+ location.getAccuracy());
                mLastKnownLocation = location;
            }

            updateWidgetView();
            //mCurrentSampleIndex++;
        }

        else if(currentStatus == STANDBY) {
            if(mContext!=null) {
                updateWidgetView();
            }
            else {
                Log.e(AppConstants.TAG, "context is not persisted");
            }
            Log.e(AppConstants.TAG, "Stop listening to location update");
            stopListeningToLocationUpdate();
        }
    }

    private void stopListeningToLocationUpdate() {
        if(mLocationClient!=null) {
            mLocationClient.disconnect();
        }
        mLocationClient = null;
        mCurrentSampleIndex = 0;
    }

    private void updateWidgetView() {
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.appwidget);

        if (mLastKnownLocation != null) {
            remoteViews.setTextViewText(R.id.tvWidgetAccuracy, mContext.getString(R.string.accuracy_) + String.valueOf(mLastKnownLocation.getAccuracy()));
            remoteViews.setTextViewText(R.id.tvWidgetLat, mContext.getString(R.string.lat_) + String.valueOf(mLastKnownLocation.getLatitude()));
            remoteViews.setTextViewText(R.id.tvWidgetLng, mContext.getString(R.string.lon_) + String.valueOf(mLastKnownLocation.getLongitude()));
        }

        if(currentStatus == STANDBY) {
            PendingIntent pendingIntentSendMessage = buildButtonPendingIntent(mContext,AppConstants.INTENT_ACTION_SEND_MESSAGE);
            remoteViews.setOnClickPendingIntent(R.id.btnShareLocation, pendingIntentSendMessage); // touch to the button will fire this pending intent

            PendingIntent pendingIntentRefreshLocation = buildButtonPendingIntent(mContext, AppConstants.INTENT_ACTION_REFRESH_LOCATION);
            remoteViews.setOnClickPendingIntent(R.id.btnRefresh, pendingIntentRefreshLocation); // touch to the button will fire this pending intent

            remoteViews.setViewVisibility(R.id.btnShareLocation, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.btnRefresh, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.btnStopRefreshing, View.GONE);
        }
        else if(currentStatus== SEARCHING) {

            PendingIntent pendingIntentRefreshLocation = buildButtonPendingIntent(mContext, AppConstants.INTENT_ACTION_STOP_REFRESHING);
            remoteViews.setOnClickPendingIntent(R.id.btnStopRefreshing, pendingIntentRefreshLocation); // touch to the button will fire this pending intent

            remoteViews.setViewVisibility(R.id.btnShareLocation, View.INVISIBLE);
            remoteViews.setViewVisibility(R.id.btnRefresh, View.GONE);
            remoteViews.setViewVisibility(R.id.btnStopRefreshing, View.VISIBLE);
        }
        pushWidgetUpdate(mContext, remoteViews);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(AppConstants.TAG,"Connection is failed");
    }

    public static PendingIntent buildButtonPendingIntent(Context context, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void pushWidgetUpdate(Context context, RemoteViews remoteViews) {
        ComponentName myWidget = new ComponentName(context, LocationWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(myWidget, remoteViews);
    }
}
