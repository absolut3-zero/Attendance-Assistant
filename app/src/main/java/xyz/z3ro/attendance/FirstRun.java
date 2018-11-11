package xyz.z3ro.attendance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import xyz.z3ro.attendance.Tasks.InternetCheckTask;
import xyz.z3ro.attendance.Tasks.SerialCheckTask;
import xyz.z3ro.attendance.Utilities.PermissionUtil;
import xyz.z3ro.attendance.Utilities.Utils;

public class FirstRun extends AppCompatActivity implements View.OnClickListener {
    private Context context_firstrun;
    private SharedPreferences sharedPreferences;
    private Utils utils;

    private AppCompatButton signUp;
    private AppCompatButton logIn;
    private PermissionUtil permissionUtil;

    private final String TAG = "FirstRun";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_run);

        // Initialisation
        context_firstrun = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, this.MODE_PRIVATE);
        utils = new Utils(context_firstrun, this);
        //setting up views
        signUp = findViewById(R.id.bttnSignUP);
        logIn = findViewById(R.id.bttnLogIn);
        TextView attendance = findViewById(R.id.tvAttendance);
        TextView sheet = findViewById(R.id.tvSheet);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Organo.ttf");
        if (attendance != null && sheet != null) {
            attendance.setTypeface(typeface);
            sheet.setTypeface(typeface);
        }

        // Ask for permission
        permissionUtil = new PermissionUtil(this, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !permissionUtil.checkPermissions(Constants.LOCATION_REQUEST_CODE)
                && !permissionUtil.checkPermissions(Constants.PHONE_REQUEST_CODE)
                && !permissionUtil.checkPermissions(Constants.SMS_REQUEST_CODE)) {
            if (sharedPreferences.getBoolean(Constants.DIALOG, true)) {
                dialogPermissionsExplaination();
            } else {
                permissionUtil.permCheck(Constants.LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPreferences.edit().putBoolean(Constants.SERIAL_REGISTERED, true).apply();
        signUp.setOnClickListener(this);
        logIn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String serial;
        boolean connected;
        int id = view.getId();
        InternetCheckTask internetCheckTask = new InternetCheckTask(this);
        SerialCheckTask serialCheckTask = new SerialCheckTask(sharedPreferences);
        try {
            connected = internetCheckTask.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            connected = false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !permissionUtil.checkPermissions(Constants.LOCATION_REQUEST_CODE)
                && !permissionUtil.checkPermissions(Constants.PHONE_REQUEST_CODE)
                && !permissionUtil.checkPermissions(Constants.SMS_REQUEST_CODE)) {
            utils.dialogPermissionDeniedSubmit();
        } else {
            switch (id) {
                case R.id.bttnSignUP:
                    if (connected) {
                        serial = utils.serialGet();
                        serialCheckTask.execute(serial);
                        Intent signUp = new Intent(this, SignUp.class);
                        startActivity(signUp);
                        finish();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.bttnLogIn:
                    if (connected) {
                        serial = utils.serialGet();
                        serialCheckTask.execute(serial);
                        Intent logIn = new Intent(this, LogIn.class);
                        startActivity(logIn);
                        finish();
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!", Snackbar.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    //Permission request handler
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.LOCATION_REQUEST_CODE:
                if (permissionUtil.verifyPermissions(grantResults)) {
                    sharedPreferences.edit().putBoolean(Constants.DIALOG, false).apply();
                    permissionUtil.permCheck(Constants.PHONE_REQUEST_CODE);
                } else {
                    utils.dialogOnPermissionDeny();
                }
                break;
            case Constants.PHONE_REQUEST_CODE:
                if (permissionUtil.verifyPermissions(grantResults)) {
                    sharedPreferences.edit().putBoolean(Constants.DIALOG, false).apply();
                    permissionUtil.permCheck(Constants.SMS_REQUEST_CODE);
                } else {
                    utils.dialogOnPermissionDeny();
                }
                break;
            case Constants.SMS_REQUEST_CODE:
                if (permissionUtil.verifyPermissions(grantResults)) {
                    sharedPreferences.edit().putBoolean(Constants.DIALOG, false).apply();
                } else {
                    utils.dialogOnPermissionDeny();
                }
                break;
        }
    }

    // Dialog to give permissions explaination
    private void dialogPermissionsExplaination() {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context_firstrun);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.dismiss();
                permissionUtil.permCheck(Constants.LOCATION_REQUEST_CODE);
            }
        })
                .setTitle("Important Stuff")
                .setMessage("To maintain transparency and to show how generously awesome I am (as a developer), here is the list of permissions required by this app along with their explaination.\n\n" +
                        "1. Phone Permission - This permission is required to read your device's Serial Number.\n" +
                        "2. SMS Permission - This permission is required to read OTPs.\n" +
                        "3. Location Permission - Isn't it obvious? To read your location.\n\n" +
                        "So allow all permissions otherwise this app might not work as it is supposed to.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        permissionUtil.permCheck(Constants.LOCATION_REQUEST_CODE);
                    }
                })
                .show();
    }
}
