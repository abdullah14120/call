package com.cdma.gsm.call;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Person;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BridgeService extends Service {
    private static final String CHANNEL_ID = "CallBridgeChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        // إشعار الخدمة الدائم (Foreground) لضمان عدم توقف التطبيق
        startForeground(1, getServiceNotification("الخدمة تعمل..."));
    }

    private Notification getServiceNotification(String text) {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle("جسر المكالمات")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .build();
    }

    private void showIncomingCallNotification(String number) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        // إعداد نية (Intent) عند الضغط على الإشعار يفتح واجهة الاتصال
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("NUM", number);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // بناء شخصية المتصل (مطلوب لـ CallStyle)
        Person incomingCaller = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            incomingCaller = new Person.Builder()
                    .setName(number)
                    .setImportant(true)
                    .build();
        }

        // بناء إشعار بنمط المكالمة (CallStyle)
        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setStyle(Notification.CallStyle.forIncomingCall(incomingCaller, fullScreenPendingIntent, fullScreenPendingIntent))
                    .setFullScreenIntent(fullScreenPendingIntent, true) // لجعلها تظهر كمنبثق علوي
                    .setCategory(Notification.CATEGORY_CALL)
                    .setOngoing(true);
        } else {
            // للإصدارات الأقدم من أندرويد 12
            builder = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setContentTitle("مكالمة واردة")
                    .setContentText(number)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setCategory(Notification.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true);
        }

        notificationManager.notify(2, builder.build());
    }

    private void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            while (true) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String data = in.readLine();
                if (data != null && data.startsWith("RING:")) {
                    String incomingNumber = data.split(":")[1];
                    // بدلاً من فتح النشاط مباشرة، نظهر الإشعار الاحترافي
                    showIncomingCallNotification(incomingNumber);
                }
                client.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startSocketServer).start();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Call Bridge", NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
