package com.gooduo.skintest;

import android.util.Log;

public class D{
	public static final String TAG_NAME = "godlee";
	
	public static void v(String str){
		if(MainActivity.IS_DEBUG)Log.v(TAG_NAME,str);
	}
	public static void i(String str){
		if(MainActivity.IS_DEBUG)Log.i(TAG_NAME,str);
	} 
	public static void e(String str){
		if(MainActivity.IS_DEBUG)Log.e(TAG_NAME,str);
	}
	
	
}