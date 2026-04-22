package com.cdma.gsm.call;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class IncomingCallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // إعدادات الظهور فوق شاشة القفل وتنبيه الشاشة
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_call);

        // عرض رقم المتصل القادم من السيرفر
        String number = getIntent().getStringExtra("NUM");
        TextView txtNumber = findViewById(R.id.txtNumber);
        if (txtNumber != null && number != null) {
            txtNumber.setText(number);
        }

        // زر إنهاء/إغلاق الواجهة
        if (findViewById(R.id.btnHangup) != null) {
            findViewById(R.id.btnHangup).setOnClickListener(v -> finish());
        }
    }
}
