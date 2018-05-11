/**
 * 本例演示了与Udp Socket Server的各种交互访：
 * 1）用Http Get启动Udp Socket Server
 * 2）绑定UdpService以便使用其各种Udp服务：启动、发送、接收、停止等
 *   发送"stop"：停止服务器udp socket服务
 * 3）解绑UdpService并停止Udp服务
 * 4）用Handler接收UdpService回传的状态信息和数据
 * 5）用
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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UdpFragment extends Fragment {

    final static String TAG = "UdpSocket";
    private static final int UDPCONNECTION = 3;
    private Button bt_send, bt_start, bt_bind, bt_unbind;
    private TextView tv_receive;
    private EditText et_send, et_url, et_server_port, et_client_port;

    private UdpService.UdpServiceBinder udpServiceBinder;
    private static MyHandler myHandler;
    private InputMethodManager mInputMethodManager;
    private boolean isServiceBinded, willDetroyed;
    private String host_addr;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        willDetroyed = false;
        View view = inflater.inflate(R.layout.fragment_udp, container, false);
        //初始化输入法
        if (getActivity() != null) {
            Util.allowMulticast(getActivity());
            mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        Button bt_save = view.findViewById(R.id.bt_save);

        bt_start = view.findViewById(R.id.bt_start);
        bt_bind = view.findViewById(R.id.bt_bind);
        bt_unbind = view.findViewById(R.id.bt_unbind);
        bt_send = view.findViewById(R.id.bt_send);
        tv_receive = view.findViewById(R.id.tv_receive);
        et_url = view.findViewById(R.id.murl);
        et_server_port = view.findViewById(R.id.serverport);
        et_client_port = view.findViewById(R.id.clientport);
        et_send = view.findViewById(R.id.et_send);
        setButtonsEnable(bt_save, bt_start);
        setButtonsDisable(bt_send, bt_bind, bt_unbind);

        //设置TextView可滚动
        tv_receive.setMovementMethod(ScrollingMovementMethod.getInstance());

        //从SharedPreferces中获取地址信息
        String udpAddr = Util.getAddr(getContext(), UDPCONNECTION);
        String[] addr = udpAddr.split("&");

        //从url中析取Host地址
        try {
            URI uri = new URI(addr[0]);
            host_addr = uri.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            host_addr = "localhost";
        }
        et_url.setText(addr[0]);
        et_server_port.setText(addr[1]);
        et_client_port.setText(addr[2]);

        myHandler = new MyHandler(this);

        //用Http Get启动Udp Socket Server
        bt_start.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                et_url.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
                if (mInputMethodManager.isActive()) {
                    mInputMethodManager.hideSoftInputFromWindow(et_url.getWindowToken(), 0);// 隐藏输入法
                }
                new Thread() {
                    @Override
                    public void run() {
                        //启动Udp Server
                        String result = "";
                        StringBuilder rs = new StringBuilder();
                        BufferedReader in = null;
                        try {
                            URL realUrl = new URL(et_url.getText().toString());
                            // String sid=getSessionID(realUrl);
                            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
                            connection.setRequestMethod("GET");
                            //  connection.setRequestProperty("cookie", sid);
                            connection.connect();
                            //  Log.i(TAG, "connect http: "+sid);
                            //服务器正常响应
                            if (connection.getResponseCode() == 200) {

                                //读取服务器响应到result中
                                in = new BufferedReader(
                                        new InputStreamReader(connection.getInputStream()));
                                String line;
                                while ((line = in.readLine()) != null) {
                                    rs.append(line);
                                }
                                result = rs.toString();
                            }
                        } catch (
                                MalformedURLException e1) {
                            e1.printStackTrace();
                            result = "Udp 服务异常";
                        } catch (IOException e2) {
                            Log.e(TAG, "发送GET请求出现异常！" + e2);
                            e2.printStackTrace();
                            result = "Udp Serve启动异常";
                        }
                        // 使用finally块来关闭输入流
                        finally {
                            try {
                                if (in != null) {
                                    in.close();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                        Log.i(TAG, result);
                        if (result.equals("Udp Server Start!"))
                            //启动正常
                            myHandler.sendEmptyMessage(2);
                        else if (result.equals("Udp Server has been started!")) {
                            myHandler.sendEmptyMessage(9);
                        }
                        //启动异常
                        else myHandler.sendEmptyMessage(0xE1);
                    }
                }.start();
            }
        });

        //绑定UdpService
        bt_bind.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //启动UdpService
                Intent intent = new Intent(getContext(), UdpService.class);
                if (getActivity() != null)
                    getActivity().bindService(intent, conn, Service.BIND_AUTO_CREATE);
            }
        });

        //解绑UdpService
        bt_unbind.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //关闭WUdpService
                if (isServiceBinded) {
                    isServiceBinded = false;
                    udpServiceBinder.stopUdpService();
                    if (getActivity() != null) {
                        getActivity().unbindService(conn);
                    }
                }

            }
        });

        //发送udp socket数据
        bt_send.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String msgSend = et_send.getText().toString();
                if (!msgSend.equals("")) {
                    //websocket发送信息给服务器
                    udpServiceBinder.sendMessage(msgSend);
                    et_send.setText("");
                }
            }
        });

        //保存Socket地址信息到SharedPrefernces中
        bt_save.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //设置输入框不可聚焦，即失去焦点和光标，以便更新host内容
                et_url.setFocusable(false);
                //收起软键盘
                if (mInputMethodManager.isActive()) {
                    mInputMethodManager.hideSoftInputFromWindow(et_url.getWindowToken(), 0);// 隐藏输入法
                }
                //存储地址信息
                Util.saveAddr(getContext(), UDPCONNECTION, et_url.getText().toString() + "&" + et_server_port.getText().toString() + "&" + et_client_port.getText().toString());

                Toast.makeText(getContext(), "Socket地址信息保存成功", Toast.LENGTH_LONG).show();
            }
        });

        //点击后获取焦点，弹出输入法
        et_url.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) v;
                et.setFocusable(true);//设置输入框可聚集
                et.setFocusableInTouchMode(true);//设置触摸聚焦
                et.requestFocus();//请求焦点
                et.findFocus();//获取焦点
                mInputMethodManager.showSoftInput(et, InputMethodManager.SHOW_FORCED);// 显示输入法
            }
        });

        //失去焦点后从中析取并更新et_host值
        et_url.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    try {
                        URI uri = new URI(((EditText) v).getText().toString());
                        host_addr = uri.getHost();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        host_addr = "localhost";
                    }
                }
            }
        });
        return view;
    }

    //当Fragment销毁时,该方法被回调
    @Override
    public void onDestroy() {
        willDetroyed = true;
        if (isServiceBinded) {
            udpServiceBinder.stopUdpService();
            if (getActivity() != null)
                getActivity().unbindService(conn);
        }
        super.onDestroy();
        Log.d(TAG, "----onDestroy----");
    }

    //UdpService绑定后调用
    private ServiceConnection conn = new ServiceConnection() {
        //当UdpService连接成功时调用
        @Override
        public void onServiceConnected(ComponentName name
                , IBinder service) {
            Log.i(TAG, "UdpService connected success");
            isServiceBinded = true;
            setButtonsEnable(bt_unbind, bt_send);
            setButtonsDisable(bt_start, bt_bind);

            udpServiceBinder = (UdpService.UdpServiceBinder) service;
            //设置Socket地址
            udpServiceBinder.setSocketAddres(host_addr, Integer.valueOf(et_server_port.getText().toString()), Integer.valueOf(et_client_port.getText().toString()));
            //设置获取信息的Handler
            udpServiceBinder.setHandler(myHandler);
            //启动Udp连接、数据读取、心跳包等
            try {
                udpServiceBinder.startUdpService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //当UdpService断开连接时调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBinded = false;
            setButtonsDisable(bt_unbind, bt_send);
            setButtonsEnable(bt_bind);

            Log.i(TAG, "UdpService disconnected");
        }
    };

    //获取UdpService的各种状态和返回数据，并进行相应处理
    private static class MyHandler extends Handler {
        WeakReference<UdpFragment> fragment;

        private MyHandler(UdpFragment udpFragment) {
            fragment = new WeakReference<>(udpFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            UdpFragment udpFragment = fragment.get();
            super.handleMessage(msg);
            if (udpFragment != null) {
                Log.i(TAG, "handleMessage: " + msg.what);
                switch (msg.what) {
                    //Udp服务启动成功
                    case 0:
                        udpFragment.setButtonsEnable(udpFragment.bt_unbind, udpFragment.bt_send);
                        udpFragment.setButtonsDisable(udpFragment.bt_bind, udpFragment.bt_start);
                        udpFragment.tv_receive.append("UdpService启动成功\n");
                        break;
                    //Udp服务器已启动，可直接使用
                    case 9:
                        udpFragment.setButtonsEnable(udpFragment.bt_bind);
                        udpFragment.setButtonsDisable(udpFragment.bt_start);
                        udpFragment.tv_receive.append("Udp Server已启动，可直接使用！\n");
                        break;
                    //Udp Socket接收服务器数据
                    case 1:
                        if (msg.obj != null) {
                            udpFragment.tv_receive.append(msg.obj.toString() + "\n");
                            Util.refreshLongText(udpFragment.tv_receive);
                        }
                        break;
                    //Udp Server启动成功
                    case 2:
                        udpFragment.isServiceBinded = true;
                        udpFragment.setButtonsEnable(udpFragment.bt_bind);
                        udpFragment.setButtonsDisable(udpFragment.bt_start);
                        udpFragment.tv_receive.setText("Udp Server 启动成功\n");
                        break;
                    //UdpService解除绑定
                    case 10:
                        if (!udpFragment.willDetroyed) {
                            udpFragment.setButtonsEnable(udpFragment.bt_bind);
                            udpFragment.setButtonsDisable(udpFragment.bt_unbind, udpFragment.bt_send);
                            udpFragment.tv_receive.append("UdpService解除绑定\n");
                        }
                        udpFragment.isServiceBinded = false;
                        break;

                    //Udp Server启动失败
                    case 0xE1:
                        udpFragment.tv_receive.append("Udp Server 启动失败\n");
                        break;

                    //Udp Socket数据接收失败
                    case 0xE3:
                        udpFragment.tv_receive.append("Udp Socket数据接收失败\n");
                        break;

                    //Udp Socket数据接收失败
                    case 0xE4:
                        //Udp Server关闭
                        udpFragment.setButtonsEnable(udpFragment.bt_start);
                        udpFragment.setButtonsDisable(udpFragment.bt_bind, udpFragment.bt_unbind, udpFragment.bt_send);
                        udpFragment.tv_receive.append("Udp Server关闭\n");
                        if (udpFragment.getActivity() != null) {
                            udpFragment.getActivity().unbindService(udpFragment.conn);
                            udpFragment.isServiceBinded = false;
                        }
                        break;
                }

            }
        }
    }

    void setButtonsEnable(Button... bts) {
        for (Button bt : bts) {
            bt.setEnabled(true);
            bt.setTextColor(getResources().getColor(R.color.white));
            bt.setBackgroundColor(getResources().getColor(R.color.dkcolor));
        }
    }

    void setButtonsDisable(Button... bts) {
        for (Button bt : bts) {
            bt.setEnabled(false);
            bt.setTextColor(getResources().getColor(R.color.textgrey));
            bt.setBackgroundColor(getResources().getColor(R.color.dkcolor));
        }
    }
}
