package xyz.z3ro.attendance;

import android.Manifest;
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
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutionException;

import dmax.dialog.SpotsDialog;
import xyz.z3ro.attendance.Tasks.InternetCheckTask;
import xyz.z3ro.attendance.Utilities.Utils;

public class LogIn extends AppCompatActivity {
    private Context context_login;
    private SharedPreferences sharedPreferences;
    private DatabaseReference databaseReference;
    private Utils utils;
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
        if(getSupportActionBar() != null){
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }
        // Initialisation
        context_login = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME,this.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        utils = new Utils(context_login,this);
        progressDialog = new SpotsDialog.Builder().setContext(context_login).setCancelable(false).setTheme(R.style.Custom).build();
        // Setting up views
        rollNo = findViewById(R.id.etRoll);
        logIn = findViewById(R.id.bttnLogIn);
        TextView login = findViewById(R.id.tvLogIn);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/Organo.ttf");
        if (login != null)
            login.setTypeface(typeface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(context_login,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_login,Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_login, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_login,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                permCheck();
            }
        }
        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                boolean connected;
                try{
                    connected = new InternetCheckTask(context_login).execute().get();
                }
                catch(InterruptedException | ExecutionException e){
                    connected = false;
                }
                if(ContextCompat.checkSelfPermission(context_login, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                        ContextCompat.checkSelfPermission(context_login, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                        ContextCompat.checkSelfPermission(context_login, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                        ContextCompat.checkSelfPermission(context_login, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                    progressDialog.dismiss();
                    utils.dialogPermissionDeniedSubmit();
                }
                else{
                    if (connected){
                        rollNumber = rollNo.getText().toString();
                        serial = utils.serialGet();
                        if(TextUtils.isEmpty(rollNumber)){
                            progressDialog.dismiss();
                            Snackbar.make(findViewById(android.R.id.content), "All fields are mandatory",Snackbar.LENGTH_SHORT).show();
                        }
                        else {
                            Login();
                        }
                    }
                    else {
                        progressDialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!",Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    private void permCheck(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION},Constants.MY_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Constants.MY_REQUEST_CODE:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&grantResults[1]==PackageManager.PERMISSION_GRANTED&&grantResults[2]==PackageManager.PERMISSION_GRANTED){
                }
                else{
                    utils.dialogOnPermissionDeny();
                }
        }
    }

    private void Login(){
        Log.d(TAG,"Checking for serial stored");
        databaseReference.child("Students").child(rollNumber).child("serial").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String serialStored;
                if(dataSnapshot.exists()){
                    try {
                        serialStored = dataSnapshot.getValue().toString();
                    }
                    catch (NullPointerException e){
                        serialStored = null;
                    }
                    Log.d(TAG,"Stored serial(database) :  "+serialStored);
                    if(serialStored.equals(serial)){
                        Log.d(TAG,"Serial Matched");
                        sharedPreferences.edit().putBoolean(Constants.FIRST_RUN,false).apply();
                        sharedPreferences.edit().putString(Constants.SERIAL,serial).apply();
                        sharedPreferences.edit().putString(Constants.ROLL,rollNumber).apply();
                        Intent mainActivity = new Intent(context_login,MainActivity.class);
                        startActivity(mainActivity);
                        Toast.makeText(context_login,"Log In Successful",Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                        finish();
                    }
                    else {
                        Log.d(TAG,"Serial not matched");
                        Snackbar.make(findViewById(android.R.id.content), "Authentication Failed!",Snackbar.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        dialog4();
                    }
                }
                else {
                    Snackbar.make(findViewById(android.R.id.content), "Authentication Failed!",Snackbar.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    dialog4();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG,"Database Error");
                Snackbar.make(findViewById(android.R.id.content), "Error connecting to database! Please try again after some time.",Snackbar.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

    //Dialog to show on authentication failed
    private void dialog4(){
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