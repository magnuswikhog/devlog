package com.magnuswikhog.devlogproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.magnuswikhog.devlog.DevLog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DevLog.remoteLoggingEnabled = false;
        DevLog.loggingEnabled = true;
        DevLog.tagPrefix = "DevLogProject ";
        DevLog.i("MainActivity", "Hello World!");
        // DevLog.storeRemoteLog(this); Use every now and then if remote logging is enabled, to send logs to server
    }
}
