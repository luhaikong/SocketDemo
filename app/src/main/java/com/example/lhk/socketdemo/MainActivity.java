package com.example.lhk.socketdemo;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText et_to_msg;
    TextView tv_receive_msg;
    Button btn_start,btn_send;
    String toMessage,receiveMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        lock = manager.createMulticastLock("test wifi");

        initViews();
        initListener();
    }

    private void initViews(){
        et_to_msg = (EditText) findViewById(R.id.et_to_msg);
        tv_receive_msg = (TextView) findViewById(R.id.tv_receive_msg);
        btn_start = (Button) findViewById(R.id.start);
        btn_send = (Button) findViewById(R.id.send);
    }

    private void initListener(){
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ReceiveUdp().start();
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toMessage = et_to_msg.getText().toString();
                new BroadCastUdp("hello boxes!",toMessage==""?"测试数据":toMessage).start();
            }
        });
    }


    private WifiManager.MulticastLock lock;
    private WifiManager manager;
    private boolean start = true;
    public static final int DEFAULT_PORT = 10004;
    private static final int MAX_DATA_PACKET_LENGTH = 1024;
    private byte[] buffer = new byte[MAX_DATA_PACKET_LENGTH];
    private byte[] redata = new byte[MAX_DATA_PACKET_LENGTH];

    public class BroadCastUdp extends Thread {
        private String qrcode;
        private String dataString;
        private DatagramSocket udpSocket;
        private int timeout = 0;//收到信息次数，代替计时器

        public BroadCastUdp(String dataString,String qrcode) {
            this.dataString = dataString;
            this.qrcode = qrcode;
        }

        @Override
        public void run() {
            DatagramPacket dataPacket = null, recivedata = null;
            lock.acquire();
            while (start) {
                try {
                    udpSocket = new DatagramSocket(DEFAULT_PORT);

                    dataPacket = new DatagramPacket(buffer, MAX_DATA_PACKET_LENGTH);
                    byte[] data = dataString.getBytes();
                    dataPacket.setData(data);
                    dataPacket.setLength(data.length);
                    dataPacket.setPort(DEFAULT_PORT);

                    InetAddress broadcastAddr;
                    broadcastAddr = InetAddress.getByName("255.255.255.255");
                    dataPacket.setAddress(broadcastAddr);

                    recivedata = new DatagramPacket(redata, MAX_DATA_PACKET_LENGTH);
                } catch (Exception e) {
                    Log.e("TAG1", e.toString());
                }
                try {
                    udpSocket.send(dataPacket);
                    System.out.print("dataPacket地址为：" + dataPacket.getAddress().toString()); //此为IP地址
                    System.out.print("dataPacketsock地址为：" + dataPacket.getSocketAddress().toString()); //此为IP加端口号
                    sleep(100);
                } catch (Exception e) {
                    Log.e("TAG2", e.toString());
                }

                try {
                    udpSocket.receive(recivedata);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Log.e("TAG3", e.toString());
                }
                if (recivedata.getLength() != 0 && recivedata!=null) {
                    final String codeString = new String(redata, recivedata.getOffset(), recivedata.getLength());

                    System.out.println("接收到数据为：" + codeString);
                    System.out.print("recivedataIP地址为：" + recivedata.getAddress().toString()); //此为IP地址
                    System.out.print("recivedata_sock地址为：" + recivedata.getSocketAddress().toString()); //此为IP加端口号
                    timeout++;
                    if (codeString.length()>0&&!codeString.equals("hello boxes!")){
                        start = false;

                        //TODO something
                    }
                    if (timeout==60){
                        start = false;

                        //TODO something
                    }
                }
            }
            if (udpSocket!=null){
                udpSocket.close();
            }
            lock.release();
        }
    }


    public class ReceiveUdp extends Thread{
        @Override
        public void run(){
            int listenPort = 10004;
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            @SuppressWarnings("resource")
            DatagramSocket responseSocket = null;
            try {
                responseSocket = new DatagramSocket(listenPort);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            System.out.println("Server started, Listen port: " + listenPort);
            while (true) {
                try {
                    responseSocket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String rcvd = "Received "
                        + new String(packet.getData(), 0, packet.getLength())
                        + " from address: " + packet.getSocketAddress();
                System.out.println(rcvd);

                // Send a response packet to sender
                String backData = "DCBA";
                byte[] data = backData.getBytes();
                System.out.println("Send " + backData + " to " + packet.getSocketAddress());

                DatagramPacket backPacket = null;
                try {
                    backPacket = new DatagramPacket(data, 0,
                            data.length, packet.getSocketAddress());
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                try {
                    responseSocket.send(backPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
