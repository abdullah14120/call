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
import java.io.BufferedReader;
import java.io.IOException; // تم إضافة هذا الاستيراد لحل المشكلة
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BridgeService extends Service {
    private static final String CHANNEL_ID = "CallBridgeChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getServiceNotification("جسر المكالمات يعمل في الخلفية..."));
    }

    private Notification getServiceNotification(String text) {
        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setContentTitle("جسر CDMA")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void addCallToLog(String number) {
        try {
            ContentValues values = new ContentValues();
            values.put(CallLog.Calls.NUMBER, number);
            values.put(CallLog.Calls.DATE, System.currentTimeMillis());
            values.put(CallLog.Calls.DURATION, 0);
            values.put(CallLog.Calls.TYPE, CallLog.Calls.INCOMING_TYPE);
            values.put(CallLog.Calls.NEW, 1);
            getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
        } catch (Exception e) { e.printStackTrace(); }
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
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String data = in.readLine();
                    if (data != null && data.startsWith("RING:")) {
                        String incomingNumber = data.split(":")[1];
                        addCallToLog(incomingNumber);
                        showIncomingCallNotification(incomingNumber);
                    }
                    client.close();
                } catch (Exception e) { e.printStackTrace(); }
            }
        } catch (IOException e) { e.printStackTrace(); } // هنا كان الخطأ
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startSocketServer).start();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Call Bridge", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
