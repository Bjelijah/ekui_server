package com.example.utils;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;

public class DisplayUtil {
	private static final String TAG = "DisplayUtil";  


	/** 
	 * dip转px 
	 * @param context 
	 * @param dipValue 
	 * @return 
	 */  
	public static int dip2px(Context context, float dipValue){              
		final float scale = context.getResources().getDisplayMetrics().density;                   
		return (int)(dipValue * scale + 0.5f);           
	}       

	/** 
	 * px转dip 
	 * @param context 
	 * @param pxValue 
	 * @return 
	 */  
	public static int px2dip(Context context, float pxValue){                  
		final float scale = context.getResources().getDisplayMetrics().density;                   
		return (int)(pxValue / scale + 0.5f);           
	}   

	/** 
	 * 获取屏幕宽度和高度，单位为px 
	 * @param context 
	 * @return 
	 */  
	public static Point getScreenMetrics(Context context){  
		DisplayMetrics dm =context.getResources().getDisplayMetrics();  
		int w_screen = dm.widthPixels;  
		int h_screen = dm.heightPixels;  
		Log.i(TAG, "Screen---Width = " + w_screen + " Height = " + h_screen + " densityDpi = " + dm.densityDpi);  
		return new Point(w_screen, h_screen);  

	}  

	/** 
	 * 获取屏幕长宽比 
	 * @param context 
	 * @return 
	 */  
	public static float getScreenRate(Context context){  
		Point P = getScreenMetrics(context);  
		float H = P.y;  
		float W = P.x;  
		return (H/W);  
	}  


	//NV21 -> I420
	public static void YUV420SP2YUV420(byte [] yuv420sp,byte [] yuv420,int width,int height){
		if (yuv420sp == null ||yuv420 == null)
			return;
		int framesize = width*height;
		int i = 0, j = 0;
		//copy y
		for (i = 0; i < framesize; i++)
		{
			yuv420[i] = yuv420sp[i];
		}
		i = 0;
		for (j = 0; j < framesize/2; j+=2)
		{
			yuv420[i + framesize*5/4] = yuv420sp[j+framesize];
			i++;
		}
		i = 0;
		for(j = 1; j < framesize/2;j+=2)
		{
			yuv420[i+framesize] = yuv420sp[j+framesize];
			i++;
		}
	}


	   public static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input, final byte[] output, final int width, final int height) {
	       /* 
	        * YV12 is YYYYYYYY VV UU
	        * COLOR_TI_FormatYUV420PackedSemiPlanar  YUV420SP   is NV12  YYYYYYYY UVUV 
	        * We convert by putting the corresponding U and V bytes together (interleaved).
	        */
	       final int frameSize = width * height;
	       final int qFrameSize = frameSize/4;

	       System.arraycopy(input, 0, output, 0, frameSize); // Y

	       for (int i = 0; i < qFrameSize; i++) {
	           output[frameSize + i*2] = input[frameSize + i + qFrameSize]; // Cb (U)
	           output[frameSize + i*2 + 1] = input[frameSize + i]; // Cr (V)
	       }
	       return output;
	   }

	   public static byte[] YV12toYUV420Planar(byte[] input, byte[] output, int width, int height) {
	       /* 
	        * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
	        * So we just have to reverse U and V.
	        */
	       final int frameSize = width * height;
	       final int qFrameSize = frameSize/4;

	       System.arraycopy(input, 0, output, 0, frameSize); // Y
	       System.arraycopy(input, frameSize, output, frameSize + qFrameSize, qFrameSize); // Cr (V)
	       System.arraycopy(input, frameSize + qFrameSize, output, frameSize, qFrameSize); // Cb (U)

	       return output;
	   }


	   public static byte[] YV12toI420(byte[] yv12bytes, int width, int height) {
		    byte[] i420bytes = new byte[yv12bytes.length];
		    for (int i = 0; i < width*height; i++)
		        i420bytes[i] = yv12bytes[i];
		    for (int i = width*height; i < width*height + (width/2*height/2); i++)
		        i420bytes[i] = yv12bytes[i + (width/2*height/2)];
		    for (int i = width*height + (width/2*height/2); i < width*height + 2*(width/2*height/2); i++)
		        i420bytes[i] = yv12bytes[i - (width/2*height/2)];
		    return i420bytes;
		}


}
