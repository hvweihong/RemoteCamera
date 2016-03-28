package com.hv.webcam;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.hv.view.MyVideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class ControledView extends Activity implements SurfaceHolder.Callback {
	public final static String CONTROLED_POSITION = "/remoteCamera/controled/";
    public static final int CMDPAGESIZE = 10;
	public ServerSocket serverSocket = null;
	int bufferSize = 0;
	public serverSocketThread myServerSocketThread = null;
    public final String TAG = "Webcam";
    private static Camera mCamera = null;
    private MjpegServer mMjpegServer = null;
    public static TextView textView = null;
    private static String lastFileName = null;
    public boolean takePictureflag = false;
    private boolean runflag = true;
    
    private boolean autoFocusflag = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.v(TAG, "onCreate");
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//设置为没有title模式
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getActionBar().setBackgroundDrawable(
         //       getResources().getDrawable(R.drawable.semi_transparent));
        
        setContentView(R.layout.controled_view);
        textView = (TextView)findViewById(R.id.receText);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.foregroundSurfaceView);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        //在主线程中运行callback
        surfaceHolder.addCallback(this);
        Toast.makeText (ControledView.this, "该手机将作为被遥控端！连接密码12345678", Toast.LENGTH_SHORT).show ();
        //开启接受数据服务接受控制端的控制指令
        myServerSocketThread = new serverSocketThread();
        myServerSocketThread.start();
        
        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        runflag = true;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPuase()");
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            runflag = false;
        }
        if (mMjpegServer != null) {
            mMjpegServer.close();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated()");
        
        int cameraId;
        int previewWidth;
        int previewHeight;
        int rangeMin;
        int rangeMax;
        int quality;
        int port;
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String cameraIdString = preferences.getString("settings_camera", null);       
        String previewSizeString = preferences.getString("settings_size", null);       
        String rangeString = preferences.getString("settings_range", null);
        String qualityString = preferences.getString("settings_quality", "50");
        String portString = preferences.getString("settings_port", "8080");
        
        // if failed, it means settings is broken.
        assert(cameraIdString != null && previewSizeString != null && rangeString != null);
        
        int xIndex = previewSizeString.indexOf("x");
        int tildeIndex = rangeString.indexOf("~");
        
        // if failed, it means settings is broken.
        assert(xIndex > 0 && tildeIndex > 0);
        
        try {
            cameraId = Integer.parseInt(cameraIdString);
            
            previewWidth = Integer.parseInt(previewSizeString.substring(0, xIndex - 1));
            previewHeight = Integer.parseInt(previewSizeString.substring(xIndex + 2));
            
            rangeMin = Integer.parseInt(rangeString.substring(0, tildeIndex - 1));
            rangeMax = Integer.parseInt(rangeString.substring(tildeIndex + 2));
            
            quality = Integer.parseInt(qualityString);
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Settings is broken");
            Toast.makeText(this, "Settings is broken", Toast.LENGTH_SHORT).show();
            
            finish();
            return;
        }
        
        mCamera = Camera.open(cameraId);
        //若没有相机，结束软件
        if (mCamera == null) {
            Log.v(TAG, "Can't open camera" + cameraId);
            
            Toast.makeText(this, getString(R.string.can_not_open_camera),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
        	//绑定holder
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.v(TAG, "SurfaceHolder is not available");
            
            Toast.makeText(this, "SurfaceHolder is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            
            return;
        }
        
        Parameters parameters = mCamera.getParameters();
        //获得手机相机支持的分辨率
        List<Size>  supportedPictureSizes  = parameters.getSupportedPictureSizes();
        //获得手机支持分辨率的个数
        
        int listSize = supportedPictureSizes.size();
        int i ;
        parameters.setPreviewSize(previewWidth, previewHeight);
        parameters.setPreviewFpsRange(rangeMin, rangeMax);
        //将手机的最高分辨率设置为拍照的分辨率
        //parameters.setPictureSize(supportedPictureSizes.get(listSize-1).width,supportedPictureSizes.get(listSize-1).height);
        //取第二大的分辨率，不然传输太久
        //parameters.setPictureSize(supportedPictureSizes.get(listSize-3).width,supportedPictureSizes.get(listSize-3).height);
        
        //最低分辨率
        //parameters.setPictureSize(supportedPictureSizes.get(0).width,supportedPictureSizes.get(0).height);
        //设置为最高分辨率
        if(supportedPictureSizes.get(0).width < supportedPictureSizes.get(listSize - 1).width){
        	parameters.setPictureSize(supportedPictureSizes.get(listSize-1).width,supportedPictureSizes.get(listSize-1).height);
        }else{
        	parameters.setPictureSize(supportedPictureSizes.get(0).width,supportedPictureSizes.get(0).height);
        }
        //parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦  
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        //mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上  
        
        JpegFactory jpegFactory = new JpegFactory(previewWidth, 
                previewHeight, quality);
        mCamera.setPreviewCallback(jpegFactory);
        
        mMjpegServer = new MjpegServer(jpegFactory);
        try {
            mMjpegServer.start(port);
        } catch (IOException e) {
            String message = "Port: " + port + " is not available";
            Log.v(TAG, message);
            
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    	
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed()");
    }
  
    //拍照用
    private final class MyPictureCallback implements PictureCallback {  
    	  
        @Override  
        public void onPictureTaken(byte[] data, Camera camera) {  
            try {  
            	
                saveToSDCard(data,ControledView.this); // 保存图片到sd卡中  
                takePictureflag = true;
                Toast.makeText(getApplicationContext(), "拍照成功",  
                        Toast.LENGTH_SHORT).show();  
                camera.startPreview(); // 拍完照后，重新开始预览  
  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
    }  
    /** 
     * 将拍下来的照片存放在SD卡中 
     * @param data   
     * @throws IOException 
     */  
    public static void saveToSDCard(byte[] data,Context context) throws IOException {  
    	
    	
    	
        Date date = new Date();  
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间  
        String filename = format.format(date) + ".jpg";  
        File fileFolder = new File(Environment.getExternalStorageDirectory()  
                + CONTROLED_POSITION);  
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"finger"的目录  
            fileFolder.mkdir();  
        }  
        
        File jpgFile = new File(fileFolder, filename);  
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流  
        outputStream.write(data); // 写入sd卡中  
        outputStream.close(); // 关闭输出流  
        
        //更新最近拍照图片名字
        lastFileName = filename;
        
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
            		jpgFile.getAbsolutePath(), filename, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新,没有通知是不会更新的
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + CONTROLED_POSITION));
        context.sendBroadcast(intent);
        
    } 
    /*
     * 获取一张图片，若图片的大小超过800*480时进行图片压缩
     * 
     */
    private byte[] getimage(String srcPath) {  
        BitmapFactory.Options newOpts = new BitmapFactory.Options();  
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了  
        newOpts.inJustDecodeBounds = true;  
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath,newOpts);//此时返回bm为空  
          
        newOpts.inJustDecodeBounds = false;  
        int w = newOpts.outWidth;  
        int h = newOpts.outHeight;  
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为  
        float hh = 800f;//这里设置高度为800f  
        float ww = 480f;//这里设置宽度为480f  
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可  
        int be = 1;//be=1表示不缩放  
        
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放  
            be = (int) (newOpts.outWidth / ww);  
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放  
            be = (int) (newOpts.outHeight / hh);  
        }  
        if (be <= 0)  
            be = 1;  
        newOpts.inSampleSize = be;//设置缩放比例  
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了  
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts); 
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();    
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        
        return baos.toByteArray();//压缩好比例大小后再进行质量压缩  
    }  
    
	public static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			byte[] recebff = new byte[1024];
			int readBufferSize = 0;
			if (msg.what==0x11) {
				Bundle bundle = msg.getData();
				recebff = bundle.getByteArray("readBuffer");
				readBufferSize = bundle.getInt("readBufferSize");
				//textView.setText("接受到的数据长度为：" + readBufferSize + "," + (recebff[0]&0xff));
			}
		};
	};
	
	
	
	/*
	 * 线程类，用于启动socketserver
	 * 
	 */

	public class serverSocketThread extends Thread{
		
		public byte[] readBuffer = new byte[1024];
		public int readBufferSize = 0;
		//
		@Override
		public void run() {
			Bundle bundle = new Bundle();
			bundle.clear();
			OutputStream output;
			String str = "IM hv";
			try {
				serverSocket = new ServerSocket(30000);
				while (true) {
					Message msg = new Message();
					msg.what = 0x11;
					try {
						Socket socket = serverSocket.accept();					
						readBufferSize = socket.getInputStream().read(readBuffer);
						if(readBufferSize  == 9){//如果得到的数据流是规定的长度
							
							if(readBuffer[0] == 'a'){
								//对焦指令
								mCamera.autoFocus(null);
							}else if(readBuffer[0] == 't'){
								
								mCamera.takePicture(null, null, new MyPictureCallback());  //拍摄一张图片
								socket.shutdownInput();
								while(!takePictureflag);//等待拍照成功
								takePictureflag = false;
								/*
								//若不为空，则将图片发送出去
								if(lastImagedata != null){
									
									DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
									int size = lastImagedata.length;
									dos.writeInt(size);    //先发送图片的大小
									dos.write(lastImagedata); 		//发送图片
									dos.flush();//刷新输出
									dos.close();
									lastImagedata = null;
									
								}
								*/
								//从sd卡中读取出刚刚保存的图片
								 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());    
						         //对图片进行压缩后再发送	,以减少发送时间			         
						         byte[] fis = getimage(Environment.getExternalStorageDirectory() + CONTROLED_POSITION + lastFileName);
						         int size = fis.length;     
						         dos.writeInt(size);    
						         dos.write(fis);    
						         dos.flush();    
						         dos.close();       
										
								bundle.putByteArray("readBuffer", readBuffer);
								bundle.putInt("readBufferSize", readBufferSize);
								msg.setData(bundle);
								mHandler.sendMessage(msg);
								
							}
							

						}
						socket.shutdownOutput();
						
						//output.close();
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
