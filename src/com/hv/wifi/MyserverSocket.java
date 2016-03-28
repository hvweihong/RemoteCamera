package com.hv.wifi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class MyserverSocket {

	public ServerSocket serverSocket = null;
	int bufferSize = 0;
	//构造函数
	public MyserverSocket(int port){
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what==0x11) {
				Bundle bundle = msg.getData();
				System.out.println(bundle.getByteArray("msg").toString());
			}
		};
	};
	
	public void serverSocketStart(){
		new serverSocketThread().start();
	}
	public class serverSocketThread extends Thread{
		
		public byte[] receBuffer = new byte[1024];
		public int receBufferSize = 0;
		@Override
		public void run() {
			Bundle bundle = new Bundle();
			bundle.clear();
			OutputStream output;
			String str = "hello hehe";
			try {
				serverSocket = new ServerSocket(30000);
				while (true) {
					Message msg = new Message();
					msg.what = 0x11;
					try {
						Socket socket = serverSocket.accept();
						output = socket.getOutputStream();
						output.write(str.getBytes("utf-8"));
						output.flush();//刷新输出
						socket.shutdownOutput();
						//mHandler.sendEmptyMessage(0);
						//读取socket并放入recebuffer中，得到读取的数据长度
						receBufferSize = socket.getInputStream().read(receBuffer);
						if(receBufferSize > 0){
							bundle.putByteArray("msg", receBuffer);
							msg.setData(bundle);
							mHandler.sendMessage(msg);
						}
						socket.shutdownInput();
						output.close();
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
}
