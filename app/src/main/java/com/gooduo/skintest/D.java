package com.gooduo.skintest;

import android.util.Log;

public class D {
	public static final String TAG_NAME = "godlee";
	
	public static void v(String str){
		if(MainActivity.IS_DEBUG){
			Exception e=new Exception();
			StackTraceElement ste=e.getStackTrace()[1];
			String sMethodName=ste.getMethodName();
			String sClassName=ste.getClassName();
			int sLineNumber=ste.getLineNumber();
			Log.v(TAG_NAME+","+sClassName+","+sMethodName+","+sLineNumber,str);
		}
	}
	public static void i(String str){

		if(MainActivity.IS_DEBUG){
			Exception e=new Exception();
			StackTraceElement ste=e.getStackTrace()[1];
			String sMethodName=ste.getMethodName();
			String sClassName=ste.getClassName();
			int sLineNumber=ste.getLineNumber();
			Log.i(TAG_NAME+","+sClassName+","+sMethodName+","+sLineNumber,str);
		}
	} 
	public static void e(String str){
		if(MainActivity.IS_DEBUG){
			Exception e=new Exception();
			StackTraceElement ste=e.getStackTrace()[1];
			String sMethodName=ste.getMethodName();
			String sClassName=ste.getClassName();
			int sLineNumber=ste.getLineNumber();
			Log.e(TAG_NAME+","+sClassName+","+sMethodName+","+sLineNumber,str);
		}
	}
	
	
}