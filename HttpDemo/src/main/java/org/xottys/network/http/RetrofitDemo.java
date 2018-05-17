/**
 * Retrofit是一种常用的同步/异步访问Http服务器的第三方框架，支持各种访问方式：GET、POST、PUT、DELETE的共同规范如下：
 * 1)定义接口，其中包含访问方式、子url、Call方法（传递给服务器的数据和服务器返回数据的类型）
 * 2)构建Retrofit， retrofit = new Retrofit.Builder()
 * .baseUrl(url)
 * .addConverterFactory(ScalarsConverterFactory.create())
 * .build();
 * 3）将构建的retrofit与接口关联，并将要传给服务器的数据通过接口Call方法传入其中
 * 4）Call方法启动，接收服务器响应
 * 同步：response = call.execute().body();
 * 异步：call.enqueue(new Callback<T>() {
 *
 * @Override public void onResponse(Call<T> call, Response<T> response) throws IOException {}
 * @Override public void onFailure(Call<T> call, Throwable t) {}
 * }
 * Retrofit文件下载，就是用GET方法访问服务器，将服务器返回的response.body().byteStream()用文件输出流方式写成本地文件
 * Retrofit文件上传，就是用POST或PUT方法访问服务器,用文件构建RequestBody，在接口Call方法中将其传递进去
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:RetrofitDemo
 * <br/>Date:July，2017
 * @author xottys@163.com
 * @version 1.0
 */

package org.xottys.network.http;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;


interface GetService {
    //@GET表示请求方式,其参数为baseURL后的组成部分
    @GET
    //@Query相当于user=?&password=?
    Call<String> getDemo(@Url String urlPath, @Query("user") String name, @Query("password") String password);
}

interface PostService {


    //@POST表示请求方式，其参数为baseURL后的组成部分
    @POST

    //提交psot数据可以用下列表单的方式传递键值对@Field或@FieldMap
    @FormUrlEncoded
    Call<String> postDemo(@Url String urlPath,@Field("user") String name, @Field("password") String password);

    //也可以直接放在body中,但需要服务有器配套的解析服务并将客户端retrofit Convertor配置为gson
    //Call <String> postDemo(@Body Account account );


}

interface PutService {
    //Content-Type也可以在代码中添加，Coverter会自动添加相应Content-Type
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @PUT

    Call<JsonObject> putDemo(@Url String urlPath,@Body JsonObject object);
   
     /*替代方法，此时在构造RequestBody时要用Json Media-Type
     Call<JsonObject> putDemo(@Body RequestBody body);*/
}

interface DeleteService {
    //@DELETE表示请求方式，其参数为baseURL后的组成部分
    @HTTP(method = "DELETE", hasBody = true)
    Call<SimpleXmlResult> deleteDemo(@Url String urlPath,@Body RequestBody body);
}

interface DownloadService {
    //BaseUrl将会引入，与updown和filename=？一起组成正式的访问url
    @GET
    Call<ResponseBody> downloadFile(@Url String urlPath,@Query("filename") String filename);

    /* 可替换方法，此时BaseUrl将不会引入
    @GET
    Call<ResponseBody> downloadFile(@Url String fileUrl);*/
}

interface UploadService {
    @PUT
    Call<ResponseBody> uploadFileWithRequestBody(@Url String urlPath,@Query("filename") String filename, @Body RequestBody body);

    /**
     * 通过 MultipartBody和@body作为参数来上传
     *
     * @param multipartBody MultipartBody可能包含多个Part
     * @return 状态信息
     */
    @HTTP(method = "DELETE", hasBody = true)
    Call<ResponseBody> uploadFileWithMultiPartBody(@Url String urlPath,@Body MultipartBody multipartBody);

    /**
     * @param part 每个part代表一个文件
     * @return 状态信息
     */
    @Multipart
    @POST
    Call<ResponseBody> uploadFileWithPart(@Url String urlPath,@Part() MultipartBody.Part part);


}

public class RetrofitDemo {
    private static final String TAG = "http";
    static private String methodName = "";
    //设置个性化超时的client，缺省为10s
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder().
            connectTimeout(3, TimeUnit.SECONDS).
            readTimeout(3, TimeUnit.SECONDS).
            writeTimeout(5, TimeUnit.SECONDS).build();

    public static String retrofitHttp(final int method, String url, String param, final Handler handler) {
        final String TAG = "HttpDemo";
        Retrofit retrofit;
        Call<String> stringCall = null;
        Call<JsonObject> jsonObjectCall = null;
        Call<SimpleXmlResult> xmlCall=null;
        final SimpleXmlResult simpleXmlResult;
        HashMap<String,String> urlmap=Util.decodeUrl(url);
        String baseUrl="http://"+urlmap.get("BaseUrl")+"/";
        String urlPath=urlmap.get("PathUrl");
        String result = "";
        HashMap<String, String> accounts = Util.decodeUrlQueryString(param);

        switch (method) {
            //提交Get请求
            case 1:

                retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();
                GetService service1 = retrofit.create(GetService.class);
                stringCall = service1.getDemo(urlPath,accounts.get("user"), accounts.get("password"));
                break;
            //提交Post请求
            case 2:
                retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();
                PostService service2 = retrofit.create(PostService.class);
                stringCall = service2.postDemo(urlPath,accounts.get("user"), accounts.get("password"));
                break;
            //提交Put请求
            case 3:

                retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                PutService service3 = retrofit.create(PutService.class);
                JsonObject jsonObject = null;
                try {
                    jsonObject = new Gson().fromJson(param, JsonObject.class);
                } catch (JsonParseException e) {
                    e.printStackTrace();
                }
               jsonObjectCall = service3.putDemo(urlPath,jsonObject);
               /* 也可以用RequestBody做参数
                RequestBody jsonBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
                jsonObjectCall = service3.putDemo(jsonBody);*/
               

                break;
            //提交Delete请求
            case 4:
                retrofit = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(SimpleXmlConverterFactory.create())
                        .build();

                RequestBody xmlBody = RequestBody.create(okhttp3.MediaType.parse("application/xml; charset=utf-8"), param);
                DeleteService service4 = retrofit.create(DeleteService.class);
                xmlCall= service4.deleteDemo(urlPath,xmlBody);
                break;
        }
        try {
            switch (method) {
                //返回普通的String
                case 1:
                case 2:
                    if (handler == null){
                        result = stringCall.execute().body();}
                    //异步返回服务器结果的接收和处理
                    else {
                        stringCall.enqueue(new Callback<String>() {
                            //返回成功
                            @Override
                            public void onResponse(
                                    Call<String> call, Response<String> response) {
                                Message msg = Message.obtain();
                                String result;
                                msg.what = 1;
                                if (!response.isSuccessful()) {
                                    result = "Retrofit服务器连接故障2";
                                    msg.arg1 = 2;
                                    Log.e(TAG, "Retrofit服务器连接故障2");
                                } else {
                                    msg.arg1 = 0;
                                result=response.body();
                                }
                                msg.obj = result;
                                handler.sendMessage(msg);
                            }

                            //返回失败
                            @Override
                            public void onFailure(Call<String > call, Throwable t) {
                                Message msg = Message.obtain();
                                msg.what = 1;
                                msg.arg1 = 1;
                                msg.obj = "Retrofit服务器连接故障1";
                                handler.sendMessage(msg);
                                Log.e(TAG, "Retrofit服务器连接故障1");
                                t.printStackTrace();
                            }
                        });
                    }
                    break;
                case 4: {
                    //同步返回服务器结果的接收和处理
                    if (handler == null){
                        simpleXmlResult = xmlCall.execute().body();
                        result= simpleXmlResult.getInfo();}
                    //异步返回服务器结果的接收和处理
                    else {
                        xmlCall.enqueue(new Callback<SimpleXmlResult>() {
                            //返回成功
                            @Override
                            public void onResponse(
                                    Call<SimpleXmlResult> call, Response<SimpleXmlResult> response) {
                                Message msg = Message.obtain();
                                String result;
                                msg.what = 1;
                                if (!response.isSuccessful()) {
                                    result = "Retrofit服务器连接故障2";
                                    msg.arg1 = 2;
                                    Log.e(TAG, "Retrofit服务器连接故障2");
                                } else {
                                    SimpleXmlResult accr = response.body();
                                    msg.arg1 = 0;
                                    result = accr.getInfo();
                                }
                                msg.obj = result;
                                handler.sendMessage(msg); }

                            //返回失败
                            @Override
                            public void onFailure(Call<SimpleXmlResult> call, Throwable t) {
                                Message msg = Message.obtain();
                                msg.what = 1;
                                msg.arg1 = 1;
                                msg.obj = "Retrofit服务器连接故障1";
                                handler.sendMessage(msg);
                                Log.e(TAG, "Retrofit服务器连接故障1");
                                t.printStackTrace();
                            }
                        });
                    }
                }
                break;
                //将返回的json数据直接解析到java数据对象Account中
                case 3: {
                    //同步返回服务器结果的接收和处理
                    if (handler == null)
                        try {
                            JsonObject jsonObject = jsonObjectCall.execute().body();
                            result =  jsonObject.get("message") + "\n" + jsonObject.toString();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    else {
                        jsonObjectCall.enqueue(new Callback<JsonObject>() {

                            //返回成功
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                Message msg = Message.obtain();
                                String result;
                                msg.what = 1;
                                if (!response.isSuccessful()) {
                                    result = "Retrofit服务器连接故障2";
                                    msg.arg1 = 2;
                                    Log.e(TAG, "Retrofit服务器连接故障2");
                                } else {
                                JsonObject jsonObject = response.body();
                                    result = jsonObject.get("message") + "\n" + jsonObject.toString();
                                    msg.arg1 = 0;
                                }
                                msg.obj=result;
                                handler.sendMessage(msg);
                            }

                            //返回失败
                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                Message msg = Message.obtain();
                                msg.what = 1;
                                msg.arg1 = 1;
                                msg.obj = "Retrofit服务器连接故障1";
                                handler.sendMessage(msg);
                                Log.e(TAG, "Retrofit服务器连接故障1");
                                t.printStackTrace();
                            }
                        });
                    }
                }
                break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, ((handler == null) ? "同步" : "异步") + "Retrofit--" + method + "完成:" + result);
        return result;
    }

    //Retrofit下载文件
    public static void startDownload(String downloadUrl, final String filename, final Handler handler) {
        HashMap<String,String> urlmap=Util.decodeUrl(downloadUrl);
        String baseUrl="http://"+urlmap.get("BaseUrl")+"/";
        String urlPath=urlmap.get("PathUrl");
        //构建Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(okHttpClient)
                .build();
        //关联Retrofit
        DownloadService downloadService = retrofit.create(DownloadService.class);
        Call<ResponseBody> dowloadCall = downloadService.downloadFile(urlPath,filename);

        /*对应可替换方法，url要全部手动注入
        Call<ResponseBody> dowloadCall = downloadService.downloadFile(downloadUrl+ "?filename=" + filename);*/

        //执行Retrofit，响应结果处理与OKHttp基本相同
        dowloadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                Message msg = Message.obtain();
                String result;
                msg.what = 5;
                if (!response.isSuccessful()) {
                    result = "Retrofit服务器连接故障2";
                    msg.arg1 = 2;
                    msg.obj = result;
                    Log.e(TAG, "Retrofit服务器连接故障2");
                } else {
                    try {
                        result = URLDecoder.decode(response.headers().get("result"), "UTF-8");
                        if (result.equals("下载成功")) {
                            File sdDir = null;
                            boolean sdCardExist = Environment.getExternalStorageState()
                                    .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
                            if (sdCardExist) {
                                sdDir = Environment.getExternalStorageDirectory();//获取跟目录
                            }else
                            {
                                sdDir = Environment.getDataDirectory();
                            }

                            final File file = new File(sdDir.toString() + "/" + filename);
                            InputStream is ;
                            byte[] buf = new byte[2048];
                            int len;
                            FileOutputStream fos ;

                            long total = response.body().contentLength();
                            long current = 0;
                            is = response.body().byteStream();
                            fos = new FileOutputStream(file);
                            while ((len = is.read(buf)) != -1) {
                                current += len;
                                fos.write(buf, 0, len);
                                Log.i(TAG, "Retrofit下载了-------> " + current * 100 / total + "%\n");
                            }
                            fos.flush();
                            msg.arg1 = 0;
                            msg.obj = "Retrofit" + result + filename;
                            Log.e(TAG, "Retrofit下载成功");

                            try {
                                is.close();
                                fos.close();
                            } catch (IOException e) {
                                Log.e(TAG, e.toString());
                            }
                        } else {
                            //将下载结果发送给UI线程
                            msg.arg1 = 4;
                            msg.obj = "Retrofit" + result + ":" + filename;
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        msg.obj = "Retrofit下载失败2！";
                        msg.arg1 = 3;
                        Log.e(TAG, "Retrofit下载失败2");
                    }
                }
                handler.sendMessage(msg);

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Message msg = Message.obtain();
                msg.what = 5;
                msg.arg1 = 1;
                msg.obj = "Retrofit服务器连接故障1";
                handler.sendMessage(msg);
                Log.e(TAG, "Retrofit服务器连接故障1");
                t.printStackTrace();
            }
        });
    }

    //Retrofit上传文件
    public static void startUpload(final int method, String uploadUrl, String filepath, final Handler handler) {
        HashMap<String,String> urlmap=Util.decodeUrl(uploadUrl);
        String baseUrl="http://"+urlmap.get("BaseUrl")+"/";
        String urlPath=urlmap.get("PathUrl");
        Call<ResponseBody> uploadCall = null;
        File file = new File(filepath);
        final String filename = filepath.substring(filepath.lastIndexOf("/") + 1);

        //构建Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(okHttpClient)
                .build();
        UploadService uploadService = retrofit.create(UploadService.class);

        //用file构建RequestBody
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody filebody = RequestBody.create(mediaType, file);

        switch (method) {
            //PUT,直接将file构建的requetBody传入接口Call方法
            case 1:
                methodName = "octet-stream";
                uploadCall = uploadService.uploadFileWithRequestBody(urlPath,filename, filebody);
                break;
            //POST,将file构建的requetBody进一步构建成MultipartBody，然后将其传入接口Call方法
            case 2:
                methodName = "multipart(servlet3.0";
                MultipartBody multipartBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filename, filebody)
                        .build();

                uploadCall = uploadService.uploadFileWithMultiPartBody(urlPath,multipartBody);
                break;
            //DELETE,将file构建的requetBody进一步构建成MultipartBody.Part，然后将其传入接口Call方法
            case 3:
                methodName = "multipart(commons-fileupload";
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, filebody);
                uploadCall = uploadService.uploadFileWithPart(urlPath,part);
                break;
        }
        //发送服务器访问请求，异步接收服务器响应
        uploadCall.enqueue(new Callback<ResponseBody>() {
            Message msg = Message.obtain();

            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                String result = "";
                msg = handler.obtainMessage();
                msg.what = 6;
                //处理服务器正常响应的情况
                if (response.isSuccessful()) {
                    try {
                        result = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (result.equals("上传成功")) {
                        msg.arg1 = 0;
                        msg.obj = "Retrofit/" + methodName + result + ":" + filename;
                        Log.i(TAG, "Retrofit上传成功");
                    } else {
                        msg.arg1 = 4;
                        msg.obj = "Retrofit" + methodName + result + ":" + filename;
                    }
                } else {
                    result = "Retrofit服务器连接故障2";
                    msg.arg1 = 2;
                    msg.obj = result;
                    Log.e(TAG, "Retrofit服务器连接故障2");
                }
                handler.sendMessage(msg);
            }

            //处理服务器异常响应的情况
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                msg = handler.obtainMessage();
                msg.what = 6;
                msg.arg1 = 1;
                msg.obj = "Retrofit服务器连接故障1！";
                handler.sendMessage(msg);
                Log.e(TAG, "Retrofit服务器连接故障1");
                t.printStackTrace();
            }
        });

    }
}