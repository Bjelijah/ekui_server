package com.example.utils;

import com.example.ekuiservice.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

/**
 * @author 霍之昊 
 *
 * 类说明
 */
public class DialogUtils {
//	private static Dialog waitDialog;
	
//	public static Dialog postWaitDialog(Context context){
//		final Dialog lDialog = new Dialog(context,android.R.style.Theme_Translucent_NoTitleBar);
//		lDialog.setContentView(R.layout.wait_dialog);
//		return lDialog;
//	}
	
//	public static void startWaitingAnimation(Context context){
//		waitDialog = postWaitDialog(context);
//		waitDialog.show();
//	}
//	
//	public static void finishWaitingAnimation(){
//		waitDialog.dismiss();
//	}
	
	public static void postAlerDialog(Context context,String message){
		new AlertDialog.Builder(context)   
	//    .setTitle("�û�����������")   
	    .setMessage(message)                 
	    .setPositiveButton("确定", null)   
	    .show();  
	}
}
