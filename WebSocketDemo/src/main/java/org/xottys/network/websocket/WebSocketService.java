/**
 * 本例用Service方式演示了WebSocket的两种访问方式：
 * 1）Java-Websocket库
 * 2）OKHttp库(在程序中被注释了的替换方式)
 * 二者的访问流程都是相似的，要点如下：
 * 1）构造webocket客户端
 * 2）建立websocket连接，然后在异步方法中接收服务器信息
 * 2）用webocket客户端的专门的方法发送数据，结束时可以关闭连接
 * 3）通过定义接口的回调方式将收到的服务器数据传递出去
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:HttpDemo
 * <br/>Date:July，2017
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.websocket;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

//这里是要用到的java-websocket库，与okhttp3库有冲突，二者只能选用一个
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketService extends Service {
    final String TAG = "WebSocketService";

    //客户端WebSocket通信的一个基本变量
    private MyWebSocketClient webSocketClient = null;

    /*OKhttp替换方式
      private  okhttp3.WebSocket myWebSocket=null;
     */

    //WebSocket通信地址
    private String ipAddr;
    private int port; //对应服务器端的端口号

    private String wsAddr;

    //传递数据给UI的CallBack
    private CallBack callBack = null;
    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    //连接WebSocket服务器
    private void connectWebSocket() {
        //生成websocket地址
//        String websocketAddr = "ws://" + ipAddr + ":" + port;
        String websocketAddr = wsAddr;
        try {
            //发起websocket连接请求
            webSocketClient = new MyWebSocketClient(new URI(websocketAddr), new Draft_6455());
            webSocketClient.connectBlocking();
        } catch (URISyntaxException e) {
            callBack.onError(e.toString());
            e.printStackTrace();
            Log.i(TAG, "connectWebSocket: Error1");
        }catch(InterruptedException e) {
            callBack.onError(e.toString());
            e.printStackTrace();
            Log.i(TAG, "connectWebSocket: Error2");
        }

         /*OKhttp替换方式
            MyWebSocketListener listener = new MyWebSocketListener();
            Request request = new Request.Builder()
                    .url(websocketAddr)
                    .build();
            OkHttpClient client = new OkHttpClient();
            myWebSocket = client.newWebSocket(request, listener);
          */
        }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    //该服务会持续存在，直到外部组件调用Context.stopService方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    //第一次BindService时会首先回调本方法，此时启动socket连接等服务
    @Override
    public IBinder onBind(Intent intent) {
        //解析传入的ip地址等信息
//        ipAddr = intent.getStringExtra("IPADDR");
//        port = intent.getIntExtra("PORT", 10000);
        wsAddr=intent.getStringExtra("WSADDR");
        //启动Websocket连接
        new Thread() {
            @Override
            public void run() {
                connectWebSocket();
            }
        }.start();

        WebSocketServiceBinder mBinder = new WebSocketServiceBinder();
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        //解析传入的ip地址等信息
//        ipAddr = intent.getStringExtra("IPADDR");
//        port = intent.getIntExtra("PORT", 10000);

        //启动WebSocket连接
        {
            new Thread() {
                @Override
                public void run() {
                    connectWebSocket();
                }
            }.start();
        }
        Log.i(TAG, "WebSocketService is Rebinded");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "WebSocketService is Unbinded");
        super.onUnbind(intent);
        return true;   //onRebind()被调用的前提条件
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "WebSocketService is Destroyed");
        super.onDestroy();
    }

    //往服务器端发送数据
    public void sendMessage(final String msg) {
        new Thread() {
            @Override
            public void run() {
                webSocketClient.send(msg);
                System.out.println("WebSocket Send:" + msg);
            }
        }.start();

        /*OKhttp替换方式
          myWebSocket.send(msg);
        */
    }

    //终止WebSocket服务
    public void closeWebSocket() {
        try {
            webSocketClient.closeBlocking();
            webSocketClient = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        /*OKhttp替换方式
        myWebSocket.close(1000,"Bye");
        */
    }

    //回调接口，传递信息给UI线程
    public interface CallBack {
        void onReceivedMessage(String msg);
        void onError(String msg);
    }

    public class WebSocketServiceBinder extends Binder {
        //获取WebSocketService实例
        public WebSocketService getWebSocketService() {
            return WebSocketService.this;
        }
    }

    //使用Java-WebSocket库实现WebSocket所有功能
    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverUri, Draft draft) {
            super(serverUri, draft);
        }

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.i(TAG, "onOpen:opened connection");

        }

        @Override
        public void onMessage(String message) {
            if (callBack != null) {
                callBack.onReceivedMessage(message);
            }
            Log.i(TAG, "onMessage,Received: " + message);
        }

        @Override
        public void onFragment(Framedata fragment) {
            Log.i(TAG, "onFragment,Received fragment: " + new String(fragment.getPayloadData().array()));
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.i(TAG, "onClose:Connection closed by " + (remote ? "remote peer" : "Local"));
        }

        @Override
        public void onError(Exception e) {
            Log.i(TAG, "onError");
            e.printStackTrace();
            callBack.onError(e.toString());
        }
    }

    /*OKhttp替换方式,使用OKHttp库实现WebSocket所有功能
    private  final class MyWebSocketListener extends okhttp3.WebSocketListener{
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.i(TAG, "okhttp3 onOpen:opened connection");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
           // output("onMessage: " + text);
            if (callBack != null) {
                callBack.onReceivedMessage(text);
            }
            Log.i(TAG, "okhttp3 onMessageString,Received: " + text);
        }

       @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            //output("onMessage byteString: " + bytes);
           Log.i(TAG, "okhttp3 onMessagebyteString,Received: " + text);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
           //webSocket.close(1000, null);
           // output("onClosing: " + code + "/" + reason);
            Log.i(TAG, "okhttp3 onClosing,Reason:"+reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            //output("onClosed: " + code + "/" + reason);
            Log.i(TAG, " okhttp3 onClosed,Reason:"+reason);

        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.i(TAG, "onError");
            callBack.onError(t.getMessage());
        }
    }  */
}






