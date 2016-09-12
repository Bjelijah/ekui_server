package com.example.action;

import java.io.IOException;
import java.util.List;

import com.example.bean.AvcEncoder;
import com.example.utils.CamParaUtil;
import com.example.utils.FileUtil;
import com.example.utils.ImageUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;



@SuppressWarnings("deprecation")
public class CameraInterface {
	private static final String TAG = "CameraInterface";  

	private Camera mCamera;  
	private Camera.Parameters mParams;  
	private boolean isPreviewing = false;  
	private float mPreviwRate = -1f;  
	private static CameraInterface mCameraInterface;  
	private int rotation;
//	private byte [] mbuffer = new byte [2*1024*1024];
	private PreviewCallback mCallback = null;
	private SurfaceTexture mSurfaceTexture;
	private AvcEncoder mAvcEncoder = null;
	private byte [] h264 = null;
	public AvcEncoder getAvcEncoder(){
		return mAvcEncoder;
	}
	public byte [] getH264Buffer(){
		return h264;
	}
	public void setPreviewCallback(PreviewCallback callback){
		this.mCallback = callback;
	}
	public void setRotation(int rotation){
		this.rotation = rotation;
		setCameraDisplayOrientation();
	}

	public void updataSurfaceTexture(){
		mSurfaceTexture.updateTexImage();
	}
	
	public interface CamOpenOverCallback{  
		public void cameraHasOpened();  
	}  

	private CameraInterface(){  

	}  

	public static synchronized CameraInterface getInstance(){  
		if(mCameraInterface == null){  
			mCameraInterface = new CameraInterface();  
		}  
		return mCameraInterface;  
	}  


	public boolean isPreviewing(){
		return isPreviewing;
	}

	public void doStartPreview(SurfaceHolder holder,  float previewRate){
		Log.i(TAG, "doStartPreview...");  
		if(isPreviewing){  
			mCamera.stopPreview();  
			return;  
		}  
		if(mCamera != null){  
			try {  
				mCamera.setPreviewDisplay(holder);  
			} catch (IOException e) {  
				// TODO Auto-generated catch block  
				e.printStackTrace();  
			}  
			initCamera(previewRate,false);  
		}  
	}

	public void doStartPreview(SurfaceHolder holder,PreviewCallback callback){
		if (isPreviewing) {
			mCamera.stopPreview();
		}
		if(mCamera != null){  
//			mCamera.addCallbackBuffer(mbuffer);  
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			setPreviewCallback(callback);
			initCamera(1.33f,true);  
		}  	
	}
	

	

	public void doStartPreview(SurfaceTexture surface, float previewRate){  
		Log.i(TAG, "doStartPreview...");  
		if(isPreviewing){  
			mCamera.stopPreview();  
			return;  
		}  
		if(mCamera != null){  
			try {  
				mCamera.setPreviewTexture(surface);  
			
			} catch (IOException e) {  
				// TODO Auto-generated catch block  
				e.printStackTrace();  
			}  
			initCamera(previewRate,false);  
		}  
	}


	public void doStartPreview(PreviewCallback callback){
		if (isPreviewing) {
			mCamera.stopPreview();
		}
		if(mCamera != null){  
			try {
				int[] textures = new int[1];
				GLES20.glGenTextures(1, textures, 0);
				mSurfaceTexture = new SurfaceTexture(textures[0]);
				mCamera.setPreviewTexture(mSurfaceTexture);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			mCamera.addCallbackBuffer(mbuffer);  
			
			setPreviewCallback(callback);
			initCamera(1.33f,true);  
		}  	
	}
	
	public void recoveryCallbackData(byte [] data){
		mCamera.addCallbackBuffer(data);  
	}
	
	public void setDisplayOrientation(boolean isHorizontal){
		if (isHorizontal) {
			mCamera.setDisplayOrientation(0);  
		}else{
			mCamera.setDisplayOrientation(90);  
		}
	}

	@SuppressWarnings("deprecation")
	private void initCamera(float previewRate,boolean byuv){  
		if(mCamera != null){  

			mParams = mCamera.getParameters();  
			int [] range = new int [2];
			mParams.getPreviewFpsRange(range);
			
			Log.i("123", "range="+range[0]+"     "+range[1]);
		
			mParams.setPreviewFpsRange(10000, 25000);
			
			mParams.setPictureFormat(PixelFormat.JPEG);//设置拍照后存储的图片格式  
			//	          CamParaUtil.getInstance().printSupportPictureSize(mParams);  
			//	          CamParaUtil.getInstance().printSupportPreviewSize(mParams);  
			//设置PreviewSize和PictureSize  
			Size pictureSize = CamParaUtil.getInstance().getPropPictureSize(  
					mParams.getSupportedPictureSizes(),previewRate, 500);  
			mParams.setPictureSize(pictureSize.width, pictureSize.height); 
			
			
			Size previewSize = CamParaUtil.getInstance().getPropPreviewSize(  
					mParams.getSupportedPreviewSizes(), previewRate, 500);  
			
			
			
			
			
			mParams.setPreviewSize(previewSize.width, previewSize.height);  
			//			mParams.setPreviewSize(1280, 720);
			mParams.setPreviewFormat(ImageFormat.YV12);
			
			
			mCamera.setDisplayOrientation(0);  
//			setCameraDisplayOrientation();

			//	          CamParaUtil.getInstance().printSupportFocusMode(mParams);  
			List<String> focusModes = mParams.getSupportedFocusModes();  
			if(focusModes.contains("continuous-video")){  
				mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);  
			}  
			mCamera.setParameters(mParams);  
			
			if (byuv && mCallback!=null) {
//				mCamera.setPreviewCallbackWithBuffer(mCallback);
				Log.i("123", "new avc encoder");
				Log.i("123", "set preview callback");
//				mCamera.setPreviewCallback(mCallback);
			
				mCamera.setPreviewCallbackWithBuffer(mCallback);
				mCamera.addCallbackBuffer(new byte[previewSize.width*previewSize.height*3/2]);  
				mAvcEncoder = new AvcEncoder(previewSize.width, previewSize.height, 25, 1000000);
				
				h264 = new byte [previewSize.width*previewSize.height*3/2];
				
			}
			
			mCamera.startPreview();//开启预览  



			isPreviewing = true;  
			mPreviwRate = previewRate;  

			mParams = mCamera.getParameters(); //重新get一次  
			Log.i(TAG, "最终设置:PreviewSize--With = " + mParams.getPreviewSize().width  
					+ "Height = " + mParams.getPreviewSize().height);  
			Log.i(TAG, "最终设置:PictureSize--With = " + mParams.getPictureSize().width  
					+ "Height = " + mParams.getPictureSize().height);  
		}  


	}


	private void setCameraDisplayOrientation(){
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(0, info);

		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		default:
			break;
		}
		Log.i("123", "rotation="+rotation+"   degreses="+degrees);
		int result;  
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {  
			result = (info.orientation + degrees) % 360;  
			result = (360 - result) % 360;   // compensate the mirror  
		} else {  
			// back-facing  
			result = ( info.orientation - degrees + 360) % 360;  
		}  
		Log.i("123", "setDisplayOrientation result="+result);
		if (mCamera!=null) {
			mCamera.setDisplayOrientation (result);  
		}
	
	}

	public void setCameraDisplayOrientation(int rotation){
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(0, info);

		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		default:
			break;
		}
		Log.i("123", "rotation="+rotation+"   degreses="+degrees);
		int result;  
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {  
			result = (info.orientation + degrees) % 360;  
			result = (360 - result) % 360;   // compensate the mirror  
		} else {  
			// back-facing  
			result = ( info.orientation - degrees + 360) % 360;  
		}  
		Log.i("123", "setDisplayOrientation result="+result);
		mCamera.setDisplayOrientation (result);  
	}


	public void doOpenCamera(CamOpenOverCallback callback){  
		Log.i(TAG, "Camera open....");  
		mCamera = Camera.open(1);  

		Log.i(TAG, "Camera open over....");  
		if (callback!=null) {
			callback.cameraHasOpened();  
		}

	}  

	public void doStartPreviewEx(SurfaceHolder holder, float previewRate){  
		Log.i(TAG, "doStartPreview...");  
		if(isPreviewing){  
			mCamera.stopPreview();  
			return;  
		}  
		if(mCamera != null){  

			mParams = mCamera.getParameters();  
			mParams.setPictureFormat(PixelFormat.JPEG);//设置拍照后存储的图片格式  
			CamParaUtil.getInstance().printSupportPictureSize(mParams);  
			CamParaUtil.getInstance().printSupportPreviewSize(mParams);  
			//设置PreviewSize和PictureSize  
			Size pictureSize = CamParaUtil.getInstance().getPropPictureSize(  
					mParams.getSupportedPictureSizes(),previewRate, 800);  
			mParams.setPictureSize(pictureSize.width, pictureSize.height);  
			Size previewSize = CamParaUtil.getInstance().getPropPreviewSize(  
					mParams.getSupportedPreviewSizes(), previewRate, 800);  
			mParams.setPreviewSize(previewSize.width, previewSize.height);  

			mCamera.setDisplayOrientation(90);  

			CamParaUtil.getInstance().printSupportFocusMode(mParams);  
			List<String> focusModes = mParams.getSupportedFocusModes();  
			if(focusModes.contains("continuous-video")){  
				mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);  
			}  
			mCamera.setParameters(mParams);   

			try {  
				mCamera.setPreviewDisplay(holder);  
				mCamera.startPreview();//开启预览  
			} catch (IOException e) {  
				// TODO Auto-generated catch block  
				e.printStackTrace();  
			}  

			isPreviewing = true;  
			mPreviwRate = previewRate;  

			mParams = mCamera.getParameters(); //重新get一次  
			Log.i(TAG, "最终设置:PreviewSize--With = " + mParams.getPreviewSize().width  
					+ "Height = " + mParams.getPreviewSize().height);  
			Log.i(TAG, "最终设置:PictureSize--With = " + mParams.getPictureSize().width  
					+ "Height = " + mParams.getPictureSize().height);  
		}  
	}  

	public void doStopCamera(){  
		if(null != mCamera)  
		{  
			mCamera.setPreviewCallback(null);  
			mCamera.stopPreview();   
			isPreviewing = false;   
			mPreviwRate = -1f;  
			mCamera.release();  
			mCamera = null;       
		}  
	}  

	public void doTakePicture(){  
		if(isPreviewing && (mCamera != null)){  
			mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);  
		}  
	}  


	/*为了实现拍照的快门声音及拍照保存照片需要下面三个回调变量*/  
	ShutterCallback mShutterCallback = new ShutterCallback()  	{   
		//快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。  
		public void onShutter() {  
			// TODO Auto-generated method stub  
			Log.i(TAG, "myShutterCallback:onShutter...");  
		}  
	};  

	PictureCallback mRawCallback = new PictureCallback()   {  
		// 拍摄的未压缩原数据的回调,可以为null  
		public void onPictureTaken(byte[] data, Camera camera) {  
			// TODO Auto-generated method stub  
			Log.i(TAG, "myRawCallback:onPictureTaken...");  

		}  
	};  

	PictureCallback mJpegPictureCallback = new PictureCallback()     { 
		//对jpeg图像数据的回调,最重要的一个回调  

		public void onPictureTaken(byte[] data, Camera camera) {  
			// TODO Auto-generated method stub  
			Log.i(TAG, "myJpegCallback:onPictureTaken...");  
			Bitmap b = null;  
			if(null != data){  
				b = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图  
				mCamera.stopPreview();  
				isPreviewing = false;  
			}  
			//保存图片到sdcard  
			if(null != b)  
			{  
				//设置FOCUS_MODE_CONTINUOUS_VIDEO)之后，myParam.set("rotation", 90)失效。  
				//图片竟然不能旋转了，故这里要旋转下  
				Bitmap rotaBitmap = ImageUtil.getRotateBitmap(b, 90.0f);  
				FileUtil.saveBitmap(rotaBitmap);  
			}  
			//再次进入预览  
			mCamera.startPreview();  
			isPreviewing = true;  
		}  
	};  


	public Size getCameraSize(){
		return mCamera.getParameters().getPreviewSize();
	}

}
