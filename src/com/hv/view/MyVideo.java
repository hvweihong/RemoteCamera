package com.hv.view;


import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.hv.webcam.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MyVideo extends Activity{
	public final static String CONTROLER_POSITION = "/remoteCamera/controler/";
	private RelativeLayout relativeLayout;
	Socket socket = null;
	String buffer = "";
	MySurfaceView r1;
	ImageButton TakePhotoBtn = null;
	ImageButton takeVideoBtn = null;
	ImageButton settingBtn = null;
	ImageView receImageView = null;
	boolean buttonFlag = true;
	 private static String lastFileName = null;
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//设置为没有title模式
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
         
        //获取wifi服务  
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);  
        //判断wifi是否开启  
        if (!wifiManager.isWifiEnabled()) {  
        wifiManager.setWifiEnabled(true);    
        }  
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();       
        int ipAddress = wifiInfo.getIpAddress();   
        //将获得的ip地址转化为8位8位的格式，并将主机号用1代替，表示被控制端的IP地址
        String ip = intToIp(ipAddress);    
        setContentView(R.layout.myvideo);
        
		this.r1=(MySurfaceView) this.findViewById(R.id.mySurfaceViewVideo1);
		this.relativeLayout=(RelativeLayout) this.findViewById(R.id.relativeLayout);
		String CameraIp="http://" + ip + ":8080/?action=stream";
		r1.GetCameraIP(CameraIp);
		findView();
    }

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		r1.surfacePause();

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		r1.surfaceResume();
	}
	
	
	//监听音量键事件，用于音量键拍照,用于想把手机放在口袋中就能实现拍照的情况
	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
	// 获取手机当前音量值
	switch (keyCode) {
	// 音量减小
	case KeyEvent.KEYCODE_VOLUME_DOWN:
	//发送拍照指令
    new MyThread("takephoto").start();
    Toast.makeText (MyVideo.this, "拍照指令已发送，等待图片回传！ ", Toast.LENGTH_SHORT).show ();
	return true;
	// 音量增大
	case KeyEvent.KEYCODE_VOLUME_UP:
	return true;
	}
	return super.onKeyDown (keyCode, event);
	}

	void findView(){
		TakePhotoBtn = (ImageButton)findViewById(R.id.takePhoto);
		takeVideoBtn = (ImageButton)findViewById(R.id.takeVideoBtn);
		settingBtn = (ImageButton)findViewById(R.id.settingBtn);
		//receImageView = (ImageView)findViewById(R.id.receImageView);
		this.relativeLayout.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				new MyThread("antufocus").start();
				
			}
		});
		
		TakePhotoBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//发送拍照指令
                new MyThread("takephoto").start();
                Toast.makeText (MyVideo.this, "拍照指令已发送，等待图片回传！ ", Toast.LENGTH_SHORT).show ();
			}
		});
		
		//拍照按钮监听函数
		TakePhotoBtn.setOnTouchListener(new OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				 if(event.getAction() == MotionEvent.ACTION_DOWN){     
                     //更改为按下时的背景图片     
                     ((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.take_photo_down)); 
                     
	             }else if(event.getAction() == MotionEvent.ACTION_UP){     
	                     //改为抬起时的图片     
	            	 ((ImageButton)v).setImageDrawable(getResources().getDrawable(R.drawable.take_photo));    
	             }     
				return false;
				}
 		});
		
		
		
		//摄像按钮监听函数
		takeVideoBtn.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				 if(event.getAction() == MotionEvent.ACTION_DOWN){     
                     //更改为按下时的背景图片     
                     
	             }else if(event.getAction() == MotionEvent.ACTION_UP){     
	                     //改为抬起时的图片     
	            	     
	             }     
				return false;
			}
			

		});
		
		//设置按钮监听函数
		settingBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
					//跳转查看图片
				if(lastFileName != null){
					Intent intent = new Intent(MyVideo.this,MyImageView.class);
					intent.setType(lastFileName);
					startActivity(intent);
				}
			}
		});
     }
    
    //ip地址转化函数
    private String intToIp(int i) {       
        
        return (i & 0xFF ) + "." +       
      ((i >> 8 ) & 0xFF) + "." +       
      ((i >> 16 ) & 0xFF) + "." + 1;  //最后的主机号转化为1
   } 
    /** 
     * 将接收到的照片存放在SD卡中 
     * @param data   
     * @throws IOException 
     */  
    public static void saveToSDCard(byte[] data,Context context) throws IOException {  

        Date date = new Date();  
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间  
        String filename = format.format(date) + ".jpg";  
        File fileFolder = new File(Environment.getExternalStorageDirectory()  
                + CONTROLER_POSITION);  
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录  
            fileFolder.mkdir();  
        }  
        File jpgFile = new File(fileFolder, filename);  
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流  
        outputStream.write(data); // 写入sd卡中  
        outputStream.close(); // 关闭输出流  
        
        lastFileName = filename;
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
            		jpgFile.getAbsolutePath(), filename, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新,没有通知是不会更新的
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + CONTROLER_POSITION));
        context.sendBroadcast(intent);
    } 
    
    
    /*
     * socket相关
     * 
     */
	public Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0x11) {
				Bundle bundle = msg.getData();
				int size = bundle.getInt("size");
				System.out.println("接受到一次数据大小为:" + size);
				byte[] data = new byte[size];
				data = bundle.getByteArray("msg");
                try {
				saveToSDCard(data,MyVideo.this);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                //byte转化为指定大小，放置作为imagebutton的背景
                Bitmap Bigbmp = BitmapFactory.decodeByteArray(data, 0, data.length); 
                Bitmap bitmap = zoomImage(Bigbmp,60,60);
                settingBtn.setImageBitmap(bitmap); 
                //有拍摄到图片后控件才可见
                settingBtn.setVisibility(View.VISIBLE);
                Toast.makeText (MyVideo.this, "图片回传成功！ ", Toast.LENGTH_SHORT).show ();
			}
		}

	};
	/*
    * 图片的缩放方法
    *
    * @param bgimage
    *            ：源图片资源
    * @param newWidth
    *            ：缩放后宽度
    * @param newHeight
    *            ：缩放后高度dp
    * @return
    */ 
   public Bitmap zoomImage(Bitmap bgimage, double newWidth, 
                   double newHeight) { 
		 //将px转化为dp
		 final float scale = getResources().getDisplayMetrics().density; 
		 int dpnewWidth = (int) (newWidth * scale + 10.5f);//进行dp与pix转化
		 int dpnewHeight = (int) (newHeight * scale + 10.5f);//进行dp与pix转化
		 
           // 获取这个图片的宽和高 
           float width = bgimage.getWidth(); 
           float height = bgimage.getHeight(); 
           // 创建操作图片用的matrix对象 
           Matrix matrix = new Matrix(); 
           // 计算宽高缩放率 
           float scaleWidth = ((float) dpnewWidth) / width; 
           float scaleHeight = ((float) dpnewHeight) / height; 
           // 缩放图片动作 
           matrix.postScale(scaleWidth, scaleHeight); 
           Bitmap bitmap = Bitmap.createBitmap(bgimage, 0, 0, (int) width, 
                           (int) height, matrix, true); 
           return bitmap; 
   } 
	class MyThread extends Thread {

		public String txt1;

		public MyThread(String str) {
			txt1 = str;
		}

		@Override
		public void run() {
			//定义消息
			Message msg = new Message();
			msg.what = 0x11;
			Bundle bundle = new Bundle();
			bundle.clear();
			try {
				//连接服务器 并设置连接超时为5秒
				socket = new Socket();
				socket.connect(new InetSocketAddress("192.168.43.1", 30000), 1000);
				//获取输入输出流
				OutputStream ou = socket.getOutputStream();
				DataInputStream dataInput = new DataInputStream(socket.getInputStream()); 	
				
				//向服务器发送信息
				ou.write(txt1.getBytes("gbk"));
				ou.flush();
				
				//获取图片
				int size = dataInput.readInt(); 
				byte[] data = new byte[size]; 
				int len = 0;    
                while (len < size) {    
                    len += dataInput.read(data, len, size - len);    
                } 
                bundle.putInt("size", size);
                bundle.putByteArray("msg", data);              		
				msg.setData(bundle);
				//发送消息 修改UI线程中的组件
				myHandler.sendMessage(msg);
				
				//关闭各种输入输出流
                dataInput.close();
				ou.close();
				socket.close();
                
			} catch (SocketTimeoutException aa) {
				//连接超时 在UI界面显示消息
				bundle.putString("msg", "服务器连接失败！请检查网络是否打开");
				msg.setData(bundle);
				//发送消息 修改UI线程中的组件
				myHandler.sendMessage(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    
}


