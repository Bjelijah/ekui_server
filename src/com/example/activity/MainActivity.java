package com.example.activity;

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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity{

	TestSocket mgr;
	Context mContext;
//	DoFrameThread doFrameThread = null;
	AvcEncoder mAvcEncoder = null;
	byte [] mH264 = null;
	boolean isPreviewing = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.i("123", "activity on create");


//
//				Intent intent = new Intent("RESTART_SERVER");
//				sendBroadcast(intent);
		finish();
		
//		mgr = TestSocket.getInstance();
//		mgr.init(SERVICE_PORT, CLIENT_PORT);
//		mgr.register(this);

//		ProtocolServerAction.getInstance().setContext(this);
//		ProtocolServerAction.getInstance().init();
		
		

	}

	@Override
	protected void onDestroy() {
		Log.i("123", "activity destroy");
		Intent intent = new Intent("RESTART_SERVER");
		sendBroadcast(intent);
		super.onDestroy();
	}



	
}
