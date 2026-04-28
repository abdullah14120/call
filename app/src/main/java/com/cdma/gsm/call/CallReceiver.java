package com.cdma.gsm.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.PrintWriter;
import java.net.Socket;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                if (number == null) number = "Private Number";

                // جلب الـ IP من الإعدادات التي يدخلها المستخدم في الواجهة
                String ip = context.getSharedPreferences("BridgePrefs", Context.MODE_PRIVATE)
                                   .getString("ip", "");

                if (!ip.isEmpty()) {
                    sendToReceiver(ip, number);
                }
            }
        }
    }

    private void sendToReceiver(String ip, String number) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, 8888)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("RING:" + number);
                Log.d("Bridge", "Sent to: " + ip);
            } catch (Exception e) {
                Log.e("Bridge", "Error: " + e.getMessage());
            }
        }).start();
    }
}
