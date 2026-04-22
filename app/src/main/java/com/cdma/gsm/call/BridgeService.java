package com.cdma.gsm.call;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BridgeService extends Service {
    private static final String CHANNEL_ID = "CallBridgeChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        // 1. إنشاء قناة الإشعارات (ضروري لأندرويد 8 فما فوق)
        createNotificationChannel();
        
        // 2. إنشاء الإشعار
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
                .setContentTitle("جسر المكالمات")
                .setContentText("خدمة الاستقبال تعمل الآن في الخلفية...")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setOngoing(true) // لمنع المستخدم من مسح الإشعار
                .build();

        // 3. الخطوة الأهم: إخبار النظام أن الخدمة بدأت فعلياً وإظهار الإشعار فوراً
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // تشغيل السيرفر في خيط مستقل حتى لا يتوقف التطبيق
        new Thread(this::startSocketServer).start();
        return START_STICKY;
    }

    private void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            while (true) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String data = in.readLine();
                if (data != null && data.startsWith("RING:")) {
                    String incomingNumber = data.split(":")[1];
                    launchCallUI(incomingNumber);
                }
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void launchCallUI(String number) {
        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra("NUM", number);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call Bridge Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
