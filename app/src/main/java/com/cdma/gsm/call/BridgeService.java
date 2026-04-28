package com.cdma.gsm.call;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BridgeService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startServer).start();
        return START_STICKY;
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            while (true) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String data = in.readLine();
                if (data != null && data.startsWith("RING:")) {
                    String number = data.split(":")[1];
                    
                    // فتح واجهة الرنين
                    Intent i = new Intent(this, IncomingCallActivity.class);
                    i.putExtra("NUM", number);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
