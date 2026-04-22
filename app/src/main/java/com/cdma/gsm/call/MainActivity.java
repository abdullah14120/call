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
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText etTargetIp;
    private SharedPreferences prefs;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTargetIp = findViewById(R.id.editIp); // تأكد أن الـ ID مطابق لـ xml
        Button btnSender = findViewById(R.id.btnCdma);
        Button btnReceiver = findViewById(R.id.btnGsm);

        prefs = getSharedPreferences("BridgePrefs", MODE_PRIVATE);
        
        // استرجاع آخر IP تم استخدامه
        etTargetIp.setText(prefs.getString("ip", ""));

        // 1. طلب أذونات الهاتف الأساسية عند التشغيل
        checkAndRequestPermissions();

        // 2. تفعيل وضع المرسل (جهاز CDMA)
        btnSender.setOnClickListener(v -> {
            String ip = etTargetIp.getText().toString().trim();
            if (ip.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال IP جهاز المستقبل أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("ip", ip).apply();
            Toast.makeText(this, "تم الحفظ. سيعمل كمرسل إلى: " + ip, Toast.LENGTH_LONG).show();
        });

        // 3. تفعيل وضع المستقبل (جهاز GSM)
        btnReceiver.setOnClickListener(v -> {
            if (checkOverlayPermission()) {
                startBridgeService();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                
                requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE, 
                        Manifest.permission.READ_CALL_LOG
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_REQUEST_CODE);
                Toast.makeText(this, "يرجى تفعيل إذن الظهور فوق التطبيقات", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void startBridgeService() {
        Intent serviceIntent = new Intent(this, BridgeService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "بدء خدمة الاستقبال...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (checkOverlayPermission()) {
                startBridgeService();
            }
        }
    }
}
