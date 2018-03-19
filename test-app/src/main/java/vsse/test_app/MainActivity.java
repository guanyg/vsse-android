package vsse.test_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import vsse.proto.TestOuterClass;
import vsse.test.Device;
import vsse.test.client.Client;

import static android.os.Build.MODEL;
import static vsse.proto.TestOuterClass.TestRegRequest.DeviceType.ANDROID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle(MODEL);
        new Thread(() -> new Client(
                new Device(ANDROID, MODEL),
                "10.21.238.251",
                5678)).start();
    }
}
