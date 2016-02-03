package com.xnote;

import static com.mob.tools.utils.R.getStringRes;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Random;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.RegisterPage;

import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXEmojiObject;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXMusicObject;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX.Req;
import com.tencent.mm.sdk.modelmsg.WXVideoObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


public class MainActivity extends Activity {
	public final static String APP_ID = "wx72873857e262c6b8"; 
	private IWXAPI api;
	private CheckBox mCircleCheck;
	private static String url = "http://www.baidu.com/img/bd_logo1.png";
	
	//短信验证的APPz_KEY和APP Secret
	public final static String APP_KEY = "f32ebca8c099";
	private final static String APP_SECRET = "809e2fb5dae13570bd4ff47ec4e34319";
	private final static String TAG = "XNOTE_WX_SMS";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        api = WXAPIFactory.createWXAPI(this, APP_ID);
        //将APP_ID注册到微信中
        api.registerApp(APP_ID);
        
        mCircleCheck = (CheckBox) findViewById(R.id.checkBox1);
        
        //初始化短信验证SDK
        SMSSDK.initSDK(this, APP_KEY, APP_SECRET);
        
    }
    //启动微信
    public void onClickLauchWeixin(View view){
    	boolean bool = api.openWXApp();
    	Toast.makeText(this, String.valueOf(bool), Toast.LENGTH_SHORT).show();
    }
    
    //分享文本
    public void onClickShareText(View view){
    	final EditText editor = new EditText(this);
    	editor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    	editor.setText("默认的分享文本");
    	
    	final AlertDialog.Builder builder = new Builder(MainActivity.this);
    	builder.setIcon(R.drawable.ic_launcher);
    	builder.setTitle("共享文本");
    	builder.setView(editor);
    	builder.setMessage("请输入要分享的文本");
    	builder.setPositiveButton("分享", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				String shareText = editor.getText().toString();
				if(shareText == null || shareText.length() == 0){
					return;
				}
				//1. 创建用于封装文本的WXTextObject对象，封装了文本信息
				WXTextObject textObj = new WXTextObject();
				textObj.text = shareText;
				//2. 创建WXMediaMessage对象，该对象封装了WXTextObject，用于向微信发送数据
				WXMediaMessage msg = new WXMediaMessage();
				msg.mediaObject = textObj;
				msg.description = shareText;
				//3. 创建请求微信的SendMessageToWX.Req对象，封装WXMediaMessage对象
				SendMessageToWX.Req req = new SendMessageToWX.Req();
				req.message = msg;
				req.transaction = buildTransaction("text");
				//发送给朋友还是朋友圈
				req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
				//4. 发送文本给微信
				boolean send = api.sendReq(req);
				Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
			}
		});
    	builder.setNegativeButton("取消", null);
    	final AlertDialog dialog = builder.create();
    	dialog.show();
    }
	protected String buildTransaction(String type) {
		// TODO Auto-generated method stub
		return (type == null)?""+System.currentTimeMillis():type+System.currentTimeMillis();
	}
	
	//二机制图像
	public void onClickSendBinImage(View view){
		//1. 获取二进制图像的Bitmap对象
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		//2. 创建WXImageObject对象，封装bitmap
		WXImageObject imgObj = new WXImageObject(bitmap);
		//3. 创建WXMediaMessage对象，封装WXImageObject对象
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		//4. 压缩图像
		Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 120, 160, true);
		bitmap.recycle(); //释放资源
		msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
		//5. 创建请求微信的SendMessageToWX.Req对象，封装WXMediaMessage对象
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//6. 发送图像给微信
		boolean send = api.sendReq(req);
		Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
	}
	//将bitmap转换成Byte[]
	private byte[] bmpToByteArray(Bitmap thumbBmp, boolean b) {
		// TODO Auto-generated method stub
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		thumbBmp.compress(Bitmap.CompressFormat.PNG, 100, output);
		if(b){
			thumbBmp.recycle();
		}
		byte[] result = output.toByteArray();
		try{
			output.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
    //分享本地图像
	public void onClickSendLocalImage(View view){
		//1. 判断图像是否存在
		String path = "/sdcard/test.jpg";
		File file = new File(path);
		if(!file.exists()){
			Toast.makeText(MainActivity.this, "文件不存在", 1).show();
			return;
		}
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		//2. 创建WXImageObject对象，设置图像文件的路径（封装图像）
		WXImageObject imgObj = new WXImageObject();
		imgObj.setImagePath(path);//设置图像文件的路径
		//3. 创建WXMediaMessage对象，封装WXImageObject对象
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		//4. 压缩图像
		Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 120, 160, true);
		bitmap.recycle(); //释放资源
		msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
		//5. 创建请求微信的SendMessageToWX.Req对象，封装WXMediaMessage对象
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//6. 发送图像给微信
		boolean send = api.sendReq(req);
		Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
	}
    //分享URL图像
	public void onClickSendURLImage(View view){
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					Bitmap bitmap = BitmapFactory.decodeStream(new URL(url).openStream());
					
					//1. 创建WXImageObject对象，设置图片的url地址（封装url图像）
					WXImageObject imgObj = new WXImageObject();
					imgObj.imageUrl = url;//设置图像的url
					//2. 创建WXMediaMessage对象，封装WXImageObject对象
					WXMediaMessage msg = new WXMediaMessage();
					msg.mediaObject = imgObj;
					//3. 压缩图像
					Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 120, 160, true);
					bitmap.recycle(); //释放资源
					msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
					//4. 创建请求微信的SendMessageToWX.Req对象，封装WXMediaMessage对象
					SendMessageToWX.Req req = new SendMessageToWX.Req();
					req.message = msg;
					req.transaction = buildTransaction("img");
					req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
					//5. 发送图像给微信
					boolean send = api.sendReq(req);
					Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	//发送url音频
	public void onClickSendUrlAudio(View view){
		//1. 创建WXMusicObject，指定url
		WXMusicObject music = new WXMusicObject();
		music.musicUrl = "http://music.baidu.com/song/999104?pst=sug";
		//2. 创建WXMediaMessage对象，封装WXMusicObject
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = music;
		msg.title = "五百年";
		msg.description = "hanlei";
		//3. 设置缩略图
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.imooc);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
		//4. 创建SendMessageToWX.Req对象
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. 发送图像给微信
		boolean send = api.sendReq(req);
	}
	//分享视频
	public void onClickSendVideo(View view){
		//1. 创建WXVideoObject，指定url
		WXVideoObject video = new WXVideoObject();
		video.videoUrl = "http://v.youku.com/v_show/id_XNTUxNDY1NDY4.html";
		//2. 创建WXMediaMessage对象，封装WXMusicObject
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = video;
		msg.title = "Jobs";
		msg.description = "Jobs";
		//3. 设置缩略图
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
		//4. 创建SendMessageToWX.Req对象
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("video");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. 发送图像给微信
		boolean send = api.sendReq(req);
	}
	
	//分享url
	public void onClickSendUrl(View view){
		//1. 创建WXWebpageObject,指定其url地址
		WXWebpageObject webObj = new WXWebpageObject();
		webObj.webpageUrl = "http://www.imooc.com/sources/list";
		//2. 创建WXMediaMessage，封装WXWebpageObject
		WXMediaMessage msg = new WXMediaMessage(webObj);
		msg.title = "慕课网";
		msg.description = "在线IT教学网站";
		//3. 设置缩略图
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
		//4. 创建SendMessageToWX.Req对象
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("url");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. 发送url给微信
		api.sendReq(req);
	}
	
	//分享表情
	public void onClickSendEmotion(View view){
		String emotion = "/sdcard/1.gif";
		//1. 創建WXEmojiObject,指定emotion路径
		WXEmojiObject emojiObj = new WXEmojiObject();
		emojiObj.emojiPath = emotion;
		//2. 创建WXMediaMessage,封装WXEmojiObject对象
		WXMediaMessage msg = new WXMediaMessage(emojiObj);
		msg.title = "表情";
		msg.description = "表情描述";
		//3. 设置缩略图
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//设置缩略图
		//4. 创建SendMessageToWX.Req对象
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. 发送url给微信
		api.sendReq(req);
	}
	
	public void onClickSmsVerify(View view){
		//注册手机号
//		RegisterPage registerPage = new RegisterPage();
		MyRegisterPage myRegisterPage = new MyRegisterPage();
		//注册回调事件
		myRegisterPage.setRegisterCallback(new EventHandler(){
			//事件完成后调用
			@Override
			public void afterEvent(int event, int result, Object data) {
				Log.i(TAG, "registe callback afterEvent()");
				// TODO Auto-generated method stub
				if (result == SMSSDK.RESULT_COMPLETE) {
					//获取数据
					HashMap<String, Object> map = (HashMap<String, Object>) data;
					String countryId = (String) map.get("country");
					String phoneNo = (String) map.get("phone");
					
					//提交用户信息
					Random r = new Random();
					String uid = Math.abs(r.nextInt()) + "";
					String nickName = "IMOOC";
					Log.i(TAG, "Submit user info");
					SMSSDK.submitUserInfo(uid, nickName, null, countryId, phoneNo);
				}
			}
		});
		//显示注册界面
		Log.i(TAG, "Show register page...");
		myRegisterPage.show(MainActivity.this);
	}
}
