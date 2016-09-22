package com.gooduo.skintest;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2016/7/30.
 */
public class JsBridge {
    public static final int JS=0xcafe;
    public static final int LOCAL_LINK=0xabcd;
    public static final int ONLINE_LINK=0xdcba;
    public static final int TO_OPT=0xbabe2;
    protected Handler mHandler;

    public JsBridge(Handler mHandler){
        this.mHandler=mHandler;
    }
    protected void postToJs(String functionName,String value){
        JSONObject obj=new JSONObject();
        try{
            obj.accumulate("function", functionName);
            obj.accumulate("value", value);
            Message msg=mHandler.obtainMessage(JsBridge.JS,obj);
            mHandler.sendMessage(msg);
        }catch(JSONException e){
            Log.e("godlee", e.getMessage());
            e.printStackTrace();
        }

    }
//    protected void controller

}
