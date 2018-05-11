/**
 * 本例演示了与Tcp Socket Server的各种交互访：
 * 1）用Http Get启动Tcp Socket Server
 * 2）绑定TcpService以便使用其各种Tcp服务：连接、发送、接收、断连等
 *    发送"end"：关闭服务器socket连接，"stop"：停止服务器socket服务
 * 3）解绑TcpService以断开Tcp连接
 * 4）用Handler接收TcpService回传的状态信息和数据
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

public class TcpFragment extends Fragment {

    final static String TAG = "TcpSocket";
    private static final int TCPCONNECTION = 2;
    private Button bt_connect, bt_disconnect, bt_send, bt_start;
    private TextView tv_receive;
    private EditText et_send, et_url, et_host, et_port;

    private TcpService.TcpServiceBinder tcpServiceBinder;
    private static MyHandler myHandler;
    private InputMethodManager mInputMethodManager;
    private boolean isServerStop,willDetroyed;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tcp, container, false);
        isServerStop=true;
        willDetroyed=false;
        //初始化输入法
        if(getActivity()!=null)
        mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        bt_connect = view.findViewById(R.id.bt_connect);
        bt_disconnect = view.findViewById(R.id.bt_disconnect);
        Button bt_save = view.findViewById(R.id.bt_save);
        bt_start = view.findViewById(R.id.bt_start);
        bt_send = view.findViewById(R.id.bt_send);
        tv_receive = view.findViewById(R.id.tv_receive);
        et_url = view.findViewById(R.id.murl);
        et_host = view.findViewById(R.id.mhost);
        et_port = view.findViewById(R.id.mport);
        et_send = view.findViewById(R.id.et_send);
        setButtonsEnable(bt_save, bt_start);
        setButtonsDisable(bt_connect, bt_disconnect, bt_send);

        //设置TextView可滚动
        tv_receive.setMovementMethod(ScrollingMovementMethod.getInstance());

        //从SharedPreferces中获取地址信息
        String tcpAddr = Util.getAddr(getContext(), TCPCONNECTION);
        String[] addr = tcpAddr.split("&");

        //从url中析取Host地址
        try {
            URI uri = new URI(addr[0]);
            et_host.setText(uri.getHost());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            et_host.setText("localhost");
        }
        et_url.setText(addr[0]);
        et_port.setText(addr[1]);

        myHandler = new MyHandler(this);

        //用Http Get启动Tcp Socket Server
        bt_start.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                et_url.setFocusable(false);//设置输入框不可聚焦，即失去焦点和光标
                if (mInputMethodManager.isActive()) {
                    mInputMethodManager.hideSoftInputFromWindow(et_url.getWindowToken(), 0);// 隐藏输入法
                }
                new Thread() {
                    @Override
                    public void run() {
                        //启动Socket Server
                        String result = "";
                        StringBuilder rs = new StringBuilder();
                        BufferedReader in = null;
                        try {
                            URL realUrl = new URL(et_url.getText().toString());
                            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
                            connection.connect();
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
                            result = "Tcp 服务异常";
                        } catch (IOException e2) {
                            Log.e(TAG, "发送GET请求出现异常！" + e2);
                            e2.printStackTrace();
                            result = "Tcp Serve启动异常";
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

                        if (result.equals("Tcp Server Start!"))
                            //启动正常
                            myHandler.sendEmptyMessage(2);
                        else if (result.equals("Tcp Server has been started!"))
                            myHandler.sendEmptyMessage(9);
                        else
                            //启动异常
                            myHandler.sendEmptyMessage(0xE1);
                    }

                }.start();
            }
        });

        //绑定TcpService
        bt_connect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //启动TcpService
                Intent intent = new Intent(getContext(), TcpService.class);
                if (getActivity() != null)
                    getActivity().bindService(intent, conn, Service.BIND_AUTO_CREATE);
            }
        });

        //断开socket，解绑TcpService
        bt_disconnect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //关闭TcpService
                tcpServiceBinder.stopTcpService();
                if (getActivity() != null)
                    getActivity().unbindService(conn);

            }
        });

        //发送socket数据
        bt_send.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String msgSend = et_send.getText().toString();
                if (!msgSend.equals("")) {
                    //websocket发送信息给服务器
                    tcpServiceBinder.sendMessage(msgSend);
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
                Util.saveAddr(getContext(), TCPCONNECTION, et_url.getText().toString() + "&" + et_port.getText().toString());

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
                        et_host.setText(uri.getHost());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        et_host.setText("localhost");
                    }
                }
            }
        });
        return view;
    }

    //TcpService绑定后调用
    private ServiceConnection conn = new ServiceConnection() {
        //当TcpService连接成功时调用
        @Override
        public void onServiceConnected(ComponentName name
                , IBinder service) {
            Log.i(TAG, "TcpService Connected Success");
            setButtonsEnable(bt_disconnect, bt_send);
            setButtonsDisable(bt_start, bt_connect);

            tcpServiceBinder = (TcpService.TcpServiceBinder) service;
            //设置Socket地址
            tcpServiceBinder.setSocketAddres(et_host.getText().toString(), Integer.valueOf(et_port.getText().toString()));
            //设置获取信息的Handler
            tcpServiceBinder.setHandler(myHandler);
            //启动Tcp连接、数据读取、心跳包等
            try {
                tcpServiceBinder.startTcpService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //当TcpService断开连接时调用
        @Override
        public void onServiceDisconnected(ComponentName name) {
            setButtonsDisable(bt_disconnect, bt_send);
            setButtonsEnable(bt_connect);

            Log.i(TAG, "TcpService DisConnected");
        }
    };

    //获取TcpService的各种状态和返回数据，并进行相应处理
    private static class MyHandler extends Handler {
        WeakReference<TcpFragment> fragment;

        private MyHandler(TcpFragment tcpFragment) {
            fragment = new WeakReference<>(tcpFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            TcpFragment tcpFragment = fragment.get();
            super.handleMessage(msg);
            if (tcpFragment != null) {
                Log.i(TAG, "handleMessage: "+msg.what);
                switch (msg.what) {
                    //Socket连接成功
                    case 0:
                        tcpFragment.setButtonsEnable(tcpFragment.bt_disconnect, tcpFragment.bt_send);
                        tcpFragment.setButtonsDisable(tcpFragment.bt_connect, tcpFragment.bt_start);
                        tcpFragment.tv_receive.append("Tcp Socket连接成功\n");
                        tcpFragment.isServerStop = false;
                        break;
                    //Socket接收服务器数据
                    case 1:
                        if (msg.obj != null) {
                            tcpFragment.tv_receive.append(msg.obj.toString() + "\n");
                            Util.refreshLongText(tcpFragment.tv_receive);
//                            if (msg.obj.toString().contains("准备关闭Tcp ServerSocket")) {
//                                tcpFragment.isServerStop = true;
//                            }
                        }
                        break;
                    //Tcp服务器已启动，可直接使用
                    case 9:
                        tcpFragment.setButtonsEnable(tcpFragment.bt_connect);
                        tcpFragment.setButtonsDisable(tcpFragment.bt_start);
                        tcpFragment.tv_receive.append("Tcp Server已启动，可直接使用！\n");
                        break;
                    //Tcp Server启动成功
                    case 2:
                        tcpFragment.isServerStop = false;
                        tcpFragment.setButtonsEnable(tcpFragment.bt_connect);
                        tcpFragment.setButtonsDisable(tcpFragment.bt_start);
                        tcpFragment.tv_receive.setText("Tcp Server 启动成功\n");
                        break;
                    //Socket客户端断开连接
                    case 10:
                        if (!tcpFragment.willDetroyed) {
                            tcpFragment.setButtonsEnable(tcpFragment.bt_connect);
                            tcpFragment.setButtonsDisable(tcpFragment.bt_disconnect, tcpFragment.bt_send);
                            tcpFragment.tv_receive.append("Tcp Socket客户端主动断开连接\n");
                            tcpFragment.isServerStop=true;
                        }
                        break;
                    //Socket连接失败
                    case 0xE0:
                        tcpFragment.tv_receive.append("Tcp Socket连接失败\n");
                        break;

                    //Tcp Server启动失败
                    case 0xE1:
                        tcpFragment.tv_receive.append("Tcp Server 启动失败\n");
                        break;
                    //Tcp Socket断连异常
                    case 0xE2:
                        tcpFragment.tv_receive.append("Tcp Socket连接断开出现异常\n");
                        break;

                    //Tcp Socket数据接收失败
                    case 0xE3:
                        tcpFragment.tv_receive.append("Tcp Socket数据接收失败\n");
                        break;

                    //Tcp Socket数据接收失败
                    case 0xE4:
                        //服务器关闭了Tcp Socket
                        if (!tcpFragment.willDetroyed) {
                            if (!tcpFragment.isServerStop) {
                                tcpFragment.setButtonsEnable(tcpFragment.bt_connect);
                                tcpFragment.setButtonsDisable(tcpFragment.bt_start, tcpFragment.bt_disconnect, tcpFragment.bt_send);
                                tcpFragment.tv_receive.append("Tcp Socket服务器主动断开连接\n");
                            }//Tcp Server关闭
                            else {
                                tcpFragment.setButtonsEnable(tcpFragment.bt_start);
                                tcpFragment.setButtonsDisable(tcpFragment.bt_connect, tcpFragment.bt_disconnect, tcpFragment.bt_send);
                                tcpFragment.tv_receive.append("Tcp Server关闭\n");
                            }
                            if (tcpFragment.getActivity() != null)
                                tcpFragment.getActivity().unbindService(tcpFragment.conn);
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
    //当Fragment销毁时,该方法被回调
    @Override
    public void onDestroy() {
        willDetroyed=true;
        if(!isServerStop){
            isServerStop=true;
        tcpServiceBinder.stopTcpService();
        if (getActivity()!=null)
        getActivity().unbindService(conn);}
        super.onDestroy();
        Log.d(TAG, "----TCP Fragment onDestroy----");
    }
}
