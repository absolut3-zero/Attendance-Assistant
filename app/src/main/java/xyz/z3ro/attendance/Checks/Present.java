package xyz.z3ro.attendance.Checks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import dmax.dialog.SpotsDialog;
import xyz.z3ro.attendance.Constants;
import xyz.z3ro.attendance.R;
import xyz.z3ro.attendance.Tasks.DateCheckTask;
import xyz.z3ro.attendance.Tasks.TimeCheckTask;

public class Present extends AppCompatActivity {
    private Context context_present;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;
    private AlertDialog progressDialog;
    private DateCheckTask dateCheckTask;

    private int currentDate;
    private int storedDate;
    private String serialStored;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.present);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setIcon(R.mipmap.ic_launcher_round);
            getSupportActionBar().setTitle(R.string.app_title);
        }

        // Initialisations
        context_present = this;
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, this.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        dateCheckTask = new DateCheckTask();
        serialStored = sharedPreferences.getString(Constants.SERIAL, null);
        storedDate = 78;
        currentDate = 77;
        progressDialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).setTheme(R.style.Custom).build();
        progressDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean b;
        TimeCheckTask timeCheckTask = new TimeCheckTask(context_present, getString(R.string.time_start), getString(R.string.time_stop));
        try {
            b = timeCheckTask.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            b = false;
        }
        if (b) {
            getStoredDate();
        } else {
            Toast.makeText(context_present, getString(R.string.time_limit_exceeded), Toast.LENGTH_LONG).show();
            progressDialog.dismiss();
            finish();
        }
    }

    // Get stored date
    private void getStoredDate() {
        databaseReference.child("RegisteredSerialNumber").child(serialStored).child("date").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                storedDate = Integer.parseInt(dataSnapshot.getValue().toString());
                conditionMatch();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                result(3);
            }
        });
    }

    // Condition matching
    private void conditionMatch() {
        try {
            currentDate = dateCheckTask.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            currentDate = 77;
        }
        if (currentDate == storedDate) {
            checkIfPresent();
        } else if (storedDate == 78 || currentDate == 77) {
            result(3);
        } else {
            doPresent();
        }
    }

    //Check If Present
    private void checkIfPresent() {
        databaseReference.child("RegisteredSerialNumber").child(serialStored).child("isPresent").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (Integer.parseInt(dataSnapshot.getValue().toString()) == 0) {
                    doPresent();
                    databaseReference.child("RegisteredSerialNumber").child(serialStored).child("isPresent").setValue(1);
                } else if (Integer.parseInt(dataSnapshot.getValue().toString()) == 1) {
                    result(2);
                } else {
                    result(3);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                result(3);
            }
        });
    }

    // Do Present
    private void doPresent() {
        final String rollNo = sharedPreferences.getString(Constants.ROLL, null);
        databaseReference.child("Students").child(rollNo).child("No_of_days_present").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int noOfDaysPresent;
                noOfDaysPresent = Integer.parseInt(dataSnapshot.getValue().toString());
                noOfDaysPresent += 1;
                databaseReference.child("Students").child(rollNo).child("No_of_days_present").setValue(noOfDaysPresent);
                result(1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                result(3);
            }
        });
    }

    // Result
    private void result(int pass) {
        switch (pass) {
            case 1:
                databaseReference.child("RegisteredSerialNumber").child(serialStored).child("date").setValue(currentDate);
                Toast.makeText(context_present, getString(R.string.attendance_done), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                finish();
                break;
            case 2:
                Toast.makeText(context_present, getString(R.string.attendance_already_done), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                finish();
                break;
            case 3:
                Toast.makeText(context_present, getString(R.string.present_database_error), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                finish();
                break;
        }
    }

}
