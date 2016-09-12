package com.example.service;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.example.action.CameraInterface;
import com.example.action.CameraInterface.CamOpenOverCallback;
import com.example.action.ProtocolServerAction;
import com.example.action.TestSocket;
import com.example.action.TestSocket.DoReceiveFoo;
import com.example.bean.AvcEncoder;
import com.example.bean.IConst;
import com.example.ekuiservice.R;
import com.example.utils.JniUtil;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification.Action;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.audiofx.BassBoost.Settings;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MainService extends Service implements CamOpenOverCallback,PreviewCallback ,IConst,DoReceiveFoo,Callback{

	SurfaceView mDummySurfaceView;
	TestSocket mgr;
	ProtocolServerAction mPmgr;
	
	Context mContext;
	DoFrameThread doFrameThread = null;
	AvcEncoder mAvcEncoder = null;
	byte [] mH264 = null;
	boolean isPreviewing = false;

	@Override
	public IBinder onBind(Intent intent) {
		Log.i("123", "service onBind");
		return null;
	}
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i("123", "service onUnbind");
		return super.onUnbind(intent);
	}
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.i("123", "serrvice  on create");
		mContext = this;
//		mgr = TestSocket.getInstance();
//
//		mgr.register(this);
		
		mPmgr = ProtocolServerAction.getInstance();
		mPmgr.setContext(this);
//		createSurfaceView();
		initNotification();
		super.onCreate();
	}
	
	private void createSurfaceView(){
		 SurfaceView dummyView = new SurfaceView(mContext);
	        SurfaceHolder holder = dummyView.getHolder();
	        holder.addCallback(this);
	        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
	        
	        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
	                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
	                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
	                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
	                PixelFormat.TRANSPARENT);
	        params.gravity = Gravity.TOP | Gravity.RIGHT;
	        params.alpha = PixelFormat.TRANSPARENT;
	        params.x = params.y = 0;
	        
	        wm.addView(dummyView, params);
	        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	        	dummyView.setAlpha(PixelFormat.TRANSPARENT);
	        }

	        //dummyView.getBackground().setAlpha(PixelFormat.TRANSPARENT);
	        mDummySurfaceView = dummyView;
	        
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.i("123", "service onStartCommand");
//		mgr.init(SERVICE_PORT, CLIENT_PORT);
//		mgr.startSocket();
		
		mPmgr.init();
		
		return super.onStartCommand(intent, flags, startId);
		
		
		
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		mgr.unRegister(this);
		mgr.deInit();
		CameraInterface.getInstance().doStopCamera();

		Log.i("123", "service onDestroy");
		//		Intent intent = new Intent("RESTART_SERVER");
		//		sendBroadcast(intent);

		super.onDestroy();
	}

	@Override
	public void cameraHasOpened() {
		// TODO Auto-generated method stub
		Log.d("123", "carmera has opened");
		
		new Thread(){
			public void run() {
				
//				CameraInterface.getInstance().doStartPreview(mDummySurfaceView.getHolder(),MainService.this);
				CameraInterface.getInstance().doStartPreview(MainService.this);
				isPreviewing = true;
				if(doFrameThread == null){
					doFrameThread = new DoFrameThread();
					doFrameThread.start();
				}
				if (mAvcEncoder == null) {
					mAvcEncoder = CameraInterface.getInstance().getAvcEncoder();
					mH264 = CameraInterface.getInstance().getH264Buffer();
				}
				
				
			};
		}.start();
		
		
	}


	private void openCamera(){
		Log.i("123", "open carmera");
		new Thread(){
			public void run() {
				CameraInterface.getInstance().doOpenCamera(MainService.this);
			};
		}.start();
	}
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		Log.i("123", "on preview frame");
		CameraInterface.getInstance().recoveryCallbackData(data);//
		final Size size = camera.getParameters().getPreviewSize();
		int w = size.width;
		int h = size.height;
		int len = w*h+w*h/2;
		FrameData frameData = new FrameData(data, w, h);
		if (doFrameThread!=null) {
			doFrameThread.pushData(frameData);
		}
	}
	@Override
	public void foo(byte[] data, int len) {
		// TODO Auto-generated method stub
		byte [] buf = new byte[len];
		System.arraycopy(data, 0, buf, 0, len);
		String str =new String(buf);
		Log.i("123","len="+len+ "   ip="+str);
		mgr.setDestinationIP(str);
		openCamera();
	}

	public class FrameData{
		byte [] data;
		int w,h;
		public byte[] getData() {
			return data;
		}
		public void setData(byte[] data) {
			this.data = data;
		}
		public int getW() {
			return w;
		}
		public void setW(int w) {
			this.w = w;
		}
		public int getH() {
			return h;
		}
		public void setH(int h) {
			this.h = h;
		}
		public FrameData(byte[] data, int w, int h) {
			super();
			this.data = data;
			this.w = w;
			this.h = h;
		}

	}

	boolean [] bI = new boolean[1];
	public class DoFrameThread extends Thread{
		private Queue<FrameData> pushDataQueue =  new ArrayBlockingQueue<FrameData>(2);
		public void pushData(FrameData frameData){
			pushDataQueue.offer(frameData);

		}
		private FrameData popData(){
			return pushDataQueue.poll();
		}

		public DoFrameThread(){
			bSending = true;
		}
		private int frameNum = 0;
		private long startTime = 0;
		private long endTime = 0;
		private boolean bDebug = true;
		private boolean bSending = false;
		public void endSend(){
			bSending = false;
		}
		@SuppressLint("NewApi")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(bSending){
				FrameData frameData = popData();
				if (frameData!=null) {
					//					Log.e("123", "do one frame");
					byte [] data = frameData.getData();
					int w = frameData.getW();
					int h = frameData.getH();
					if (bDebug) {
						if (frameNum==0) {
							startTime = System.currentTimeMillis();
							Log.i("123", "start ="+startTime);
						}
						frameNum++;
						if (frameNum==25) {
							endTime = System.currentTimeMillis();
							Log.i("123", "end ="+endTime);
							long spendTime = endTime-startTime;
							Log.e("123", "25 frame spend time="+spendTime);
							frameNum=0;
						}
					}
					int h264Len = mAvcEncoder.offerEncoder(data, mH264,bI);
					byte [] buf = JniUtil.H264toHWStream(mH264, h264Len, bI[0]?1:0);
					Log.i("123", "buf len="+buf.length);
					TestSocket.getInstance().sendPacket(buf, buf.length);

					//					try {//FIXME time spend on encode
					//						sleep(40);
					//					} catch (InterruptedException e) {
					//						// TODO Auto-generated catch block
					//						e.printStackTrace();
					//					}

				}else{
					//					try {
					////						Log.e("123", "no frame");
					//						sleep(40);
					//					} catch (InterruptedException e) {
					//						// TODO Auto-generated catch block
					//						e.printStackTrace();
					//					}
				}
			}
			super.run();
		}
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}
	
	private void initNotification(){
		NotificationManager nm = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
		Notification.Builder nb = new Notification.Builder(this);
		nb.setContentTitle("Ekui后台服务正在运行");
		nb.setContentText("点击关闭服务");
		nb.setSmallIcon(R.drawable.ic_launcher);
		nb.setWhen(System.currentTimeMillis());
		nb.setOngoing(true);
		
		Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		Uri uri = Uri.fromParts("package", "com.example.ekuiservice", null);
		intent.setData(uri);
		
		
		PendingIntent pendingIntent = 
				PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		nb.setContentIntent(pendingIntent);	
		nm.notify(0, nb.build());
		//取消消息(通过id);
//		nm.cancel(1);
	}
	

}
