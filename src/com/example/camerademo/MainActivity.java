package com.example.camerademo;

import java.io.ByteArrayOutputStream;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;

import java.util.HashMap;

import com.example.camerademo.PersonProcessImage;
import com.example.camerademo.ProcessActivity;
import com.example.util.DesUtil;
import com.example.util.HttpUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter.ViewBinder;

public class MainActivity extends Activity implements OnClickListener {
	private GridView gridView1;  //网格显示缩略图
	private TextView txtmention;
	private Button buttonPublish;              //发布按钮
	private ImageView imageShow;   
	private final int IMAGE_OPEN = 1;      //打开图片标记
	private final int GET_DATA = 2;           //获取处理后图片标记
	private final int TAKE_PHOTO = 3;       //拍照标记
	private String pathImage = null;                     //选择图片路径
	private Bitmap bmp;                             //导入临时图片
	private Bitmap dbmp;
	private Uri imageUri;                            //拍照Uri
	private String pathTakePhoto;              //拍照路径，拍照
	private String bitToStrBase64;
	private String result;
	private Handler handler;
	private String resultRsp="";
	//获取图片上传URL路径 文件夹名+时间命名图片
	//private String[] urlPicture;    
	//存储Bmp图像
	private ArrayList<HashMap<String, Object>> imageItem;
	//适配器
	private SimpleAdapter simpleAdapter;
	//插入PublishId通过Json解析
	//private String publishIdByJson;
	//
	PersonProcessImage personProcess =null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 /*
         * 防止键盘挡住输入框
         * 不希望遮挡设置activity属性 android:windowSoftInputMode="adjustPan"
         * 希望动态调整高度 android:windowSoftInputMode="adjustResize"
         */
        getWindow().setSoftInputMode(WindowManager.LayoutParams.
        		SOFT_INPUT_ADJUST_PAN);
        //锁定屏幕
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_main);
		//获取控件对象
        gridView1 = (GridView) findViewById(R.id.gridView1);
        buttonPublish = (Button) findViewById(R.id.button1);
        imageShow =(ImageView) findViewById(R.id.imageView1);
        txtmention =(TextView) findViewById(R.id.txtmention);
        //自定义引用类
        personProcess = new PersonProcessImage(bmp);
        //发布内容
        buttonPublish.setOnClickListener(this);        
        /*
         * 载入默认图片添加图片加号
         * 通过适配器实现
         * SimpleAdapter参数imageItem为数据源 R.layout.griditem_addpic为布局
         */
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.gridview_addpic); //加号
        imageItem = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("itemImage", bmp);
        map.put("pathImage", "add_pic");
        imageItem.add(map);
        simpleAdapter = new SimpleAdapter(this, 
        		imageItem, R.layout.griditem_addpic, 
                new String[] { "itemImage"}, new int[] { R.id.imageView1});
        /*
         * HashMap载入bmp图片在GridView中不显示,但是如果载入资源ID能显示 如
         * map.put("itemImage", R.drawable.img);
         * 解决方法:
         *              1.自定义继承BaseAdapter实现
         *              2.ViewBinder()接口实现
         *  参考 http://blog.csdn.net/admin_/article/details/7257901
         */
        simpleAdapter.setViewBinder(new ViewBinder() {  
		    @Override  
		    public boolean setViewValue(View view, Object data,  
		            String textRepresentation) {  		     
		        if(view instanceof ImageView && data instanceof Bitmap){  
		            ImageView i = (ImageView)view;  
		            i.setImageBitmap((Bitmap) data);  
		            return true;  
		        }  
		        return false;  
		    }
        });  
        gridView1.setAdapter(simpleAdapter); 
        /*
         * 监听GridView点击事件
         * 报错:该函数必须抽象方法 故需要手动导入import android.view.View;
         */
        gridView1.setOnItemClickListener(new OnItemClickListener() {
  			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id)
			{
  				if( imageItem.size() == 4) { //第一张为默认图片
  					Toast.makeText(MainActivity.this, "图片数9张已满", Toast.LENGTH_SHORT).show();
  				}
  				else if(position == 0) { //点击图片位置为+ 0对应0张图片
  					//Toast.makeText(MainActivity.this, "添加图片", Toast.LENGTH_SHORT).show();
  					AddImageDialog();
  				}
  				else {
  					DeleteDialog(position);
  					//Toast.makeText(MainActivity.this, "点击第" + (position + 1) + " 号图片", 
  					//		Toast.LENGTH_SHORT).show();
  				}
				
			}
  		});  
        
        //更新UI
        handler=new Handler(){
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        String data = (String)msg.obj+"";
                        txtmention.setText(data);                       
                        break;
                    default:
                        break;
                }
            }
        };
	}
	
	 /*
     * 添加图片 可通过本地添加、拍照添加
     */
    protected void AddImageDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    	builder.setTitle("添加图片");
    	builder.setIcon(R.drawable.ic_launcher);
    	builder.setCancelable(false); //不响应back按钮
    	builder.setItems(new String[] {"本地相册选择","手机相机添加","取消选择图片"}, 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					switch(which) {
					case 0: //本地相册
						dialog.dismiss();
						Intent intent = new Intent(Intent.ACTION_PICK,       
		                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  
		                startActivityForResult(intent, IMAGE_OPEN);  
		                //通过onResume()刷新数据
						break;
					case 1: //手机相机
						dialog.dismiss();
						File outputImage = new File(Environment.getExternalStorageDirectory(), "suishoupai_image.jpg");
						pathTakePhoto = outputImage.toString();
						try {
							if(outputImage.exists()) {
								outputImage.delete();
							}
							outputImage.createNewFile();
						} catch(Exception e) {
							e.printStackTrace();
						}
						imageUri = Uri.fromFile(outputImage);
						Intent intentPhoto = new Intent("android.media.action.IMAGE_CAPTURE"); //拍照
						intentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
						startActivityForResult(intentPhoto, TAKE_PHOTO);
						break;
					case 2: //取消添加
						dialog.dismiss();
						break;
					default:
						break;
					}
				}
			});
    	//显示对话框
    	builder.create().show();
    }
    
    /*
     * Dialog对话框提示用户删除操作
     * position为删除图片位置
     */
    protected void DeleteDialog(final int position) {
    	AlertDialog.Builder builder = new Builder(MainActivity.this);
    	builder.setMessage("确认移除已添加图片吗？");
    	builder.setTitle("提示");
    	builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int which) {
    			dialog.dismiss();
    			imageItem.remove(position);
    	        simpleAdapter.notifyDataSetChanged();
    		}
    	});
    	builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    			}
    		});
    	builder.create().show();
    }
    
    
    //获取图片路径 响应startActivityForResult  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
        super.onActivityResult(requestCode, resultCode, data);        
        //打开图片  
        if(requestCode==IMAGE_OPEN) {        
            Uri uri = data.getData();  
            if (!TextUtils.isEmpty(uri.getAuthority())) {  
                //查询选择图片  
                Cursor cursor = getContentResolver().query(  
                        uri,  
                        new String[] { MediaStore.Images.Media.DATA },  
                        null,   
                        null,   
                        null);  
                //返回 没找到选择图片  
                if (null == cursor) {  
                    return;  
                }  
                //光标移动至开头 获取图片路径  
                cursor.moveToFirst();  
                String path = cursor.getString(cursor  
                        .getColumnIndex(MediaStore.Images.Media.DATA));  
                
                //Load up the image's dimensions not the image itself
                BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();  
    	        bmpFactoryOptions.inJustDecodeBounds = true; 
    	        dbmp = BitmapFactory.decodeFile(path,bmpFactoryOptions);
				//图像真正解码
    	        bmpFactoryOptions.inJustDecodeBounds = false;                 
    	        dbmp = BitmapFactory.decodeFile(path,bmpFactoryOptions);
    	        //图片压缩转化为String一步应放在上传时，放在UI界面更新卡顿情况
    	        //bitToStrBase64 = Bitmap2StrByBase64(dbmp);
    	        imageShow.setImageBitmap(dbmp);   	        
			} else {
				Intent intent = new Intent(this, ProcessActivity.class); //主活动->处理活动
				intent.putExtra("path", uri.getPath());
				//startActivity(intent);
				startActivityForResult(intent, GET_DATA);
			}
        }  //end if 打开图片
        
        //拍照
        if(requestCode==TAKE_PHOTO) {  
        	Intent intent = new Intent("com.android.camera.action.CROP"); //剪裁  
            intent.setDataAndType(imageUri, "image/*"); 
            intent.putExtra("scale", true);  
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);  
            //广播刷新相册 
            //自定义广播
            Intent intentBc = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);  
            intentBc.setData(imageUri);       
            this.sendBroadcast(intentBc);      
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();  
	        bmpFactoryOptions.inJustDecodeBounds = true; 
	        dbmp = BitmapFactory.decodeFile(pathTakePhoto,bmpFactoryOptions);
			//图像真正解码
	        bmpFactoryOptions.inJustDecodeBounds = false;                 
	        dbmp = BitmapFactory.decodeFile(pathTakePhoto,bmpFactoryOptions);
	        //bmp复制模板    	        
	        imageShow.setImageBitmap(dbmp);
        }  
        
        //返回OK
        if(resultCode==RESULT_OK){
        	//图像上传 先保存 后传递图片路径
    		try {
    			//Uri 返回一个uri,
				Uri Imguri = personProcess.loadBitmap(dbmp);
				//返回一个
				//Toast.makeText(this, Imguri.toString(), Toast.LENGTH_SHORT).show();
				//扫描指定文件
				Intent intent  = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				intent.setData(Imguri);
				sendBroadcast(intent);
				pathImage = personProcess.pathPicture;
				if(!TextUtils.isEmpty(pathImage)){
					//从文件系统加载
					Bitmap addbmp=BitmapFactory.decodeFile(pathImage);
					HashMap<String, Object> map = new HashMap<String, Object>();
			        map.put("itemImage", addbmp);
			        map.put("pathImage", pathImage);
			        imageItem.add(map);	
			        simpleAdapter = new SimpleAdapter(this, 
			        		imageItem, R.layout.griditem_addpic, 
			                new String[] { "itemImage"}, new int[] { R.id.imageView1}); 
			        //接口载入图片
			        simpleAdapter.setViewBinder(new ViewBinder() {  
					    @Override  
					    public boolean setViewValue(View view, Object data,  
					            String textRepresentation) {  
					        // TODO Auto-generated method stub  
					        if(view instanceof ImageView && data instanceof Bitmap){  
					            ImageView i = (ImageView)view;  
					            i.setImageBitmap((Bitmap) data);  
					            return true;  
					        }  
					        return false;  
					    }
			        }); 
			        gridView1.setAdapter(simpleAdapter);
			        simpleAdapter.notifyDataSetChanged();
					//刷新后释放防止手机休眠后自动添加
			        pathImage = null;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}        		
        }
         
    }
    //Bitmap2StrByBase64
    //把Bitmap转化为String
    public String Bitmap2StrByBase64(Bitmap bit){  
    	ByteArrayOutputStream bos=new ByteArrayOutputStream();
    	//1.5M压缩后大小在100kb以内
    	bit.compress(CompressFormat.JPEG, 40, bos);//参数100表示不压缩  
    	byte[] bytes=bos.toByteArray();  
    	return Base64.encodeToString(bytes, Base64.DEFAULT);  
    }
    
    
    //刷新图片
    @Override
	protected void onResume() {
		super.onResume();
		
	}

	@Override
	public void onClick(View v) {		
    		/*
    		 * 上传图片 进度条显示
    		 * String path = "/storage/emulated/0/DCIM/Camera/lennaFromSystem.jpg";
    		 * upload_SSP_Pic(path,"ranmei");
    		 * Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
    		 */
    		//判断是否添加图片
    		if(imageItem.size()==1) {
    			Toast.makeText(MainActivity.this, "没有图片需要上传", Toast.LENGTH_SHORT).show();
    			return;
    		}else{
    		//需上传的bitmap转化的字符串
    		//bitToStrBase64 bitToStrBase64 = Bitmap2StrByBase64(dbmp);
    		bitToStrBase64 = Bitmap2StrByBase64(dbmp);
    		//子线程耗时操作    			
    		new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL postUrl = new URL("http://192.168.1.106:8080/SchoolServer/uploadPicture");
                        
                        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();

                        connection.setDoOutput(true);

                        connection.setDoInput(true);

                        connection.setRequestMethod("POST");

                        connection.setUseCaches(false);

                        connection.setInstanceFollowRedirects(true);

                        connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

                        connection.connect();
                        //传递参数
                        DataOutputStream out = new DataOutputStream(connection  
                                .getOutputStream());  
                        // 正文，正文内容其实跟get的URL中 '? '后的参数字符串一致  
                        String content = "picture=" + URLEncoder.encode(bitToStrBase64, "UTF-8");  
                        // DataOutputStream.writeBytes将字符串中的16位的unicode字符以8位的字符形式写到流里面  
                        out.writeBytes(content);  
                        //流用完记得关  
                        out.flush();  
                        out.close();
                        
                        if (connection.getResponseCode() == 200) {
                        	// 获取响应的输入流对象  
                            InputStream is = connection.getInputStream(); 
                            // 创建字节输出流对象  
                            ByteArrayOutputStream message = new ByteArrayOutputStream();                         
                            // 定义读取的长度  
                            int len = 0;  
                            // 定义缓冲区  
                            byte buffer[] = new byte[1024];  
                            // 按照缓冲区的大小，循环读取  
                            while ((len = is.read(buffer)) != -1) {  
                                // 根据读取的长度写入到os对象中  
                                message.write(buffer, 0, len);  
                            }  
                            // 释放资源  
                            is.close();  
                            message.close();  
                            // 返回字符串  
                            resultRsp = new String(message.toByteArray());  
                        }
                    } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(0);
                    Message msg =new Message();                 
                    msg.obj =resultRsp;
                    handler.sendMessage(msg);
                }
            }).start();
    	}
			//消息提示
			Toast.makeText(MainActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
    }
	
    
    //上传图片
}
