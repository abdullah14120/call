package com.cdma.gsm.call;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private TextView txtStatus;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط عناصر الواجهة الجديدة
        txtStatus = findViewById(R.id.txtStatus);
        Button btnSender = findViewById(R.id.btnCdma);
        Button btnReceiver = findViewById(R.id.btnGsm);
        Switch swCalls = findViewById(R.id.switchReceiveCalls);
        Switch swSms = findViewById(R.id.switchReceiveSms);

        prefs = getSharedPreferences("ReceiverPrefs", MODE_PRIVATE);
        
        // تحميل إعدادات الاستقبال السحابي
        swCalls.setChecked(prefs.getBoolean("allow_calls", true));
        swSms.setChecked(prefs.getBoolean("allow_sms", true));

        // 1. طلب المصفوفة الشاملة للأذونات (الهاتف، السجل، الرسائل، الإشعارات)
        checkAndRequestAllPermissions();

        // 2. تفعيل وضع المرسل (جهاز CDMA) - النظام الخماسي
        btnSender.setOnClickListener(v -> {
            activateSenderMode();
        });

        // 3. تفعيل وضع المستقبل (جهاز GSM) - الاستقبال السحابي
        btnReceiver.setOnClickListener(v -> {
            if (checkOverlayPermission()) {
                startBridgeService();
            }
        });

        // حفظ تفضيلات المستخدم للاستقبال
        swCalls.setOnCheckedChangeListener((b, isChecked) -> prefs.edit().putBoolean("allow_calls", isChecked).apply());
        swSms.setOnCheckedChangeListener((b, isChecked) -> prefs.edit().putBoolean("allow_sms", isChecked).apply());
    }

    private void checkAndRequestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // أذونات المكالمات والسجل (الطريقة 1 و 2)
        permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        permissionsNeeded.add(Manifest.permission.READ_CALL_LOG);
        permissionsNeeded.add(Manifest.permission.WRITE_CALL_LOG);
        
        // أذونات الرسائل النصية (الطريقة 3)
        permissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        permissionsNeeded.add(Manifest.permission.READ_SMS);

        // أذونات الإشعارات لأندرويد 13 فما فوق
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> listPermissionsAssign = new ArrayList<>();
        for (String per : permissionsNeeded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(per) != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsAssign.add(per);
                }
            }
        }

        if (!listPermissionsAssign.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(listPermissionsAssign.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }
        }

        // فحص وتفعيل ميزة Notification Listener (الطريقة 4)
        if (!isNotificationServiceEnabled()) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            Toast.makeText(this, "يرجى تفعيل 'جسر CDMA' في قائمة الإشعارات", Toast.LENGTH_LONG).show();
        }

        // فحص وتفعيل ميزة Accessibility Service (الطريقة 5)
        if (!isAccessibilityServiceEnabled(this, MyAccessibilityService.class)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "يرجى تفعيل 'جسر CDMA' في خدمات الوصول", Toast.LENGTH_LONG).show();
        }
    }

    private void activateSenderMode() {
        // تفعيل CallLog Observer لمراقبة قاعدة بيانات النظام مباشرة
        CallLogObserver observer = new CallLogObserver(new Handler(), this);
        getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer);

        txtStatus.setText("الحالة: مرسل سحابي (نظام خماسي)");
        txtStatus.setTextColor(0xFF4CAF50); // لون أخضر
        Toast.makeText(this, "تم تفعيل كافة طرق التتبع السحابي بنجاح", Toast.LENGTH_LONG).show();
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null && pkgName.equals(cn.getPackageName())) return true;
            }
        }
        return false;
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        ComponentName expected = new ComponentName(context, service);
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null) return false;
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expected)) return true;
        }
        return false;
    }

    private boolean checkOverlayPermission() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        txtStatus.setText("الحالة: مستقبل سحابي نشط");
        txtStatus.setTextColor(0xFF2196F3); // لون أزرق
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (checkOverlayPermission()) startBridgeService();
        }
    }
}
