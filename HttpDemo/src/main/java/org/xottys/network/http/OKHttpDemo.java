/**
 * OKHttp3的各种访问方式：GET、POST、PUT、DELETE的共同流程如下：
 * 1)OkHttpClient okHttpClient=new OkHttpClient()
 * 2)生成url、请求header和请求body的内容
 * 3)构造请求，request = new Request.Builder()
 *                      .url(url)
 *                      .addHeader("Charsert", "UTF-8")
 *                      .post(body)
 *                      .build();
 * 4）发送访问请求给服务器并接收服务器响应
 * Call call = mOkHttpClient.newCall(request);
 * 同步：response = call.execute();
 * 异步：call.enqueue(new okhttp3.Callback() {
 *
 * @Override public void onResponse(Call call, Response response) throws IOException {}
 * @Override public void onFailure(Call call, IOException e) {}
 * }
 * OKHttp文件下载，就是用GET方法访问服务器，将服务器返回的response.body().byteStream()用文件输出流方式写成本地文件
 * OKHttp文件上传，就是用POST或PUT方法访问服务器,用文件构建RequestBody及Request
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:OKHttpDemo
 * <br/>Date:July，2017
 * @author xottys@163.com
 * @version 1.0
 */

package org.xottys.network.http;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OKHttpDemo {
    private static final String TAG = "OKHttpDemo";
    private static OkHttpClient mOkHttpClient;

    //用OKHttp访问服务器，同步接收服务器响应信息
    public static String syncHttp(String url, int method) {
        //创建OkHttpClient，设置
        // mOkHttpClient = new OkHttpClient();
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)//设置读超时时间
                .writeTimeout(20, TimeUnit.SECONDS)//设置写超时时间
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .build();
        Request request = null;
        RequestBody body = null;
        Response mResponse = null;

        //创建请求Request，其中包含url、访问方式、header和body内容
        switch (method) {
            //请求参数封装在url里
            case 1:
                request = new Request.Builder()
                        .url(url)
                        .build();
                break;
            //请求参数以formEncoding方式封装在body里
            case 2:
                body = new FormBody.Builder()
                        .add("name", "李四")
                        .add("age", "20")
                        .build();
                request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                break;
            //请求参数以json方式封装在body里
            case 3:
                MediaType JsonType = MediaType.parse("application/json");
                body = RequestBody.create(JsonType, "{'name':'王五','age':'25'}");
                request = new Request.Builder()
                        .url(url)
                        .put(body)
                        .build();
                break;
            //请求参数封装在url里
            case 4:
                request = new Request.Builder()
                        .url(url)
                        .delete()
                        .build();
                break;
        }
        //发送请求，同步获取服务器响应
        try {
            Call call = mOkHttpClient.newCall(request);
            mResponse = call.execute();
            if (mResponse.isSuccessful()) {
                if (method == 3) {
                    Log.i(TAG, "syncHttp: " + mResponse.body().contentType());
                    return mResponse.body().string();
                } else {
                    return mResponse.body().string();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    //用OKHttp访问服务器，异步接收服务器响应信息
    public static void asyncHttp(String url, final int method, final Handler handler) {
        mOkHttpClient = new OkHttpClient();
        Request request = null;
        RequestBody body = null;

        switch (method) {
            case 1:
                request = new Request.Builder()
                        .url(url)
                        .build();
                break;
            case 2:
                body = new FormBody.Builder()
                        .add("name", "李四")
                        .add("age", "32")
                        .build();
                request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                break;
            case 3:
                MediaType JsonType = MediaType.parse("application/json");
                body = RequestBody.create(JsonType, "{'name':'王五','age':'25'}");
                request = new Request.Builder()
                        .url(url)
                        .put(body)
                        .build();
                break;
            case 4:
                request = new Request.Builder()
                        .url(url)
                        .delete()
                        .build();
                break;
        }

        Call mcall = mOkHttpClient.newCall(request);
        //异步接收服务器响应信息
        mcall.enqueue(new okhttp3.Callback() {

            //访问失败返回的信息
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "OKHttp服务器访问失败");
            }

            //访问成功返回的信息
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String str;
                if (null != response.cacheResponse()) {
                    str = response.cacheResponse().toString();
                    Log.i(TAG, "缓存获取---" + str);
                } else {

                    str = response.networkResponse().toString();
                    Log.i(TAG, "服务器获取---" + str + "--" + method);
                }

                //服务器返回信息发送给UI线程
                Message msg = Message.obtain();
                msg.what = method;
                msg.obj = response.body().string();
                handler.sendMessage(msg);

                Log.i(TAG, "OKHttp服务器访问成功");
            }
        });
    }

    //okhttp文件下载
    public static void okHttpDownload(String url, final String filename, final Handler handler) {
        //用GET传递文件名方式访问服务器
        final Request request = new Request.Builder().url(url + "?filename=" + filename).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg = Message.obtain();
                msg.what = 0x05;
                msg.obj = "服务器访问异常,OKHttp下载失败：";
                handler.sendMessage(msg);
                Log.e(TAG, "服务器访问异常OKHttp下载失败");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //将要下载的文件定义到SDcard中
                File sdDir = null;
                boolean sdCardExist = Environment.getExternalStorageState()
                        .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
                mOkHttpClient = new OkHttpClient();
                if (sdCardExist) {
                    sdDir = Environment.getExternalStorageDirectory();//获取跟目录
                }
                final File file = new File(sdDir.toString() + "/" + filename);

                //从服务器获取response，读取其body().byteStream(),将其以文件输出流形式写到上面定义的文件中去
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    long total = response.body().contentLength();
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        System.out.println("OKHttp下载了-------> " + current * 100 / total +
                                "%\n");
                    }
                    fos.flush();
                    //服务器返回信息发送给UI线程
                    Message msg = Message.obtain();
                    msg.what = 0x05;
                    msg.obj = "OKHttp下载成功：" + filename;
                    handler.sendMessage(msg);

                    Log.i(TAG, "OKHttp下载成功");
                } catch (IOException e) {
                    Message msg = Message.obtain();
                    msg.what = 0x05;
                    msg.obj = "服务器数据解析异常,OKHttp下载失败：";
                    handler.sendMessage(msg);
                    Log.e(TAG, "服务器数据解析异常,OKHttp下载失败");
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    //okhttp文件上传
    public static void okHttpUpload(final int method, String url, String filename, final Handler handler) {
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .build();

        //将要上传的文件关联到file
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在

        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        File file = new File(sdDir.toString() + "/" + filename);

        //用上述file创建RequestBody
        final MediaType MEDIA_OBJECT_STREAM = MediaType.parse("application/octet-stream");
        RequestBody filebody = RequestBody.create(MEDIA_OBJECT_STREAM, file);

        //用上述RequestBodye创建Request
        Request request = null;
        if (method == 1) {
            request = new Request.Builder()
                    .url(url + "?filename=" + filename)
                    .put(filebody)
                    .build();
        } else if (method == 2) {
            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", filename, filebody)
                    .build();
            request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
        } else {
            return;
        }
        //发送服务器访问请求
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            //处理服务器无响应的情况
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg = Message.obtain();
                msg.what = 0x06;
                msg.arg1 = method;
                msg.obj = "服务器访问异常,OKHttp上传失败";
                handler.sendMessage(msg);
                Log.e(TAG, "服务器访问异常,OKHttp上传失败");
                e.printStackTrace();
            }

            //处理服务器有响应的情况
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //处理服务器响应成功的情况
                if (response.isSuccessful()) {
                    String responseString = response.body().string();
                    Message msg = Message.obtain();
                    msg.what = 0x06;
                    msg.arg1 = method;
                    msg.obj = responseString;
                    handler.sendMessage(msg);
                    Log.i(TAG, "OKHttp上传成功," + responseString);
                  //处理服务器响应失败的情况
                } else {
                    Message msg = Message.obtain();
                    msg.what = 0x06;
                    msg.arg1 = method;
                    msg.obj = "服务器响应失败,OKHttp上传失败";
                    handler.sendMessage(msg);
                    Log.e(TAG, "服务器响应失败,OKHttp上传失败");
                }
            }
        });
    }
}



