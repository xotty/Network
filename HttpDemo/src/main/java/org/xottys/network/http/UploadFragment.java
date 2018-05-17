/**
 * 本例用于演示与HTTP向服务器上传文件方法，文件名可以放在url中、header中或multipart中
 * 1）用三种不同方式实现上述功能：HttpUrlConnection、OkHttp、Retrofit
 * 2）两种文件格式：application/octet-stream和multipart/form-data(有两种不同服务器处理方式)
 * 3）需要获取本地存储读写权限，上传文件放在服务器/upload目录下
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;


public class UploadFragment extends Fragment {

    private static final String TAG = "http";
    MyHandler mHandler;
    TextView txv_servermessage, txv_filepath;
    String path;
    private int httpMethod, uploadType;
    private String httpUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_upload, container, false);
        uploadType = 1;
        Button btn_upload = view.findViewById(R.id.btn_upload);
        Button btn_selectFile = view.findViewById(R.id.btn_selectfile);
        txv_filepath = view.findViewById(R.id.txv_filename);
        txv_servermessage = view.findViewById(R.id.txv_servermessage);
        final EditText edt_url = getActivity().findViewById(R.id.edt_url);

        //获取http的访问方式
        RadioGroup radiogrp_http = getActivity().findViewById(R.id.rdg_httptype);
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
        radiogrp_http.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
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

        //获取文件上传方式
        RadioGroup radiogrp_upload = view.findViewById(R.id.rdg_uploadtype);
        radiogrp_upload.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                edt_url.clearFocus();
                switch (checkedId) {
                    case R.id.rdo_octet:
                        uploadType = 1;
                        break;
                    case R.id.rdo_multipart_servlet:
                        uploadType = 2;
                        break;
                    case R.id.rdo_multipart_commons:
                        uploadType = 3;
                        break;

                }
            }
        });

        //从系统中选文件
        btn_selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("image/*"); //选择图片
                //intent.setType(“audio/*”); //选择音频
                //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
                //intent.setType(“video/*;image/*”);//同时选择视频和图片
                intent.setType("*/*");//无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                // startActivityForResult(intent, 1);
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 1);
            }
        });

        //发送上传请求
        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String filepath = txv_filepath.getText().toString();
                httpUrl = edt_url.getText().toString();
                if (!filepath.equals("") && !httpUrl.equals("")) {
                    switch (httpMethod) {
                        //同步请求封装在线程中使用
                        case 1:
                            new Thread() {
                                @Override
                                public void run() {
                                    //数据返回使用Handler直接给到UI线程
                                    switch (uploadType) {
                                        case 1:
                                            HttpURLConnectionDemo.uploadFileByStream(httpUrl, filepath, mHandler);
                                            break;
                                        case 2:
                                            HttpURLConnectionDemo.uploadFileByForm(httpUrl, filepath, mHandler, 1);
                                            break;
                                        case 3:
                                            HttpURLConnectionDemo.uploadFileByForm(httpUrl, filepath, mHandler, 0);
                                            break;
                                    }
                                }
                            }.start();
                            break;
                        //异步
                        case 2:
                            switch (uploadType) {
                                case 1:
                                    OKHttpDemo.okHttpUpload(1, httpUrl, filepath, mHandler);
                                    break;
                                case 2:
                                    OKHttpDemo.okHttpUpload(2, httpUrl, filepath, mHandler);
                                    break;
                                case 3:
                                    OKHttpDemo.okHttpUpload(3, httpUrl, filepath, mHandler);
                                    break;
                            }
                            break;
                       //异步
                        case 3:
                                 switch (uploadType) {
                                    case 1:
                                        RetrofitDemo.startUpload(1, httpUrl, filepath, mHandler);
                                        break;
                                    case 2:
                                        RetrofitDemo.startUpload(2, httpUrl, filepath, mHandler);
                                        break;
                                    case 3:
                                        RetrofitDemo.startUpload(3, httpUrl, filepath, mHandler);
                                        break;
                                }

                            break;
                    }
                } else {
                    txv_servermessage.setText("上传文件名和/或url地址不能是空的\n");
                }
            }
        });
        return view;
    }

    //获取Http服务器的各种状态和返回数据，并进行相应处理
    private static class MyHandler extends Handler {
        WeakReference<UploadFragment> fragment;

        private MyHandler(UploadFragment thisFragment) {
            fragment = new WeakReference<>(thisFragment);
        }
        @Override
        public void handleMessage(Message msg) {
            UploadFragment thisFragment = fragment.get();
            super.handleMessage(msg);
            if (thisFragment != null) {
                Log.i(TAG, "handleMessage: " + msg.what);
                thisFragment.txv_servermessage.append(msg.obj.toString() + "\n");
            }
        }
    }

    //选择手机文件后返回取得文件全路径名作为上传文件用
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                path = uri.getPath();
                return;
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                    path = getPath(getActivity(), uri);
                } else {//4.4以下下系统调用方法
                    path = getRealPathFromURI(uri);
                }
            }
            txv_filepath.setText(path);
            Log.i(TAG, "onActivityResult: " + path);
        }
    }
    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }
    //Android4.4以上版本从Uri获取文件绝对路径
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }
    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
