package xyz.z3ro.attendance.Tasks;


import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import xyz.z3ro.attendance.Constants;

public class SerialCheckTask extends AsyncTask<String, Void, Void> {
    private WeakReference<SharedPreferences> sharedPreferencesWeakReference;

    public SerialCheckTask(SharedPreferences sharedPreferences) {
        sharedPreferencesWeakReference = new WeakReference<>(sharedPreferences);
    }

    @Override
    protected Void doInBackground(String... strings) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("RegisteredSerialNumber").child(strings[0]).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    sharedPreferencesWeakReference.get().edit().putBoolean(Constants.SERIAL_REGISTERED, true).apply();
                } else {
                    sharedPreferencesWeakReference.get().edit().putBoolean(Constants.SERIAL_REGISTERED, false).apply();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                sharedPreferencesWeakReference.get().edit().putBoolean(Constants.SERIAL_REGISTERED, true).apply();
            }
        });
        return null;
    }
}
