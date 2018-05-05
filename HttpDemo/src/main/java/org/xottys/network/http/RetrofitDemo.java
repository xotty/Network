/**
 * Retrofit的各种访问方式：GET、POST、PUT、DELETE的共同规范如下：
 * 1)定义接口，其中包含访问方式、子url、Call方法（传递给服务器的数据和服务器返回数据的类型）
 * 2)构建Retrofit， retrofit = new Retrofit.Builder()
 *                            .baseUrl(url)
 *                            .addConverterFactory(ScalarsConverterFactory.create())
 *                            .build();
 * 3）将构建的retrofit与接口关联，并将要传给服务器的数据通过接口Call方法传入其中
 * 4）Call方法启动，接收服务器响应
 * 同步：response = call.execute().body();
 * 异步：call.enqueue(new Callback<T>() {
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

import static android.content.ContentValues.TAG;


interface GetService {
    //@GET表示请求方式,其参数为baseURL后的组成部分
    @GET("httpDemo/myServlet")

    //@Query相当于name=?&age=?
    Call<String> getDemo(@Query("name") String name, @Query("age") int age);
}

interface PostService {


    //@POST表示请求方式，其参数为baseURL后的组成部分
    @POST("httpDemo/myServlet")

    //提交psot数据可以用下列表单的方式传递键值对@Field或@FieldMap
    @FormUrlEncoded
    Call<String> postDemo(@Field("name") String name, @Field("age") int age);

    //也可以直接放在body中,但需要服务有器配套的解析服务并将客户端retrofit Convertor配置为gson
    //Call <String> postDemo(@Body Person person );


}

interface PutService {

    //@PUT表示请求方式，其参数为baseURL后的组成部分
    @PUT("httpDemo/myServlet")
    //其中<Person>表示可以直接返回数据对象，在body中装入json字符串
    Call<Person> putDemo(@Body RequestBody bdy);

    /*json数据也可以直接封装成java数据对象后传给服务器，如Person
     Call <Person> putDemo(@Body Person person);*/

}

interface DeleteService {
    //@DELETE表示请求方式，其参数为baseURL后的组成部分
    @DELETE("httpDemo/myServlet")
    Call<String> deleteDemo(@Query("name") String name);
}

interface DownloadService {
    @GET("httpDemo/updownLoad")
    Call<ResponseBody> downloadFile(@Query("filename") String filename);
}

interface UploadService {
    @PUT("httpDemo/updownLoad")
    Call<ResponseBody> uploadFileWithRequestBody(@Query("filename") String filename, @Body RequestBody body);

    /**
     * 通过 MultipartBody和@body作为参数来上传
     *
     * @param multipartBody MultipartBody可能包含多个Part
     * @return 状态信息
     */
    @POST("httpDemo/updownLoad")
    Call<ResponseBody> uploadFileWithMultiPartBody(@Body MultipartBody multipartBody);

    /**
     * @param part 每个part代表一个文件
     * @return 状态信息
     */
    @Multipart
    @POST("httpDemo/updownLoad")
    Call<ResponseBody> uploadFileWithPart(@Part() MultipartBody.Part part);


}

public class RetrofitDemo {

    public static String retrofitHttp(String url, final int method, final Handler handler) {
        final String TAG = "HttpDemo";
        Retrofit retrofit;
        Call<String> stringCall = null;
        Call<Person> personCall = null;

        String result = "";
        switch (method) {
            //提交Get请求
            case 1:
                retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();
                GetService service1 = retrofit.create(GetService.class);
                stringCall = service1.getDemo("张三", 12);
                break;
            //提交Post请求
            case 2:
                retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();
                PostService service2 = retrofit.create(PostService.class);
                stringCall = service2.postDemo("李四", 20);
                break;
            //提交Put请求
            case 3:
                retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                PutService service3 = retrofit.create(PutService.class);
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject("{'name':'王五','age':'25'}");
                    ;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
                personCall = service3.putDemo(body);

                /*另一种json数据传递方法：用Gson给Person赋值后传递
                Person person = (Person)new Gson().fromJson(jsonObject.toString(),Person.class);
                personCall = service3.putDemo(person);*/
                break;
            //提交Delete请求
            case 4:
                retrofit = new Retrofit.Builder()
                        .baseUrl(url)
                        .addConverterFactory(ScalarsConverterFactory.create())
                        .build();
                DeleteService service4 = retrofit.create(DeleteService.class);
                stringCall = service4.deleteDemo("赵六");
                break;
            case 5:
                startDownload(url, "d4.mp3", handler);
                break;
            case 6:
                startUpload(1, url, "u1.jpg", handler);
                waitAmoment();
                startUpload(2, url, "u2.doc", handler);
                waitAmoment();
                startUpload(3, url, "u3.pdf", handler);
                break;
        }
        try {
            switch (method) {
                //返回普通的String
                case 1:
                case 2:
                case 4: {
                    //同步返回服务器结果的接收和处理
                    if (handler == null)
                        result = stringCall.execute().body();
                        //异步返回服务器结果的接收和处理
                    else {
                        stringCall.enqueue(new Callback<String>() {
                            //返回成功
                            @Override
                            public void onResponse(
                                    Call<String> call, Response<String> response) {
                                Message msg = Message.obtain();
                                msg.what = method;
                                msg.obj = response.body();
                                handler.sendMessage(msg);
                            }

                            //返回失败
                            @Override
                            public void onFailure(Call<String> call, Throwable t) {
                                t.printStackTrace();
                            }
                        });
                    }
                }
                break;
                //将返回的json数据直接解析到java数据对象Person中
                case 3: {
                    //同步返回服务器结果的接收和处理
                    if (handler == null)
                        try {
                            Person person = (Person) personCall.execute().body();
                            result = new Gson().toJson(person);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    else {
                        personCall.enqueue(new Callback<Person>() {
                            //返回成功
                            @Override
                            public void onResponse(
                                    Call<Person> call, Response<Person> response) {
                                Person person = (Person) response.body();

                                Message msg = Message.obtain();
                                msg.what = method;
                                msg.obj = new Gson().toJson(person);
                                handler.sendMessage(msg);
                            }

                            //返回失败
                            @Override
                            public void onFailure(Call<Person> call, Throwable t) {
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
    private static void startDownload(String downloadUrl, final String filename, final Handler handler) {

        //构建Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(downloadUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        //关联Retrofit
        DownloadService downloadService = retrofit.create(DownloadService.class);
        Call<ResponseBody> dowloadCall = downloadService.downloadFile(filename);
        //执行Retrofit，响应结果处理与OKHttp基本相同
        dowloadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                File sdDir = null;
                boolean sdCardExist = Environment.getExternalStorageState()
                        .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
                if (sdCardExist) {
                    sdDir = Environment.getExternalStorageDirectory();//获取跟目录
                }

                final File file = new File(sdDir.toString() + "/" + filename);
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
                        System.out.println("Retrofit下载了-------> " + current * 100 / total +
                                "%\n");
                    }
                    fos.flush();
                    Message msg = Message.obtain();
                    msg.what = 0x05;
                    msg.obj = "Retrofit下载成功:" + filename;
                    handler.sendMessage(msg);
                    Log.e(TAG, "Retrofit下载成功");
                } catch (IOException e) {
                    Message msg = Message.obtain();
                    msg.what = 0x05;
                    msg.obj = "Retrofit下载失败！";
                    handler.sendMessage(msg);
                    Log.e(TAG, e.toString());
                    Log.e(TAG, "Retrofit下载失败");
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

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Message msg = Message.obtain();
                msg.what = 0x05;
                msg.obj = "Retrofit下载失败！";
                handler.sendMessage(msg);
                Log.e(TAG, "Retrofit下载失败");
                t.printStackTrace();
            }
        });
    }

    //Retrofit上传文件
    private static void startUpload(final int method, String uploadUrl, String filename, final Handler handler) {
        Call<ResponseBody> uploadCall = null;
        //将要上传的文件关联到file
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED); //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory(); //获取跟目录
        }
        final File file = new File(sdDir.toString() + "/" + filename);

        //构建Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(uploadUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        UploadService uploadService = retrofit.create(UploadService.class);

        //用file构建RequestBody
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody filebody = RequestBody.create(mediaType, file);

        switch (method) {
            //直接将file构建的requetBody传入接口Call方法
            case 1:
                uploadCall = uploadService.uploadFileWithRequestBody(filename, filebody);
                break;
            //将file构建的requetBody进一步构建成MultipartBody，然后将其传入接口Call方法
            case 2:
                MultipartBody multipartBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filename, filebody)
                        .build();

                uploadCall = uploadService.uploadFileWithMultiPartBody(multipartBody);
                break;
            //将file构建的requetBody进一步构建成MultipartBody.Part，然后将其传入接口Call方法
            case 3:
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, filebody);
                uploadCall = uploadService.uploadFileWithPart(part);
                break;
        }
        //发送服务器访问请求，异步接收服务器响应
        uploadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                //处理服务器正常响应的情况
                if (response.isSuccessful()) {
                    String responseString = "";
                    try {
                        responseString = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Message msg = Message.obtain();
                    msg.what = 0x06;
                    msg.arg1 = method;
                    msg.obj = responseString;
                    handler.sendMessage(msg);

                    Log.i(TAG, "Retrofit上传成功，" + responseString);
                } else {
                    Message msg = Message.obtain();
                    msg.what = 0x06;
                    msg.arg1 = method;
                    msg.obj = "Retrofit上传失败";
                    handler.sendMessage(msg);
                    Log.e(TAG, "Retrofit上传失败");
                }
            }
            //处理服务器异常响应的情况
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Message msg = Message.obtain();
                msg.what = 0x06;
                msg.arg1 = method;
                msg.obj = "Retrofit上传失败";
                handler.sendMessage(msg);
                Log.e(TAG, "Retrofit上传失败");
                t.printStackTrace();
            }
        });

    }

    private static void waitAmoment() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}