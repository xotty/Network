/**
 * HttpURLConnection是Android自带的同步访问Http服务器的基础类，其各种访问方式：GET、POST、PUT、DELETE的共同流程如下：
 * 1)HttpURLConnection connection = (HttpURLConnection) url.openConnection();
 * 2)在Header中定义请求和响应的属性，此处务必要与服务器保持一致
 * 3）connection.connect();--可选
 * 4）connection.getOutputStream()发送Body数据给服务器
 * 5）connection.getInputStream()接收服务器响应
 * 只支持同步，异步可以与AsyncTask协同来完成
 * httpURLConnection文件下载，就是用GET方法将从服务器获得的网络输入流用文件输出流方式写成本地文件
 * httpURLConnection文件上传，就是用POST或PUT方法将本地文件作为文件输入流，读入后通过网络输出流发往服务器
 *
 * Cookie(在sendGet中演示):可以通过connection.getHeaderFields()对Response的Header进行获取和解析，但解析比较繁琐。
 * 直接用Android CookieManager的getCookieStore().get(uri)简洁一些。发送Cookie就直接使用 connection.setRequestProperty("Cookie", cookieString)
 * 即可，其中cookieString格式要符合下列Cookie规则（其中value格式为name=value）：
 * --Set-Cookie:value [ ;expires=date][ ;domain=domain][ ;path=path][ ;Httponly][ ;secure]--服务器端
 * --Cookie : value或Cookie:value1 ; value2 ; name1=value1--客户端
 * 多键值对可以用多value的 Cookie（服务器就要多个Set-Cookie行）方式，也可以在单value的Cookie中这样定义：name=key1=value1&key2=value2......
 *
 * Session(在sendPut中演示):用上述方法在Cookie中接收和发送sessionID即可，格式为：JSESSIONID=149023982C1E1F2B78B92E03809B1779，其代表的Attribute内容只在服务器可见。
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

import org.json.JSONException;
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
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpURLConnectionDemo {
    private static final String TAG = "http";
    static CookieManager manager;
    static private String myCookie = "";
    static private String sessionID = "";

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param baseUrl 发送请求的URL
     * @return URL所代表远程资源的响应
     */
    public static String sendGet(String baseUrl, String params) {
        String result = "";
        BufferedReader in = null;
        StringBuilder cookieBuilder = new StringBuilder();
        String divider = "";
        myCookie="";
        //用于接收和保存Cookie，定义后服务器传来的Cookie就自动放到CookieManager中了
        if (manager == null) {
            // 创建一个默认的 CookieManager
            manager = new CookieManager();
            // 将规则改掉，接受所有的 Cookie
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            // 保存这个定制的 CookieManager
            CookieHandler.setDefault(manager);
        }

        try {
            String url = baseUrl + "?" + params;
            URL realUrl = new URL(url);
            URI uri = new URI(baseUrl);

            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "text/plain");

            //对服务器传来Cookie进行解析和处理
            List<HttpCookie> httpCookies = manager.getCookieStore().get(uri);
            Log.i(TAG, "CookieManager Cookie :" + httpCookies.toString());

            if (httpCookies.size() > 0) {
                for (HttpCookie hcookie : httpCookies) {
                    if (hcookie != null & !hcookie.hasExpired()) {
                        cookieBuilder.append(divider);
                        divider = ";";
                        cookieBuilder.append(hcookie.getName());
                        cookieBuilder.append("=");
                        cookieBuilder.append(hcookie.getValue());
                    }
                }
                //将有效Cookie再次发送给服务器
                connection.setRequestProperty("Cookie", cookieBuilder.toString());

                Log.i(TAG, "Cookie to server: " + cookieBuilder.toString());
            }

            //设置连接主机超时
            connection.setConnectTimeout(3000);
            //设置从主机读取数据超时
            connection.setReadTimeout(3000);

            /*获取request中的Cookie
            Map<String, List<String>> mapcookie= manager.get(uri,connection.getRequestProperties());
            Log.i(TAG, "Request Cookie： " + mapcookie);*/

            //建立实际的连接
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                result = "HttpURLConnection服务器连接故障1:" + responseCode;
                Log.e(TAG, "HttpURLConnection服务器连接故障1:" + responseCode);
            } else {
                // 定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }

                //从服务器响应Header中收取其传来Cookie，需要另行解析
                Map<String, List<String>> maps = connection.getHeaderFields();
                List<String> cookielist = maps.get("Set-Cookie");
                if (cookielist != null && cookielist.size() != 0) {
                    Iterator<String> it = cookielist.iterator();
                    StringBuffer sbu = new StringBuffer();
                    while (it.hasNext()) {
                        sbu.append(it.next() + ";");
                    }
                    myCookie = sbu.toString();
                }
                Log.i(TAG, "Header Cookie: " + myCookie);

                /*另一种读取Response 中Cookie的方法
                Map<String, List<String>> mapcookie= manager.get(uri,connection.getHeaderFields());
                Log.i(TAG, "Response Cookie:: " + mapcookie);*/
            }
        } catch (Exception e) {
            result = "HttpURLConnection服务器连接故障3";
            Log.e(TAG, "HttpURLConnection服务器连接故障3");
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

        return "（GET）" + result+(myCookie.equals("")?"":"  Cookie From Server:\n"+myCookie);
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
            //设置连接主机超时
            conn.setConnectTimeout(3000);
            //设置从主机读取数据超时
            conn.setReadTimeout(3000);
            conn.connect();
            //获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            //发送请求参数给服务器
            out.print(params);
            //flush输出流的缓冲
            out.flush();

            int responseCode = conn.getResponseCode();
            if (responseCode >= 400) {
                result = "HttpURLConnection服务器连接故障1:" + responseCode;
                Log.e(TAG, "HttpURLConnection服务器连接故障1:" + responseCode);
            } else {
                //定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
            }
        } catch (IOException e1) {
            result = "HttpURLConnection服务器连接故障3";
            Log.e(TAG, "HttpURLConnection服务器连接故障3");
            e1.printStackTrace();
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
        return "（POST）" + result;
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
        String result = "", resultCode, resultMsg = "";
        JSONObject jsonObject;
        if (manager == null) {
            // 创建一个默认的 CookieManager
            manager = new CookieManager();
            // 将规则改掉，接受所有的 Cookie
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            // 保存这个定制的 CookieManager
            CookieHandler.setDefault(manager);
        }
        try {
            URL realUrl = new URL(url);
            URI uri = new URI(url);
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestProperty("Content-Type", "application/json");
            Log.i(TAG, "SessionID :---" + sessionID);
            if (!sessionID.equals("")) conn.setRequestProperty("Cookie", sessionID);

            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            //设置连接主机超时
            conn.setConnectTimeout(3000);
            //设置从主机读取数据超时
            conn.setReadTimeout(3000);

            //显示请求Header中Cookie内容
            Map<String, List<String>> mapcookie= manager.get(uri,conn.getRequestProperties());
            Log.i(TAG, "Request Cookie： " + mapcookie);

            conn.connect();

            //发送json数据给服务器
            out = new PrintWriter(conn.getOutputStream());
            jsonObject = new JSONObject(params);
            out.print(jsonObject);
            out.flush();
            int responseCode = conn.getResponseCode();
            if (responseCode >= 400) {
                resultMsg = "HttpURLConnection服务器连接故障1:" + responseCode;
                Log.e(TAG, "HttpURLConnection服务器连接故障1:" + responseCode);
            } else {

                //定义BufferedReader输入流来读取URL的响应
                in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
                try {
                    jsonObject = new JSONObject(result);
                    resultCode = jsonObject.getString("code");
                    resultMsg = jsonObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                resultMsg = resultMsg + "\n" + result;

                //对服务器传来Cookie进行解析和处理
                List<HttpCookie> httpCookies = manager.getCookieStore().get(uri);
                Log.i(TAG, "CookieManager Cookie :" + httpCookies.toString());
                if (httpCookies.size() > 0) {
                    for (HttpCookie hcookie : httpCookies) {
                        if (hcookie != null & !hcookie.hasExpired()) {
                            if (hcookie.getName().equals("JSESSIONID"))
                                sessionID=hcookie.getValue();
                                if (!sessionID.equals(""))
                                resultMsg+="\nSessionID："+sessionID;
                        }
                    }
                }
                Log.i(TAG, "SessionID :" + sessionID);
            }
        } catch (IOException e1) {
            resultMsg = "HttpURLConnection服务器连接故障3";
            Log.e(TAG, "HttpURLConnection服务器连接故障3");
            e1.printStackTrace();
        } catch (JSONException e2) {
            resultMsg = "HttpURLConnection数据解析错误，Json解析失败";
            Log.e(TAG, "HttpURLConnection数据解析错误，Json解析失败");
            e2.printStackTrace();
        }catch (URISyntaxException e3) {
            e3.printStackTrace();
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

        Log.i(TAG, "sendPut: " + resultMsg);
        return "（PUT）" + resultMsg;
    }

    /**
     * 向指定URL发送DELETE方法的请求
     *
     * @param baseUrl 发送请求的URL
     * @return URL所代表远程资源的响应
     */
    public static String sendDelete(String baseUrl, String params) {
        PrintWriter out;
        String result = "";
        InputStream in = null;
        Message msg = Message.obtain();
        msg.what = 0x04;
        //将客户端body中的xml数据读取并解析到ArrayList中
        try {
            URL realUrl = new URL(baseUrl);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setRequestMethod("DELETE");
            //设置连接主机超时
            connection.setConnectTimeout(3000);
            //设置从主机读取数据超时
            connection.setReadTimeout(3000);
            connection.connect();
            //发送xml数据给服务器
            out = new PrintWriter(connection.getOutputStream());
            out.print(params);
            out.flush();
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                result = "HttpURLConnection服务器连接故障1:" + responseCode;
                Log.e(TAG, "HttpURLConnection服务器连接故障1:" + responseCode);
            } else {

                in = connection.getInputStream();
                ArrayList<HashMap> results = Util.readxmlByDom(in);
                if (results.size() != 0) {
                    for (HashMap rs : results) {
                        result = rs.get("message").toString();
                    }
                }
                result += Util.xmlString;
            }
        } catch (Exception e) {
            result = "HttpURLConnection服务器连接故障3";
            Log.e(TAG, "HttpURLConnection服务器连接故障3");
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

        return "（DELETE）" + result;
    }


    /**
     * @param url      下载路径
     * @param filename 下载文件名
     */
    public static String downloadFile(String url, String filename) {
        String result;
        try {
            //做好GET访问方式的准备
            URL realUrl = new URL(url + "?filename=" + filename);
            URLConnection urlConnection = realUrl.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            //设置连接主机超时
            httpURLConnection.setConnectTimeout(3000);
            //设置从主机读取数据超时
            httpURLConnection.setReadTimeout(3000);

            httpURLConnection.connect();

            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode >= 400) {
                result = "HttpURLConnection服务器连接故障1";
                Log.e(TAG, "HttpURLConnection服务器连接故障1");
            } else {
                result = URLDecoder.decode(httpURLConnection.getHeaderField("result"), "UTF-8");
                if (result.equals("下载成功")) {
                    //准备将下载的文件写到SDCard中
                    File sdDir ;
                    boolean sdCardExist = Environment.getExternalStorageState()
                            .equals(Environment.MEDIA_MOUNTED);//判断sd卡是否存在
                    if (sdCardExist) {
                        sdDir = Environment.getExternalStorageDirectory();//获取SD卡根目录
                    } else {
                        sdDir = Environment.getDataDirectory();
                    }

                    String filepath = "";
                    if (sdDir != null)
                        filepath = sdDir.toString() + "/AndroidDemo/download/" + filename;
                    File file = new File(filepath);
                    File fileParent = file.getParentFile();
                    if (!fileParent.exists()) {
                        fileParent.mkdirs();
                    }

                    //从服务器返回的Header中获取下载文件大小，前提是服务器有这样安排
                    int fileLength = httpURLConnection.getContentLength();

                    //从服务器获取的数据流直接写入文件流
                    BufferedInputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
                    OutputStream out = new FileOutputStream(file);
                    int size;
                    int len = 0;
                    byte[] buf = new byte[1024];
                    while ((size = in.read(buf)) != -1) {
                        len += size;
                        out.write(buf, 0, size);
                        //打印下载百分比
                        Log.i(TAG, "下载了-------> " + len * 100 / fileLength + "%\n");
                    }
                    in.close();
                    out.close();
                }

                result = "HttpURLConnection" + result + ":" + filename;
                Log.i(TAG, result);
            }
        } catch (MalformedURLException e) {
            result = "HttpURLConnection服务器连接故障2";
            Log.e(TAG, "HttpURLConnection服务器连接故障2");
            e.printStackTrace();
        } catch (IOException e) {
            result = "HttpURLConnection服务器连接故障3";
            Log.e(TAG, "HttpURLConnection服务器连接故障3");
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 用普通流方式上传文件
     *
     * @param url：                 上传的路径
     * @param filepath：需要上传的文件全路径名
     * @param handler              向主线程返回UI信息的handler
     */
    public static void uploadFileByStream(String url, String filepath, Handler handler) {
        String result = "";
        DataInputStream in = null;
        OutputStream out = null;
        HttpURLConnection conn = null;
        BufferedReader ins;
        Message msg = Message.obtain();
        msg.what = 0x06;

        File file = new File(filepath);
        String filename = filepath.substring(filepath.lastIndexOf("/") + 1);

        //将file作为输入流读入后发往服务器
        try {
            URL realUrl = new URL(url + "?filename=" + filename);
            conn = (HttpURLConnection) realUrl.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("PUT");     //也可以是POST，与服务器对应即可
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Charsert", "UTF-8");
            //设置连接主机超时
            conn.setConnectTimeout(3000);
            //设置从主机读取数据超时
            conn.setReadTimeout(3000);
            conn.connect();
            //读取和上传文件
            in = new DataInputStream(new FileInputStream(file));
            out = conn.getOutputStream();
            int bytes;
            byte[] buffer = new byte[1024];
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
            }
            out.flush();

            //读取服务器响应结果
            int responseCode = conn.getResponseCode();
            if (responseCode >= 400) {
                result = "HttpURLConnection服务器连接故障1";
                msg.arg1 = 2;
                msg.obj = result;
                Log.e(TAG, "HttpURLConnection服务器连接故障1");
            } else {
                // 读取返回流,定义BufferedReader输入流来读取URL的响应
                ins = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = ins.readLine()) != null) {
                    result += line + "\n";
                }
                if (result.equals("上传成功")) {
                    //将返回信息发往UI线程

                    msg.arg1 = 0;
                } else {
                    msg.arg1 = 1;
                }
                msg.obj = "HttpURLConnection/octet-stream" + result + ":" + filename;

                Log.i(TAG, "HttpURL流方式上传完成");
            }
        } catch (IOException e) {
            msg.arg1 = 3;
            msg.obj = "HttpURLConnection服务器连接故障2";
            Log.e(TAG, "HttpURLConnection服务器连接故障2");
            e.printStackTrace();

            // 使用finally块来关闭输入输出流
        } finally {

            try {
                if (conn != null) {
                    try {
                        //主动关闭inputStream
                        //这里不需要进行判空操作
                        conn.getInputStream().close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    conn.disconnect();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
            handler.sendMessage(msg);
        }
    }

    /**
     * 用Multipart表单方式上传文件
     *
     * @param actionUrl：上传的路径
     * @param filepath：需要上传的全路径文件名
     * @param handler:             向主线程返回UI信息的handler
     */
    public static void uploadFileByForm(String actionUrl, String filepath, Handler handler, int serverType) {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "WUm4580jbtwfJhNp7zi1djFEO3wNNm";

        DataOutputStream ds = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        StringBuffer resultBuffer;
        String tempLine;
        String result;
        Message msg = Message.obtain();
        msg.what = 0x06;
        try {
            URL url = new URL(actionUrl);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            if (serverType == 0)
                httpURLConnection.setRequestMethod("POST");
            else
                httpURLConnection.setRequestMethod("DELETE");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            //设置连接主机超时
            httpURLConnection.setConnectTimeout(3000);
            //设置从主机读取数据超时
            httpURLConnection.setReadTimeout(3000);
            httpURLConnection.connect();

            File file = new File(filepath);
            String filename = filepath.substring(filepath.lastIndexOf("/") + 1);
            ds = new DataOutputStream(httpURLConnection.getOutputStream());
            //模拟表单格式，上传相应格式数据
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"file\";filename=\"" + filename + "\"" + end);
            ds.writeBytes(end);
            //上传文件内容
            FileInputStream fStream = new FileInputStream(file);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length;
            while ((length = fStream.read(buffer)) != -1) {
                ds.write(buffer, 0, length);
            }
            //模拟表单格式，上传相应格式数据
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);

            fStream.close();
            ds.flush();

            if (httpURLConnection.getResponseCode() >= 400) {
                result = "HttpURLConnection服务器连接故障1";
                msg.arg1 = 2;
                msg.obj = result;
                Log.e(TAG, "HttpURLConnection服务器连接故障1");
            } else {
                //收取服务器返回信息
                inputStream = httpURLConnection.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputStreamReader);
                resultBuffer = new StringBuffer();
                while ((tempLine = reader.readLine()) != null) {
                    resultBuffer.append(tempLine);
                }
                result = resultBuffer.toString();
                if (result.equals("上传成功")) {
                    //将返回信息发往UI线程
                    msg.arg1 = 0;
                } else {
                    msg.arg1 = 1;
                }
                result = resultBuffer.toString();
                if (serverType == 0)
                    msg.obj = "HttpURLConnection/multipart(servlet3.0)" + result + ":" + filename;
                else
                    msg.obj = "HttpURLConnection/multipart(commons-fileupload)" + result + ":" + filename;
            }
        } catch (Exception e) {
            msg.arg1 = 3;
            msg.obj = "HttpURLConnection服务器连接故障2";
            Log.e(TAG, "HttpURLConnection服务器连接故障2");
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
            handler.sendMessage(msg);
        }
    }
}