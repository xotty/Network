/**
 * 本例为各种网络访问的常用工具类程序
 * 1）读取、保存或解析url地址的
 * 2）判断网路状态的
 * 3）xml解析的
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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.widget.TextView;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class Util {
    private static final String url = "localhost:8080/ServerDemo/";
    public static String xmlString="";
    static  public String getAddr(Context ctx,int addressType) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String str="";
        switch (addressType) {
            case 0:
                str=sp.getString("httpData", "http://"+url+"login");
                break;
            case 1:
                str=sp.getString("httpFile", "http://"+url+"updown");
                break;
            case 2:

                str=sp.getString("socketTcp", "http://"+url+"tcp"+"&8000");
                break;
            case 3:
                str=sp.getString("socketUdp", "http://"+url+"udp"+"&9000"+"&9001");
                break;
            case 4:
                str=sp.getString("websocket", "ws://"+url+"websocket");
                break;

        }
        return  str;
    }

    static  public void saveAddr(Context ctx,int addressType ,String addr) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        switch (addressType) {
            case 0:
                sp.edit().putString("httpData", addr).apply();
                break;
            case 1:
                sp.edit().putString("httpFile",  addr).apply();
                break;
            case 2:
                sp.edit().putString("socketTcp",  addr).apply();
                break;
            case 3:
                sp.edit().putString("socketUdp",  addr).apply();
                break;
            case 4:
                sp.edit().putString("websocket",  addr).apply();
                break;
        }
    }

    static public boolean isConnectableByHttp(URL url){
        boolean isConn = false;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(1000*5);
            if(conn.getResponseCode()==200){
                isConn = true;
            }
        }catch (IOException e) {
            e.printStackTrace();
            isConn = false;
        }finally{
            if (conn!=null)
              conn.disconnect();
        }
        return isConn;
    }


    /**
     * 判断是否断开连接，断开返回true,没有返回false
     * @param socket 需要连接的socket
     * @return true：可连接   false：不可连接
     */
    static public Boolean isConnectableBySocket(Socket socket){
        try{
            socket.sendUrgentData(0);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    static  public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                // 当前网络是连接的
                return (info.getState() == NetworkInfo.State.CONNECTED);

            }
        }
        return false;
    }

    static public void refreshLongText(TextView textView) {
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }

    //用DOM方式解析xml
    static public ArrayList<HashMap> readxmlByDom(InputStream xmlInput) {
        ArrayList<HashMap> results = new ArrayList<>();
        HashMap<String, String> result;
        DocumentBuilderFactory dbFactory;
        DocumentBuilder db = null;
        org.w3c.dom.Document document = null;
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            db = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        //将给定 URI 的内容解析为一个 XML 文档,并返回Document对象
        try {
            if (db != null) document = db.parse(xmlInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (document!=null) {
            document.getDocumentElement().normalize();

            org.dom4j.io.DOMReader xmlReader = new org.dom4j.io.DOMReader();
            org.dom4j.Document document4j = xmlReader.read(document);
            if (document4j != null) xmlString = formatXML(document4j);
            //按文档顺序返回包含在文档中且具有给定标记名称的所有 Element 的 NodeList
            NodeList resultList = document.getElementsByTagName("body");
            for (int temp = 0; temp < resultList.getLength(); temp++) {
                Node nNode = resultList.item(temp);
                System.out.println("\nCurrent Element :"
                        + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    result = new HashMap<>();
                    Element eElement = (Element) nNode;

                    //get element content
                    result.put("code", eElement.getElementsByTagName("code")
                            .item(0)
                            .getTextContent());
                    result.put("message", eElement.getElementsByTagName("info")
                            .item(0)
                            .getTextContent());

                    results.add(result);
                }
            }
        }
        return results;
    }

    static private String formatXML(org.dom4j.Document xmlDocument) {
        StringWriter stringWriter = new StringWriter();

        try {
            OutputFormat formater= OutputFormat.createPrettyPrint();
            formater.setOmitEncoding(true);
            formater.setEncoding("UTF-8");
            formater.setSuppressDeclaration(true);
            stringWriter=new StringWriter();
            XMLWriter writer=new XMLWriter(stringWriter,formater);
            writer.write(xmlDocument);
            writer.close();

        }catch (IOException e){
            e.printStackTrace();
        }


        return stringWriter.toString();
    }
    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     * @param strUrlParam  url查询串，格式如：name1=value1&name2=value2
     * @return HashMap，get(name1)=value1,get(name2)=value2
     */
    public static HashMap<String, String> decodeUrlQueryString(String strUrlParam) {
        HashMap<String, String> mapResult = new HashMap<>();

        String[] arrSplit;

        //每个键值为一组
        arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual;
            arrSplitEqual = strSplit.split("[=]");

            //解析出键值
            if (arrSplitEqual.length > 1) {
                //正确解析
                mapResult.put(arrSplitEqual[0], arrSplitEqual[1]);

            } else {
                if (!arrSplitEqual[0].equals("")) {
                    //只有参数没有值，不加入
                    mapResult.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapResult;
    }

    /**
     * 解析出url
     * 如 ""http://192.168.1.1:8080/login，解析出"BaseUrl:http://192.168.1.1,PathUrl:login"存入map中
     * @param url 格式如：http://192.168.1.1:8080/login
     * @return HashMap，get("BaseUrl")=http://192.168.1.1:8080,get('PathUrl")=login
     */
    public static HashMap<String, String> decodeUrl(String url) {
        HashMap<String, String> mapResult = new HashMap<>();
        try {
            URL myurl = new URL(url);
            mapResult.put("BaseUrl", myurl.getAuthority());
            mapResult.put("PathUrl", myurl.getPath());
        }catch (MalformedURLException e){
            mapResult.put("BaseUrl", "http://192.168.0.1:8080");
            mapResult.put("PathUrl", "login");
            e.printStackTrace();
        }
        return mapResult;
    }
}
