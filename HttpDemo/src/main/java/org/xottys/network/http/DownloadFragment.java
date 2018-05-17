/**
 * 本例用于演示与HTTP从服务器下载文件方法，通过GET提交下载文件名
 * 1）用三种不同方式实现上述功能：HttpUrlConnection、OkHttp、Retrofit
 * 2）需要获取本地存储读写权限，下载文件放在/AndroidDemo/download目录下
 * 3）HttpHttpUrlConnection被封装在AsyncTask中，另两种方式直接使用异步响应
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

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
public class DownloadFragment extends Fragment {
    private static final String TAG = "http";
    private InputMethodManager mInputMethodManager;
    private int httpMethod;
    private String httpUrl;
    static private MyHandler mHandler;
    private TextView txv_servermessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            mHandler = new MyHandler(this);
            mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        Button btn_download = view.findViewById(R.id.btn_download);
        final EditText edt_filename = view.findViewById(R.id.edt_filename);
        txv_servermessage = view.findViewById(R.id.txv_servermessage);
        final EditText edt_url = getActivity().findViewById(R.id.edt_url);
        //获取http的访问方式
        RadioGroup radiogrp_http = getActivity().findViewById(R.id.rdg_httptype);
        //从Activity 传入的方式
        switch (radiogrp_http.getCheckedRadioButtonId()) {
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
        //本Fragment中变更后的方式
        radiogrp_http.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                edt_url.clearFocus();
                edt_filename.clearFocus();
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

        //发送下载请求
        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String filename = edt_filename.getText().toString();
                httpUrl = edt_url.getText().toString();
                if (!filename.equals("")) {
                    switch (httpMethod) {
                        //同步转异步
                        case 1:
                            //AsyncTask封装是ttpURLConnection变为异步访问的主要方法
                            HttpUrlDownload myAsyncTask = new HttpUrlDownload();
                            myAsyncTask.execute(httpUrl, filename);

                            /*传统线程封装,但仍然是同步调用
                           new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    result=HttpURLConnectionDemo.downloadFile(httpUrl, filename, mHandler);

                            }.start();*/
                            break;
                        //异步
                        case 2:
                            OKHttpDemo.okHttpDownload(httpUrl, filename, mHandler);
                            break;
                        //异步
                        case 3:
                            RetrofitDemo.startDownload(httpUrl, filename, mHandler);

                            break;
                    }
                } else {
                    txv_servermessage.setText("下载文件名不能是空的\n");
                }
            }
        });

        //url点击后获取焦点，弹出输入法
        edt_filename.setOnClickListener(new View.OnClickListener() {
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

        //url失去焦点后从关闭软键盘
        edt_filename.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //收起软键盘
                    if (mInputMethodManager!=null&&mInputMethodManager.isActive()) {
                        mInputMethodManager.hideSoftInputFromWindow(edt_url.getWindowToken(), 0);// 隐藏输入法
                    }

                }
            }
        });
        return view;
    }

    //获取http服务器的各种状态和返回数据，并进行相应处理
    private static class MyHandler extends Handler {
        WeakReference<DownloadFragment> fragment;

        private MyHandler(DownloadFragment thisFragment) {
            fragment = new WeakReference<>(thisFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            DownloadFragment thisFragment = fragment.get();
            super.handleMessage(msg);
            if (thisFragment != null) {
                Log.i(TAG, "handleMessage: " + msg.what);
                thisFragment.txv_servermessage.append(msg.obj.toString() + "\n");
            }
        }
    }

    static class HttpUrlDownload extends AsyncTask<String,Integer,String> {
        @Override
        protected String doInBackground(String ...params ) {
            return HttpURLConnectionDemo.downloadFile(params[0], params[1]);
        }

        @Override
        protected void onPostExecute(String result) {
            Message message=Message.obtain();
            message.what=5;
            message.obj=result;
            mHandler.sendMessage(message);
            super.onPostExecute(result);
        }

    }

}
