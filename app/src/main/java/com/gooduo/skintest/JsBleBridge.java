package com.gooduo.skintest;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.gooduo.skintest.BleClass.CHARACTERISTIC_NOTIFICATION_UUID;
import static com.gooduo.skintest.BleClass.CHARACTERISTIC_WRITE_UUID;
import static com.gooduo.skintest.BleClass.RECEIVE_NOTIFY;
import static com.gooduo.skintest.BleClass.SKIN_OIL;
import static com.gooduo.skintest.BleClass.SKIN_WATER;

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
        mBle.setConnectListener(mConnectionListener);
        mBle.setDataListener(mDataAvailableListener);
        mBle.setServiceDiscover(mServiceDiscover);
    }

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

    private BleClass.OnConnectionListener mConnectionListener=new BleClass.OnConnectionListener() {
        @Override
        public void onConnectionChange(BluetoothGatt gett, int status, int newState) {
            D.i("ConnectionStateChange status: "+status+" newStatus:"+newState);
            if(2==newState){
                D.i("connected");
                boolean hasSearvice=mBle.discoverServices();
                D.i("have Service? "+hasSearvice);
//                BluetoothGattCharacteristic sC=new BluetoothGattCharacteristic(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),)
            }
        }
    };
    private BleClass.OnServiceDiscoverListener mServiceDiscover=new BleClass.OnServiceDiscoverListener() {
        @Override
        public void onServiceDiscover(BluetoothGatt gatt, int status) {
            D.i("servicesDiscovered");
            List<BluetoothGattService> sServerList= mBle.getServiceList();

            if(sServerList!=null){
                for(BluetoothGattService bgs:sServerList){
                    List<BluetoothGattCharacteristic> sCharacteristics=bgs.getCharacteristics();
                    for(BluetoothGattCharacteristic bgc: sCharacteristics){
                        UUID cUuid=bgc.getUuid();
                        String uuidStr=cUuid.toString();
                        if(uuidStr.equals(CHARACTERISTIC_WRITE_UUID)){//发现
                            D.i("UUid ok");
//                            mWriteCharacteristic=bgc;
                            mBle.setDefultWriteCharacteristic(bgc);
                            mBle.writeCharacteristic(new byte[]{(byte)0x68,0x04,0x34,0x45,(byte)0x98,(byte)0xa1,(byte)0xee});//确认联结，防止设备认为误连而断开
                        }
                        if(uuidStr.equals(CHARACTERISTIC_NOTIFICATION_UUID)){
                            D.i("set notification");
                            mBle.setCharacteristicNotification(bgc,true);
                        }

                    }
                }
            }else{
                D.i("can't find service");
            }
        }

    };
    private BleClass.OnDataAvailableListener mDataAvailableListener=new BleClass.OnDataAvailableListener() {
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            sendGattOrder();
            D.i("characteristic");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,int status) {
//            sendGattOrder();
            D.i("characteristic Write");
        }

        @Override
        public void onCharacteristicNoticefy(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            sendGattOrder();
            D.i("CharacteristicChanged");
            byte[] receiveDate=characteristic.getValue();
            int skinWater=((receiveDate[2]&0xff)<<8)+(receiveDate[3]&0xff);
            int skinOil=((receiveDate[4]&0xff)<<8)+(receiveDate[5]&0xff);
            HashMap<String,Integer> sMap=new HashMap<String,Integer>();
            sMap.put(SKIN_WATER,skinWater);
            sMap.put(SKIN_OIL,skinOil);
//            D.i("temp move:"+((int)receiveDate[2]<<8));
//            D.i("water:"+skinWater+" oil:"+skinOil);
//            int
            D.i("getChacteristic:"+Tool.bytesToHexString(characteristic.getValue()));
            JSONObject obj=new JSONObject();
            try{
                obj.accumulate("water",skinWater);
                obj.accumulate("oil",skinOil);
                D.i(obj.toString());
                postToJs("onGetData",obj.toString());
            }catch(JSONException e){
                e.printStackTrace();
            }
            Message msg= mHandler.obtainMessage(RECEIVE_NOTIFY,sMap);
            mHandler.sendMessage(msg);

        }
    };


}
