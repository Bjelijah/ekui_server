package com.example.bean;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.example.bean.EkuiHead.EkuiCmd;
import com.example.bean.ISocketConnect.SocketClientCallbackObserver;
import com.example.bean.ISocketConnect.SocketServerCallbackObserver;
import com.example.bean.SocketFactory.SocketType;
import com.example.utils.JsonUtil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import struct.JavaStruct;
import struct.StructException;

public class EkuiProtocol implements IProtocol,IConst {
	public static final String BUNDLE_LOGIN_IP = "MyIP" ;
	public static final String BUNDLE_LOGOUT_HANDLE = "MyHandle";
	private ISocketConnect conn;
	private SocketServerOB ssOB;
	private SocketClientOB scOB;
	private static final SocketType PROTOCOL_TYPE = SocketType.TCP_CONNECT; 
	private int sHandle;//client handle get from server
	private ByteBuffer readbuf = ByteBuffer.allocate(10*1024*1024);
	private int headLen = -1;
	private Handler handler;
	private String ipAck;
	IPushOb pushOb;
	public EkuiProtocol() {
		// TODO Auto-generated constructor stub
		conn = SocketFactory.getSocket(PROTOCOL_TYPE); 
	}

	/**
	 * connect method for server
	 */
	private void serverConnect(){
		Bundle bundle = new Bundle();
		bundle.putInt(ISocketConnect.BUNDLE_LOCAL_PORT, SERVICE_PORT);
		conn.setInfoBundle(bundle);
		conn.init();
		ssOB = new SocketServerOB();
		conn.attch(ssOB);
	}

	/**
	 * connect method for client
	 */
	private void clientConnect(){
		conn.init(SERVICE_IP, SERVICE_PORT, CLIENT_PORT);
		scOB = new SocketClientOB();
		conn.attch(scOB);
	}

	@Override
	public void connect() {
		readbuf.clear();
		serverConnect();
	}
	
	@Override
	public void disconnect(){
//		conn.detach(scOB);
//		conn.detach(ssOB);
//		conn.deInit();
	}

	@Override
	public void login(String myIP) {//for client
		// TODO Auto-generated method stub
		readbuf.clear();
		Bundle b = new Bundle();
		b.putString(BUNDLE_LOGIN_IP, myIP);
		byte [] body = JsonUtil.buildLoginJsonStr(b).getBytes();
		byte [] head = null;
		try {
			head = buildHead(EkuiCmd.LOGIN, body.length);
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] stream = new byte[head.length+body.length];
		System.arraycopy(head, 0, stream, 0, head.length);
		System.arraycopy(body, 0, stream, head.length, body.length);
		conn.send(stream, stream.length);
	}

	@Override
	public void logout() {//client
		readbuf.clear();
		Bundle b = new Bundle();
		b.putInt(BUNDLE_LOGOUT_HANDLE, sHandle);
		byte [] body = JsonUtil.buildLogoutJsonStr(b).getBytes();
		byte [] head = null;
		try {
			head = buildHead(EkuiCmd.LOGOUT, body.length);
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] stream = new byte[head.length+body.length];
		System.arraycopy(head, 0, stream, 0, head.length);
		System.arraycopy(body, 0, stream, head.length, body.length);
		conn.send(stream, stream.length);
	}

	@Override
	public void pushStream(boolean isI,byte[] data, int len) {//server
		byte [] head = null;
		try {
			head = buildHead(EkuiCmd.PUSH, len);
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] stream = new byte[head.length+len];
		System.arraycopy(head, 0, stream, 0, head.length);
		System.arraycopy(data, 0, stream, head.length, len);
		conn.send(stream, stream.length);
	}

	private byte [] buildHead(EkuiCmd cmd,int bodyLen) throws StructException, IOException{
		EkuiHead head = new EkuiHead();
		head.setCommand(cmd.getVal());
		head.setPayload_len(bodyLen);
		return JavaStruct.pack(head);
	}

	private int getHeadLen(){
		if (headLen==-1) {
			try {
				headLen = JavaStruct.pack(new EkuiHead()).length;
			} catch (StructException e) {
				e.printStackTrace();
			}
		}
		return headLen;
	}


	private synchronized void packMsg(byte [] data){
		if (readbuf.remaining()< data.length) {
			readbuf.clear();
		}
	
		readbuf.put(data);
		
		processPack();
	}

	private synchronized boolean processPack(){
		int headLen = getHeadLen();
		int dataLen = -1;
		int tempPos = 0;
		int tempEnd = 0;
		int tempLen = 0;
		byte tempByte = 0x00;
		if (readbuf.position()<headLen) {
			Log.e("123", "readbuf pos < headLen error return false");
			return false;
		}
		
		tempPos = readbuf.position();
		Log.i("123", "processPack start temppos="+tempPos);
		readbuf.flip();
		while(readbuf.remaining()>headLen){
			Log.i("123", "do pos="+readbuf.position());
			if((byte)0xa5 != (tempByte=readbuf.get())){
				Log.e("123", "readbuf.get!=0xa5 continue  tempByte="+String.format("0x%x", tempByte)+"  pos="+readbuf.position());
				continue;
			} else {
				//get head
				Log.d("123", "get head sync");
				readbuf.position(readbuf.position()-1);
				byte [] head = new byte [headLen];
				readbuf.mark();
				readbuf.get(head);
				EkuiHead headObj = new EkuiHead();

				try {
					JavaStruct.unpack(headObj, head);
					dataLen = headObj.getPayload_len();
					Log.d("123", "dataLen="+dataLen);
				} catch (StructException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (dataLen<0||dataLen>2*1024*1024) {
					//错误的头数据 回退标签
					Log.e("123", "head -> data len < 0 error");
					readbuf.reset();
					continue;
				}
				if (readbuf.remaining()<dataLen) {
					//数据不全	
					Log.e("123", "remain < datalen");
					readbuf.reset();
					tempLen = readbuf.remaining();
					break;
				}

				//get body

				short cmd = 0;
				try {
					cmd = headObj.getCommand();
				} catch (IOException e) {
					e.printStackTrace();
				}
				byte [] body = new byte[dataLen];
				readbuf.get(body);

				if (EkuiCmd.getCmd(cmd)==EkuiCmd.PUSH) {
					Log.i("123", "kuicmd.push");
					doPush(body,dataLen);
				}else{
					
					String jsonStr = new String(body);
					Log.i("123","receive jsonstr="+jsonStr );
					phaseMsg(EkuiCmd.getCmd(cmd), jsonStr);
				}
				
			}
		}
		
		readbuf.compact();
		readbuf.position(tempLen);
		Log.i("123", "processPack   over");
		return true;
	}
	
	
	
	
	private synchronized boolean processMsg(byte [] data,int len){
		
		
//		readbuf.clear();
		readbuf.put(data);
		int headLen = getHeadLen();
		if(readbuf.position()<headLen){
			Log.e("123","pos < headlen");
			return false;
		}
		byte [] readBufArray = readbuf.array();
		int dataLen = -1;
		byte [] head = new byte[headLen];
//		byte [] bs = new byte[16];
		
		Log.i("123", "headlen="+headLen+"   process msg len="+len+"  readbuf arry pos="+readbuf.position());
		System.arraycopy(readBufArray, 0, head, 0, headLen);
		EkuiHead headObj = new EkuiHead();
//		HWTransmissonHead headObj2 = new HWTransmissonHead();
		for(int i=0;i<16;i++){
			Log.i("123", "head["+i+"] :"+String.format("0x%x", head[i]));
		}
		
		
		try {
			JavaStruct.unpack(headObj, head);
			dataLen = headObj.getPayload_len();
		} catch (StructException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.i("123", "datalen="+dataLen);
		if (dataLen==-1) {
			Log.e("123",""+ dataLen );
			return false;
		}
		
		if(headObj.getSync()!=  (byte) (0xa5) ){
			Log.e("123", "sync!=0xa5");
			readbuf.clear();
			return false;
		}
		short cmd = 0;
		try {
			cmd = headObj.getCommand();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] body = new byte[dataLen];
		System.arraycopy(readBufArray, headLen, body, 0, dataLen);
		if (EkuiCmd.getCmd(cmd)==EkuiCmd.PUSH) {
			doPush(body,dataLen);
		}else{
			String jsonStr = new String(body);
			phaseMsg(EkuiCmd.getCmd(cmd), jsonStr);
		}
		readbuf.clear();
		return true;
	}

	private void phaseMsg(EkuiCmd cmd,String jsonStr){
		switch(cmd){
		case LOGIN:
			Log.d("123", "get login msg");
			doLogin(JsonUtil.phaseLoginJson(jsonStr));
			break;
		case LOGOUT:
			Log.d("123", "get logout msg");
			doLogout(JsonUtil.phaseLogoutJsonStr(jsonStr));
			break;
		default:
			break;
		}
	}

	private void doLogout(Bundle b){
		int handle = b.getInt(BUNDLE_LOGOUT_HANDLE);
		Message msg = new Message();
		msg.what = MSG_LOGOUT_ACK;
		msg.obj = handle;
		handler.sendMessage(msg);
	}
	
	
	private void doLogin(Bundle b){
		String ip = b.getString(EkuiProtocol.BUNDLE_LOGIN_IP);
		ipAck = ip;
		Message msg = new Message();
		msg.what = MSG_LOGIN_ACK;
		msg.obj = ip;
		handler.sendMessage(msg);
	}
	
	private void doPush(byte [] data,int len){
		if (pushOb!=null) {
			pushOb.onPushDataComing(data, len);
		}
		
	}

	class SocketClientOB implements SocketClientCallbackObserver{

		@Override
		public void onConnect() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onConnectReceive(byte[] data, int len) {
			// TODO Auto-generated method stub
//			processMsg(data, len);
			packMsg(data);
		}

		@Override
		public void onDisconnect() {
			// TODO Auto-generated method stub

		}

	}

	class SocketServerOB implements SocketServerCallbackObserver{

		@Override
		public void onConnect(int handle) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onConnectReceive(int handle, byte[] data, int len) {
			// TODO Auto-generated method stub
			sHandle = handle;
//			processMsg(data, len);
			packMsg(data);
		}

		@Override
		public void onDisconnect(int handle) {
			// TODO Auto-generated method stub

		}

	}

	@Override
	public void setHandler(Handler h) {
		this.handler = h;
	}

	@Override
	public void setCallback(IPushOb ob) {
		this.pushOb = ob;
	}

	@Override
	public void setInfo(Bundle b) {
		// TODO Auto-generated method stub
		conn.setInfoBundle(b);
	}



}
