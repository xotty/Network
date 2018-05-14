package org.xottys.network.http;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "http";
    private static final int HTTPDATA = 0;
    private static final int HTTPFILE = 1;
    EditText edt_url;
    String url;
    TextView txv_serverResponse;
    Fragment fragment;
    int httpMethod,httpType;
    private InputMethodManager mInputMethodManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        verifyStoragePermissions(this);
        edt_url=findViewById(R.id.edt_url);
        //从SharedPreferces中获取url信息
        url = Util.getAddr(this, HTTPDATA);
        edt_url.setText(url);
        Button btn_save = findViewById(R.id.btn_save);
        //保存Socket地址信息到SharedPrefernces中
        btn_save.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                //设置输入框不可聚焦，即失去焦点和光标，以便更新host内容
                edt_url.setFocusable(false);
                //收起软键盘
                if (mInputMethodManager.isActive()) {
                    mInputMethodManager.hideSoftInputFromWindow(edt_url.getWindowToken(), 0);// 隐藏输入法
                }
                //存储地址信息
                Util.saveAddr(MainActivity.this, httpType, edt_url.getText().toString());
                if (txv_serverResponse!=null) txv_serverResponse.setText("");
                Toast.makeText(MainActivity.this, "Url地址信息保存成功", Toast.LENGTH_LONG).show();

                //清空服务器返回信息
                if(fragment.getView()!=null){
                txv_serverResponse=fragment.getView().findViewById(R.id.txv_servermessage);
                txv_serverResponse.setText("");
                }
            }
        });

        httpMethod=1;
        httpdataProcess();

        RadioGroup radiogrp_action = findViewById(R.id.rdg_actiontype);
        radiogrp_action.check(R.id.rdo_data);
        radiogrp_action.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //根据checkedId获取选中项RadioButton的实例
                switch (checkedId) {
                    case R.id.rdo_data:
                        url = Util.getAddr(MainActivity.this, HTTPDATA);
                        edt_url.setText(url);
                        httpType=HTTPDATA;
                        httpdataProcess();
                        break;
                    case R.id.rdo_download:
                        url = Util.getAddr(MainActivity.this, HTTPFILE);
                        edt_url.setText(url);
                        httpType=HTTPFILE;
                        downloadProcess();
                        break;
                    case R.id.rdo_upload:
                        url = Util.getAddr(MainActivity.this, HTTPFILE);
                        edt_url.setText(url);
                        httpType=HTTPFILE;
                        uploadProcess();
                        break;
                }

            }
        });

        RadioGroup radiogrp_http = findViewById(R.id.rdg_httptype);
        radiogrp_http.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
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

        //点击后获取焦点，弹出输入法
        edt_url.setOnClickListener(new View.OnClickListener() {
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
        edt_url.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
    }

    private void httpdataProcess() {
       fragment = new DataFragment();
       getFragmentManager().beginTransaction()
                .replace(R.id.fml_httpcontent, fragment).commit();
    }

    private void downloadProcess() {
        fragment = DownloadFragment.newInstance(httpMethod,url);
        getFragmentManager().beginTransaction()
                .replace(R.id.fml_httpcontent, fragment).commit();

    }
    private void uploadProcess() {
       fragment =  UploadFragment.newInstance(httpMethod,url);
        getFragmentManager().beginTransaction()
                .replace(R.id.fml_httpcontent, fragment).commit();

    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };


    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
