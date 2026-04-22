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
        String action = intent.getAction();
        
        // التحقق من أن الحدث هو تغير حالة الهاتف
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // بما أننا "المتصل الافتراضي"، سنحصل على الرقم هنا مباشرة
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                // في بعض الأجهزة، قد نحتاج لجلب الرقم بطريقة احتياطية
                if (incomingNumber == null || incomingNumber.isEmpty()) {
                    incomingNumber = "رقم خاص/مخفي";
                }

                Log.d("CallBridge", "مكالمة واردة من: " + incomingNumber);
                
                // جلب IP جهاز المستقبل المحفوظ
                String targetIp = context.getSharedPreferences("BridgePrefs", Context.MODE_PRIVATE)
                                         .getString("ip", "");

                if (!targetIp.isEmpty()) {
                    sendSignalToReceiver(targetIp, incomingNumber);
                }
            }
        }
    }

    private void sendSignalToReceiver(String ip, String number) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, 8888)) {
                // ضبط مهلة الاتصال لضمان عدم تعليق التطبيق
                socket.setSoTimeout(5000); 
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("RING:" + number);
                Log.d("CallBridge", "تم إرسال الإشارة بنجاح");
            } catch (Exception e) {
                Log.e("CallBridge", "فشل الإرسال: " + e.getMessage());
            }
        }).start();
    }
}
