package vsse.vsse_and;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.zxing.integration.android.IntentIntegrator;

import static android.os.Build.MODEL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ClipboardManager clipboardManager = (ClipboardManager) getApplicationContext().getSystemService(CLIPBOARD_SERVICE);
        if(!clipboardManager.hasPrimaryClip())
            return;
        ClipData c = clipboardManager.getPrimaryClip();
        String s = c.getItemAt(0).getText().toString();
        if(!s.startsWith("vsse://"))
            return;

        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        intent.setData(Uri.parse(s));
        startActivity(intent);
        finish();
    }
}
