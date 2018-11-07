package xyz.z3ro.attendance.Tasks;

import android.os.AsyncTask;

import org.apache.commons.net.time.TimeTCPClient;

import java.io.IOException;

public class DateCheckTask extends AsyncTask<Void, Void, Integer> {
    //    private WeakReference<Context> contextWeakReference;
//    public DateCheckTask(Context context){
//        contextWeakReference = new WeakReference<>(context);
//    }
    @Override
    protected Integer doInBackground(Void... voids) {
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
            String strDate = rawDate.substring(8, 10);
            return Integer.parseInt(strDate);
        }
        return 77;
    }
}
