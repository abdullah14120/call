package com.cdma.gsm.call;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private TextView txtStatus;
    private EditText editIp; // إضافة حقل الـ IP
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. ربط العناصر (تأكد من وجود editIp في الـ XML)
        txtStatus = findViewById(R.id.txtStatus);
        editIp = findViewById(R.id.editIp); 
        Button btnSender = findViewById(R.id.btnCdma);
        Button btnReceiver = findViewById(R.id.btnGsm);

        prefs = getSharedPreferences("BridgePrefs", MODE_PRIVATE);
        
        // استرجاع الـ IP المحفوظ سابقاً
        editIp.setText(prefs.getString("ip", ""));

        // 2. طلب الأذونات الأساسية (أندرويد 5 لا يطلبها وقت التشغيل، لكن أندرويد 6+ يطلبها)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        // 3. وضع المرسل (CDMA) - يرسل للـ IP المكتوب
        btnSender.setOnClickListener(v -> {
            String ip = editIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال IP جهاز المستقبل أولاً", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("ip", ip).apply();
                txtStatus.setText("الحالة: وضع المرسل نشط (" + ip + ")");
                txtStatus.setTextColor(0xFF4CAF50);
                Toast.makeText(this, "تم حفظ الـ IP وتفعيل المراقبة", Toast.LENGTH_LONG).show();
            }
        });

        // 4. وضع المستقبل (GSM) - يفتح السيرفر المحلي
        btnReceiver.setOnClickListener(v -> {
            if (checkOverlayPermission()) {
                startBridgeService();
            }
        });
    }

    private void checkAndRequestPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.READ_PHONE_STATE);
        perms.add(Manifest.permission.INTERNET);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> listToRequest = new ArrayList<>();
            for (String p : perms) {
                if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    listToRequest.add(p);
                }
            }
            if (!listToRequest.isEmpty()) {
                requestPermissions(listToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean checkOverlayPermission() {
        // إذن الظهور فوق التطبيقات بدأ من أندرويد 6.0 (API 23)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private void startBridgeService() {
        Intent serviceIntent = new Intent(this, BridgeService.class);
        // أندرويد 8+ يتطلب startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        txtStatus.setText("الحالة: وضع المستقبل نشط (بانتظار IP المرسل)");
        txtStatus.setTextColor(0xFF2196F3);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (checkOverlayPermission()) startBridgeService();
        }
    }
}
