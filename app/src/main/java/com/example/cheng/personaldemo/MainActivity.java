package com.example.cheng.personaldemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NetworkUtil.getHostIP();
        NetworkUtil.getIp(this);
        NetworkUtil.getMac(this.getApplicationContext());
        NetworkUtil.getSsid(this.getApplicationContext());
    }
}
