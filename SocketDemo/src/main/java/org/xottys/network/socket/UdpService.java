/**
 * 将Udp Socket的所有操作定义成Service，要点如下：
 * 1）在Binder中定义Udp启动、停止、数据发送方法
 * 2）在Udp启动方法中开启Udp数据读取线程，该线程是循环阻塞的，其中调用发送方法向客户端发送数据
 * 3）通过设置超时和启动心跳包来检测Udp服务器是否工作正常，否则关闭Udp服务
 * 4）通过地址复用的设置保证多次启动服务器时无需更换端口号
 * 5）Udp广播只需使用广播地址即可，而组播采用了DatagramSocket的子类MulticastSocket
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
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

public class UdpService extends Service {
    private static final String TAG = "UdpSocket";
    //控制Udp socket读写循环变量
    private boolean isUdpServiceStarted = false;
    //Udp通信地址
    private String ipAddr;
    private int clientport;
    private int serverport;
    private int outtimeCounter;

    private byte[] buffer = new byte[1024];

    //传递数据给UI的Handler
    private Handler uHandler;

    //组播接收用
    private MulticastSocket multicastDatagramSocket;
    static String multicastHost="239.0.0.1";

    //客户端Udp Socket通信的两个基本变量
    private DatagramSocket datagramSocket = null;
    private DatagramPacket datagramPacket = null;
    //控制tcp socket读写循环变量
    private static final int HEARTBEAT_INTERVAL = 3 * 1000;         //心跳包间隔设为5分钟

    private int notReceiveHeartBeatCount = 0;    //心跳包发出后未收到服务器返回的实时次数　
    private UdpServiceBinder mBinder;

    
    //该服务会持续存在，直到外部组件调用Context.stopService方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    //第一次BindService时会首先回调本方法
    @Override
    public IBinder onBind(Intent intent) {

        mBinder = new UdpServiceBinder();
        Log.i(TAG, "-----onBind:---- ");
        return mBinder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "UdpService will Destroyed");
    }

    //Udp Socket read 线程（阻塞）
    private class udpClientReciever implements Runnable {
        String msgFromServer ;
        @Override
        public void run() {
            while (isUdpServiceStarted) {
                Message message = Message.obtain();
                try {
                    byte[] bufferRecived = new byte[1024];
                    datagramPacket = new DatagramPacket(bufferRecived, bufferRecived.length);
                    //单播接收数据
                    datagramSocket.receive(datagramPacket);

                    /*组播接收数据，目前一直收不到数据，可能是Android系统屏蔽了组播包
                    multicastDatagramSocket.receive(datagramPacket);*/

                    notReceiveHeartBeatCount = 0;
                    msgFromServer = new String(datagramPacket.getData(), 0, datagramPacket.getLength(), "UTF-8");
                    if (!msgFromServer.contains("XAH")) {
                        message.what = 1;
                        message.obj = msgFromServer;
                        uHandler.sendMessage(message);
                    }
                } catch (IOException e) {
                    if (isUdpServiceStarted) {
                        outtimeCounter++;
                        e.printStackTrace();
                        msgFromServer = "Error1";
                        Log.i(TAG, "notReceiveHeartBeatCount: " +notReceiveHeartBeatCount);
                        if (notReceiveHeartBeatCount > 3) {
                            isUdpServiceStarted = false;
                            mBinder.stopUdpService();
                            uHandler.sendEmptyMessage(0xE4);
                            break;
                        }
                    } else {
                        msgFromServer = "Error2";
                    }
                }
                if (msgFromServer.contains("Error")) {
                    message.what = 0xE3;
                    message.obj = msgFromServer;
                    uHandler.sendMessage(message);
                }
                Log.i(TAG, "Udp Received: " + msgFromServer);

            }
            Log.i(TAG, "Outof Receive Loop :"+ msgFromServer);
        }
    }

    //将本Service中对外开放使用的方法皆放入其中，如：socket数据发送、socket服务停止等方法
    public class UdpServiceBinder extends Binder {

        //设置socket通信地址
        public void setSocketAddres(String ip, int server_port, int client_port) {
            ipAddr = ip;
            serverport = server_port;
            clientport = client_port;
        }

        //设置handler
        public void setHandler(Handler handler) {
            uHandler = handler;
        }

        //启动UdpSocket服务
        public void startUdpService() {
            new Thread() {
                @Override
                public void run() {
                    if (!isUdpServiceStarted) {
                        try {

                            isUdpServiceStarted = true;
                            //单播udp socket启动
                            datagramSocket = new DatagramSocket(null);
                            datagramSocket.setReuseAddress(true);
                            datagramSocket.bind(new InetSocketAddress(clientport));
                            datagramSocket.setSoTimeout(10000);

                            /*组播udp socket启动
                            multicastDatagramSocket = new MulticastSocket(clientport);
                            multicastDatagramSocket.joinGroup(InetAddress.getByName(multicastHost));*/

                            datagramPacket = new DatagramPacket(buffer, buffer.length);
                            //启动Udp Socket的循环读取服务器数据线程
                            new Thread(new udpClientReciever()).start();
                            startHeartBeat();
                            uHandler.sendEmptyMessage(0x00);

                        } catch (SocketException e) {
                            isUdpServiceStarted = false;
                            e.printStackTrace();
                            outtimeCounter++;
                            if (outtimeCounter > 3) {
                                uHandler.sendEmptyMessage(0xE0);
                                mBinder.stopUdpService();
                            } else
                                startUdpService();
                        }
//                        catch(IOException ex){
//                            ex.printStackTrace();
//                            isUdpServiceStarted = false;
//                            ex.printStackTrace();
//                            outtimeCounter++;
//                            if (outtimeCounter > 3) {
//                                uHandler.sendEmptyMessage(0xE0);
//                                mBinder.stopUdpService();
//                            } else
//                                startUdpService();
//                        }

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
                            byte[] bufferSend = str.getBytes("UTF-8");
                            Log.i(TAG, "Udp Send Length:" + str.length());
                            //udp报文单播或广播发送，取决于ipAddr是单播地址还是广播地址
                            InetAddress addr = InetAddress.getByName(ipAddr);
                            datagramSocket.send(new DatagramPacket(bufferSend, bufferSend.length, addr, serverport));

                            /*组播发送数据
                            InetAddress addrs = InetAddress.getByName(multicastHost);
                            multicastDatagramSocket.send(new DatagramPacket(bufferSend, bufferSend.length, addrs, serverport));*/


                            Log.i(TAG, "Udp Send:" + str+"port--"+serverport);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }

        // 停止UdpSocket服务
        public void stopUdpService() {
            if (isUdpServiceStarted) {
                uHandler.removeCallbacks(runnable);
                isUdpServiceStarted = false;
                datagramSocket.close();
                datagramSocket = null;
                datagramPacket = null;
                uHandler.sendEmptyMessage(10);
//              stopSelf();
            }
        }
    }
    
    //通过递归调用实现心跳包
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isUdpServiceStarted) {
                mBinder.sendMessage("XAH");
                notReceiveHeartBeatCount++;
                Log.i(TAG, "startHeartBeat:" + notReceiveHeartBeatCount);
                //延迟一会儿后再次启动自己
                uHandler.postDelayed(this, HEARTBEAT_INTERVAL);
                //超过最大允许次数则重新启动服务器连接
            }
        }
    };

    //启动心跳包
    private void startHeartBeat() {
        new Thread(runnable).start();
        Log.i(TAG, "startHeartBeat启动");
    }
}
