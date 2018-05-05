package org.xottys.network.http;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String url="http://192.168.1.8:8080/abc/login?user=admin&pass=amin";
       new Thread() {
           @Override
           public void run(){

        HttpURLConnectionDemo.sendGet(url);
           }
       }.start();

    }
}
