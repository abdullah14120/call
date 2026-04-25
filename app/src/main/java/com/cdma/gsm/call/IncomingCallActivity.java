package com.cdma.gsm.call;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class IncomingCallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // --- تطوير إضاءة الشاشة للأجهزة الحديثة ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_call);

        // جلب الرقم القادم من Firebase عبر BridgeService
        String number = "رقم غير معروف";
        if (getIntent() != null && getIntent().hasExtra("NUM")) {
            number = getIntent().getStringExtra("NUM");
        }

        TextView txtNumber = findViewById(R.id.txtNumber);
        if (txtNumber != null) {
            txtNumber.setText(number);
        }

        // زر إغلاق الواجهة
        if (findViewById(R.id.btnHangup) != null) {
            findViewById(R.id.btnHangup).setOnClickListener(v -> finish());
        }
        
        // إغلاق تلقائي بعد 40 ثانية إذا لم يتم التفاعل (لحماية البطارية)
        new android.os.Handler().postDelayed(this::finish, 40000);
    }
}
