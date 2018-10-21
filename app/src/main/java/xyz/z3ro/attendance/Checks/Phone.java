package xyz.z3ro.attendance.Checks;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stfalcon.smsverifycatcher.OnSmsCatchListener;
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher;

import java.util.concurrent.TimeUnit;

import dmax.dialog.SpotsDialog;
import xyz.z3ro.attendance.Constants;
import xyz.z3ro.attendance.R;

public class Phone extends AppCompatActivity{
    Context context_phone;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String otp;
    private String VerificationId;
    private SmsVerifyCatcher smsVerifyCatcher;
    private android.app.AlertDialog progressDialog;
    private SharedPreferences sharedPreferences;
    private boolean verificationDone;
    Intent returnIntent;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_check);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }
        context_phone = this;
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        verificationDone = false;
        progressDialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).setTheme(R.style.Custom).build();
        returnIntent = new Intent();
        progressDialog.show();
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME,this.MODE_PRIVATE);
        smsVerifyCatcher = new SmsVerifyCatcher(this, new OnSmsCatchListener<String>() {
            @Override
            public void onSmsCatch(String message) {
                if (!verificationDone){
                    verificationDone = true;
                    otp = message.substring(0,6);
//                    otp = "654321";
                    Log.d("SMSCATCH",otp);
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(VerificationId, otp);
                    Toast.makeText(context_phone,"Verification Done",Toast.LENGTH_SHORT).show();
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                verificationDone = true;
                Toast.makeText(context_phone,"Verification Done",Toast.LENGTH_SHORT).show();
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Toast.makeText(context_phone,"Verification Not Done",Toast.LENGTH_SHORT).show();
                if (e instanceof FirebaseTooManyRequestsException){
                    Toast.makeText(context_phone,"Quota Exceeded",Toast.LENGTH_SHORT).show();
                }
                result(false);
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone_check number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d("VERIFICATION_CALLBACK", "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                VerificationId = verificationId;
            }
            @Override
            public void onCodeAutoRetrievalTimeOut(String verificationId){
                Toast.makeText(context_phone,"Timeout!",Toast.LENGTH_SHORT).show();
                result(false);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        smsVerifyCatcher.onStart();
        if (ContextCompat.checkSelfPermission(context_phone, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context_phone, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
            getPhoneNumber();
        else {
            Toast.makeText(context_phone,"App permissions error",Toast.LENGTH_LONG).show();
            result(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        smsVerifyCatcher.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        smsVerifyCatcher.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }
    private void getPhoneNumber(){
        String rollNo = sharedPreferences.getString(Constants.ROLL,null);
        databaseReference.child("Students").child(rollNo).child("phone_number").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String phoneNo = dataSnapshot.getValue().toString();
                startPhoneVerification(phoneNo);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context_phone,"Verification Failed",Toast.LENGTH_SHORT).show();
                result(false);
            }
        });
    }

    private void startPhoneVerification(String phoneNo){
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNo,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks);
    }
    // [START sign_in_with_phone]
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SIGN_IN", "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            user.delete();
                            firebaseAuth.signOut();
                            result(true);
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w("SIGN_IN", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(context_phone,"Verification Failed",Toast.LENGTH_SHORT).show();
                                result(false);
                            }
                        }
                    }
                });
    }
    // [END sign_in_with_phone]

    private void  result(boolean pass){
        if (pass){
            sharedPreferences.edit().putBoolean(Constants.PHONE_CHECK,true).apply();
            returnIntent.putExtra("resultPhone",true);
            setResult(RESULT_OK,returnIntent);
            progressDialog.dismiss();
            finish();
        }
        else {
            sharedPreferences.edit().putBoolean(Constants.PHONE_CHECK,false).apply();
            returnIntent.putExtra("resultPhone",false);
            setResult(RESULT_OK,returnIntent);
            progressDialog.dismiss();
            finish();
        }
    }
}
