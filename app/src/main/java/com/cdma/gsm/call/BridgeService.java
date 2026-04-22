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
        // إشعار الخدمة الدائم لضمان استقرار التطبيق في الخلفية
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
     * حفظ الرقم في سجل النظام كـ "مكالمة فائتة" فوراً.
     * هذا يضمن ظهور إشعار النظام الرسمي للمكالمات الفائتة.
     */
    private void addMissedCallToLog(String number) {
        try {
            ContentValues values = new ContentValues();
            values.put(CallLog.Calls.NUMBER, number);
            values.put(CallLog.Calls.DATE, System.currentTimeMillis());
            values.put(CallLog.Calls.DURATION, 0);
            
            // تعيين النوع كـ مكالمة فائتة (Missed Call)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                values.put(CallLog.Calls.TYPE, CallLog.Calls.MISSED_TYPE);
            } else {
                values.put(CallLog.Calls.TYPE, 3); // القيمة 3 تعني Missed في الإصدارات القديمة
            }
            
            values.put(CallLog.Calls.NEW, 1); // وسمها كمكالمة جديدة غير مقروءة

            getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
            Log.d(TAG, "تم تسجيل مكالمة فائتة للرقم: " + number);
        } catch (Exception e) {
            Log.e(TAG, "فشل تسجيل المكالمة في السجل: " + e.getMessage());
        }
    }

    private void showIncomingCallNotification(String number) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("NUM", number);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // استخدام FLAG_IMMUTABLE للتوافق مع أندرويد 12 فأحدث
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // نمط المكالمة الرسمي لأندرويد 12+ (يمنح مظهر الاتصال الحقيقي)
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
            // نمط الإشعار المخصص للإصدارات الأقدم مع أولوية قصوى
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setContentTitle("مكالمة واردة عبر الجسر")
                    .setContentText(number)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setAutoCancel(true);
        }

        notificationManager.notify(2, builder.build());
    }

    private void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            Log.d(TAG, "السيرفر يعمل وينتظر الاتصال على المنفذ 8888...");
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String data = in.readLine();
                    
                    if (data != null && data.startsWith("RING:")) {
                        String incomingNumber = data.split(":")[1];
                        
                        // الترتيب: تسجيل السجل أولاً ثم إظهار واجهة الاتصال
                        addMissedCallToLog(incomingNumber);
                        showIncomingCallNotification(incomingNumber);
                    }
                    client.close();
                } catch (Exception e) {
                    Log.e(TAG, "خطأ في استقبال البيانات: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "تعذر بدء السيرفر: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // تشغيل السيرفر في خيط مستقل لضمان عدم تجميد التطبيق
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
            channel.setDescription("إشعارات استقبال المكالمات من جهاز CDMA");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
