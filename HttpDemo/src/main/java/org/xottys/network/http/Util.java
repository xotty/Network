package org.xottys.network.http;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public final class Util {
    private static final String url = "localhost:8080/ServerDemo/";
    static  public String getAddr(Context ctx,int addressType) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String str="";
        String[] urls = url.split(":");
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
     * @param socket
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
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    // 当前所连接的网络可用
                    return true;
                }
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
}
