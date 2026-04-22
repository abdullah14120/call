public class MainActivity extends AppCompatActivity {
    EditText etTargetIp;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTargetIp = findViewById(R.id.etTargetIp);
        prefs = getSharedPreferences("BridgePrefs", MODE_PRIVATE);
        etTargetIp.setText(prefs.getString("ip", ""));

        // طلب الأذونات الضرورية
        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.READ_PHONE_STATE, 
            Manifest.permission.READ_CALL_LOG
        }, 1);

        findViewById(R.id.btnSender).setOnClickListener(v -> {
            String ip = etTargetIp.getText().toString();
            prefs.edit().putString("ip", ip).apply();
            Toast.makeText(this, "وضع المرسل مفعل", Toast.LENGTH_SHORT).show();
            // المرسل يعتمد على BroadcastReceiver المعرف في المانيفست
        });

        findViewById(R.id.btnReceiver).setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, BridgeService.class);
            startService(serviceIntent);
            Toast.makeText(this, "وضع المستقبل يعمل في الخلفية", Toast.LENGTH_SHORT).show();
        });
    }
}
