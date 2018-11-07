package xyz.z3ro.attendance.Checks;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import dmax.dialog.SpotsDialog;
import xyz.z3ro.attendance.Constants;
import xyz.z3ro.attendance.R;

public class Proximity extends AppCompatActivity {
    private Context context_proximity;
    private SharedPreferences sharedPreferences;
    private AlertDialog progressDialog;
    private DatabaseReference databaseReference;
    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private Intent returnIntent;

    private String bssidStored;
    private String networkSSID;
    private String networkPass;
    private boolean isSSIDAvailable;
    private BroadcastReceiver scanReceiver;
    private BroadcastReceiver connectReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proximity_check);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }
        // Initializations
        context_proximity = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, this.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        returnIntent = new Intent();
        progressDialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).setTheme(R.style.Custom).build();
        isSSIDAvailable = false;

        wifiManager.setWifiEnabled(true);

        //Scan receiver initialisation
        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int i;
                List<ScanResult> scanResults = wifiManager.getScanResults();
                int size = scanResults.size();
                for (i = 0; i < size; i++) {
                    if (scanResults.get(i).SSID.equals(networkSSID))
                        isSSIDAvailable = true;
                }
                if (isSSIDAvailable) {
//                    Toast.makeText(context,"SSID available",Toast.LENGTH_LONG).show();
                    connect();
                } else {
                    // TODO when ssid not available
                    Log.d("XXXXX", "FAILED AT SSID RECEIVER");
                    Log.d("XXXX", networkSSID + " " + networkPass + " " + bssidStored);
                    result(false);
                    Finish();
                }
            }
        };
        // Connect Receiver Initialisation
        connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (NetworkInfo.State.CONNECTED.equals(networkInfo.getState())) {
                        wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo.getBSSID().equals(bssidStored)) {
//                            Toast.makeText(context,"Yeah!!!",Toast.LENGTH_LONG).show();
                            // TODO SUCCESS
                            result(true);
                            Finish();
                        } else {
//                            Toast.makeText(context,"Noooo!!",Toast.LENGTH_LONG).show();
                            // TODO BSSID FAILURE
                            Log.d("XXXXX", "FAILED AT CONNECT RECEIVER");
                            result(false);
                            Finish();
                        }
                    }
                }
            }
        };

        // register receiver for scan
        context_proximity.registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // Show progress dialog and enable wifi
        wifiManager.setWifiEnabled(true);
        progressDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CountDownTimer countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                result(false);
                finish();
            }
        };
        countDownTimer.start();
        if (ContextCompat.checkSelfPermission(context_proximity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            getSSID();
        else {
            Toast.makeText(context_proximity, "App permissions error", Toast.LENGTH_LONG).show();
            result(false);
            finish();
        }
        context_proximity.registerReceiver(connectReceiver, new IntentFilter((WifiManager.NETWORK_STATE_CHANGED_ACTION)));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(scanReceiver);
        unregisterReceiver(connectReceiver);
        super.onDestroy();
    }


    private void getSSID() {
        databaseReference.child("Proximity").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                networkSSID = dataSnapshot.child("ssid").getValue().toString();
                networkPass = dataSnapshot.child("pass").getValue().toString();
                bssidStored = dataSnapshot.child("bssid").getValue().toString();
//                Toast.makeText(context_proximity,networkSSID+" "+bssidStored,Toast.LENGTH_SHORT).show();
                wifiManager.startScan();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // TODO 1
                Log.d("XXXXX", "FAILED TO GET SSID FROM FIREBASE");
                result(false);
                Finish();
            }
        });
    }

    private void connect() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + networkSSID + "\"";
        wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
        wifiManager.addNetwork(wifiConfiguration);
//        Toast.makeText(context_proximity,"Attempting to connect",Toast.LENGTH_SHORT).show();
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
//                Toast.makeText(context_proximity,"Attempting to connect successs",Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private void result(boolean pass) {
        if (pass) {
            sharedPreferences.edit().putBoolean(Constants.PROXIMITY_CHECK, true).apply();
            returnIntent.putExtra("resultProximity", true);
            setResult(RESULT_OK, returnIntent);
            Log.d("result", "success");
            wifiManager.setWifiEnabled(false);
            progressDialog.dismiss();
        } else {
            sharedPreferences.edit().putBoolean(Constants.PROXIMITY_CHECK, false).apply();
            returnIntent.putExtra("resultProximity", false);
            setResult(RESULT_OK, returnIntent);
            Log.d("result", "failure");
            wifiManager.setWifiEnabled(false);
            progressDialog.dismiss();
        }
    }

    public void Finish() {
        super.finish();
    }
}
