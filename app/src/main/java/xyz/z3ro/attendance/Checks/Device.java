package xyz.z3ro.attendance.Checks;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import dmax.dialog.SpotsDialog;
import xyz.z3ro.attendance.Constants;
import xyz.z3ro.attendance.R;
import xyz.z3ro.attendance.Utilities.Utils;

public class Device extends AppCompatActivity {
    private Context context_device;
    private SharedPreferences sharedPreferences;
    private AlertDialog progressDialog;
    private DatabaseReference databaseReference;
    private Utils utils;
    private Intent returnIntent;

    // Data
    private String serial;
    private String imei;
    private String serialStoredSharedPreferences;
    private String imeiStoredDatabase;
    private boolean isSerialMatched;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_check);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }
        // Initializations
        context_device = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, this.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        progressDialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).setTheme(R.style.Custom).build();
        utils = new Utils(context_device, this);
        returnIntent = new Intent();
        serialStoredSharedPreferences = sharedPreferences.getString(Constants.SERIAL, null);
        serial = "";
        // progress dialog show
        progressDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isSerialMatched = false;
        String roll = sharedPreferences.getString(Constants.ROLL, null);
        if (ContextCompat.checkSelfPermission(context_device, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context_device, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context_device, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context_device, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            serial = utils.serialGet();
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
            // Serial check
            if (serial.equals(serialStoredSharedPreferences)) {
                isSerialMatched = true;
            } else {
                result(false);
            }
        } else {
            Toast.makeText(this, "App Permissions error", Toast.LENGTH_LONG).show();
            result(false);
        }
        databaseReference.child("Students").child(roll).child("zimei").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                imeiStoredDatabase = dataSnapshot.getValue().toString();
                if (imeiStoredDatabase.equals(imei) && isSerialMatched) {
                    result(true);
                } else {
                    result(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                result(false);
            }
        });
    }

    private void result(boolean pass) {
        if (pass) {
            sharedPreferences.edit().putBoolean(Constants.DEVICE_CHECK, true).apply();
            sharedPreferences.edit().putString(Constants.IMEI, imeiStoredDatabase).apply();
            returnIntent.putExtra("resultDevice", true);
            setResult(RESULT_OK, returnIntent);
            progressDialog.dismiss();
            finish();
        } else {
            sharedPreferences.edit().putBoolean(Constants.DEVICE_CHECK, false).apply();
//            sharedPreferences.edit().putString(Constants.IMEI,imeiStoredDatabase).apply();
            returnIntent.putExtra("resultDevice", false);
            setResult(RESULT_OK, returnIntent);
            progressDialog.dismiss();
            finish();
        }
    }
}
