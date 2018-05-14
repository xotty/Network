package org.xottys.network.http;

import android.app.Fragment;
import android.content.Context;
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
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private InputMethodManager mInputMethodManager;
    private int httpMethod;
    private String httpUrl;
    private MyHandler mHandler;
    private TextView txv_servermessage;

    public DownloadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UploadFragment.
     */
    public static DownloadFragment newInstance(int param1, String param2) {
        DownloadFragment fragment = new DownloadFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            httpMethod = getArguments().getInt(ARG_PARAM1);
            httpUrl = getArguments().getString(ARG_PARAM2);
            mHandler=new MyHandler(this);
            mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        Button btn_download=view.findViewById(R.id.btn_download);
        final EditText edt_filename=view.findViewById(R.id. edt_filename);
        txv_servermessage=view.findViewById(R.id.txv_servermessage);
        final EditText edt_url =getActivity().findViewById(R.id.edt_url);
        RadioGroup radiogrp_http = getActivity().findViewById(R.id.rdg_httptype);
        radiogrp_http.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                edt_url.clearFocus();
                edt_filename.clearFocus();
                switch (checkedId) {
                    case R.id.rdo_urlconnection:
                        httpMethod=1;
                        break;
                    case R.id.rdo_okhttp:
                        httpMethod=2;
                        break;
                    case R.id.rdo_retrofit:
                        httpMethod=3;
                        break;
                }
            }
        });

        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String filename=edt_filename.getText().toString();
                httpUrl=edt_url.getText().toString();
                if (!filename.equals("")){
                   switch (httpMethod) {
                       case 1:
                           new Thread(){
                               @Override
                               public void run() {
                                   super.run();
                               HttpURLConnectionDemo.downloadFile(httpUrl,filename,mHandler);
                               }}.start();
                           break;
                       case 2:
                          OKHttpDemo.okHttpDownload(httpUrl,filename,mHandler);
                           break;
                       case 3:
                           try{
                           URL url = new URL(httpUrl);
                           String urlHost=url.getProtocol()+"://"+url.getHost()+":"+url.getPort();
                           RetrofitDemo.startDownload(urlHost,filename,mHandler);}
                           catch(MalformedURLException e){
                               e.printStackTrace();
                           }
                           break;
                   }
                    Log.i(TAG, "onClick: "+httpMethod+"--"+httpUrl+"---"+filename);
                }
              else{
                    txv_servermessage.setText("下载文件名不能是空的\n");
                }
            }
        });

        //点击后获取焦点，弹出输入法
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

        //失去焦点后从关闭软键盘
        edt_filename.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //收起软键盘
                    if (mInputMethodManager.isActive()) {
                        mInputMethodManager.hideSoftInputFromWindow(edt_url.getWindowToken(), 0);// 隐藏输入法
                    }

                }
            }
        });
        return view;
    }

    //获取TcpService的各种状态和返回数据，并进行相应处理
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
                switch (msg.what) {
                    case 5:
                        thisFragment.txv_servermessage.append(msg.obj.toString() + "\n");
                        break;
                }
            }
        }
    }
}
