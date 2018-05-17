/**
 * 本例用于演示与HTTP与服务器交互的四种常用方法：GET(查询)、POST(新增)、PUT(修改)、DELETE(删除)的主控程序
 * 1）用三种不同方式实现上述访问：HttpUrlConnection、OkHttp、Retrofit
 * 2）用三种不同数据格式进行客户端和服务器端的交互：FormUrlEncoded、Json、XML
 * 3）同步网络访问需放在线程中调用，异步网络访问可以直接调用，访问结果通过Handler传递给主线程
 * 4）通过超时设置（连接超时、读超时、写超时）和服务器响应代码来判断服务器连接是否正常
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:HttpDemo
 * <br/>Date:May，2018
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.http;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

public class DataFragment extends Fragment {

    final static String TAG = "http";
    private TextView txv_servermessage;
    private EditText edt_url;
    private InputMethodManager mInputMethodManager;
    private int httpMethod, httpType;
    private String httpUrl;
    private MyHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
        mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data, container, false);
        httpType = 1;
        final Button btn_action = view.findViewById(R.id.btn_action);
        final EditText edt_username = view.findViewById(R.id.edt_username);
        final EditText edt_password = view.findViewById(R.id.edt_password);
        txv_servermessage = view.findViewById(R.id.txv_servermessage);
        edt_url = getActivity().findViewById(R.id.edt_url);

        RadioGroup radiogrp_httpmethod = getActivity().findViewById(R.id.rdg_httptype);
        switch (radiogrp_httpmethod.getCheckedRadioButtonId()) {
            case R.id.rdo_urlconnection:
                httpMethod = 1;
                break;
            case R.id.rdo_okhttp:
                httpMethod = 2;
                break;
            case R.id.rdo_retrofit:
                httpMethod = 3;
                break;
        }
        radiogrp_httpmethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                edt_url.clearFocus();
                switch (checkedId) {
                    case R.id.rdo_urlconnection:
                        httpMethod = 1;
                        break;
                    case R.id.rdo_okhttp:
                        httpMethod = 2;
                        break;
                    case R.id.rdo_retrofit:
                        httpMethod = 3;
                        break;
                }
            }
        });

        RadioGroup radiogrp_httpType = view.findViewById(R.id.rdg_http);
        radiogrp_httpType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                edt_url.clearFocus();
                switch (checkedId) {
                    case R.id.rdo_get:
                        httpType = 1;
                        btn_action.setText("登    录");
                        break;
                    case R.id.rdo_post:
                        httpType = 2;
                        btn_action.setText("注    册");
                        break;
                    case R.id.rdo_put:
                        httpType = 3;
                        btn_action.setText("修改密码");
                        break;
                    case R.id.rdo_delete:
                        httpType = 4;
                        btn_action.setText("删除账户");
                        break;
                }
            }
        });
        btn_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = edt_username.getText().toString();
                final String password = edt_password.getText().toString();
                if (mInputMethodManager.isActive()) {
                    mInputMethodManager.hideSoftInputFromWindow(edt_url.getWindowToken(), 0);// 隐藏输入法
                }
                httpUrl = edt_url.getText().toString();

                if (!httpUrl.equals("") && !username.equals("") && !password.equals("")) {
                    //准备各种格式输入数据
                    //FormUrlEncoded格式，服务器直接通过request.getParameter()获取，适用于url地址上放数据和格式为application/x-www-form-urlencoded的Body数据
                    final String paramQueryString = "user=" + username + "&password=" + password;
                    //Json格式，服务器通过.request.getReader()获取.适用于Body中放Json数据或一般文本数据
                    final String paramJson = "{user:" + username + ",password:" + password + "}";
                    //XML格式，服务器通过request.getInputStream()获取.适用于Body中放XML数据或二进制数据
                    StringBuilder sb = new StringBuilder();
                    sb.append("<request>");
                    sb.append("<account>");
                    sb.append("<user>" + username + "</user>");
                    sb.append("<password>" + password + "</password>");
                    sb.append("</account>");
                    sb.append("</request>");
                    final String paramXml = sb.toString();
                    switch (httpMethod) {
                        //HttpURLConnectio同步访问
                        case 1:
                            new Thread() {
                                @Override
                                public void run() {
                                    String result = "";
                                    switch (httpType) {
                                        case 1:
                                            result = HttpURLConnectionDemo.sendGet(httpUrl, paramQueryString);
                                            break;
                                        case 2:
                                            result = HttpURLConnectionDemo.sendPost(httpUrl, paramQueryString);
                                            break;
                                        case 3:
                                            result = HttpURLConnectionDemo.sendPut(httpUrl, paramJson);
                                            break;
                                        case 4:
                                            result = HttpURLConnectionDemo.sendDelete(httpUrl, paramXml);
                                            break;
                                    }
                                    Message msg = mHandler.obtainMessage();
                                    msg.what = httpType;
                                    msg.obj = result;
                                    mHandler.sendMessage(msg);
                                }
                            }.start();
                            break;
                        //OkHttp同步访问
                        case 2:
                            new Thread() {
                                @Override
                                public void run() {
                                    String result = "";
                                    switch (httpType) {
                                        case 1:
                                            result = OKHttpDemo.okHttp(1, httpUrl, paramQueryString, null);
                                            break;
                                        case 2:
                                            result = OKHttpDemo.okHttp(2, httpUrl, paramQueryString, null);
                                            break;
                                        case 3:
                                            result = OKHttpDemo.okHttp(3, httpUrl, paramJson, null);
                                            break;
                                        case 4:
                                            result = OKHttpDemo.okHttp(4, httpUrl, paramXml, null);
                                            break;
                                    }
                                    Message msg = mHandler.obtainMessage();
                                    msg.what = httpType;
                                    msg.obj = result;
                                    mHandler.sendMessage(msg);
                                }
                            }.start();
                            break;
                       //Retrofit异步访问
                        case 3:
                                 switch (httpType) {
                                    case 1:
                                        RetrofitDemo.retrofitHttp(1, httpUrl, paramQueryString, mHandler);
                                        break;
                                    case 2:
                                        RetrofitDemo.retrofitHttp(2, httpUrl, paramQueryString, mHandler);
                                        break;
                                    case 3:
                                        RetrofitDemo.retrofitHttp(3, httpUrl, paramJson, mHandler);
                                        break;
                                    case 4:
                                        RetrofitDemo.retrofitHttp(4, httpUrl, paramXml, mHandler);
                                        break;
                                }
                         break;
                    }
                } else {
                    txv_servermessage.setText("url地址/用户名/密码不能是空的\n");
                }
            }
        });
        return view;
    }

    //获取Http访问服务器返回的各种状态和数据，并进行相应处理
    private static class MyHandler extends Handler {
        WeakReference<DataFragment> fragment;

        private MyHandler(DataFragment thisFragment) {
            fragment = new WeakReference<>(thisFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            DataFragment thisFragment = fragment.get();
            super.handleMessage(msg);
            if (thisFragment != null) {
                Log.i(TAG, "handleMessage: " + msg.what);
                thisFragment.txv_servermessage.append(msg.obj.toString() + "\n");
            }
        }
    }
}
