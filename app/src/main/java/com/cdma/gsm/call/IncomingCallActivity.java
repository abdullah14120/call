public class IncomingCallActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // لجعل الواجهة تظهر فوق القفل
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        setContentView(R.layout.activity_call); 
        
        String number = getIntent().getStringExtra("NUM");
        TextView txtNumber = findViewById(R.id.txtNumber);
        txtNumber.setText(number);

        // زر الرفض لإغلاق الواجهة فقط
        findViewById(R.id.btnHangup).setOnClickListener(v -> finish());
    }
}
