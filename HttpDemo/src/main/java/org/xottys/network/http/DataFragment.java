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
package org.xottys.network.http;

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
import android.app.Fragment;
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

public class DataFragment extends Fragment {

    final static String TAG = "http";
    private static final int HTTPDATA = 0;
    private Button bt_connect, bt_disconnect, bt_send, bt_start;
    private TextView tv_receive;
    private EditText et_send, et_url, et_host, et_port;

    private InputMethodManager mInputMethodManager;
    private boolean isServerStop,willDetroyed;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data, container, false);

        return view;
    }

}
