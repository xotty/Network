/**
 * 本例为Socket演示主控程序：
 * 1）Tcp Socket
 * 2）Udp Socket

 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:HttpDemo
 * <br/>Date:July，2017
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.socket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {
    final static String TAG = "Socket";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((RadioButton)findViewById(R.id.udp)).setTextColor(getResources().getColor(R.color.textgrey));
        tcpProcess();
        RadioGroup radio_tcp_udp = findViewById(R.id.radio_tcp_udp);
        radio_tcp_udp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //根据checkedId获取选中项RadioButton的实例
                RadioButton rb =  findViewById(checkedId);
                RadioButton arb;
                rb.setTextColor(getResources().getColor(R.color.white));
                switch (checkedId) {
                    case R.id.tcp:
                        //显示选中项RadioButton的的文本
                        arb =  findViewById(R.id.udp);
                        arb.setTextColor(getResources().getColor(R.color.textgrey));
                        tcpProcess();
                        break;
                    case R.id.udp:
                        arb =  findViewById(R.id.tcp);
                        arb.setTextColor(getResources().getColor(R.color.textgrey));
                        udpProcess();
                        break;
                }

            }
        });
    }

    private void tcpProcess() {
        TcpFragment fragment = new TcpFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.socketcontent, fragment).commit();
    }

    private void udpProcess() {
        UdpFragment fragment = new UdpFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.socketcontent, fragment).commit();
    }
}
