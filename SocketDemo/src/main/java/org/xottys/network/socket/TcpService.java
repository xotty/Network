/**
 * 将Tcp Socket的所有操作定义成Service，要点如下：
 * 1）在onBind中获取IP地址，然后启动startTcpService，其中主要完成socket连接和启动数据读取线程
 * 2）在Binder中单独定义发送数据方法sendMessage和停止socket的方法stopTcpService
 * 3）读取数据的线程是循环阻塞的
 * 4) 心跳包的实现(四种常用方式中选择一种)和相关服务器重连机制，使得本程序更加实用
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
import java.util.Timer;
import java.util.TimerTask;

public class TcpService extends Service {
    private static final String TAG="TcpSocketService";

    private static final int HEARTBEAT_INTERVAL = 3*1000;         //心跳包间隔设为5分钟
    private static final int CONNECT_INTERVAL = 3*1000;           //连接失败后再次连接的健哥示设为10秒
    private static final int MAX_COUNT_NOTRECEIVEDHEARTBEAT = 3;  //心跳包发出后未收到服务器返回的最大次数设为10，达到后关闭清理，然后重新连接
    private static final int MAX_COUNT_TCPCONNECT = 5;            //连接服务器失败重试的最大次数设为3，达到后本service服务停止，通知用户

    private int notReceiveHeartBeatCount=0;    //心跳包发出后未收到服务器返回的实时次数　
    private int tcpConnectCount=0;             //连接服务器失败重试的实时次数
    private int receiveErrorCount=0;           //服务器读取数据出错实时次数
    
    //不同方式实现心跳包的相关变量
    private HeartBeatThread heartbeatThread;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private AlarmManager mAlarmManager;
    private PendingIntent mPendingIntent;
    private HeartBeatReceiver mRreceiver;

    //传递数据给UI的Handler
    private Handler cHandler;

    //控制tcp socket读写循环变量
    private boolean isTcpServiceStartedSucess=false;

    //tcp通信地址
    private String ipAddr;
    private int port; //对应服务器端的端口号


    //客户端Socket通信的三个基本变量
    private Socket cSocket = null;
    private BufferedReader cInput = null;
    private BufferedWriter cOutput = null;

    private TcpServiceBinder mBinder = new TcpServiceBinder();


     
    //心跳包实现方式1，用Thread+Sleep实现
    private class HeartBeatThread extends Thread {
        @Override
        public void run() {
            while (isTcpServiceStartedSucess) {
                //判断未收到服务器对发出心跳包响应的次数是否超过最大允许次数
                if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    try {
                        //发送心跳包"WTZ"
                        mBinder.sendMessage("WTZ\n");
                        
                        //发送次数加1
                        notReceiveHeartBeatCount++;
                        
                        //间隔休息一段时间
                        sleep(HEARTBEAT_INTERVAL);
                    } catch (Exception e) {

                        break;
                    }
                  //超过最大允许次数则重新启动服务器连接
                } else
                    {
                        restartTcpService();
                    break;

                }
            }
        }
    }
    //启动心跳包方式1
    private void startHeartBeat1() {
        heartbeatThread=new HeartBeatThread();
        heartbeatThread.start();
      
    }

    //心跳包实现方式2，用PostDelay递归实现
    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isTcpServiceStartedSucess) {
                //判断未收到服务器对发出心跳包响应的次数是否超过最大允许次数
                if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                        mBinder.sendMessage("WTZ\n");
                        notReceiveHeartBeatCount++;
                        //延迟一会儿后再次启动自己
                        handler.postDelayed(this, HEARTBEAT_INTERVAL);
                    //超过最大允许次数则重新启动服务器连接
                    }else
                    {
                        restartTcpService();
                    }
                }
            }
    };

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
                        mBinder.sendMessage("WTZ\n");
                        notReceiveHeartBeatCount++;
                    } else {
                        restartTcpService();
                    }
                }
            }
        };
        //启动心跳包方式3
        if (mTimer != null && mTimerTask != null)
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
                triggerAtTime+HEARTBEAT_INTERVAL, mPendingIntent);
        Log.d(TAG, "startHeartBeat4启动");
    }

    //闹钟消息接收者
    public class HeartBeatReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isTcpServiceStartedSucess) {
                if (notReceiveHeartBeatCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    mBinder.sendMessage("WTZ\n");
                    notReceiveHeartBeatCount++;
                    Log.d(TAG, "startHeartBeat   启动:" + notReceiveHeartBeatCount);
                    //循环启动闹钟，动作间隔时间为HEARTBEAT_INTERVAL
                    long triggerAtTime = SystemClock.elapsedRealtime();
                    mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAtTime + HEARTBEAT_INTERVAL, mPendingIntent);
                } else {
                    restartTcpService();
                }
            }
        }
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
        cHandler.removeCallbacks(runnable);
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
            mAlarmManager.cancel(mPendingIntent);
            unregisterReceiver(mRreceiver);

        Log.i(TAG, "HeartBeat Stop!");
        //清理环境
        mBinder.stopTcpService();
        //重新连接
        mBinder.startTcpService();
    }

    //Tcp Socket read 线程（阻塞）
   private class tcpClientReciever implements Runnable {
        @Override
        public void run() {
            while (isTcpServiceStartedSucess) {
                if (receiveErrorCount < MAX_COUNT_NOTRECEIVEDHEARTBEAT) {
                    Message message = Message.obtain();
                    message.what = 1;
                    String msgFromServer;
                    if (cSocket != null && cSocket.isConnected() && cInput != null) {
                        try {
                            //read和readLine都是阻塞的
                            msgFromServer = cInput.readLine();
                            receiveErrorCount=0;
                            if (msgFromServer != null) notReceiveHeartBeatCount = 0;

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
                            // Log.i(TAG, "received: "+msgFromServer);
                        } catch (IOException e) {
                            e.printStackTrace();
                            msgFromServer = "Error";
                            receiveErrorCount++;

                        }
                    } else {
                        msgFromServer = "Error";
                        receiveErrorCount++;
                    }
                    if (msgFromServer != null && !msgFromServer.equals("WTZ")) {
                        message.obj = msgFromServer;
                        cHandler.sendMessage(message);
                        Log.i(TAG, "Tcp Read : " + msgFromServer);
                    }
                } else {
                    isTcpServiceStartedSucess = false;
                    restartTcpService();
                    Log.i(TAG, "Tcp Read Error,Reconnnect:"+receiveErrorCount);
                    break;
                }
            }
        }
    }

    //将本Service中对外开放使用的方法皆放入其中，如：socket数据发送、socket服务停止等方法
    public class TcpServiceBinder extends Binder {

        //获取TcpService实例
        private TcpService getTcpService() {
            return TcpService.this;
        }

        //设置handler
        public void setHandler(Handler handler) {
            cHandler = handler;
        }
        //启动TcpSocket服务
        public void startTcpService() {
            new Thread() {
                @Override
                public void run() {
                    //若连接失败，再次连接，最多连接MAX_COUNT_TCPCONNECT次
                    while (tcpConnectCount < MAX_COUNT_TCPCONNECT) {
                        //是否已成功连接过，没有功连接过才连接
                        if (!isTcpServiceStartedSucess) {
                            try {
                                InetAddress addr = InetAddress.getByName(ipAddr);
                                SocketAddress sAddress = new InetSocketAddress(addr, port);

                                //建立socket连接，也可以简写为cSocket = new Socket(addr,port);
                                cSocket = new Socket();
                                cSocket.setSoTimeout(10000);         //设置接收消息超时时间为10s
                                cSocket.connect(sAddress, 5000);    //设置连接超时为5s

                                if (cSocket != null && cSocket.isConnected()) {
                                    //建立socket输入输出流
                                    cInput = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
                                    cOutput = new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream()));

                                    //连接成功后设为true
                                    isTcpServiceStartedSucess = true;

                                    //启动Tcp Socket的循环读取服务器数据线程
                                    new Thread(new tcpClientReciever()).start();

                                    //启动心跳包发送线程，共有4中方式可供选择
                                    startHeartBeat4();

                                    //通知UI线程服务器连接成功
                                    cHandler.sendEmptyMessage(0x00);

                                    //连接成功后清零
                                    tcpConnectCount = 0;


                                    Log.i(TAG, "startTcpService Success，连接成功");
                                    break;
                                }

                            } catch (IOException e) {
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
                    //超过最大连接次数且连接失败
                    if (!isTcpServiceStartedSucess) {   //变量清理，并向宿主线程报告服务器暂时无法连接成功
                        tcpConnectCount = 0;
                        cSocket = null;
                        cHandler.sendEmptyMessage(0xE0);
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
                        if (cOutput != null) {
                            try {
                                //tcp数据发送
                                cOutput.write(str);
                                cOutput.flush();

                                System.out.println("Tcp Send:" + str+"---"+notReceiveHeartBeatCount);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }.start();
        }

        //停止TcpSocket服务
        public void stopTcpService() {
                try {
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
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
        //解析传入的ip地址等信息
        ipAddr = intent.getStringExtra("IPADDR");
        port= intent.getIntExtra("PORT", 10000);

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        //解析传入的ip地址等信息
        ipAddr = intent.getStringExtra("IPADDR");
        port= intent.getIntExtra("PORT", 10000);

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
        super.onDestroy();
    }
}
