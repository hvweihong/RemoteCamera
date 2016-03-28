package com.hv.view;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import com.hv.webcam.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class MyImageView extends Activity {
	public final static String CONTROLER_POSITION = "/remoteCamera/controler/";
	ImageView previewImage = null;
	private String fileName = null;
	Intent intent = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        intent = getIntent();
        fileName =  intent.getType();
        System.out.println(">>>>>>>" + fileName);
        setContentView(R.layout.myimageview);
        
        previewImage = (ImageView)findViewById(R.id.previewImage);

		FileInputStream f = null;
		try {
			System.out.println("1111111111");
			f = new FileInputStream(Environment.getExternalStorageDirectory()  
	                + CONTROLER_POSITION + fileName);
			System.out.println("2222222");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("》》》》》》》》》》加载路径出错");
			e.printStackTrace();
		} 
		Bitmap bm = null; 
		BitmapFactory.Options options = new BitmapFactory.Options(); 
		options.inSampleSize = 1;//图片的长宽都是原来的 
		BufferedInputStream bis = new BufferedInputStream(f); 
		bm = BitmapFactory.decodeStream(bis, null, options); 
		if(bm != null){
            previewImage.setImageBitmap(bm);
            previewImage.setVisibility(View.VISIBLE);
		}
	}
}
