package mobile.wnext.sharemylocation;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Address;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by Nnguyen on 5/12/2014.
 */
public class LocationMessage {
    public final String NEW_LINE = "\n";

    private boolean foundUserAddress;

    private Address address;
    private LatLng latLng;

    private IndoorBuilding indoorBuilding;
    private Resources mResource;

    private SettingsActivity.AppSettings mAppSettings;

    private TextView mView;
    Intent messageIntent;

    public LocationMessage(Context context, TextView view, SettingsActivity.AppSettings appSettings){
        mResource = context.getResources();
        mAppSettings = appSettings;
        mView = view;

        messageIntent = new Intent(Intent.ACTION_SEND);
        messageIntent.setType("text/plain");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if(mAppSettings.enableCustomMessage()
                && mAppSettings.textCustomMessage()!=null
                && mAppSettings.textCustomMessage().length()>0) {
            result.append(mAppSettings.textCustomMessage() + NEW_LINE );
        }

        if(mAppSettings.enableLatLng() && getLatLngDisplay().length()>0) {
            result.append(getLatLngDisplay() + NEW_LINE + NEW_LINE );
        }

        if(mAppSettings.enableExtraAddress() && indoorBuilding!=null){
            String extraLocationMessage = getExtraLocationMessage();
            if(extraLocationMessage.length()>0) {
                result.append(extraLocationMessage + NEW_LINE);
            }
        }

        if(mAppSettings.enableAddress()
                && getAddressLine().length()>0) {
            result.append(getAddressLine() + NEW_LINE + NEW_LINE);
        }

        if(mAppSettings.enableMapLink()&& getMapLinkDisplay().length()>0) {
            result.append(getMapLinkDisplay() + NEW_LINE + NEW_LINE);
        }

        if(mAppSettings.enableAppInfo()) {
            result.append(mResource.getString(R.string.app_info_message)+ NEW_LINE + NEW_LINE);
        }

        result.delete(result.length()-2,result.length());

        return result.toString();
    }

    private String getExtraLocationMessage() {
        if(indoorBuilding==null) return "";
        else {
            if(indoorBuilding.isUnderground()) {
                return "Underground";
            }
            else {
                return indoorBuilding.getLevels().get(indoorBuilding.getActiveLevelIndex()).getName().trim().replace("\n"," ");
            }
        }
    }

    public String getAddressLine() {
        if(address!=null) {
            String result = "";
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                result += address.getAddressLine(i) + ", ";
            }
            result = result.substring(0, result.length() - 2);
            return result;
        }
        return "";
    }

    public void setAddress(Address address) {
        this.address = address;
        bindData();
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
        bindData();
    }

    public boolean isFoundUserAddress() {
        return foundUserAddress;
    }

    public void setFoundUserAddress(boolean foundUserAddress) {
        this.foundUserAddress = foundUserAddress;
    }

    public String getLatLngDisplay() {
        if(latLng==null) return "";
        return String.format(DOUBLE_FORMAT+","+DOUBLE_FORMAT,
                        decimalFormat.format(latLng.latitude),
                        decimalFormat.format(latLng.longitude));
    }

    public String getMapLinkDisplay() {
        if(latLng==null) return "";
        return String.format("http://maps.google.com/maps?&z=16&q="+DOUBLE_FORMAT+"+"+DOUBLE_FORMAT+
                        "&ll="+DOUBLE_FORMAT+"+"+DOUBLE_FORMAT,
                decimalFormat.format(latLng.latitude), decimalFormat.format(latLng.longitude),
                decimalFormat.format(latLng.latitude), decimalFormat.format(latLng.longitude));
    }

    private DecimalFormat decimalFormat = new DecimalFormat("###.######",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private String DOUBLE_FORMAT = "%s";

    public Intent getShareIntent() {
        if(messageIntent.hasExtra(Intent.EXTRA_TEXT)) {
            messageIntent.removeExtra(Intent.EXTRA_TEXT);
        }
        messageIntent.putExtra(Intent.EXTRA_TEXT, toString());
        return messageIntent;
    }

    public void bindData() {
        if(mView!=null) {
            String result = toString();
            mView.setText(result);
        }
    }

    public void setIndoorBuilding(IndoorBuilding indoorBuilding) {
        this.indoorBuilding = indoorBuilding;
        bindData();
    }
}
