package mobile.wnext.sharemylocation.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


import mobile.wnext.sharemylocation.AppConstants;
import mobile.wnext.sharemylocation.R;

/**
 * Created by Nnguyen on 17/04/2015.
 */
public class LocationWidgetProvider extends AppWidgetProvider  {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager,int[] appWidgetIds) {
        Log.i(AppConstants.TAG, "Location widget provider updating...");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget);
        PendingIntent pendingIntentSendMessage = buildButtonPendingIntent(context,AppConstants.INTENT_ACTION_SEND_MESSAGE);
        remoteViews.setOnClickPendingIntent(R.id.btnShareLocation, pendingIntentSendMessage); // touch to the button will fire this pending intent

        PendingIntent pendingIntentRefreshLocation = buildButtonPendingIntent(context, AppConstants.INTENT_ACTION_REFRESH_LOCATION);
        remoteViews.setOnClickPendingIntent(R.id.btnRefresh, pendingIntentRefreshLocation); // touch to the button will fire this pending intent

        pushWidgetUpdate(context, remoteViews);
        Log.i(AppConstants.TAG, "Location widget provider updated!!");
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
