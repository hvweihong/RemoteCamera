package com.hv.view;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder.Callback;
import android.content.Context;   
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;   
import android.graphics.Color;
import android.graphics.Paint;   
import android.view.SurfaceHolder;
import android.view.SurfaceView;
public class MySurfaceView extends SurfaceView implements Callback
{
    private SurfaceHolder sfh;    
    private Canvas canvas;    
    URL videoUrl;
    private String urlstr;
    HttpURLConnection conn;
    Bitmap bmp;
    private Paint p;      
    InputStream inputstream=null;
    private Bitmap mBitmap;    
    private static int mScreenWidth; 
    private static int mScreenHeight;
    public boolean Is_Scale = false;
    
    public boolean runFlag = true;//用于保证线程会被执行完毕
    public boolean runToOver = false;
    public boolean isFirstRun = true;
    
    public MySurfaceView(Context context, AttributeSet attrs) 
    {
    	super(context, attrs);
    	initialize();
    	p = new Paint();
    	p.setAntiAlias(true);
    	sfh = this.getHolder();
    	sfh.addCallback(this);
    	this.setKeepScreenOn(true);
    	setFocusable(true);
    	this.getWidth();
    	this.getHeight();  
	}
	
	 private void initialize() 
	 { 
		 
		 //将px转化为dp
		 final float scale = getResources().getDisplayMetrics().density; 
		 int dp = (int) (150 * scale + 10.5f);//这里的100是在显示屏上用了100DP的空间用于显示控制按键，根据需要修改
		 
		DisplayMetrics dm = getResources().getDisplayMetrics(); 
		mScreenWidth = (dm.widthPixels*5)/6; //使用屏幕六分之5的大小
		mScreenHeight = dm.heightPixels;    
		this.setKeepScreenOn(true);//保持屏幕常亮
	 } 
	 class DrawVideo extends Thread
	 {
		 public DrawVideo(){} 
		 
		 @Override
		 protected Object clone() throws CloneNotSupportedException 
		 {
			 // TODO Auto-generated method stub
			 return super.clone();
		 }
		 
		public void run() 
	    { 	
	        Paint pt = new Paint();
			pt.setAntiAlias(true);
			pt.setColor(Color.GREEN);
			pt.setTextSize(20);
			pt.setStrokeWidth(1); 
	        int bufSize = 512 * 1024;	            
	        byte[] jpg_buf = new byte[bufSize];	        // buffer to read jpg  
	        int readSize = 4096;		       
            byte[] buffer = new byte[readSize];	        // buffer to read stream
            HttpURLConnection urlConn = null;
            
	        while (runFlag)
	        {
	        	
	        	long Time = 0;
	    		long Span = 0;   
	            int fps = 0;	            
	            String str_fps = "0 fps";
	            URL url = null;
	            
	            try
	            {
	            	url = new URL(urlstr);	            	
	            	urlConn = (HttpURLConnection)url.openConnection();	//HTTPURLConnetion     	
	            	Time = System.currentTimeMillis();
	                int read = 0;
	                int status = 0;
	                int jpg_count = 0;                          //jpg					 	
	                while (runFlag)
	                {	
	                	runToOver = false;
	                	read = urlConn.getInputStream().read(buffer, 0, readSize);

	                    if (read  > 0)
	                    {	                    	
	                    	
	                        for (int i = 0; i < read; i++)
	                        {
	                            switch (status)
	                            {
	                                //Content-Length:
	                                case 0: if (buffer[i] == (byte)'C') status++; else status = 0; break;
	                                case 1: if (buffer[i] == (byte)'o') status++; else status = 0; break;
	                                case 2: if (buffer[i] == (byte)'n') status++; else status = 0; break;
	                                case 3: if (buffer[i] == (byte)'t') status++; else status = 0; break;
	                                case 4: if (buffer[i] == (byte)'e') status++; else status = 0; break;
	                                case 5: if (buffer[i] == (byte)'n') status++; else status = 0; break;
	                                case 6: if (buffer[i] == (byte)'t') status++; else status = 0; break;
	                                case 7: if (buffer[i] == (byte)'-') status++; else status = 0; break;
	                                case 8: if (buffer[i] == (byte)'L') status++; else status = 0; break;
	                                case 9: if (buffer[i] == (byte)'e') status++; else status = 0; break;
	                                case 10: if (buffer[i] == (byte)'n') status++; else status = 0; break;
	                                case 11: if (buffer[i] == (byte)'g') status++; else status = 0; break;
	                                case 12: if (buffer[i] == (byte)'t') status++; else status = 0; break;
	                                case 13: if (buffer[i] == (byte)'h') status++; else status = 0; break;
	                                case 14: if (buffer[i] == (byte)':') status++; else status = 0; break;
	                                case 15:
	                                    if (buffer[i] == (byte)0xFF) status++; 
	                                    jpg_count = 0;
	                                    jpg_buf[jpg_count++] = (byte)buffer[i];	                                    
	                                    break;
	                                case 16:
	                                    if (buffer[i] == (byte)0xD8)
	                                    {  
	                                        status++;
	                                        jpg_buf[jpg_count++] = (byte)buffer[i];
	                                    }
	                                    else
	                                    {
	                                        if (buffer[i] != (byte)0xFF) status = 15;
	                                        
	                                    }
	                                    break;
	                                case 17:
	                                    jpg_buf[jpg_count++] = (byte)buffer[i];
	                                    if (buffer[i] == (byte)0xFF) status++;
	                                    if (jpg_count >= bufSize) status = 0;
	                                    break;
	                                case 18:
	                                    jpg_buf[jpg_count++] = (byte)buffer[i];
	                                    if (buffer[i] == (byte)0xD9)
	                                    {
	                                        status = 0;
	                                        //jpg
	                                        
	                                        fps++;
	                                    	Span = System.currentTimeMillis()-Time;
	                                    	if(Span > 1000L)
	                                    	{
	                                    		Time = System.currentTimeMillis();
	                                    		str_fps = String.valueOf(fps)+" fps";
	                                    		fps = 0;
	                                    	}	
	                                    	//
	                                    	canvas = sfh.lockCanvas();
	                                    	canvas.drawColor(Color.BLACK);	 
	                                    	Bitmap bmp = BitmapFactory.decodeStream(new ByteArrayInputStream(jpg_buf));
	                                    	
	                                    	int width = mScreenWidth;
	                                    	int height = mScreenHeight;
	                                    	
	                            			float rate_width = (float)mScreenWidth / (float)bmp.getWidth();
	                            			float rate_height = (float)mScreenHeight / (float)bmp.getHeight();
	                            			
	                            			if(Is_Scale)
	                            			{
	                            				if(rate_width>rate_height) width = (int)((float)bmp.getWidth()*rate_height);
	                            				if(rate_width<rate_height) height = (int)((float)bmp.getHeight()*rate_width);

	                            			}	                            			
	                                    	
	                                    	mBitmap = Bitmap.createScaledBitmap(bmp, width, height, false);
	                                    	canvas.drawBitmap(mBitmap, (mScreenWidth-width)/2,(mScreenHeight-height)/2, null);
	                                    	
	           							 	canvas.drawText(str_fps, 2, 22, pt);
	           							 	
	           							 	sfh.unlockCanvasAndPost(canvas);

	                                    }
	                                    else
	                                    {
	                                        if (buffer[i] != (byte)0xFF) status = 17;
	                                    }
	                                    break;
	                                default:
	                                    status = 0;
	                                    break;

	                            }
	                        }
	                    }
	                   // runToOver = true;
	                }
	            }
	            catch (IOException ex)
	            {
	            	urlConn.disconnect();
	            	ex.printStackTrace();
	            }
	            runToOver = true;
	        }
	    }
	}
    	 
    public void surfaceChanged(SurfaceHolder holder, int format, int width,    
            int height) {    
    }    
    public void surfaceDestroyed(SurfaceHolder holder) {    
    	
    }   
    public void GetCameraIP(String p){
    	urlstr=p;
    }
    
    
   public void surfacePause(){
	   while(runToOver);//必须等线程执行完毕才能暂停
	   runFlag = false;
   }
   public void surfaceResume(){
	   runFlag = true; 
   }
   //自动调用，在surface创建时
	public void surfaceCreated(SurfaceHolder holder) {
	    new DrawVideo().start();
	}
}    