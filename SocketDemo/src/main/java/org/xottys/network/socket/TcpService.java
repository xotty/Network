/**
 * 将Tcp Socket的所有操作定义成Service，要点如下：
 * 1)在Binder中定义相关地址获取、服务启动（socket连接和等待接收数据）、发送数据和服务停止方法供外部调用
 * 2)各种socket状态和收到的数据通过Handler向外传递
 * 3)读取数据的线程是循环阻塞的，且带有换行符
 * 4)心跳包的实现(包括发送和检测，四种常用方式中选择一种)和相关服务器重连机制，使得本程序更加实用
 * 5)数据交互不频繁时，也可以通过简单的socket.sendUrgentData(0xFF)来检测服务器是否通讯正常
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:TcpService
 * <br/>Date:July，2017
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.socket;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class TcpService extends Service {
    private static final String TAG = "TcpSocket";

    private static final int HEARTBEAT_INTERVAL = 3 * 1000;         //心跳包间隔设为5分钟
    private static final int CONNECT_INTERVAL = 3 * 1000;           //连接失败后再次连接的健哥示设为10秒
    private static final int MAX_COUNT_NOTRECEIVEDHEARTBEAT = 3;  //心跳包发出后未收到服务器返回的最大次数设为10，达到后关闭清理，然后重新连接
    private static final int MAX_COUNT_TCPCONNECT = 5;            //连接服务器失败重试的最大次数设为3，达到后本service服务停止，通知用户
    //心跳包实现方式2，用PostDelay递归实现
    Handler handler = new Handler();
    private int notReceiveHeartBeatCount = 0;    //心跳包发出后未收到服务器返回的实时次数　
    private int tcpConnectCount = 0;             //连接服务器失败重试的实时次数
    private int receiveErrorCount = 0;           //服务器读取数据出错实时次数
    //不同方式实现心跳包的相关变量
    private HeartBeatThread heartbeatThread;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private AlarmManager mAlarmManager;
    private PendingIntent mPendingIntent;
    private HeartBeatReceiver mRreceiver;
    //传递数据给UI的Handler
    private Handler sHandler;
    //控制tcp socket读写循环变量
    private boolean isTcpServiceStartedSucess = false;
    //tcp通信地址,封装了ip和端口号
    private SocketAddress socketAddress;
    //客户端Socket通信的三个基本变量
    private Socket cSocket = null;
    private BufferedReader cInput = null;
    private BufferedWriter cOutput = null;
    private TcpServiceBinder mBinder = new TcpServiceBinder();
    private Thread tcpClientRecieverThread;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isTcpServiceStartedSucess) {
                //判断未收到服务器对发出心跳包响应的次数是否超过最大允许次数
                if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    mBinder.sendMessage("XAH");
                    notReceiveHeartBeatCount++;
                    //延迟一会儿后再次启动自己
                    handler.postDelayed(this, HEARTBEAT_INTERVAL);
                    //超过最大允许次数则重新启动服务器连接
                } else {
                    restartTcpService();
                }
            }
        }
    };

    //启动心跳包方式1
    private void startHeartBeat1() {
        heartbeatThread = new HeartBeatThread();
        heartbeatThread.start();

    }

    //启动心跳包方式2
    private void startHeartBeat2() {
        handler.postDelayed(runnable, HEARTBEAT_INTERVAL);
        //new Thread(runnable).start();
        Log.i(TAG, "startHeartBeat2启动");
    }

    //心跳包实现方式3，用Timer和TimerTask递归实现
    private void startHeartBeat3() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (isTcpServiceStartedSucess) {
                    if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                        mBinder.sendMessage("XAH");
                        notReceiveHeartBeatCount++;
                    } else {
                        restartTcpService();
                    }
                }
            }
        };
        //启动心跳包方式3
        if (mTimer != null)
            mTimer.schedule(mTimerTask, 0, HEARTBEAT_INTERVAL);
        Log.d(TAG, "startHeartBeat3启动");
    }

    //心跳包实现方式4，用AlarmManager实现,与前三种方式比，具有在手机休眠时仍能工作的优点
    private void startHeartBeat4() {
        //动态注册闹钟消息接收者HeartBeatReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("HeartBeatReceiver");
        mRreceiver = new HeartBeatReceiver();
        registerReceiver(mRreceiver, filter);

        //用AlarmManager定时发送消息给HeartBeatReceiver
        Intent intent = new Intent();
        intent.setAction("HeartBeatReceiver");   //设置接受者匹配的Action
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //启动心跳包方式4，闹钟首次动作时间为当前时间+HEARTBEAT_INTERVAL
        //setExact是目前唯一能精确锁定时间的方法
        long triggerAtTime = SystemClock.elapsedRealtime();
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtTime + HEARTBEAT_INTERVAL, mPendingIntent);
        Log.d(TAG, "startHeartBeat4启动");
    }

    //再次启动tcp连接服务，之前要停止心跳包发送，并清理环境
    private void restartTcpService() {
        Log.i(TAG, "服务器通讯故障数据，启动重连......");
        notReceiveHeartBeatCount = 0;
        receiveErrorCount = 0;
        isTcpServiceStartedSucess = false;

        //方式1停止心跳包发送
        // heartbeatThread.interrupt();
        /*方式2停止心跳包发送
        sHandler.removeCallbacks(runnable);
        */
         /*方式3停止心跳包发送
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            if (mTimerTask != null) {
                mTimerTask.cancel();
                mTimerTask = null;
            }*/

        /*方式4停止心跳包发送*/
//        mAlarmManager.cancel(mPendingIntent);
        unregisterReceiver(mRreceiver);

        Log.i(TAG, "HeartBeat Stop!");
        //清理环境
        mBinder.stopTcpService();
        //重新连接
        try {
            mBinder.startTcpService();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        Log.i(TAG, "onBind tcp: ");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "TcpService is Rebinded");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "TcpService is Unbinded");
        super.onUnbind(intent);
        return true;   //onRebind()被调用的前提条件
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "TcpService is Destroyed");
        try {
            unregisterReceiver(mRreceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    //心跳包实现方式1，用Thread+Sleep实现
    private class HeartBeatThread extends Thread {
        @Override
        public void run() {
            while (isTcpServiceStartedSucess) {
                //判断未收到服务器对发出心跳包响应的次数是否超过最大允许次数
                if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    try {
                        //发送心跳包"XAH"
                        mBinder.sendMessage("XAH");

                        //发送次数加1
                        notReceiveHeartBeatCount++;

                        //间隔休息一段时间
                        sleep(HEARTBEAT_INTERVAL);
                    } catch (Exception e) {

                        break;
                    }
                    //超过最大允许次数则重新启动服务器连接
                } else {
                    restartTcpService();
                    break;

                }
            }
        }
    }

    //闹钟消息接收者
    public class HeartBeatReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isTcpServiceStartedSucess) {
                if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    mBinder.sendMessage("XAH");
                    notReceiveHeartBeatCount++;
                    Log.d(TAG, "发送startHeartBeat包，未收到心跳包次数:" + notReceiveHeartBeatCount);
                    //循环启动闹钟，动作间隔时间为HEARTBEAT_INTERVAL
                    long triggerAtTime = SystemClock.elapsedRealtime();
                    mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAtTime + HEARTBEAT_INTERVAL, mPendingIntent);
                } else {
                    Log.d(TAG, "未收到心跳包次数超上限，重新启动服务:" + notReceiveHeartBeatCount);

                    restartTcpService();
                }
            }
        }
    }

    //收数据：Tcp Socket read 线程（阻塞）
    private class TcpClientReciever implements Runnable {
        @Override
        public void run() {
            String msgFromServer;
            while (isTcpServiceStartedSucess) {
                Message message = Message.obtain();
                if (receiveErrorCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    if (cSocket != null && cSocket.isConnected() && cInput != null) {
                        try {
                            //read和readLine都是阻塞的，服务器数据读取到msgFromServer
                            if ((msgFromServer = cInput.readLine()) != null) {
                                receiveErrorCount = 0;
                                notReceiveHeartBeatCount = 0;
                                Log.i(TAG, "Server Message Received:" + msgFromServer);
                                //心跳包数据不忘外传递，其它收到的数据通过handler外传
                                if (!msgFromServer.contains("XAH")) {
                                    message.what = 1;
                                    message.obj = msgFromServer;
                                    sHandler.sendMessage(message);
                                }

                            } else {
                                //若无数据可读，通常在对方关闭stream或socket时会触发
                                try {
                                    msgFromServer = null;
                                    //用来判断服务器socket是否关闭，若关闭会进入Catch IOException
                                    cSocket.sendUrgentData(0xFF);
                                    Thread.sleep(3000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                    //服务器socket关闭后则关闭本地socket
                                    cInput.close();
                                    cOutput.close();
                                    cSocket.close();
                                    Log.i(TAG, "Server Socket Closed or Server Closed.....");
                                    sHandler.sendEmptyMessage(0xE4);
                                    break;
                                }


                                Log.i(TAG, "Tcp Received:当前无数据可读！");
                            }

                    /*另一种读取数据的方法
                    String inputString = null;
                    int len = cInputStream.available();
                    if (len > 0) {
                        char[] input = new char[len];
                        int l = -1;
                        int readlen = 0;
                        while(len-readlen > 0 && (l = cInput.read(input, readlen , len-readlen)) != -1){
                            readlen += l;
                        }
                        inputString = new String(input);
                         }
                    */
                            //read读取异常
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (isTcpServiceStartedSucess) {
                                msgFromServer = "Error1";
                                receiveErrorCount++;
                            } else {
                                break;
                            }
                        }
                    }
                    //socket或流被关闭
                    else {
                        msgFromServer = "Error2";
                        receiveErrorCount++;
                    }
                    //输出Error信息给外部
                    if (msgFromServer != null && msgFromServer.contains("Error")) {
                        message.what = 0xE3;
                        message.obj = msgFromServer;
                        sHandler.sendMessage(message);
                    }
                }//超过最大自动重连次数时关闭然后重启tcp连接
                else {
                    isTcpServiceStartedSucess = false;
                    //重启Tcp
                    restartTcpService();
                    Log.i(TAG, "Tcp Read Error,Reconnnect:" + receiveErrorCount);
                    break;
                }
            }
        }
    }

    //将本Service中对外开放使用的方法皆放入其中，如：socket数据发送、socket服务停止等方法
    public class TcpServiceBinder extends Binder {
        //设置socket通信地址
        public void setSocketAddres(String ip, int port) {

            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                socketAddress = new InetSocketAddress(inetAddress, port);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                socketAddress = new InetSocketAddress("localhost", port);
            }

        }

        //设置handler
        public void setHandler(Handler handler) {
            sHandler = handler;
        }

        //用线程启动TcpSocket服务：连接socket、启动等待接收数据线程、然后启心跳包
        public void startTcpService() throws Exception {
            if (sHandler == null || socketAddress == null)
                throw new Exception("Handler或Socket地址参数是null");
            new Thread() {
                @Override
                public void run() {
                    //若连接失败，再次连接，最多连接MAX_COUNT_TCPCONNECT次
                    while (tcpConnectCount < MAX_COUNT_TCPCONNECT) {
                        //是否已成功连接过，没有功连接过才连接
                        if (!isTcpServiceStartedSucess) {
                            try {
                                //建立socket连接，也可以简写为cSocket = new Socket(addr,port);
                                cSocket = new Socket();
//                              cSocket.setSoTimeout(100000);         //设置接收消息超时时间为10s
                                cSocket.connect(socketAddress, 5000);    //设置连接超时为5s
                                if (cSocket != null && cSocket.isConnected()) {
                                    //建立socket输入输出流
                                    cOutput = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream()));
                                    cInput = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));

                                    //连接成功后设为true
                                    isTcpServiceStartedSucess = true;

                                    //启动Tcp Socket的循环读取服务器数据线程
                                    tcpClientRecieverThread = new Thread(new TcpClientReciever());
                                    tcpClientRecieverThread.start();

                                    //启动心跳包发送线程，共有4种方式可供选择
                                    startHeartBeat4();

                                    //通知UI线程服务器连接成功
                                    sHandler.sendEmptyMessage(0x00);

                                    //连接成功后清零
                                    tcpConnectCount = 0;

                                    Log.i(TAG, "startTcpService Success，连接成功");
                                    break;
                                }
                            }//连接异常
                            catch (IOException e) {
                                e.printStackTrace();
                                isTcpServiceStartedSucess = false;
                                tcpConnectCount++;
                                //连接失败后暂停一会儿再次连接
                                try {
                                    Thread.sleep(CONNECT_INTERVAL);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                                Log.i(TAG, "startTcpService Fail: " + tcpConnectCount);
                            }
                        }
                    }
                    //超过最大连接次数且连接失败，变量清理，并向宿主线程报告服务器暂时无法连接成功
                    if (!isTcpServiceStartedSucess) {
                        tcpConnectCount = 0;
                        cSocket = null;
                        sHandler.sendEmptyMessage(0xE0);
                    }
                }
            }.start();
        }

        //往服务器端发送数据
        public void sendMessage(final String str) {
            new Thread() {
                @Override
                public void run() {
                    if (cSocket != null && cSocket.isConnected()) {
                        try {
                            //tcp数据发送
                            cOutput.write(str);
                            cOutput.flush();
                            if (!str.contains("XAH"))
                                Log.i(TAG, "Tcp Send:" + str);
                        } //写数据失败
                        catch (IOException e) {
                            sHandler.sendEmptyMessage(0xE5);
                            e.printStackTrace();
                        }

                    }
                }
            }.start();
        }

        //停止TcpSocket服务
        public void stopTcpService() {
            if (isTcpServiceStartedSucess) {
                try {
                    //停止心跳包发送
                    unregisterReceiver(mRreceiver);
                    mAlarmManager.cancel(mPendingIntent);

                    //停止接收数据循环
                    tcpClientRecieverThread.interrupt();
                    //关闭所有流和socket
                    isTcpServiceStartedSucess = false;
                    if (cOutput != null) {
                        cOutput.close();
                        cOutput = null;
                    }
                    if (cInput != null) {
                        cInput.close();
                        cInput = null;
                    }
                    if (cSocket != null) {
                        cSocket.close();
                        cSocket = null;
                    }
                    //发送关闭成功信息
                    sHandler.sendEmptyMessage(10);
                } catch (IOException e) {
                    //发送关闭失败信息
                    sHandler.sendEmptyMessage(0xE2);
                    e.printStackTrace();
                }

            }
        }
    }
}
