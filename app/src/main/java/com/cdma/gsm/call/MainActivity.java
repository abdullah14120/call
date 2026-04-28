package com.cdma.gsm.call;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText editIp;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editIp = findViewById(R.id.editIp);
        Button btnSave = findViewById(R.id.btnSave);
        prefs = getSharedPreferences("BridgePrefs", MODE_PRIVATE);

        // استعادة الـ IP المحفوظ
        editIp.setText(prefs.getString("ip", ""));

        btnSave.setOnClickListener(v -> {
            String ip = editIp.getText().toString().trim();
            if (!ip.isEmpty()) {
                prefs.edit().putString("ip", ip).apply();
                Toast.makeText(this, "تم حفظ IP المستقبل بنجاح", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
