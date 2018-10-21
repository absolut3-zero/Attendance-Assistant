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
import android.telephony.TelephonyManager;
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
import xyz.z3ro.attendance.Models.Serial;
import xyz.z3ro.attendance.Models.User;
import xyz.z3ro.attendance.Tasks.DateCheckTask;
import xyz.z3ro.attendance.Tasks.InternetCheckTask;
import xyz.z3ro.attendance.Utilities.Utils;


public class SignUp extends AppCompatActivity {
    private Context context_signup;
    private SharedPreferences sharedPreferences;
    private DatabaseReference databaseReference;
    private TelephonyManager telephonyManager;
    private android.app.AlertDialog progressDialog;
    private Utils utils;

    private EditText firstName;
    private EditText lastName;
    private EditText roll;
    private EditText phone;
    private AppCompatButton signUp;

    private String userId;

    private final String TAG = "SignUp";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }
        // Initialisation
        context_signup = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME,this.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        progressDialog = new SpotsDialog.Builder().setContext(context_signup).setCancelable(false).setTheme(R.style.Custom).build();
        utils = new Utils(context_signup,this);
        // Setting up views
        firstName = findViewById(R.id.etFirstName);
        lastName = findViewById(R.id.etLastName);
        roll = findViewById(R.id.etRoll);
        phone = findViewById(R.id.etPhone);
        signUp = findViewById(R.id.bttnSubmit);
        TextView registration = findViewById(R.id.tvRegistration);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/Organo.ttf");
        if(registration != null)
            registration.setTypeface(typeface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(context_signup,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_signup,Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_signup, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                    ContextCompat.checkSelfPermission(context_signup,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                permCheck();
            }
        }
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                boolean connected;
                boolean serialMatched;
                try{
                    connected = new InternetCheckTask(context_signup).execute().get();
                }
                catch(InterruptedException | ExecutionException e){
                    connected = false;
                }
                serialMatched = sharedPreferences.getBoolean(Constants.SERIAL_REGISTERED,true);
                if(ContextCompat.checkSelfPermission(context_signup,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                        ContextCompat.checkSelfPermission(context_signup,Manifest.permission.READ_SMS) == PackageManager.PERMISSION_DENIED ||
                        ContextCompat.checkSelfPermission(context_signup, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_DENIED ||
                        ContextCompat.checkSelfPermission(context_signup,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                    progressDialog.dismiss();
                    utils.dialogPermissionDeniedSubmit();
                }
                else {
                    if(connected){
                        if (serialMatched){
                            Log.d(TAG,"Serial already Registered");
                            progressDialog.dismiss();
                            dialog5();
                        }
                        else {
                            String first_name = firstName.getText().toString();
                            String last_name = lastName.getText().toString();
                            String roll_no = roll.getText().toString();
                            String phone_no = phone.getText().toString();
                            String serial = utils.serialGet();
                            String imei = telephonyManager.getDeviceId();
                            if(TextUtils.isEmpty(first_name)||TextUtils.isEmpty(last_name)||TextUtils.isEmpty(roll_no)||TextUtils.isEmpty(phone_no)){
                                progressDialog.dismiss();
                                Snackbar.make(findViewById(android.R.id.content), "All fields are mandatory",Snackbar.LENGTH_SHORT).show();
                            }
                            else{
                                if(phone_no.trim().length()==10){
                                    phone_no = "+91"+phone_no;
                                    // Register user
                                    Register(first_name,last_name,roll_no,phone_no,serial,imei);
                                }
                                else{
                                    progressDialog.dismiss();
                                    Snackbar.make(findViewById(android.R.id.content), "Phone number should be equal to 10 digits",Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                    else{
                        progressDialog.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "No Internet Access!",Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    // To check for required permissions
    public void permCheck(){
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
                    sharedPreferences.edit().putBoolean(Constants.DIALOG,true).apply();
                    utils.dialogOnPermissionDeny();
                }
        }
    }

    // Database check method to check if a node exists
    private void Register(String f_name,String l_name,String roll,String phone,String serial,String imei){
        final String firstName =f_name,lastName=l_name,rollNo=roll,phoneNo=phone,serialNo=serial,imeiNo=imei;
       databaseReference.child("Students").child(rollNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Log.d(TAG,"Roll Number Already Registered");
                    Snackbar.make(findViewById(android.R.id.content), "Student with this Roll number is registered already",Snackbar.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
                else {
                    Log.d(TAG,"Roll Number not registered Already");
                    createUser(firstName,lastName,rollNo,phoneNo,serialNo,imeiNo);
                    Toast.makeText(context_signup,"Registered",Toast.LENGTH_LONG).show();
                    sharedPreferences.edit().putBoolean(Constants.FIRST_RUN,false).apply();
                    sharedPreferences.edit().putString(Constants.ROLL,rollNo).apply();
                    sharedPreferences.edit().putString(Constants.SERIAL,serialNo).apply();
                    progressDialog.dismiss();
                    Intent mainActivity = new Intent(context_signup,MainActivity.class);
                    startActivity(mainActivity);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG,"Database Error");
                Toast.makeText(context_signup,"Error connecting to database! Please try again after some time.",Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
    }

    // Create student node
    private void createUser(String f_name, String l_name, String roll, String phone, String serial, String imei) {
        if (TextUtils.isEmpty(userId)) {
            userId = roll;
        }
        DateCheckTask dateCheckTask = new DateCheckTask();
        int currentDate;
        try {
            currentDate = dateCheckTask.execute().get();
        }
        catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
            currentDate = 78;
        }
        User user = new User(f_name, l_name, roll,phone,serial,imei,0);
        Serial serialReg = new Serial(currentDate,0);
        databaseReference.child("Students").child(userId).setValue(user);
        databaseReference.child("RegisteredSerialNumber").child(serial).setValue(serialReg);
    }

    //Dialog to show on serialmatch case
    void dialog5(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context_signup);
        builder.setTitle("Notice")
                .setMessage("Your device is already registered. If that's not the case then try again after a while.\n\nIf this problem persists then contact administrator.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

}
