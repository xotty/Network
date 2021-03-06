/**
 * OKHttp3是一种常用的同步/异步访问Http服务器的第三方框架，支持各种访问方式：GET、POST、PUT、DELETE的共同流程如下：
 * 1)OkHttpClient okHttpClient=new OkHttpClient()
 * 2)生成url、请求header和请求body的内容
 * 3)构造请求，request = new Request.Builder()
 * .url(url)
 * .addHeader("Charsert", "UTF-8")
 * .post(body)
 * .build();
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
 *
 * Cookie处理（这须使用OKHTTP自定义的类：CookieJar和Cookie）流程
 * 1）定义CookieJar，并覆写其中saveFromResponse和loadForRequest方法，前者直接得到服务器的Cookie，后者将客户端Cookie发送给服务器
 * 2）在OkHttpClient构造时将上述CookieJar放入
 * 3）在需要的地方使用Cookie结果
 *
 * Session 处理：与上述Cookie类似，只是从中单独识别和处理JSESSIONID=xxxx这样一条Cookie
 *
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OKHttpDemo {
    private static final String TAG = "http";
    private static OkHttpClient mOkHttpClient;
    static private Message msg = Message.obtain();
    static private String methodName = "";
    static private List<Cookie> myCookies;
    static private String myCookie = "", sessionID = "";

    //用OKHttp访问服务器，同步或异步接收服务器响应信息
    public static String okHttp(final int method, String url, String param, final Handler handler) {
        //创建OkHttpClient，设置
        // mOkHttpClient = new OkHttpClient();
        String result = "", resultCode, resultMsg = "";
        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)//设置读超时时间
                .writeTimeout(3, TimeUnit.SECONDS)//设置写超时时间
                .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                .build();
        Request request = null;
        RequestBody body ;
        Response mResponse;

        //OKHTTP专门用来保存和发送Cookie的
        CookieJar myCookieJar = new CookieJar() {
            //读取和处理服务器传来的Cookie
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                myCookies = cookies;
                myCookie = cookies.toString();
            }

            //向服务器发送Cookie
            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                ArrayList<Cookie> cookies = new ArrayList<>();
                if (myCookies != null) {
                    //OKHTTP的Cookie生成
                    Cookie cookie = new Cookie.Builder()
                            .hostOnlyDomain(url.host())
                            .name("company").value("CCTV")
                            .build();
                    cookies.add(cookie);
                    for (Cookie cook : myCookies) {
                        //Cookie没过期
                        if (System.currentTimeMillis() <= cook.expiresAt()) {
                            cookies.add(cook);
                        }
                    }
                }
                return cookies;
            }
        };

        //用来收发SessionID
        CookieJar mySessionJar = new CookieJar() {
            //读取和保存服务器传来的SessionID
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                for (Cookie cookie : cookies) {
                    if (cookie.name().equals("JSESSIONID")) {
                        sessionID = cookie.value();
                        Log.i(TAG, "Session Created in Server: " + cookie.name() + "/" + cookie.value());
                        break;
                    }
                }
            }

            //向服务器发送SessionID
            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                ArrayList<Cookie> cookies = new ArrayList<>();
                if (!sessionID.equals("")) {
                    Cookie cookie = new Cookie.Builder()
                            .hostOnlyDomain(url.host())
                            .name("JSESSIONID").value(sessionID)
                            .build();
                    cookies.add(cookie);
                }
                Log.i(TAG, "loadForRequest: " + cookies + "  sessionID:" + sessionID);
                return cookies;
            }
        };


        //创建请求Request，其中包含url、访问方式、header和body内容
        switch (method)

        {
            //GET:请求参数封装在url里
            case 1:
                String realUrl = url + "?" + param;
                request = new Request.Builder()
                        .url(realUrl)
                        .build();
                break;
            //POST:请求参数以formEncoding方式封装在body里
            case 2:
                HashMap<String, String> accounts = Util.decodeUrlQueryString(param);
                body = new FormBody.Builder()
                        .add("user", accounts.get("user"))
                        .add("password", accounts.get("password"))
                        .build();
                request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();
                break;
            //PUT:请求参数以json方式封装在body里
            case 3:
                MediaType JsonType = MediaType.parse("application/json;charset=utf-8");
                body = RequestBody.create(JsonType, param);
                request = new Request.Builder()
                        .url(url)
                        .put(body)
                        .build();
                break;
            //DELETE:请求参数封装在url里
            case 4:
                MediaType XMLType = MediaType.parse("application/xml;charset=utf-8");
                body = RequestBody.create(XMLType, param);

                request = new Request.Builder()
                        .url(url)
                        .delete(body)
                        .build();
                break;
        }

        Call call;
        if (method == 1)
        {   //在client 中放入myCookieJar
            OkHttpClient mClient1 = new OkHttpClient.Builder()
                    .readTimeout(3, TimeUnit.SECONDS)//设置读超时时间
                    .writeTimeout(3, TimeUnit.SECONDS)//设置写超时时间
                    .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                    .cookieJar(myCookieJar)
                    .build();

            call = mClient1.newCall(request);
        } else if (method == 3) {
            //在client 中放入mySessionJar
            OkHttpClient mClient2 = new OkHttpClient.Builder()
                    .readTimeout(3, TimeUnit.SECONDS)//设置读超时时间
                    .writeTimeout(3, TimeUnit.SECONDS)//设置写超时时间
                    .connectTimeout(3, TimeUnit.SECONDS)//设置连接超时时间
                    .cookieJar(mySessionJar)
                    .build();

            call = mClient2.newCall(request);
        } else

            call = mOkHttpClient.newCall(request);

        if (handler == null)

        {
            //发送请求，同步获取服务器响应
            try {
                mResponse = call.execute();
                if (mResponse.isSuccessful()) {
                    result = mResponse.body().string();

                    //可以直接这样获取服务器的Cookie
                    List<Cookie> cookies = Cookie.parseAll(HttpUrl.get(new URL(url)), mResponse.headers());
                    if(cookies!=null) Log.i(TAG, "okHttp cookie from server:"+cookies.toString());

                    if (method == 3) {
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            resultCode = jsonObject.getString("code");
                            resultMsg = jsonObject.getString("message");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //直接使用CookieJar处理Session的结果
                        resultMsg = resultMsg + "\n" + result + (sessionID.equals("") ? "" : "\nSessionID：" + sessionID);
                    } else if (method == 4) {
                        InputStream in = new ByteArrayInputStream(result.getBytes("UTF-8"));
                        ArrayList<HashMap> results = Util.readxmlByDom(in);
                        if (results.size() != 0) {
                            for (HashMap rs : results) {
                                result = rs.get("message").toString();
                            }
                        }
                        resultMsg = result + Util.xmlString;
                    } else if (method == 1) {
                        //直接使用CookieJar处理Cookie的结果
                        resultMsg = result + (myCookie.equals("") ? "" : "  Cookie From Server:\n" + myCookie);
                    } else
                        resultMsg = result;
                } else {
                    resultMsg = "OKHTTP服务器连接故障2";
                }
            } catch (IOException e) {
                resultMsg = "OKHttp服务器连接故障1";
                e.printStackTrace();
            }
            Log.i(TAG, "OKHttp: " + resultMsg);
        } else

        {
            //异步接收服务器响应信息
            call.enqueue(new okhttp3.Callback() {

                //访问失败返回的信息
                @Override
                public void onFailure(Call call, IOException e) {
                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.arg1 = 1;
                    msg.obj = "Retrofit服务器连接故障1";
                    handler.sendMessage(msg);
                    Log.e(TAG, "Retrofit服务器连接故障1");
                    e.printStackTrace();
                }

                //访问成功返回的信息
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String result = "", resultCode, resultMsg = "";
                    //服务器返回信息发送给UI线程
                    Message msg = Message.obtain();
                    msg.what = 1;
                    if (!response.isSuccessful()) {
                        result = "Retrofit服务器连接故障2";
                        msg.arg1 = 2;
                        Log.e(TAG, "Retrofit服务器连接故障2");
                    } else {
                        switch (method) {
                            //返回普通的String
                            case 1:
                            case 2:
                                String str;
                                if (null != response.cacheResponse()) {
                                    str = response.cacheResponse().toString();
                                    Log.i(TAG, "缓存获取---" + str);
                                } else {

                                    str = response.networkResponse().toString();
                                    Log.i(TAG, "服务器获取---" + str + "--" + method);
                                }
                                result = response.body().toString();
                                break;
                            case 3:
                                try {
                                    JSONObject jsonObject = new JSONObject(result);
                                    resultCode = jsonObject.getString("code");
                                    resultMsg = jsonObject.getString("message");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                result = resultMsg + "\n" + result;
                                break;
                            case 4:
                                InputStream in = new ByteArrayInputStream(result.getBytes("UTF-8"));
                                ArrayList<HashMap> results = Util.readxmlByDom(in);
                                if (results.size() != 0) {
                                    for (HashMap rs : results) {
                                        result = rs.get("message").toString();
                                    }
                                }
                                result = result + Util.xmlString;
                                break;
                        }
                        msg.arg1 = 0;
                    }
                    msg.obj = result;
                    handler.sendMessage(msg);
                    Log.i(TAG, "OKHttp服务器访问返回：" + result);
                }
            });

        }
        return resultMsg;
    }


    //okhttp文件下载
    public static void okHttpDownload(String url, final String filename, final Handler handler) {
        mOkHttpClient = new OkHttpClient();
        //用GET传递文件名方式访问服务器
        final Request request = new Request.Builder().url(url + "?filename=" + filename).build();
        final Call call = mOkHttpClient.newCall(request);
        msg.what = 0x05;
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                msg.arg1 = 1;
                msg.obj = "OKHttp服务器连接故障1！";
                handler.sendMessage(msg);
                Log.e(TAG, "OKHttp服务器连接故障1");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result;
                if (!response.isSuccessful()) {
                    result = "OKHTTP服务器连接故障2";
                    msg.arg1 = 2;
                    msg.obj = result;
                    Log.e(TAG, "OKHTTP服务器连接故障2");
                } else {
                    result = URLDecoder.decode(response.header("result"), "UTF-8");
                    if (result.equals("下载成功")) {
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
                        int len;
                        FileOutputStream fos = null;
                        try {
                            long total = response.body().contentLength();
                            long current = 0;
                            is = response.body().byteStream();
                            fos = new FileOutputStream(file);
                            while ((len = is.read(buf)) != -1) {
                                current += len;
                                fos.write(buf, 0, len);
                                Log.i(TAG, "OKHttp下载了-------> " + current * 100 / total + "%\n");
                            }
                            fos.flush();
                            //服务器返回信息发送给UI线程
                            msg.arg1 = 0;
                            msg.obj = "OKHTTP" + result + ":" + filename;
                            Log.i(TAG, "OKHTTP下载成功");

                        } catch (IOException e) {
                            msg.arg1 = 3;
                            msg.obj = "OKHTTP服务器数据解析异常，下载失败";
                            Log.e(TAG, "OKHTTP服务器数据解析异常,下载失败");
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
                    } else {
                        //将下载结果发送给UI线程
                        msg.arg1 = 4;
                        msg.obj = "OKHTTP" + result + ":" + filename;
                    }
                }
                handler.sendMessage(msg);
            }
        });
    }

    //okhttp文件上传,method=1:stream方式   2：multipart（servlet3.0）  3:multipart（commons-fileupload）
    public static void okHttpUpload(final int method, String url, String filepath,
                                    final Handler handler) {
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
//        Message msg = Message.obtain();
        msg.what = 0x06;

//        //将要上传的文件关联到file
//        File sdDir = null;
//        boolean sdCardExist = Environment.getExternalStorageState()
//                .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
//
//        if (sdCardExist) {
//            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
//        }
//        File file = new File(sdDir.toString() + "/" + filename);
        File file = new File(filepath);
        final String filename = filepath.substring(filepath.lastIndexOf("/") + 1);
        //用上述file创建RequestBody
        final MediaType MEDIA_OBJECT_STREAM = MediaType.parse("application/octet-stream");
        RequestBody filebody = RequestBody.create(MEDIA_OBJECT_STREAM, file);

        //用上述RequestBodye创建Request
        Request request = null;
        if (method == 1) {
            methodName = "octet-stream";
            request = new Request.Builder()
                    .url(url + "?filename=" + filename)
                    .put(filebody)
                    .build();
        } else if (method == 3) {
            methodName = "multipart(commons-fileupload";
            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", filename, filebody)
                    .build();
            request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
        } else if (method == 2) {
            methodName = "multipart(servlet3.0";
            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", filename, filebody)
                    .build();
            request = new Request.Builder()
                    .url(url)
                    .delete(body)
                    .build();
        }
        //发送服务器访问请求
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            //处理服务器无响应的情况
            @Override
            public void onFailure(Call call, IOException e) {
                msg = handler.obtainMessage();
                msg.what = 6;
                msg.arg1 = 1;
                msg.obj = "OKHttp服务器连接故障1！";
                handler.sendMessage(msg);
                Log.e(TAG, "OKHttp服务器连接故障1");
                e.printStackTrace();
            }

            //处理服务器有响应的情况
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result;
                msg = handler.obtainMessage();
                msg.what = 6;
                //处理服务器响应成功的情况
                if (response.isSuccessful()) {
                    result = response.body().string();
                    if (result.equals("上传成功")) {
                        msg.arg1 = 0;
                        msg.obj = "OKHTTP/" + methodName + result + ":" + filename;
                        Log.i(TAG, "OKHTTP上传成功");
                    } else {
                        //将下载结果发送给UI线程
                        msg.arg1 = 4;
                        msg.obj = "OKHTTP" + methodName + result + ":" + filename;
                    }
                    //处理服务器响应失败的情况
                } else {
                    result = "OKHTTP服务器连接故障2";
                    msg.arg1 = 2;
                    msg.obj = result;
                    Log.e(TAG, "OKHTTP服务器连接故障2");
                }
                handler.sendMessage(msg);
            }
        });
    }


}



