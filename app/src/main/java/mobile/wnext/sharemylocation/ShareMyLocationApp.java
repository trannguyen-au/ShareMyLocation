package mobile.wnext.sharemylocation;

import android.app.Application;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by Nnguyen on 12/12/2014.
 */
public class ShareMyLocationApp extends Application {

    public boolean isPlayServiceAvailable() {
        //return false;
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return status != ConnectionResult.SUCCESS;
    }
}
