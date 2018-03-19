package vsse.vsse_and;

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

        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        intent.setData(Uri.parse("vsse://10.21.238.12:5678/?k=swtnylqmrbocgixjahfzpudkev&k0=AMnnACF5vH7RxyrB&k1=LZ5IBWVWHzXe3c8a"));
        startActivity(intent);
        finish();
    }
}
