package xyz.z3ro.attendance.Tasks;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.time.TimeTCPClient;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class TimeCheckTask extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<Context> contextWeakReference;
    private WeakReference<String> stringStartWeakReference;
    private WeakReference<String> stringStopWeakReference;

    public TimeCheckTask(Context context, String start, String stop) {
        contextWeakReference = new WeakReference<>(context);
        stringStartWeakReference = new WeakReference<>(start);
        stringStopWeakReference = new WeakReference<>(stop);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        int VALID_TIME_START = Integer.parseInt(stringStartWeakReference.get());
        int VALID_TIME_STOP = Integer.parseInt(stringStopWeakReference.get());
        String rawDate = null;
        try {
            TimeTCPClient timeTCPClient = new TimeTCPClient();
            try {
                timeTCPClient.setDefaultTimeout(30000);
                timeTCPClient.connect("time.nist.gov");
                rawDate = timeTCPClient.getDate().toString();
            } finally {
                timeTCPClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rawDate != null) {
            String rawTime = rawDate.substring(11, 13);
            rawTime = rawTime.concat(rawDate.substring(14, 16));
            rawTime = rawTime.concat(rawDate.substring(17, 19));
            Log.d("TimeCheckTask", rawTime);
            int time = Integer.parseInt(rawTime);
            if (time >= VALID_TIME_START && time <= VALID_TIME_STOP) {
                return true;
            } else
                return false;
        }
        return false;
    }
}
