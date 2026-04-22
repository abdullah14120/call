public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            String targetIp = context.getSharedPreferences("BridgePrefs", Context.MODE_PRIVATE).getString("ip", "");
            
            if (!targetIp.isEmpty()) {
                new Thread(() -> {
                    try (Socket socket = new Socket(targetIp, 8888)) {
                        new PrintWriter(socket.getOutputStream(), true).println("RING:" + number);
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        }
    }
}
