package com.cdma.gsm.call;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Person;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.CallLog;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BridgeService extends Service {
    private static final String CHANNEL_ID = "CallBridgeChannel";
    private static final String TAG = "BridgeServiceCloud";
    private static final String DB_URL = "https://banproject-2f9c6-default-rtdb.firebaseio.com/";
    
    private DatabaseReference bridgeRef;
    private ValueEventListener cloudListener;
    private long lastSyncTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // إشعار الخدمة الدائمة
        startForeground(1, getServiceNotification("جسر CDMA: بانتظار الإشارة السحابية..."));
        
        // ربط السحابة (Firebase)
        initCloudMonitoring();
    }

    private void initCloudMonitoring() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(DB_URL);
        bridgeRef = database.getReference("bridge_signals");
        
        // جعل القناة "ساخنة" لضمان السرعة القصوى حتى لو الشاشة مغلقة
        bridgeRef.keepSynced(true);

        cloudListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                String number = dataSnapshot.child("number").getValue(String.class);
                String type = dataSnapshot.child("type").getValue(String.class);
                Long timestamp = dataSnapshot.child("timestamp").getValue(Long.class);

                // التأكد من أن الإشارة جديدة (لم تعالج من قبل)
                if (timestamp != null && timestamp > lastSyncTime) {
                    lastSyncTime = timestamp;
                    handleIncomingSignal(number, type);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "فشل الاتصال بـ Firebase: " + databaseError.getMessage());
            }
        };

        bridgeRef.addValueEventListener(cloudListener);
    }

    private void handleIncomingSignal(String number, String type) {
        SharedPreferences prefs = getSharedPreferences("ReceiverPrefs", MODE_PRIVATE);
        
        if ("CALL".equals(type)) {
            // التحقق من خيار "استقبال المكالمات" في الواجهة
            if (prefs.getBoolean("allow_calls", true)) {
                addMissedCallToLog(number);
                showIncomingCallNotification(number);
            }
        } else if ("SMS".equals(type)) {
            // التحقق من خيار "استقبال أرقام الرسائل" في الواجهة
            if (prefs.getBoolean("allow_sms", true)) {
                // نسجلها في السجل مع تمييز أنها رسالة نصية
                addMissedCallToLog("SMS: " + number);
            }
        }
    }

    private void addMissedCallToLog(String number) {
        if (number == null || number.isEmpty() || number.contains("Unknown")) return;

        try {
            ContentValues values = new ContentValues();
            values.put(CallLog.Calls.NUMBER, number);
            values.put(CallLog.Calls.DATE, System.currentTimeMillis());
            values.put(CallLog.Calls.DURATION, 0);
            values.put(CallLog.Calls.TYPE, CallLog.Calls.MISSED_TYPE);
            values.put(CallLog.Calls.NEW, 1);

            getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
            Log.d(TAG, "تم تسجيل البيانات بنجاح: " + number);
        } catch (Exception e) {
            Log.e(TAG, "فشل التسجيل: " + e.getMessage());
        }
    }

    private void showIncomingCallNotification(String number) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("NUM", number);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Person incomingCaller = new Person.Builder().setName(number).setImportant(true).build();
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setStyle(Notification.CallStyle.forIncomingCall(incomingCaller, fullScreenPendingIntent, fullScreenPendingIntent))
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setOngoing(true);
        } else {
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setContentTitle("مكالمة جسر CDMA")
                    .setContentText("متصل الآن: " + number)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true);
        }

        notificationManager.notify(2, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // إعادة تشغيل الخدمة تلقائياً إذا قتلها النظام
    }

    private Notification getServiceNotification(String text) {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle("جسر CDMA السحابي")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "جسر المكالمات", NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (bridgeRef != null && cloudListener != null) {
            bridgeRef.removeEventListener(cloudListener);
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
