package com.cdma.gsm.call;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BridgeService extends Service {
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startSocketServer).start();
        return START_STICKY;
    }

    private void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            while (true) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String data = in.readLine();
                if (data != null && data.startsWith("RING:")) {
                    String incomingNumber = data.split(":")[1];
                    launchCallUI(incomingNumber);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void launchCallUI(String number) {
        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra("NUM", number);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
