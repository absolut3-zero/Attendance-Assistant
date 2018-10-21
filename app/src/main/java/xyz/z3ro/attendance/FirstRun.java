package xyz.z3ro.attendance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;
import android.widget.TextView;



import java.util.concurrent.ExecutionException;

import xyz.z3ro.attendance.Tasks.InternetCheckTask;
import xyz.z3ro.attendance.Tasks.SerialCheckTask;
import xyz.z3ro.attendance.Utilities.Utils;

public class FirstRun extends AppCompatActivity implements View.OnClickListener{
    private Context context_firstrun;
    private SharedPreferences sharedPreferences;
    private Utils utils;

    private AppCompatButton signUp;
    private AppCompatButton logIn;

    private final String TAG = "FirstRun";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_run);

        // Initialisation
        context_firstrun = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME,this.MODE_PRIVATE);
        utils = new Utils(context_firstrun,this);
        //setting up views
        signUp = findViewById(R.id.bttnSignUP);
        logIn = findViewById(R.id.bttnLogIn);
        TextView attendance = findViewById(R.id.tvAttendance);
        TextView sheet = findViewById(R.id.tvSheet);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/Organo.ttf");
        if(attendance != null && sheet != null){
            attendance.setTypeface(typeface);
            sheet.setTypeface(typeface);
        }

        // Assk for permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                boolean isDialog = sharedPreferences.getBoolean(Constants.DIALOG,true);
                if(isDialog){
                    dialogPermissionsExplaination();
                }
                else {
                    permCheck();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPreferences.edit().putBoolean(Constants.SERIAL_REGISTERED,true).apply();
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
        try{
            connected = internetCheckTask.execute().get();
        }
        catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
            connected = false;
        }
        if(ContextCompat.checkSelfPermission(context_firstrun, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(context_firstrun, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(context_firstrun, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(context_firstrun, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            utils.dialogPermissionDeniedSubmit();
        }
        else {
            switch (id){
                case R.id.bttnSignUP:
                    if(connected){
                        serial = utils.serialGet();
                        serialCheckTask.execute(serial);
                        Intent signUp = new Intent(this,SignUp.class);
                        startActivity(signUp);
                        finish();
                    }
                    else{
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!",Snackbar.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.bttnLogIn:
                    if(connected){
                        serial = utils.serialGet();
                        serialCheckTask.execute(serial);
                        Intent logIn = new Intent(this,LogIn.class);
                        startActivity(logIn);
                        finish();
                    }
                    else{
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!",Snackbar.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }

    // To check for required permissions
    private void permCheck(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION},Constants.MY_REQUEST_CODE);
        }
    }

    //Permission request handler
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Constants.MY_REQUEST_CODE:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&grantResults[1]==PackageManager.PERMISSION_GRANTED&&grantResults[2]==PackageManager.PERMISSION_GRANTED){
                    sharedPreferences.edit().putBoolean(Constants.DIALOG,false).apply();
                }
                else{
                    utils.dialogOnPermissionDeny();
                }
        }
    }

    // Dialog to give permissions explaination
    private void dialogPermissionsExplaination(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context_firstrun);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.dismiss();
                permCheck();
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
                        permCheck();
                    }
                })
                .show();
    }
}
