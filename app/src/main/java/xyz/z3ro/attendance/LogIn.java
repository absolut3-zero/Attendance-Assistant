package xyz.z3ro.attendance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import dmax.dialog.SpotsDialog;
import xyz.z3ro.attendance.Tasks.InternetCheckTask;
import xyz.z3ro.attendance.Utilities.PermissionUtil;
import xyz.z3ro.attendance.Utilities.Utils;

public class LogIn extends AppCompatActivity {
    private Context context_login;
    private SharedPreferences sharedPreferences;
    private DatabaseReference databaseReference;
    private Utils utils;
    private PermissionUtil permissionUtil;
    private android.app.AlertDialog progressDialog;

    private EditText rollNo;
    private AppCompatButton logIn;

    private String serial;
    private String rollNumber;

    private final String TAG = "LogIn";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }
        // Initialisation
        context_login = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, this.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        utils = new Utils(context_login, this);
        permissionUtil = new PermissionUtil(this, this);
        progressDialog = new SpotsDialog.Builder().setContext(context_login).setCancelable(false).setTheme(R.style.Custom).build();
        // Setting up views
        rollNo = findViewById(R.id.etRoll);
        logIn = findViewById(R.id.bttnLogIn);
        TextView login = findViewById(R.id.tvLogIn);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Organo.ttf");
        if (login != null)
            login.setTypeface(typeface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !permissionUtil.checkPermissions(Constants.LOCATION_REQUEST_CODE)
                && !permissionUtil.checkPermissions(Constants.PHONE_REQUEST_CODE)
                && !permissionUtil.checkPermissions(Constants.SMS_REQUEST_CODE)) {
            permissionUtil.permCheck(Constants.LOCATION_REQUEST_CODE);
        }
        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                boolean connected;
                try {
                    connected = new InternetCheckTask(context_login).execute().get();
                } catch (InterruptedException | ExecutionException e) {
                    connected = false;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !permissionUtil.checkPermissions(Constants.LOCATION_REQUEST_CODE)
                        && !permissionUtil.checkPermissions(Constants.PHONE_REQUEST_CODE)
                        && !permissionUtil.checkPermissions(Constants.SMS_REQUEST_CODE)) {
                    progressDialog.dismiss();
                    utils.dialogPermissionDeniedSubmit();
                } else {
                    if (connected) {
                        rollNumber = rollNo.getText().toString();
                        serial = utils.serialGet();
                        if (TextUtils.isEmpty(rollNumber)) {
                            progressDialog.dismiss();
                            Snackbar.make(findViewById(android.R.id.content), "All fields are mandatory", Snackbar.LENGTH_SHORT).show();
                        } else {
                            Login();
                        }
                    } else {
                        progressDialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.LOCATION_REQUEST_CODE:
                if (!permissionUtil.verifyPermissions(grantResults)) {
                    utils.dialogOnPermissionDeny();
                } else
                    permissionUtil.permCheck(Constants.PHONE_REQUEST_CODE);
                break;
            case Constants.PHONE_REQUEST_CODE:
                if (!permissionUtil.verifyPermissions(grantResults)) {
                    utils.dialogOnPermissionDeny();
                } else
                    permissionUtil.permCheck(Constants.SMS_REQUEST_CODE);
                break;
            case Constants.SMS_REQUEST_CODE:
                if (!permissionUtil.verifyPermissions(grantResults)) {
                    utils.dialogOnPermissionDeny();
                }
                break;
        }
    }

    private void Login() {
        Log.d(TAG, "Checking for serial stored");
        databaseReference.child("Students").child(rollNumber).child("serial").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String serialStored;
                if (dataSnapshot.exists()) {
                    try {
                        serialStored = dataSnapshot.getValue().toString();
                    } catch (NullPointerException e) {
                        serialStored = null;
                    }
                    Log.d(TAG, "Stored serial(database) :  " + serialStored);
                    if (serialStored.equals(serial)) {
                        Log.d(TAG, "Serial Matched");
                        sharedPreferences.edit().putBoolean(Constants.FIRST_RUN, false).apply();
                        sharedPreferences.edit().putString(Constants.SERIAL, serial).apply();
                        sharedPreferences.edit().putString(Constants.ROLL, rollNumber).apply();
                        Intent mainActivity = new Intent(context_login, MainActivity.class);
                        startActivity(mainActivity);
//                        Toast.makeText(context_login, "Log In Successful", Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                        finish();
                    } else {
                        Log.d(TAG, "Serial not matched");
                        Snackbar.make(findViewById(android.R.id.content), "Authentication Failed!", Snackbar.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        dialog4();
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Authentication Failed!", Snackbar.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    dialog4();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "Database Error");
                Snackbar.make(findViewById(android.R.id.content), "Error connecting to database! Please try again after some time.", Snackbar.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

    //Dialog to show on authentication failed
    private void dialog4() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context_login);
        builder.setTitle("Notice")
                .setMessage("Well! seems that the authentication failed. Make sure you are using your own device and entered correct Roll number.\n\nIf you believe there was some error, contact the administrator.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}