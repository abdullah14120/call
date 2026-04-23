package com.cdma.gsm.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.PrintWriter;
import java.net.Socket;

public class CallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "CallBridge_Sender";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // جلب الرقم الوارد
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                /* تعديل هام: إذا كان الرقم null، نرسل نص "Unknown" 
                   ليتم التعرف عليه وفلترته في جهاز المستقبل ومنع السجل المكرر.
                */
                if (incomingNumber == null || incomingNumber.isEmpty()) {
                    incomingNumber = "Unknown";
                }

                Log.d(TAG, "اكتشاف رنين من رقم: " + incomingNumber);
                
                // جلب عنوان IP الهدف من الإعدادات المحفوظة
                String targetIp = context.getSharedPreferences("BridgePrefs", Context.MODE_PRIVATE)
                                         .getString("ip", "");

                if (!targetIp.isEmpty() && !targetIp.equals("")) {
                    sendSignalToReceiver(targetIp, incomingNumber);
                } else {
                    Log.w(TAG, "لم يتم إرسال الإشارة: عنوان IP غير مضبوط في الإعدادات.");
                }
            }
        }
    }

    private void sendSignalToReceiver(String ip, String number) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, 8888)) {
                // مهلة زمنية قصيرة للاتصال لضمان السرعة
                socket.setSoTimeout(3000); 
                
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                // إرسال الإشارة بالصيغة المتفق عليها مع السيرفر
                writer.println("RING:" + number);
                
                Log.i(TAG, "تم بث الإشارة بنجاح إلى: " + ip);
            } catch (Exception e) {
                Log.e(TAG, "خطأ في بث الإشارة عبر الشبكة: " + e.getMessage());
            }
        }).start();
    }
}
