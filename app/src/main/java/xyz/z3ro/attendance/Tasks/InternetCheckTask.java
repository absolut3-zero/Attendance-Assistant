package xyz.z3ro.attendance.Tasks;


import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class InternetCheckTask extends AsyncTask<Void,Void,Boolean> {
    private WeakReference<Context> contextWeakReference;
    public InternetCheckTask(Context context){
        contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try{
            int timeOutMs = 1500;
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress("8.8.8.8",53);
            socket.connect(socketAddress,timeOutMs);
            socket.close();
            return true;
        }
        catch (IOException e){
            return false;
        }
    }
}
