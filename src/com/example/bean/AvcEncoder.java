package com.example.bean;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import com.example.utils.DisplayUtil;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

public class AvcEncoder {
	private MediaCodec mediaCodec;  
	int m_width;  
	int m_height;  
	byte[] m_info = null;  
	private byte[] yuv420 = null;   

	public AvcEncoder(int width, int height, int framerate, int bitrate) {   

		m_width  = width;  
		m_height = height;  
		yuv420 = new byte[width*height*3/2];  

		try {
			mediaCodec = MediaCodec.createEncoderByType("video/avc");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);  
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);  
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);  
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar); 
//		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible); 
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //关键帧间隔时间 单位s  

		mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);  
		mediaCodec.start();  
	}  

    public void close() {  
        try {  
            mediaCodec.stop();  
            mediaCodec.release();  
        } catch (Exception e){   
            e.printStackTrace();  
        }  
    }  

    /**
     * 
     * @param input yv12  YYYYYYYY VV UU  fix -> yuv420sp nv21
     * @param output H264
     * @return ret >0 ok
     */
    private int count = 0;
    public int offerEncoder(byte[] input, byte[] output,boolean [] isIframe){     
        int pos = 0;  
//        swapYV12toI420(input, yuv420, m_width, m_height);  
        
        DisplayUtil.YV12toYUV420PackedSemiPlanar(input, yuv420, m_width, m_height);
//        Log.i("123", "yuv420 len="+yuv420.length);
//        for(int i=0;i<30;i++){
//        	Log.i("123", " yuv420   "+i+"  : "+yuv420[i]);
//        }
        
//        yuv420 = input;
        try {  
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();  
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();  
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(10000);  
//            Log.e("123", "inputBufferIndex="+inputBufferIndex);
            if (inputBufferIndex >= 0)   
            {  
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];  
                inputBuffer.clear();  
                inputBuffer.put(yuv420);  
              
               long timepts = 1000000*count / 20;
                count++;
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv420.length, timepts, 0);  
            }  
  
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();  
            int outputBufferIndex = 0,turn = 400;
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0); 
//            do {
//            	 outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0); 
//            	 Log.i("123", "outputBufferIndex="+outputBufferIndex);//  -1  FIXME
//            	 if (outputBufferIndex>=0) {
//					break;
//				}
//            
//            	turn--; 
//			} while (outputBufferIndex<0 && turn>0);
    
            
            
            while (outputBufferIndex >= 0){  
//            	Log.e("123", "out buffer index ok ="+outputBufferIndex);
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];  
                byte[] outData = new byte[bufferInfo.size];  
                outputBuffer.get(outData);  
                  
                if(m_info != null)  
                {                 
                    System.arraycopy(outData, 0,  output, pos, outData.length);  
                    pos += outData.length;  
                      
                }  
                  
                else //保存pps sps 只有开始时 第一个帧里有， 保存起来后面用  
                {  
                     ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);    
                     if (spsPpsBuffer.getInt() == 0x00000001)   
                     {    
                         m_info = new byte[outData.length];  
                         System.arraycopy(outData, 0, m_info, 0, outData.length);  
                     }   
                     else   
                     {    
                            return -1;  
                     }        
                }  
                  
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);  
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);  
            }  
  
            if(output[4] == 0x65) //key frame   编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上  
            {  
                System.arraycopy(output, 0,  yuv420, 0, pos);  
                System.arraycopy(m_info, 0,  output, 0, m_info.length);  
                System.arraycopy(yuv420, 0,  output, m_info.length, pos);  
                pos += m_info.length; 
                //I帧
                isIframe[0] = true;
            }  else{
            	//P帧
            	isIframe[0] = false; 
            }
              
        } catch (Throwable t) {  
            t.printStackTrace();  
        }  
  
//        Log.i("123", "pos="+pos);
        return pos;  
    } 

   
   private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height)   
   {        
       System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);  
       System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height,width*height/4);  
       System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4,width*height/4);    
   }  











}
