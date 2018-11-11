package xyz.z3ro.attendance.Utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import xyz.z3ro.attendance.Constants;

public class PermissionUtil {

    private Context mContext;
    private Activity mActivity;

    private static final String TAG = "PermissionUtil";

    public PermissionUtil(Context context, Activity activity) {
        this.mContext = context;
        this.mActivity = activity;
    }

    public boolean checkPermissions(int requestCode) {
        switch (requestCode) {
            case Constants.LOCATION_REQUEST_CODE:
                Log.d(TAG, "Permission Util CheckPermissions LOCATION REQUEST");
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED
                        || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                    return false;
                } else
                    return true;
            case Constants.PHONE_REQUEST_CODE:
                Log.d(TAG, "Permission Util CheckPermissions PHONE REQUEST");
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED)
                    return false;
                else
                    return true;
            case Constants.SMS_REQUEST_CODE:
                Log.d(TAG, "Permission Util CheckPermissions SMS REQUEST");
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                        ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED)
                    return false;
                else
                    return true;
        }
        return false;
    }

    public void reqPermissions(int requestCode) {
        switch (requestCode) {
            case Constants.LOCATION_REQUEST_CODE:
                String[] locationPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(mActivity, locationPermissions, Constants.LOCATION_REQUEST_CODE);
                break;
            case Constants.PHONE_REQUEST_CODE:
                String[] phonePermission = new String[]{Manifest.permission.READ_PHONE_STATE};
                ActivityCompat.requestPermissions(mActivity, phonePermission, Constants.PHONE_REQUEST_CODE);
                break;
            case Constants.SMS_REQUEST_CODE:
                String[] smsPermissions = new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS};
                ActivityCompat.requestPermissions(mActivity, smsPermissions, Constants.SMS_REQUEST_CODE);
        }
    }

    public void permCheck(int reqCode) {
        switch (reqCode) {
            case Constants.LOCATION_REQUEST_CODE:
                if (!checkPermissions(Constants.LOCATION_REQUEST_CODE))
                    reqPermissions(Constants.LOCATION_REQUEST_CODE);
                break;
            case Constants.PHONE_REQUEST_CODE:
                if (!checkPermissions(Constants.PHONE_REQUEST_CODE))
                    reqPermissions(Constants.PHONE_REQUEST_CODE);
                break;
            case Constants.SMS_REQUEST_CODE:
                if (!checkPermissions(Constants.SMS_REQUEST_CODE))
                    reqPermissions(Constants.SMS_REQUEST_CODE);
                break;
        }
    }

    public boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1)
            return false;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
}
