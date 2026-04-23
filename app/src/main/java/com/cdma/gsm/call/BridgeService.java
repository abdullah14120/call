package com.cdma.gsm.call;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Person;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.CallLog;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BridgeService extends Service {
    private static final String CHANNEL_ID = "CallBridgeChannel";
    private static final String TAG = "BridgeService";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getServiceNotification("جسر CDMA يعمل في الخلفية..."));
    }

    private Notification getServiceNotification(String text) {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle("جسر CDMA")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .build();
    }

    /**
     * حفظ الرقم في سجل النظام كـ "مكالمة فائتة" مع فلترة الأرقام الوهمية.
     */
    private void addMissedCallToLog(String number) {
        // --- فلتر منع التكرار والأرقام الوهمية ---
        if (number == null || number.isEmpty() || number.equalsIgnoreCase("null") || 
            number.contains("Unknown") || number.contains("مخفي") || number.contains("خاص")) {
            Log.d(TAG, "تم تجاهل سجل وهمي (رقم خاص/غير معروف) لمنع التكرار.");
            return; 
        }

        try {
            ContentValues values = new ContentValues();
            values.put(CallLog.Calls.NUMBER, number);
            values.put(CallLog.Calls.DATE, System.currentTimeMillis());
            values.put(CallLog.Calls.DURATION, 0);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                values.put(CallLog.Calls.TYPE, CallLog.Calls.MISSED_TYPE);
            } else {
                values.put(CallLog.Calls.TYPE, 3); 
            }
            
            values.put(CallLog.Calls.NEW, 1);

            getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
            Log.d(TAG, "تم تسجيل المكالمة الفائتة للرقم الحقيقي: " + number);
        } catch (Exception e) {
            Log.e(TAG, "فشل تسجيل المكالمة: " + e.getMessage());
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
            Person incomingCaller = new Person.Builder()
                    .setName(number)
                    .setImportant(true)
                    .build();

            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setStyle(Notification.CallStyle.forIncomingCall(incomingCaller, fullScreenPendingIntent, fullScreenPendingIntent))
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setOngoing(true);
        } else {
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setContentTitle("مكالمة واردة عبر الجسر")
                    .setContentText(number)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true);
        }

        notificationManager.notify(2, builder.build());
    }

    private void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            Log.d(TAG, "انتظار إشارة الرنين من جهاز CDMA...");
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String data = in.readLine();
                    
                    if (data != null && data.startsWith("RING:")) {
                        String incomingNumber = data.split(":")[1];
                        
                        // تسجيل المكالمة كفائتة فوراً
                        addMissedCallToLog(incomingNumber);
                        // إظهار واجهة الرنين
                        showIncomingCallNotification(incomingNumber);
                    }
                    client.close();
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في معالجة البيانات: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "فشل تشغيل السيرفر: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startSocketServer).start();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, 
                    "جسر المكالمات", 
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
