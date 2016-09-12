package com.example.bean;

import java.util.HashSet;

import com.example.utils.JniUtil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HWProtocol implements IProtocol{

	public static final int HW_PROTOCOL_LOGIN =  0;
	public static final int HW_PROTOCOL_LOGOUT = 1;
	public static final int HW_PROTOCOL_START = 2;
	public static final int HW_PROTOCOL_STOP = 3;
	
	Handler handler;
	
	
	@Override
	public void setHandler(Handler h) {
		// TODO Auto-generated method stub
		handler = h;
	}

	@Override
	public void setCallback(IPushOb ob) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInfo(Bundle b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		//开启 server
		Log.i("123", "hwprotocol connect so start net server");
		JniUtil.hwNetServerInit();
		JniUtil.hwNetServerSetCallbackObject(this, 0);
		JniUtil.hwNetServerSetCallbackMethodName("processMsg", 0);
		Log.i("123", "conn start server");
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		//关闭server
		JniUtil.hwNetServerDeinit();
	}

	@Override
	public void login(String myIP) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pushStream(boolean isI,byte[] data, int len) {
		// TODO Auto-generated method stub
		JniUtil.hwNetSetPushData(isI,data, len);
	}

	private void processMsg(int flag){
	
		switch (flag) {
		case HW_PROTOCOL_LOGIN:
			handler.sendEmptyMessage(IProtocol.MSG_LOGIN_ACK);
			break;
		case HW_PROTOCOL_LOGOUT:
			handler.sendEmptyMessage(IProtocol.MSG_LOGOUT_ACK);
			break;
			
		case HW_PROTOCOL_START:
			handler.sendEmptyMessage(IProtocol.MSG_START_VIDEO);
			break;
			
		case HW_PROTOCOL_STOP:
			handler.sendEmptyMessage(IProtocol.MSG_STOP_VIDEO);
			break;
			
		default:
			break;
		}
		
	}
	

}
