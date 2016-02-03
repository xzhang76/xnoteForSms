package com.xnote;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        api = WXAPIFactory.createWXAPI(this, APP_ID);
        //��APP_IDע�ᵽ΢����
        api.registerApp(APP_ID);
        
        mCircleCheck = (CheckBox) findViewById(R.id.checkBox1);
        
    }
    //����΢��
    public void onClickLauchWeixin(View view){
    	boolean bool = api.openWXApp();
    	Toast.makeText(this, String.valueOf(bool), Toast.LENGTH_SHORT).show();
    }
    
    //�����ı�
    public void onClickShareText(View view){
    	final EditText editor = new EditText(this);
    	editor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    	editor.setText("Ĭ�ϵķ����ı�");
    	
    	final AlertDialog.Builder builder = new Builder(MainActivity.this);
    	builder.setIcon(R.drawable.ic_launcher);
    	builder.setTitle("�����ı�");
    	builder.setView(editor);
    	builder.setMessage("������Ҫ������ı�");
    	builder.setPositiveButton("����", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				String shareText = editor.getText().toString();
				if(shareText == null || shareText.length() == 0){
					return;
				}
				//1. �������ڷ�װ�ı���WXTextObject���󣬷�װ���ı���Ϣ
				WXTextObject textObj = new WXTextObject();
				textObj.text = shareText;
				//2. ����WXMediaMessage���󣬸ö����װ��WXTextObject��������΢�ŷ�������
				WXMediaMessage msg = new WXMediaMessage();
				msg.mediaObject = textObj;
				msg.description = shareText;
				//3. ��������΢�ŵ�SendMessageToWX.Req���󣬷�װWXMediaMessage����
				SendMessageToWX.Req req = new SendMessageToWX.Req();
				req.message = msg;
				req.transaction = buildTransaction("text");
				//���͸����ѻ�������Ȧ
				req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
				//4. �����ı���΢��
				boolean send = api.sendReq(req);
				Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
			}
		});
    	builder.setNegativeButton("ȡ��", null);
    	final AlertDialog dialog = builder.create();
    	dialog.show();
    }
	protected String buildTransaction(String type) {
		// TODO Auto-generated method stub
		return (type == null)?""+System.currentTimeMillis():type+System.currentTimeMillis();
	}
	
	//������ͼ��
	public void onClickSendBinImage(View view){
		//1. ��ȡ������ͼ���Bitmap����
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		//2. ����WXImageObject���󣬷�װbitmap
		WXImageObject imgObj = new WXImageObject(bitmap);
		//3. ����WXMediaMessage���󣬷�װWXImageObject����
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		//4. ѹ��ͼ��
		Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 120, 160, true);
		bitmap.recycle(); //�ͷ���Դ
		msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
		//5. ��������΢�ŵ�SendMessageToWX.Req���󣬷�װWXMediaMessage����
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//6. ����ͼ���΢��
		boolean send = api.sendReq(req);
		Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
	}
	//��bitmapת����Byte[]
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
    //������ͼ��
	public void onClickSendLocalImage(View view){
		//1. �ж�ͼ���Ƿ����
		String path = "/sdcard/test.jpg";
		File file = new File(path);
		if(!file.exists()){
			Toast.makeText(MainActivity.this, "�ļ�������", 1).show();
			return;
		}
		Bitmap bitmap = BitmapFactory.decodeFile(path);
		//2. ����WXImageObject��������ͼ���ļ���·������װͼ��
		WXImageObject imgObj = new WXImageObject();
		imgObj.setImagePath(path);//����ͼ���ļ���·��
		//3. ����WXMediaMessage���󣬷�װWXImageObject����
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = imgObj;
		//4. ѹ��ͼ��
		Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 120, 160, true);
		bitmap.recycle(); //�ͷ���Դ
		msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
		//5. ��������΢�ŵ�SendMessageToWX.Req���󣬷�װWXMediaMessage����
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//6. ����ͼ���΢��
		boolean send = api.sendReq(req);
		Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
	}
    //����URLͼ��
	public void onClickSendURLImage(View view){
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					Bitmap bitmap = BitmapFactory.decodeStream(new URL(url).openStream());
					
					//1. ����WXImageObject��������ͼƬ��url��ַ����װurlͼ��
					WXImageObject imgObj = new WXImageObject();
					imgObj.imageUrl = url;//����ͼ���url
					//2. ����WXMediaMessage���󣬷�װWXImageObject����
					WXMediaMessage msg = new WXMediaMessage();
					msg.mediaObject = imgObj;
					//3. ѹ��ͼ��
					Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, 120, 160, true);
					bitmap.recycle(); //�ͷ���Դ
					msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
					//4. ��������΢�ŵ�SendMessageToWX.Req���󣬷�װWXMediaMessage����
					SendMessageToWX.Req req = new SendMessageToWX.Req();
					req.message = msg;
					req.transaction = buildTransaction("img");
					req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
					//5. ����ͼ���΢��
					boolean send = api.sendReq(req);
					Toast.makeText(MainActivity.this, ""+send, Toast.LENGTH_SHORT).show();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	//����url��Ƶ
	public void onClickSendUrlAudio(View view){
		//1. ����WXMusicObject��ָ��url
		WXMusicObject music = new WXMusicObject();
		music.musicUrl = "http://music.baidu.com/song/999104?pst=sug";
		//2. ����WXMediaMessage���󣬷�װWXMusicObject
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = music;
		msg.title = "�����";
		msg.description = "hanlei";
		//3. ��������ͼ
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.imooc);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
		//4. ����SendMessageToWX.Req����
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("img");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. ����ͼ���΢��
		boolean send = api.sendReq(req);
	}
	//������Ƶ
	public void onClickSendVideo(View view){
		//1. ����WXVideoObject��ָ��url
		WXVideoObject video = new WXVideoObject();
		video.videoUrl = "http://v.youku.com/v_show/id_XNTUxNDY1NDY4.html";
		//2. ����WXMediaMessage���󣬷�װWXMusicObject
		WXMediaMessage msg = new WXMediaMessage();
		msg.mediaObject = video;
		msg.title = "Jobs";
		msg.description = "Jobs";
		//3. ��������ͼ
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
		//4. ����SendMessageToWX.Req����
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("video");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. ����ͼ���΢��
		boolean send = api.sendReq(req);
	}
	
	//����url
	public void onClickSendUrl(View view){
		//1. ����WXWebpageObject,ָ����url��ַ
		WXWebpageObject webObj = new WXWebpageObject();
		webObj.webpageUrl = "http://www.imooc.com/sources/list";
		//2. ����WXMediaMessage����װWXWebpageObject
		WXMediaMessage msg = new WXMediaMessage(webObj);
		msg.title = "Ľ����";
		msg.description = "����IT��ѧ��վ";
		//3. ��������ͼ
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
		//4. ����SendMessageToWX.Req����
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("url");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. ����url��΢��
		api.sendReq(req);
	}
	
	//�������
	public void onClickSendEmotion(View view){
		String emotion = "/sdcard/1.gif";
		//1. ����WXEmojiObject,ָ��emotion·��
		WXEmojiObject emojiObj = new WXEmojiObject();
		emojiObj.emojiPath = emotion;
		//2. ����WXMediaMessage,��װWXEmojiObject����
		WXMediaMessage msg = new WXMediaMessage(emojiObj);
		msg.title = "����";
		msg.description = "��������";
		//3. ��������ͼ
		Bitmap thumbBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		msg.thumbData = bmpToByteArray(thumbBmp, true);//��������ͼ
		//4. ����SendMessageToWX.Req����
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.message = msg;
		req.transaction = buildTransaction("");
		req.scene = mCircleCheck.isChecked()?SendMessageToWX.Req.WXSceneTimeline:SendMessageToWX.Req.WXSceneSession;
		//5. ����url��΢��
		api.sendReq(req);
	}
}
