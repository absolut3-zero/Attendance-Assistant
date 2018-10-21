package xyz.z3ro.attendance.Utilities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;



public class Utils extends AppCompatActivity{
    public Context context;
    public Activity activity;
    public Utils(){}
    public Utils(Context context,Activity activity){
        this.context = context;
        this.activity = activity;
    }

    //Dialog to show on permission denied
    public void dialogOnPermissionDeny(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("Notice")
                .setMessage("Well! Seems you denied one or more permissions I requested for.\nThis app will not work as it is supposed to work.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //Dialog to show on permission denied when button is clicked
    public void dialogPermissionDeniedSubmit(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("Notice")
                .setMessage("Well! Seems you denied one or more permissions I requested for.\nThis app will not work as it is supposed to work.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        activity.finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        dialogInterface.dismiss();
                        activity.finish();
                    }
                })
                .show();
    }

    // Get Serial
    public String serialGet(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
                return Build.getSerial();
            }
            else{
                Log.d("serialGet","Failed to get Serial Number in API >=26");
                return null;
            }
        }
        return Build.SERIAL;
    }


}
