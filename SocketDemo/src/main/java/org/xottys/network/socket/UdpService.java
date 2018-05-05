/**
 * 将Udp Socket的所有操作定义成Service，要点如下：
 * 1）在onBind中获取IP地址，然后启动startUdpService，其中主要完成udp关键变量创建和启动数据读取线程
 * 2）在Binder中单独定义发送数据方法sendMessage和停止udp的方法stopUdpService
 * 3）读取数据的线程是循环阻塞的
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:UdpService
 * <br/>Date:July，2017
 *
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.socket;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpService extends Service {
    final String TAG = "UdpSocketService";
    //控制Udp socket读写循环变量
    private boolean isUdpServiceStarted = false;
    //Udp通信地址
    private String ipAddr;
    private int clientport;
    private int serverport;

    private byte[] buffer = new byte[1024];

    //传递数据给UI的Handler
    private Handler cHandler;

    //客户端Udp Socket通信的两个基本变量
    private DatagramSocket datagramSocket = null;
    private DatagramPacket datagramPacket = null;



    @Override
    public void onCreate() {
        super.onCreate();
    }

    //该服务会持续存在，直到外部组件调用Context.stopService方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    //第一次BindService时会首先回调本方法，此时启动Udp关键变量初始化服务
    @Override
    public IBinder onBind(Intent intent) {
        //解析传入的ip地址等信息
        ipAddr = intent.getStringExtra("IPADDR");
        clientport= intent.getIntExtra("CLIENTPORT", 10000);
        serverport= intent.getIntExtra("SERVERPORT", 10001);

        UdpServiceBinder mBinder = new UdpServiceBinder();
        Log.i(TAG, "onBind:---- ");
        return mBinder;
    }


    @Override
    public void onRebind(Intent intent) {
        //解析传入的ip地址等信息
        ipAddr = intent.getStringExtra("IPADDR");
        clientport= intent.getIntExtra("CLIENTPORT", 10000);
        serverport= intent.getIntExtra("SERVERPORT", 10001);

        Log.i(TAG, "UdpService is Rebinded");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "UdpService is Unbinded");
        super.onUnbind(intent);
        return true;   //onRebind()被调用的前提条件
    }


    @Override
    public void onDestroy() {
        //stopUdpService();
        super.onDestroy();
        Log.i(TAG, "UdpService is Destroyed");
    }

     //Udp Socket read 线程（阻塞）
    private class udpClientReciever implements Runnable {
        @Override
        public void run() {
            while (isUdpServiceStarted) {
                Message message = Message.obtain();
                message.what = 2;
                String msgFromServer = null;
                try {
                    //receive是阻塞的
                    datagramSocket.receive(datagramPacket);
                    msgFromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    Log.i(TAG, "udp read: " + msgFromServer);

                } catch (IOException e) {
                    if (isUdpServiceStarted) {
                        isUdpServiceStarted = false;
                        e.printStackTrace();
                        msgFromServer = "Error";
                    }
                }
                if (msgFromServer != null) {
                    message.obj = msgFromServer;
                    cHandler.sendMessage(message);
                    Log.i(TAG, "udp: " + msgFromServer);
                }
            }
        }
    }

    //将本Service中对外开放使用的方法皆放入其中，如：socket数据发送、socket服务停止等方法
    public class UdpServiceBinder extends Binder {
        //获取UdpService实例
        public UdpService getUdpService() {
            return UdpService.this;
        }

        //设置handler
        public void setHandler(Handler handler) {
            cHandler = handler;
        }
        //启动UdpSocket服务
        public void startUdpService() {
            new Thread() {
                @Override
                public void run() {
            if (!isUdpServiceStarted) {
                try {

                    isUdpServiceStarted = true;

                    //初始化udp报文关键变量
                    datagramSocket = new DatagramSocket(clientport);
                    datagramPacket = new DatagramPacket(buffer, buffer.length);

                    //启动Udp Socket的循环读取服务器数据线程
                    new Thread(new udpClientReciever()).start();

                    cHandler.sendEmptyMessage(0x00);

                } catch (IOException e) {
                    isUdpServiceStarted = false;
                    e.printStackTrace();
                    cHandler.sendEmptyMessage(0xE0);
                }

            }
        }
            }.start();
        }
        //往服务器端发送数据
        public void sendMessage(final String str) {
            new Thread() {
                @Override
                public void run() {
                    if (datagramSocket != null) {
                        try {
                            InetAddress addr = InetAddress.getByName(ipAddr);
                            buffer = str.getBytes();

                            //udp报文发送
                            datagramSocket.send(new DatagramPacket(buffer, str.length(), addr, serverport));

                            System.out.println("Udp Send:" + str);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }

        // 停止UdpSocket服务
        public void stopUdpService() {
            if (isUdpServiceStarted && datagramSocket != null) {
                isUdpServiceStarted = false;
                datagramSocket.close();
                datagramSocket = null;
                datagramPacket = null;
                stopSelf();
            }
        }
    }
}