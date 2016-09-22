package com.gooduo.skintest;

import android.os.Handler;
import android.webkit.JavascriptInterface;

/**
 * Created by Administrator on 2016/9/13.
 */
public class JsBleBridge extends JsBridge {
    public static final int CTR_SIGNAL_SCAN=0x0a;
    public static final int CTR_SIGNAL_GET=0xab;
    public static final int CTR_SIGNAL_READ=0x12a;
    BleClass mBle;

    public JsBleBridge(Handler mHandler, BleClass mBle) {
        super(mHandler);
        this.mBle=mBle;
    }

//    @JavascriptInterface
//    public void temp(){
//        D.i("temp");
//        mHandler.sendEmptyMessage(CTR_SIGNAL_SCAN);
//    }
    @JavascriptInterface
    public void scanDevice(){
        D.i("scanBleDevice");
        mHandler.sendEmptyMessage(CTR_SIGNAL_SCAN);
    }

    @JavascriptInterface
    public void orderToTest(){
        mHandler.sendEmptyMessage(CTR_SIGNAL_GET);
    }

    @JavascriptInterface
    public void read(){
        D.i("read Char");
        mHandler.sendEmptyMessage(CTR_SIGNAL_READ);
    }


}
