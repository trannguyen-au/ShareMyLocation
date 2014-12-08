package mobile.wnext.sharemylocation;

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Nnguyen on 5/12/2014.
 */
public class LocationMessage {
    public final String NEW_LINE = "\n";

    private boolean foundUserAddress;

    private Address address;
    private LatLng latLng;

    private boolean isIndoor;
    private int indoorLevel;
    private boolean isUnderground;
    private Resources mResource;

    public LocationMessage(Context context){
        mResource = context.getResources();
    }

    @Override
    public String toString() {
        return getLatLngDisplay() + NEW_LINE + NEW_LINE +
                getAddressLine()+ getExtraLocationMessage() + NEW_LINE + NEW_LINE +
                getMapLinkDisplay();
    }

    private String getExtraLocationMessage() {
        if(!isIndoor) return "";
        else {
            if(isUnderground) {
                return NEW_LINE + "Underground";
            }
            else {
                return NEW_LINE + "On level "+indoorLevel;
            }
        }
    }

    public String getAddressLine() {
        String result = "";
        for (int i=0;i<address.getMaxAddressLineIndex();i++) {
            result += address.getAddressLine(i)+", ";
        }
        result = result.substring(0,result.length()-2);
        return result;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public boolean isIndoor() {
        return isIndoor;
    }

    public void setIndoor(boolean isIndoor) {
        this.isIndoor = isIndoor;
    }

    public int getIndoorLevel() {
        return indoorLevel;
    }

    public void setIndoorLevel(int indoorLevel) {
        this.indoorLevel = indoorLevel;
    }

    public boolean isUnderground() {
        return isUnderground;
    }

    public void setUnderground(boolean isUnderground) {
        this.isUnderground = isUnderground;
    }

    public boolean isFoundUserAddress() {
        return foundUserAddress;
    }

    public void setFoundUserAddress(boolean foundUserAddress) {
        this.foundUserAddress = foundUserAddress;
    }

    public String getLatLngDisplay() {
        return String.format(mResource.getString(R.string.str_my_location_is),
                String.format("%s,%s", latLng.latitude, latLng.longitude));
    }

    public String getMapLinkDisplay() {
        return String.format("http://maps.google.com/maps?&z=16&q=%s+%s&ll=%s+%s",
                latLng.latitude, latLng.longitude,
                latLng.latitude, latLng.longitude);
    }


}
