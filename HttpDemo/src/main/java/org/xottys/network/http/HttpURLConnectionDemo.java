/**
 * HttpURLConnection的各种访问方式：GET、POST、PUT、DELETE的共同流程如下：
 * 1)HttpURLConnection connection = (HttpURLConnection) url.openConnection();
 * 2)定义请求和响应的属性，此处务必要与服务器保持一致
 * 3）connection.connect();--可选
 * 4）connection.getOutputStream()发送数据给服务器
 * 5）connection.getInputStream()接收服务器响应
 * 只支持同步，异步可以与AsyncTask协同来完成
 * httpURLConnection文件下载，就是用GET方法将从服务器获得的网络输入流用文件输出流方式写成本地文件
 * httpURLConnection文件上传，就是用POST或PUT方法将本地文件作为文件输入流，读入后通过网络输出流发往服务器
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:HttpURLConnectionDemo
 * <br/>Date:July，2017
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.http;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class HttpURLConnectionDemo {
    private static final String TAG = "HttpDemo";
    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url 发送请求的URL
     * @return URL所代表远程资源的响应
     */
    public static String sendGet(String url) {
        String result = "";
        BufferedReader in = null;
        try {
            //String urlName = url + "?" + params;
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            //建立实际的连接
            connection.connect();
            if (connection.getResponseCode() == 200) {

                // 定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line + "\n";
                }
            }
        } catch (Exception e) {
            Log.e(TAG,"发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        Log.i(TAG, "sendGet: " + result);
        return result;
    }

    /**
     * 向指定URL发送POST方法的请求
     *
     * @param url    发送请求的URL
     * @param params 请求参数，请求参数应该是name1=value1&name2=value2的形式。
     * @return URL所代表远程资源的响应
     */
    public static String sendPost(String url, String params) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            //发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();
            //获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            //发送请求参数给服务器
            out.print(params);
            //flush输出流的缓冲
            out.flush();
            //定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line + "\n";
            }
        } catch (Exception e) {
            Log.e(TAG,"发送POST请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        Log.i(TAG, "sendPost: " + result);
        return result;
    }
    /**
     * 向指定URL发送PUT方法的请求
     *
     * @param url    发送请求的URL
     * @param params 请求参数，Json格式
     * @return URL所代表远程资源的响应
     */
    public static String sendPut(String url, String params) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        JSONObject jsonObject;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();

            //发送json数据给服务器
            out = new PrintWriter(conn.getOutputStream());
            jsonObject = new JSONObject(params);
            out.print(jsonObject);
            out.flush();

            //定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line + "\n";
            }

        } catch (Exception e) {
            Log.e(TAG,"发送PUT请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        Log.i(TAG, "sendPut: " + result);
        return result;
    }

    /**
     * 向指定URL发送DELETE方法的请求
     *
     * @param url    发送请求的URL
     * @return URL所代表远程资源的响应
     */
    public static String sendDelete(String url) {
        String result = "";
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            connection.setRequestMethod("DELETE");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                // 定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line + "\n";
                }
            }
        } catch (Exception e) {
            Log.e(TAG,"发送DELETE请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        Log.i(TAG, "sendDelete: " + result);
        return result;
    }

    /**
     * 用普通流方式上传文件
     *
     * @param url：     上传的路径
     * @param filename：需要上传的文件名
     * @param handler   向主线程返回UI信息的handler
     * @return
     */
    public static String uploadFileByStream(String url, String filename, Handler handler) {
        String result = "";
        DataInputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        BufferedReader ins = null;
        //将本地SDCard中要上传的文件赋值给file
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        File file = new File(sdDir.toString() + "/" + filename);

        //将file作为输入流读入后发往服务器
        try {
            URL realUrl = new URL(url + "?filename=" + filename);
            conn = (HttpURLConnection) realUrl.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("PUT");    //也可以是POST，与服务器对应即可
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.connect();
            conn.setConnectTimeout(10000);
            out = conn.getOutputStream();
            in = new DataInputStream(new FileInputStream(file));
            int bytes = 0;
            byte[] buffer = new byte[1024];
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
            }
            out.flush();

            // 读取返回流
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // 定义BufferedReader输入流来读取URL的响应
                ins = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = ins.readLine()) != null) {
                    result += line + "\n";
                }

                //将返回信息发往UI线程
                Message msg = Message.obtain();
                msg.what = 0x06;
                msg.arg1 = 1;
                msg.obj = result;
                handler.sendMessage(msg);

                Log.i(TAG, "HttpURL流方式上传完成");
            }
        } catch (Exception e) {
            Message msg = Message.obtain();
            msg.what = 0x06;
            msg.arg1 = 1;
            msg.obj = "HttpURL表单方式上传失败：";
            handler.sendMessage(msg);
            Log.e(TAG, "HttpURL表单方式上传失败");
            e.printStackTrace();

          // 使用finally块来关闭输入输出流
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (ins != null) {
                    ins.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return result;
    }

    /**
     * @param url      下载路径
     * @param filename 下载文件名
     * @param handler  向主线程返回UI信息的handler
     */
    public static void downloadFile(String url, String filename, Handler handler) {
        try {
            //做好GET访问方式的准备
            URL realUrl = new URL(url + "?filename=" + filename);
            URLConnection urlConnection = realUrl.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.connect();

            //准备将下载的文件写到SDCard中
            File sdDir = null;
            boolean sdCardExist = Environment.getExternalStorageState()
                    .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory();//获取跟目录
            }
            File file = new File(sdDir.toString() + "/" + filename);

            //从服务器返回的Header中获取下载文件大小，前提是服务器有这样安排
            int fileLength = httpURLConnection.getContentLength();

            //从服务器获取的数据流直接写入文件流
            BufferedInputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
            OutputStream out = new FileOutputStream(file);
            int size = 0;
            int len = 0;
            byte[] buf = new byte[1024];
            while ((size = in.read(buf)) != -1) {
                len += size;
                out.write(buf, 0, size);
                //打印下载百分比
                System.out.println("下载了-------> " + len * 100 / fileLength +
                        "%\n");
            }
            in.close();
            out.close();

            //将下载结果发送给UI线程
            Message msg = Message.obtain();
            msg.what = 0x05;
            msg.obj = "HttpURL下载成功：" + filename;
            handler.sendMessage(msg);

            Log.i(TAG, "HttpURL下载成功");
        } catch (MalformedURLException e) {
            Message msg = Message.obtain();
            msg.what = 0x05;
            msg.obj = "HttpURL下载失败：";
            handler.sendMessage(msg);
            Log.e(TAG, "HttpURL下载失败");
            e.printStackTrace();
        } catch (IOException e) {
            Message msg = Message.obtain();
            msg.what = 0x05;
            msg.obj = "HttpURL下载失败：";
            handler.sendMessage(msg);
            Log.e(TAG, "HttpURL下载失败");
            e.printStackTrace();
        } finally {
        }

    }

    /**
     * 用Multipart表单方式上传文件
     *
     * @param actionUrl：上传的路径
     * @param filename： 需要上传的文件名
     * @param handler    向主线程返回UI信息的handler
     * @return 服务器返回值
     */
    @SuppressWarnings("finally")
    public static String uploadFileByForm(String actionUrl, String filename, Handler handler) {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "WUm4580jbtwfJhNp7zi1djFEO3wNNm";

        DataOutputStream ds = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        StringBuffer resultBuffer = new StringBuffer();
        String tempLine = null;

        try {
            URL url = new URL(actionUrl);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            //从SDCard中找到要上传的文件，将其定义为uploadFile
            File uploadFile = null;
            File sdDir = null;
            boolean sdCardExist = Environment.getExternalStorageState()
                    .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
            if (sdCardExist) {
                sdDir = Environment.getExternalStorageDirectory();//获取根目录
                uploadFile = new File(sdDir.toString() + "/" + filename);
            }

            ds = new DataOutputStream(httpURLConnection.getOutputStream());
            //模拟表单格式，上传相应格式数据
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"file\";filename=\"" + filename + "\"" + end);
            ds.writeBytes(end);
            //上传文件内容
            FileInputStream fStream = new FileInputStream(uploadFile);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            while ((length = fStream.read(buffer)) != -1) {
                ds.write(buffer, 0, length);
            }
            //模拟表单格式，上传相应格式数据
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);

            fStream.close();
            ds.flush();

            if (httpURLConnection.getResponseCode() >= 300) {
                throw new Exception(
                        "HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
            }
            //收取服务器返回信息
            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = httpURLConnection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputStreamReader);
                tempLine = null;
                resultBuffer = new StringBuffer();
                while ((tempLine = reader.readLine()) != null) {
                    resultBuffer.append(tempLine);
                    resultBuffer.append("\n");
                }
                //将服务器返回信息发送给UI线程
                Message msg = Message.obtain();
                msg.what = 0x06;
                msg.arg1 = 2;
                msg.obj = resultBuffer.toString();
                handler.sendMessage(msg);

                Log.i(TAG, "HttpURL表单方式上传成功");
            }
        } catch (Exception e) {
            Message msg = Message.obtain();
            msg.what = 0x06;
            msg.arg1 = 1;
            msg.obj = "HttpURL表单方式上传失败：";
            handler.sendMessage(msg);
            Log.e(TAG, "HttpURL表单方式上传失败");
            e.printStackTrace();
        } finally {
            if (ds != null) {
                try {
                    ds.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return resultBuffer.toString();
        }
    }


    /**
     * @method getSessionID
     * @description 执行从cookie获取会话sessionID的方法，用于保持与服务器的会话
     * @param url 远程服务器的URL
     * */
    public String getSessionID(URL url){
        String sessionID;
        try {
//            URL url = new URL(actionURL);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            String cookieValue = connection.getHeaderField("set-cookie");
            if(cookieValue != null){
                sessionID = cookieValue.substring(0, cookieValue.indexOf(";"));
            }else{
                sessionID = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            sessionID = "";
        }
        return sessionID;
    }
}
