package com.cdma.gsm.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import java.io.PrintWriter;
import java.net.Socket;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (incomingNumber == null) incomingNumber = "Private";

            String ip = context.getSharedPreferences("Prefs", Context.MODE_PRIVATE).getString("ip", "");
            if (!ip.isEmpty()) {
                sendLocalSignal(ip, incomingNumber);
            }
        }
    }

    private void sendLocalSignal(String ip, String number) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, 8888)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("RING:" + number);
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
