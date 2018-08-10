package com.example.camerademo;

import com.example.camerademo.R;

import com.example.camerademo.PersonProcessImage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ProcessActivity extends Activity {
	private Bitmap bmp;                          //载入图片
	private Bitmap mbmp;  
	private ImageView imageShow;         //显示图片
	
	PersonProcessImage personProcess = null;
	
	private LinearLayout layoutPerson;            //图片上传
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_process);
		imageShow = (ImageView) findViewById(R.id.imageView1);
		Intent intent = getIntent();
	    //Toast.makeText(this, "传递参数", Toast.LENGTH_SHORT).show();
	    String path = intent.getStringExtra("path"); //对应putExtra("path", path);
	    ShowPhotoByImageView(path); 
	}
	
	
	private void ShowPhotoByImageView(String path)
	{
		if (null == path) {
			Toast.makeText(this, "载入图片失败", Toast.LENGTH_SHORT).show();
			finish();
		}
		/*
		 * 问题:
		 * 获取Uri不知道getStringExtra()没对应uri参数
		 * 使用方法Uri uri=Uri.parse(path)获取路径不能显示图片
		 * mBitmap=BitmapFactory.decodeFile(path)方法不能适应大小
		 * 解决:
		 * 但我惊奇的发现decodeFile(path,opts)函数可以实现,哈哈哈
		 */
		//获取分辨率
		DisplayMetrics dm = new DisplayMetrics();  
	    getWindowManager().getDefaultDisplay().getMetrics(dm);  
	    int width = dm.widthPixels;    //屏幕水平分辨率  
	    int height = dm.heightPixels;  //屏幕垂直分辨率  
	    try {
	    	//Load up the image's dimensions not the image itself  
	        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();  
	        bmpFactoryOptions.inJustDecodeBounds = true; 
	        bmp = BitmapFactory.decodeFile(path,bmpFactoryOptions);
	        int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);  
	        int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);  
	        //压缩显示
	        if(heightRatio>1&&widthRatio>1) {  
	            if(heightRatio>widthRatio) {  
	                bmpFactoryOptions.inSampleSize = heightRatio*2;  
	            }  
	            else {  
	                bmpFactoryOptions.inSampleSize = widthRatio*2;  
	            }  
	        }  
	        //图像真正解码   
	        bmpFactoryOptions.inJustDecodeBounds = false;                 
	        bmp = BitmapFactory.decodeFile(path,bmpFactoryOptions);
	        //bmp复制模板
	        mbmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
	        imageShow.setImageBitmap(bmp); //显示照片	    
	        
	    } catch(Exception e) {   
            e.printStackTrace();    
        }    
	}
}
