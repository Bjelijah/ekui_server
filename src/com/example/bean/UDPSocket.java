package com.example.bean;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.example.action.ProtocolServerAction;
import com.example.utils.DialogUtils;

import android.media.tv.TvContract.Channels.Logo;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.widget.Toast;



public class UDPSocket implements ISocketConnect {

	private HashSet<SocketClientCallbackObserver> mCallbackSet = new HashSet<ISocketConnect.SocketClientCallbackObserver>();
	private HashSet<SocketServerCallbackObserver> mServerCallback = new HashSet<ISocketConnect.SocketServerCallbackObserver>();
	private DatagramSocket mSocket = null;  
	private InetAddress mDestinationAddress = null;
	private int mDestinationPort = -1;
	private int mLoaclPort = -1;
	private SendThread mSendThread = null;
	private ReceiveThread mReceiveThread = null;
	private byte [] mReceiveData = new byte[3*1024*1024];
	private ByteBuffer sendBuffer = ByteBuffer.allocate(3*1024*1024);

	@Override
	public void setInfoBundle(Bundle b) {

		String dIP = b.getString(BUNDLE_DESTINATION_IP,null);
		if (dIP!=null) {
			try {
				Log.d("123", "setin bundle ip="+dIP);
				mDestinationAddress = InetAddress.getByName(dIP);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}


		if (mDestinationPort == -1) {
			mDestinationPort = b.getInt(BUNDLE_DESTINATION_PORT,-1);
		}
		if (mLoaclPort == -1) {
			mLoaclPort = b.getInt(BUNDLE_LOCAL_PORT,-1);
		}
	}

	@Override
	public void init() {//for server   mDestinationIP and port will be told by client's login method
		if (mSocket!=null) {
			try {
				endSocket();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			mSocket = new DatagramSocket(mLoaclPort);
			startSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
	}


	@Override
	public void init(String destinationIP, int destinationPort, int localPort) {//for client
		
		if (mSocket!=null) {
			try {
				endSocket();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			mSocket = new DatagramSocket(localPort);
			mDestinationAddress = InetAddress.getByName(destinationIP);
			mDestinationPort = destinationPort;
			startSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}

	private void startSocket(){
		if (mSendThread!=null || mReceiveThread!=null) {
			try {
				endSocket();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mSendThread = new SendThread();
		mSendThread.start();
		mReceiveThread = new ReceiveThread();
		mReceiveThread.start();
	}

	private void endSocket() throws InterruptedException{
		if (mSendThread!=null) {
			mSendThread.endThread();
			mSendThread.join();
			mSendThread = null;
		}
		if (mReceiveThread!=null) {
			mReceiveThread.endThread();
			mReceiveThread.join();
			mReceiveThread = null;
		}
		if (mSocket!=null) {
			mSocket.disconnect();
			mSocket.close();
			mSocket = null;
		}
	}


	@Override
	public void deInit() {
		try {
			endSocket();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void attch(SocketClientCallbackObserver ob) {
		if (mCallbackSet == null) {
			mCallbackSet = new HashSet<ISocketConnect.SocketClientCallbackObserver>();
		}
		mCallbackSet.add(ob);
	}

	@Override
	public void detach(SocketClientCallbackObserver ob) {
		mCallbackSet.remove(ob);
	}

	private void doReceiveData(byte [] data,int len){
		for(SocketClientCallbackObserver ob:mCallbackSet){
			if (ob!=null) {
				ob.onConnectReceive(data,len);
			}
		}
		for(SocketServerCallbackObserver ob:mServerCallback){
			if (ob!=null) {
				ob.onConnectReceive(0, data, len);
			}
		}
	}

	@Override
	public void send(byte[] data, int len) {
		
		
		if (len> 60*1024) { //单次最大发送10k  udp limit is equals 64k-28
			sendBuffer.clear();
			sendBuffer.put(data);
			Log.i("123", "sendbuffer pos="+sendBuffer.position()+"   len="+len);
			sendBuffer.flip();
			while(sendBuffer.hasRemaining()){
				int bufLen = 60*1024;
				if (sendBuffer.remaining()<bufLen) {
					bufLen = sendBuffer.remaining();
				}
				byte [] buf = new byte [bufLen];
				sendBuffer.get(buf);
				DatagramPacket pack = new DatagramPacket(buf,bufLen,mDestinationAddress,mDestinationPort);
				if (mSendThread!=null) {
					mSendThread.pushData(pack);
				}


			}


		}else{
			Log.i("123", "send d ip="+mDestinationAddress.getHostName()+"  prot="+mDestinationPort);
			DatagramPacket pack = new DatagramPacket(data, len,mDestinationAddress,mDestinationPort);
			if (mSendThread!=null) {
				mSendThread.pushData(pack);
			}
		}


	}

	private int sendNum = 0;
	private class SendThread extends Thread{
		private Queue<DatagramPacket> packetQueue =  new ArrayBlockingQueue<DatagramPacket>(150); 
		private AtomicReference<Queue<DatagramPacket>> ar = null;
		private boolean bRunning = false;
		public void pushData(DatagramPacket dp){
			
			boolean ret = ar.get().offer(dp);
			if (!ret) {
				Log.e("123", "packet offer error");
				ar.get().clear();//满了
			}
			
		
//			synchronized (mSendThread) {
//				notify();
//			}

		}
		public DatagramPacket popData(){
//			try {
//				synchronized (mSendThread) {
//					wait();
//				}
//
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			return packetQueue.poll();
		}
		public SendThread() {
			bRunning = true;
			ar = new AtomicReference<Queue<DatagramPacket>>(packetQueue);
		}
		public void endThread(){
			bRunning = false;
		}
		@Override
		public void run() {
			while(bRunning){
				DatagramPacket packet = popData();
				if (packet!=null) {
					try {
						Log.i("123","udp send packet" );
						
						mSocket.send(packet);
						
						
						
						sendNum++;
						Log.d("123", "send num="+sendNum+" sendLen="+packet.getLength());
//						if (sendNum>1000) {
//							DialogUtils.postAlerDialog(ProtocolServerAction.getInstance().getContext(), "send > 1000");
//						}
						
						
					} catch (IOException e) {
						Log.e("123", "packet len="+packet.getLength());
						e.printStackTrace();
					}
				}else{
					try {
						sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			super.run();
		}
	}

	private class ReceiveThread extends Thread{
		private boolean bRunning = false;
		public ReceiveThread() {
			bRunning = true;
		}
		public void endThread(){
			bRunning = false;
		}
		@Override
		public void run() {
			while(bRunning){
				DatagramPacket pack = new DatagramPacket(mReceiveData, mReceiveData.length);
				Log.i("123", "receive socket");
				try {
					mSocket.receive(pack);
				} catch (IOException e) {
					e.printStackTrace();
				}
				byte [] data = pack.getData();
				int dataLen = pack.getLength();
				Log.i("123", "buf len="+data.length+" data len"+dataLen);
				byte [] buf = new byte[dataLen];
				System.arraycopy(data, 0, buf, 0, dataLen);
				doReceiveData(buf,dataLen);//FIXME
			}
			super.run();
		}
	}

	@Override
	public void attch(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		if (mServerCallback==null) {
			mServerCallback = new HashSet<ISocketConnect.SocketServerCallbackObserver>();
		}
		mServerCallback.add(ob);
	}

	@Override
	public void detach(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		if (mServerCallback!=null) {
			mServerCallback.remove(ob);
		}
	}
}
