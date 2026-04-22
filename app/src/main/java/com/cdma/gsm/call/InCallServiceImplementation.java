package com.cdma.gsm.call;

import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

/**
 * هذه الخدمة ضرورية جداً لأندرويد لكي يقبل تعيين التطبيق كـ "متصل افتراضي".
 * هي المسؤولة عن التفاعل مع نظام الاتصال في أندرويد.
 */
public class InCallServiceImplementation extends InCallService {

    private static final String TAG = "InCallService";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "تم اكتشاف مكالمة جديدة عبر النظام");
        
        /* بما أن وظيفة تطبيقك هي "جسر" (Bridge)، فنحن نترك نظام أندرويد 
           يتعامل مع المكالمة الحقيقية، بينما يقوم الـ CallReceiver الخاص بنا 
           بإرسال الإشارة عبر الواي فاي.
        */
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "تم إنهاء المكالمة");
    }
}
