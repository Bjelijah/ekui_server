package com.example.bean;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.os.Bundle;

public class NIOServer implements ISocketConnect{

	private Selector selector;
	int mLocalPort = -1;
	
	@Override
	@Deprecated
	public void init(String destinationIP, int destinationPort, int localPort) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInfoBundle(Bundle b) {
		// TODO Auto-generated method stub
		if (mLocalPort==-1) {
			mLocalPort = b.getInt(BUNDLE_LOCAL_PORT,-1);
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		if (mLocalPort==-1) {
			throw new IllegalStateException("need set local port by  setInfoBundle Method first ");
		}
		try {
			serverInit(mLocalPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attch(SocketClientCallbackObserver ob) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detach(SocketClientCallbackObserver ob) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attch(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detach(SocketServerCallbackObserver ob) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(byte[] data, int len) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	private NIOServer serverInit(int port) throws IOException{
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.socket().bind(new InetSocketAddress(port));
		selector = Selector.open();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		return this;
	}
	
	
	private void listen() throws IOException{
		
		while(true){
			
			selector.select();
			
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			
			while(it.hasNext()){
				SelectionKey key = it.next();
				it.remove();
				if (key.isAcceptable()) {
					ServerSocketChannel server = (ServerSocketChannel)key.channel();
				    SocketChannel channel = server.accept();
                    channel.configureBlocking(false);
                    //向客户端发消息
                    channel.write(ByteBuffer.wrap(new String("send message to client").getBytes()));
                    //在与客户端连接成功后，为客户端通道注册SelectionKey.OP_READ事件。
                    channel.register(selector, SelectionKey.OP_READ);
                    
                    System.out.println("客户端请求连接事件");
					
					
					
				}else if(key.isReadable()){//有可读数据事件
                    //获取客户端传输数据可读取消息通道。
                    SocketChannel channel = (SocketChannel)key.channel();
                    //创建读取数据缓冲器
                    ByteBuffer buffer = ByteBuffer.allocate(10);
                    int read = channel.read(buffer);
                    byte[] data = buffer.array();
                    String message = new String(data);
                    
                    System.out.println("receive message from client, size:" + buffer.position() + " msg: " + message);
//                    ByteBuffer outbuffer = ByteBuffer.wrap(("server.".concat(msg)).getBytes());
//                    channel.write(outbuffer);
                }
				
				
			}
			
			
			
		}
		
	}
	
	

}
