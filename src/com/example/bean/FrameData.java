package com.example.bean;

public class FrameData {
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
