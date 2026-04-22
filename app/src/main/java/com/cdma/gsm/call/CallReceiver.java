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
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            String targetIp = context.getSharedPreferences("BridgePrefs", Context.MODE_PRIVATE).getString("ip", "");
            
            if (!targetIp.isEmpty()) {
                new Thread(() -> {
                    try (Socket socket = new Socket(targetIp, 8888)) {
                        new PrintWriter(socket.getOutputStream(), true).println("RING:" + number);
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        }
    }
}
