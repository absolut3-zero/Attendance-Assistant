package xyz.z3ro.attendance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import xyz.z3ro.attendance.Tasks.InternetCheckTask;
import xyz.z3ro.attendance.Utilities.Utils;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Context context_main;
    private SharedPreferences sharedPreferences;
    private Utils utils;
    private boolean permissionsGiven;

    // Defining Views
    private Button proximityCheckButton;
    private Button deviceCheckButton;
    private Button phoneCheckButton;
    private Button presentButton;
    private TextView proximityCheckResult;
    private TextView deviceCheckResult;
    private TextView phoneCheckResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Initialisation
        context_main = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, this.MODE_PRIVATE);
        utils = new Utils(context_main, this);
        sharedPrefInitialisation();

        // First Run
        boolean isFirstRun = sharedPreferences.getBoolean(Constants.FIRST_RUN, true);
        if (isFirstRun) {
            FirstRun(this);
            finish();
        }

        // Setting layout and Toolbar
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }

        // Setting up views
        proximityCheckButton = findViewById(R.id.button_proximity);
        deviceCheckButton = findViewById(R.id.button_device);
        phoneCheckButton = findViewById(R.id.button_phone);
        presentButton = findViewById(R.id.button_present);
        proximityCheckResult = findViewById(R.id.text_proximity_check);
        deviceCheckResult = findViewById(R.id.text_device_check);
        phoneCheckResult = findViewById(R.id.text_phone_check);
        proximityCheckButton.setOnClickListener(this);
        deviceCheckButton.setOnClickListener(this);
        phoneCheckButton.setOnClickListener(this);
        presentButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(true);
            wifiManager.setWifiEnabled(false);
        }
        //Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context_main, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_main, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_main, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_main, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
                permCheck();
            } else
                permissionsGiven = true;
        } else
            permissionsGiven = true;
//        TimeCheckTask timeCheckTask = new TimeCheckTask(context_main,presentButton,getString(R.string.time_start),getString(R.string.time_stop));
//        timeCheckTask.execute();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "Onpause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MainActivity", "OnStop");
    }

    @Override
    public void onClick(View view) {
        boolean connected;
        try {
            connected = new InternetCheckTask(this).execute().get();
        } catch (InterruptedException | ExecutionException e) {
            connected = false;
        }
        int id = view.getId();
        switch (id) {
            case R.id.button_proximity:
                //
                if (connected) {
                    boolean locationOn = isLocationEnabled();
                    if (locationOn) {
                        Intent proximityIntent = new Intent(this, xyz.z3ro.attendance.Checks.Proximity.class);
                        startActivityForResult(proximityIntent, 1);
                    } else {
                        dialogOnLocationFalse();
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.button_device:
                //
                if (connected) {
                    Intent deviceIntent = new Intent(this, xyz.z3ro.attendance.Checks.Device.class);
                    startActivityForResult(deviceIntent, 2);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.button_phone:
                //
                if (connected) {
                    Intent phoneIntent = new Intent(this, xyz.z3ro.attendance.Checks.Phone.class);
                    startActivityForResult(phoneIntent, 3);
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case R.id.button_present:
                //
                if (connected) {
                    presentOnClick();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    boolean proximityCheck = data.getBooleanExtra("resultProximity", false);
                    if (proximityCheck) {
                        proximityCheckResult.setText("Passed");
                        proximityCheckResult.setTextColor(ResourcesCompat.getColor(getResources(), R.color.green, null));
                        proximityCheckResult.setTypeface(proximityCheckResult.getTypeface(), Typeface.BOLD_ITALIC);
                    } else {
                        proximityCheckResult.setText("Failed");
                        proximityCheckResult.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
                        proximityCheckResult.setTypeface(proximityCheckResult.getTypeface(), Typeface.BOLD_ITALIC);
                    }
                }
                break;
            case 2:
                if (resultCode == RESULT_OK) {
                    boolean deviceCheck = data.getBooleanExtra("resultDevice", false);
                    if (deviceCheck) {
                        deviceCheckResult.setText("Passed");
                        deviceCheckResult.setTextColor(ResourcesCompat.getColor(getResources(), R.color.green, null));
                        deviceCheckResult.setTypeface(deviceCheckResult.getTypeface(), Typeface.BOLD_ITALIC);
                    } else {
                        deviceCheckResult.setText("Failed");
                        deviceCheckResult.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
                        deviceCheckResult.setTypeface(deviceCheckResult.getTypeface(), Typeface.BOLD_ITALIC);
                    }
                }
                break;
            case 3:
                if (resultCode == RESULT_OK) {
                    boolean phoneCheck = data.getBooleanExtra("resultPhone", false);
                    if (phoneCheck) {
                        phoneCheckResult.setText("Passed");
                        phoneCheckResult.setTextColor(ResourcesCompat.getColor(getResources(), R.color.green, null));
                        phoneCheckResult.setTypeface(phoneCheckResult.getTypeface(), Typeface.BOLD_ITALIC);
                    } else {
                        phoneCheckResult.setText("Failed");
                        phoneCheckResult.setTextColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
                        phoneCheckResult.setTypeface(phoneCheckResult.getTypeface(), Typeface.BOLD_ITALIC);
                    }
                }
                break;
        }
    }

    private void FirstRun(Context context) {
        Intent firstRun = new Intent(context, FirstRun.class);
        startActivity(firstRun);
    }

    // To check for required permissions
    public void permCheck() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION}, Constants.MY_REQUEST_CODE);
        }
    }

    //Permission request handler
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGiven = true;
                } else {
                    permissionsGiven = false;
                    utils.dialogOnPermissionDeny();
                }
        }
    }

    // Shared Preferences Initialisation
    private void sharedPrefInitialisation() {
        sharedPreferences.edit().putBoolean(Constants.PROXIMITY_CHECK, false).apply();
        sharedPreferences.edit().putBoolean(Constants.DEVICE_CHECK, false).apply();
        sharedPreferences.edit().putBoolean(Constants.PHONE_CHECK, false).apply();
    }

    private void presentOnClick() {
        boolean proximity_check, device_check, phone_check;
        proximity_check = sharedPreferences.getBoolean(Constants.PROXIMITY_CHECK, false);
        device_check = sharedPreferences.getBoolean(Constants.DEVICE_CHECK, false);
        phone_check = sharedPreferences.getBoolean(Constants.PHONE_CHECK, false);
        if (proximity_check && device_check && phone_check) {
            Intent presentIntent = new Intent(this, xyz.z3ro.attendance.Checks.Present.class);
            startActivity(presentIntent);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "All three conditions are not passed!", Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context_main.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
        return (gpsEnabled && networkEnabled);
    }

    private void dialogOnLocationFalse() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context_main);
        builder.setTitle("Notice")
                .setMessage("Location Services is Unavailable")
                .setPositiveButton("Open Location Settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        context_main.startActivity(myIntent);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        dialogInterface.dismiss();
                    }
                })
                .show();
    }

}
