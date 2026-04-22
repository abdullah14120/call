package com.cdma.gsm.call;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class IncomingCallActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إعدادات الظهور فوق القفل
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        setContentView(R.layout.activity_call); 
        
        String number = getIntent().getStringExtra("NUM");
        TextView txtNumber = findViewById(R.id.txtNumber);
        if (txtNumber != null) {
            txtNumber.setText(number);
        }

        if (findViewById(R.id.btnHangup) != null) {
            findViewById(R.id.btnHangup).setOnClickListener(v -> finish());
        }
    }
}
