package com.gooduo.skintest;

//import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    public static final boolean IS_DEBUG = true;

    public BluetoothManager bluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    //    private LeDeviceListAdapter mLeDeviceListAdapter;
//    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeClass mBLE;
    private static BleClass mBle;
    private static JsBleBridge mJsBleBridge;
    private static WebView mWebView;
    private boolean mScanning;
    private Handler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        WeakReference<AppCompatActivity> mActivity;

        MyHandler(AppCompatActivity activity) {
            mActivity = new WeakReference<AppCompatActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AppCompatActivity sActivity = mActivity.get();
            switch (msg.what) {
                case JsBridge.JS: {
                    JSONObject obj=(JSONObject)msg.obj;
                    try{
                        String function=obj.getString("function");
                        String value=obj.getString("value");
                        mWebView.loadUrl("javascript:"+function+"('"+value+"')");
                    }catch(JSONException e){
                        Log.e("godlee",e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
                case JsBleBridge.CTR_SIGNAL_SCAN:
                    mBle.scanLeDevice(true);
                    break;
                case JsBleBridge.CTR_SIGNAL_GET:
                    mBle.writeCharacteristic(new byte[]{(byte)0x68,(byte)0x0c,(byte)0x17,(byte)0x00,(byte)0xee});
                    break;
                case JsBleBridge.CTR_SIGNAL_READ:
                    mBle.readCharacteristic();
                    break;
                case BleClass.SCAN_SUCCESS:
                    ScanResult result=(ScanResult) msg.obj;
                    ScanRecord sRecord=result.getScanRecord();
                    BluetoothDevice sDevice=result.getDevice();
                    String deviceAddress=sDevice.getAddress();
                    boolean isConnect=mBle.connect(deviceAddress);
//                    D.i("scanRecord:"+Tool.bytesToHexString(sRecord.getBytes()));
//                    D.i("scanRecordDecode:"+ new String(sRecord.getBytes()));
                    D.i("Device Address:"+deviceAddress);
                    D.i("is Connected? "+isConnect);
//                    sDevice.connectGatt(this,)
                    break;
                case BleClass.RECEIVE_NOTIFY:{
                    HashMap<String,Integer> data=(HashMap<String,Integer>)msg.obj;
                    D.i("get Data at main thread water: "+data.get(BleClass.SKIN_WATER));
                    D.i("get Data at main thread oil:"+data.get(BleClass.SKIN_OIL));
//                    JSONObject obj=new JSONObject();
//                    try{
//                        obj.accumulate("water",data.get(BleClass.SKIN_WATER));
//                        obj.accumulate("oil",data.get(BleClass.SKIN_OIL));
//                        D.i(obj.toString());
//                        mJsBleBridge.postToJs("onGetData",obj.toString());
//                    }catch(JSONException e){
//                        e.printStackTrace();
//                    }


                }

                default:
                    break;
            }
        }


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final WebSettings mWebSetting;
        D.i("onCreate");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "notsupport", Toast.LENGTH_SHORT).show();
            finish();
        }
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
//            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mBluetoothAdapter.enable();
        mBLE = new BluetoothLeClass(this);
        mBle= new BleClass(this,mBluetoothAdapter,mHandler);
        if (!mBLE.initialize()) {
            Log.e("godlee", "Unable to initialize Bluetooth");
            finish();
            return;
        }
        D.i("created ok");
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        mBLE.setOnDataAvailableListener(mOnDataAvailable);
        mWebView = new WebView(this);
        mWebSetting = mWebView.getSettings();
        mWebSetting.setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new mWebViewClint());
        mWebView.loadUrl("file:///android_asset/index.html");
        mJsBleBridge=new JsBleBridge(mHandler,mBle);
        setContentView(mWebView);
        mWebView.addJavascriptInterface(mJsBleBridge,"ble");
        temp();
//        mWebView.addJavascriptInterface(mWifiBridge, "wifi");
//        mWebView.addJavascriptInterface(mLightBridge, "light");
//        mWebView.addJavascriptInterface(mWebBridge,"web");
//        mBluetoothAdapter.startLeScan();
//        setContentView(R.layout.activity_main);
//        Toast.makeText(this,"hello Don Li",Toast.LENGTH_SHORT).show();
    }
    private void temp(){
        byte a=0x01;
        byte b=(byte)0x5e;
        int c=a<<8;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
//        setListAdapter(mLeDeviceListAdapter);
        mBle.scanLeDevice(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBLE.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBle.scanLeDevice(false,mScanCallback);
//        mLeDeviceListAdapter.clear();
        mBLE.disconnect();

    }

    @Override
    protected void onDestroy() {
        System.exit(0);
        super.onDestroy();
    }
    //    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
//        if (device == null) return;
//        if (mScanning) {
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mScanning = false;
//        }
//
//        mBLE.connect(device.getAddress());
//    }

//    protected void scanLeDevice(final boolean enable) {
//        final BluetoothLeScanner sScanner = mBluetoothAdapter.getBluetoothLeScanner();
//
//        if (enable) {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    D.i("stopScan");
//                    mScanning = false;
//
////                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    sScanner.stopScan(mScanCallback);
//                    invalidateOptionsMenu();
//
//                }
//            }, SCAN_PERIOD);
//            mScanning = true;
//            D.i("startScan");
//            sScanner.startScan(mScanCallback);
//            D.i("start over");
//
//        } else {
//            mScanning = false;
//            D.i("stopScan2");
//            sScanner.stopScan(mScanCallback);
////            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//    }

    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new BluetoothLeClass.OnDataAvailableListener() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("godlee", "onCharRead" + gatt.getDevice().getName() +
                        "read" + characteristic.getUuid().toString() + "->" + Utils.bytesToHexString(characteristic.getValue()));

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e("godlee", "onCharWrite" + gatt.getDevice().getName() + "write" + characteristic.getUuid().toString() + "->" + new String(characteristic.getValue()));
        }
    };
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeClass.OnServiceDiscoverListener() {
        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
//            displayGattServices(mBLE.getSupportedGattServices());
        }
    };
//    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
//
//        @Override
//        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
//            D.i("create callBack");
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    D.e("scan");
////                    mLeDeviceListAdapter.addDevice(device);
////                    mLeDeviceListAdapter.notifyDataSetChanged();
//                }
//            });
//        }
//    };
    //    private ScanCallback mScanCallback= new ScanCallback() {
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//
//            super.onBatchScanResults(results);
//        }
//    };
    private ScanCallback mScanCallback = new ScanCallback() {
//        D.i("haha");
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord sRecord=result.getScanRecord();
            D.i("scanOk,Type:"+callbackType);
            D.i("scanRecord:"+Tool.bytesToHexString(sRecord.getBytes()));
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            D.i("scanBatchOk");
            super.onBatchScanResults(results);

        }


        @Override
        public void onScanFailed(int errorCode) {
            D.e("scanfailed:"+errorCode);
            super.onScanFailed(errorCode);
        }
    };

//    private void displayGattServices(List<BluetoothGattService> gattServices) {
//        if (null == gattServices) return;
//        for (BluetoothGattService gattService : gattServices) {
//            int type = gattService.getType();
//            Log.e("godlee", "-->service type:" + Utils.getServiceType(type));
//            Log.e("godlee", "-->includedServices size:" + gattService.getIncludedServices().size());
//            Log.e("godlee", "-->service uuid:" + gattService.getUuid());
//
//            //-----Characteristics的字段信息-----//
//            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
//            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                Log.e("godlee", "---->char uuid:" + gattCharacteristic.getUuid());
//
//                int permission = gattCharacteristic.getPermissions();
//                Log.e("godlee", "---->char permission:" + Utils.getCharPermission(permission));
//
//                int property = gattCharacteristic.getProperties();
//                Log.e("godlee", "---->char property:" + Utils.getCharPropertie(property));
//
//                byte[] data = gattCharacteristic.getValue();
//                if (data != null && data.length > 0) {
//                    Log.e("godlee", "---->char value:" + new String(data));
//                }
//
//                //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic
//                if (gattCharacteristic.getUuid().toString().equals("0x123")) {
//                    //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
//                    mHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            mBLE.readCharacteristic(gattCharacteristic);
//                        }
//                    }, 500);
//
//                    //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
//                    mBLE.setCharacteristicNotification(gattCharacteristic, true);
//                    //设置数据内容
//                    gattCharacteristic.setValue("send data->");
//                    //往蓝牙模块写入数据
//                    mBLE.writeCharacteristic(gattCharacteristic);
//                }
//
//                //-----Descriptors的字段信息-----//
//                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
//                for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
//                    Log.e("godlee", "-------->desc uuid:" + gattDescriptor.getUuid());
//                    int descPermission = gattDescriptor.getPermissions();
//                    Log.e("godlee", "-------->desc permission:" + Utils.getDescPermission(descPermission));
//
//                    byte[] desData = gattDescriptor.getValue();
//                    if (desData != null && desData.length > 0) {
//                        Log.e("godlee", "-------->desc value:" + new String(desData));
//                    }
//                }
//            }
//        }
//    }

    class mWebViewClint extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);

            return true;

        }
    }

}
