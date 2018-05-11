/**
 * 本例演示了WebSocket的一般访问流程：
 * 1）建立连接(bt1操作）
 * 2）发送消息(bt_send操作）
 * 3）接收消息(数据接收区显示，通过service的回调方法将服务器数据传进来）
 * 4）关闭连接(bt2操作）
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:HttpDemo
 * <br/>Date:July，2017
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.websocket;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    final static String TAG = "WebSocketDemo";
    private static final int WSCONNECTION = 4;
    private Button bt1, bt2, bt_send,bt_save;
    private TextView tv_receive;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0) {
            //Websocket服务器返回结果UI显示
            String stringFromServer = msg.getData().getString("MSG");
            tv_receive.append(stringFromServer + "\n");
             Util.refreshLongText(tv_receive);
            Log.i(TAG, "Websocket收到服务器返回的消息：" + stringFromServer);
            }
            else
            //连接失败，报错
            Toast.makeText(MainActivity.this,"Websocket连接失败",Toast.LENGTH_LONG).show();
        }
    };

    //websocket通信地址
    private WebSocketService webSocketService;
    private ServiceConnection conn = new ServiceConnection() {
        //当WebSocketService连接成功时调用该方法
        @Override
        public void onServiceConnected(ComponentName name
                , IBinder service) {
            Log.i(TAG, "WebSocket Connected: Success");
            WebSocketService.WebSocketServiceBinder webSocketServiceBinder = (WebSocketService.WebSocketServiceBinder) service;
            webSocketService = webSocketServiceBinder.getWebSocketService();
            bt_send.setTextColor(0xFFFFFFFF);
            bt_send.setEnabled(true);
            bt1.setTextColor(0xFFA0A0A0);
            bt1.setBackgroundColor(0xbd292f34);
            bt1.setEnabled(false);

            bt2.setTextColor(0xFFFFFFFF);
            bt2.setEnabled(true);
            //利用回调传递WebSocketService的服务器响应结果
            webSocketService.setCallBack(new WebSocketService.CallBack() {
                @Override
                public void onReceivedMessage(String msg) {
                    Message message = new Message();
                    message.what=0;
                    Bundle bundle = new Bundle();
                    bundle.putString("MSG", msg);
                    message.setData(bundle);


                    //传递消息给UI
                    handler.sendMessage(message);
                }
                 @Override
                 public void onError(String msg) {
                     Log.i(TAG, "Websocket Error:"+msg);
                     unbindService(conn);
                     handler.sendEmptyMessage(1);
                     webSocketService.closeWebSocket();}
            });


        }
        //当WebSocketService连接失败时调用该方法
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "--Service Disconnected--");
        }
    };
    private EditText et_send,et_addr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt1 =  findViewById(R.id.bt1);
        bt2 =  findViewById(R.id.bt2);
        Button bt_save =  findViewById(R.id.bt_save);
        bt_send =  findViewById(R.id.bt_send);
        tv_receive =  findViewById(R.id.tv_receive);
        et_addr =  findViewById(R.id.et_addr);
        et_send =  findViewById(R.id.et_send);

        bt1.setBackgroundColor(0xbd292f34);
        bt1.setTextColor(0xFFFFFFFF);

        bt2.setBackgroundColor(0xbd292f34);
        bt2.setTextColor(0xFFA0A0A0);
        bt2.setEnabled(false);

        bt_save.setBackgroundColor(0xbd292f34);
        bt_save.setTextColor(0xFFFFFFFF);
        bt_send.setBackgroundColor(0xbd292f34);
        bt_send.setTextColor(0xFFA0A0A0);
        bt_send.setEnabled(false);
        String websocketAddr=Util.getAddr(this,WSCONNECTION );
        et_addr.setText(websocketAddr);

        tv_receive.setMovementMethod(ScrollingMovementMethod.getInstance());

        //连接Websocket
        bt1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //启动WebSocketService，将IP地址等信息传入Service
                Intent intent = new Intent(MainActivity.this, WebSocketService.class);

                intent.putExtra("WSADDR", et_addr.getText().toString());
                bindService(intent, conn, Service.BIND_AUTO_CREATE);
            }
        });

        //断开Websocket
        bt2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //关闭WebSocketService
                webSocketService.closeWebSocket();
                unbindService(conn);

                bt1.setTextColor(0xFFFFFFFF);
                bt2.setTextColor(0xFFA0A0A0);
                bt1.setEnabled(true);
                bt2.setEnabled(false);
                bt_send.setTextColor(0xFFA0A0A0);
                bt_send.setEnabled(false);
                et_send.setText("");
            }
        });

        //发送数据
        bt_send.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String msgSend = et_send.getText().toString();
                if (!msgSend.equals("")) {
                    //websocket发送信息给服务器
                    webSocketService.sendMessage(msgSend);
                    et_send.setText("");
                }
            }
        });

        bt_save.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
               Util.saveAddr(MainActivity.this,WSCONNECTION ,et_addr.getText().toString());
               Toast.makeText(MainActivity.this,"Websocket地址保存成功",Toast.LENGTH_LONG).show();
            }
        });
    }

}



