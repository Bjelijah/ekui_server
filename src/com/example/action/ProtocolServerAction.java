package com.example.action;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import com.example.action.CameraInterface.CamOpenOverCallback;
import com.example.bean.AvcEncoder;
import com.example.bean.FrameData;
import com.example.bean.IConst;
import com.example.bean.IProtocol;
import com.example.bean.ISocketConnect;
import com.example.bean.ProtocolFactory;
import com.example.bean.ProtocolFactory.ProtocolType;
import com.example.utils.JniUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ProtocolServerAction implements CamOpenOverCallback,PreviewCallback,IConst{
	private static ProtocolServerAction mInstance = null;
	public static ProtocolServerAction getInstance(){
		if (mInstance == null) {
			mInstance = new ProtocolServerAction();
		}
		return mInstance;
	}
	private ProtocolServerAction(){}
	Context context;
	
	public Context getContext() {
		return context;
	}
	public void setContext(Context context) {
		this.context = context;
	}
	IProtocol mp;
	
	String clientIP;
	int handleNum;
	
	DoFrameThread doFrameThread = null;
	AvcEncoder mAvcEncoder = null;
	byte [] mH264 = null;
	
	boolean isPreviewing = false;
	
	Timer timer;
	
	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case IProtocol.MSG_LOGIN_ACK:
				if(msg.obj!=null){
					clientIP = (String)msg.obj;
					Log.i("123", "get msg login ack in instance ip="+clientIP);
					Bundle b = new Bundle();
					b.putString(ISocketConnect.BUNDLE_DESTINATION_IP, clientIP);
					b.putInt(ISocketConnect.BUNDLE_DESTINATION_PORT, CLIENT_PORT);
					mp.setInfo(b);
				}
			
				break;
			case IProtocol.MSG_LOGOUT_ACK:
				if (msg.obj!=null) {
					handleNum = (Integer) msg.obj;
				}
//				endAct();
				break;
			case IProtocol.MSG_START_VIDEO:
				startAct();
				startTimeTask();
				break;
			case IProtocol.MSG_STOP_VIDEO:
				endAct();
				stopTimeTask();
				break;
				
			default:
				break;
			}
			super.handleMessage(msg);
		}
		
	};
	
	
	public void init(){
		mp = ProtocolFactory.getProtocol(ProtocolType.HW5198);
		mp.setHandler(handler);
		mp.connect();
	}
	
	private void startAct(){
		//TODO do start
		if (isPreviewing) {
			Log.e("123", "camera is already preview");
			return;
		}
		openCamera();
	}
	
	private void endAct(){
		//end
		CameraInterface.getInstance().doStopCamera();
		isPreviewing = false;
		
		
	}
	
	private void openCamera(){
		Log.i("123", "open carmera");
		new Thread(){
			public void run() {
				CameraInterface.getInstance().doOpenCamera(ProtocolServerAction.this);
			};
		}.start();
	}
	
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub

		CameraInterface.getInstance().recoveryCallbackData(data);//
		final Size size = camera.getParameters().getPreviewSize();
		int w = size.width;
		int h = size.height;
		int len = w*h+w*h/2;
//		Log.i("123", "onpreviewframe   len="+data.length+" w="+w+" h="+h+"   buf="+len);
//		JniUtil.YUVsetData(_data, _data.length, size.width, size.height);
		
		FrameData frameData = new FrameData(data, w, h);
		if (doFrameThread!=null) {
			doFrameThread.pushData(frameData);
		}
	}
	@Override
	public void cameraHasOpened() {
		Log.i("123", "start preview");
		CameraInterface.getInstance().doStartPreview(ProtocolServerAction.this);
		isPreviewing = true;
		if(doFrameThread == null){
			doFrameThread = new DoFrameThread();
			doFrameThread.start();
		}
		if (mAvcEncoder == null) {
			mAvcEncoder = CameraInterface.getInstance().getAvcEncoder();
			mH264 = CameraInterface.getInstance().getH264Buffer();
		}
	}
	
	
	public class DoFrameThread extends Thread{
		private Queue<FrameData> pushDataQueue =  new ArrayBlockingQueue<FrameData>(25);
		public void pushData(FrameData frameData){
			pushDataQueue.offer(frameData);
		}
		private FrameData popData(){
			return pushDataQueue.poll();
		}

		public DoFrameThread(){}
		private int frameNum = 0;
		private long startTime = 0;
		private long endTime = 0;
		private boolean bDebug = true;
		boolean [] bI = new boolean[1];
		@SuppressLint("NewApi")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(isPreviewing){
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


					//					byte [] yuv420 = new byte[data.length];
					////					DisplayUtil.YUV420SP2YUV420(data, yuv420, w, h);
					//					DisplayUtil.YV12toYUV420Planar(data, yuv420, w, h);
					//					JniUtil.YUVsetData(yuv420, yuv420.length,w, h);

					int h264Len = mAvcEncoder.offerEncoder(data, mH264,bI);
//					byte [] buf = JniUtil.H264toHWStream(mH264, h264Len, bI[0]?1:0);
					//					Log.i("123", "buf len="+buf.length);
//					TestSocket.getInstance().sendPacket(buf, buf.length);
					mp.pushStream(bI[0],mH264, h264Len);
					
//					try {//FIXME time spend on encode
//						sleep(40);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}

				}else{
					try {
//						Log.e("123", "no frame");
						sleep(40);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			super.run();
		}
	}
	
	
	
	private void startTimeTask(){
		timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d("123", "run time task");
				boolean isNoClient = JniUtil.isNoClient();
				if (isNoClient) {
					Log.e("123", "no client!!!!!!!");
					handler.sendEmptyMessage(IProtocol.MSG_STOP_VIDEO);
				}
			}
		},1000, 1000*60*2);
	
	}
	
	private void stopTimeTask(){
		if (timer==null) {
//			throw new NullPointerException("timer == null");
			Log.e("123", "timer == null return");
			return;
		}
		timer.cancel();
		timer.purge();
		timer = null;
	}
	
}
