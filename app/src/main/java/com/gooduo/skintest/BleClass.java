package com.gooduo.skintest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2016/9/13.
 */
public class BleClass {
    public static final String SKIN_WATER="water";
    public static final String SKIN_OIL="oil";
    public static final long SCAN_PERIOD=10000;
    public static final int SCAN_SUCCESS=0xabc;
    public static final int RECEIVE_NOTIFY=0x232;
    public static final String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID="00002902-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_WRITE_UUID="0000fff1-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_NOTIFICATION_UUID="0000fff4-0000-1000-8000-00805f9b34fb";
    private static final int GATT_MODE_READ=0x01;
    private static final int GATT_MODE_WRITE=0x02;
    private static final int GATT_MODE_NOTIFY=0x03;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mWriteCharacteristic,mReadChracteristic;
    private Handler mHandler;
    private boolean mScanning;
    private Context mContext;
    private LinkedList<GattOrder> mGattOrderQueue;
    private boolean mGattFlag=false;







    public BleClass(Context context,BluetoothAdapter adapter,Handler handler){
        mBluetoothAdapter=adapter;
        mHandler=handler;
        mContext=context;
        mGattOrderQueue =new LinkedList<GattOrder>();
        mGattFlag=true;

    }
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
//    public boolean initialize() {
//        // For API level 18 and above, get a reference to BluetoothAdapter through
//        // BluetoothManager.
//        if (mBluetoothManager == null) {
//            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
//            if (mBluetoothManager == null) {
//                Log.e(TAG, "Unable to initialize BluetoothManager.");
//                return false;
//            }
//        }
//
//        mBluetoothAdapter = mBluetoothManager.getAdapter();
//        if (mBluetoothAdapter == null) {
//            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
//            return false;
//        }
//
//        return true;
//    }
    public void setConnectListener(OnConnectionListener l){
        mConnectionListener=l;
    }
    public void setDataListener(OnDataAvailableListener l){
        mDataAvailableListener=l;
    }
    public void setServiceDiscover(OnServiceDiscoverListener l){mServiceDiscoverListener=l;}

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
           D.i( "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
           D.i( "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
           D.i( "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(mContext, false, mBleGattCallback);
       D.i( "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }
    public List<BluetoothGattService> getServiceList(){
        if(null!=mBluetoothGatt){
            return mBluetoothGatt.getServices();
        }else{
            return null;
        }
    }
    public void setDefultWriteCharacteristic(BluetoothGattCharacteristic bgc){
        mWriteCharacteristic=bgc;
    }
    public boolean discoverServices(){
            return mBluetoothGatt.discoverServices();
    }


    protected void scanLeDevice(final boolean enable,final ScanCallback mScanCallback) {
        final BluetoothLeScanner sScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    D.i("stopScan");
                    mScanning = false;

//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    sScanner.stopScan(mScanCallback);
//                    invalidateOptionsMenu();

                }
            }, SCAN_PERIOD);
            mScanning = true;
            D.i("startScan");
            sScanner.startScan(mScanCallback);

        } else {
            mScanning = false;
            sScanner.stopScan(mScanCallback);
        }
    }
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            D.i( "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public synchronized void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            D.i( "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
    public void readCharacteristic(){
        readCharacteristic(mReadChracteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            D.i( "BluetoothAdapter not initialized");
            return;
        }
//        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if(mBluetoothGatt.setCharacteristicNotification(characteristic,enabled))
        {
            if(enabled){
                mReadChracteristic=characteristic;
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
                writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                D.i("descriptor set ok");
            }else{
                D.i("disable Notification");
            }
            //设置通知接收

        }else{
            D.i("set notification fail");
        }
    }
//    public void writeCharacteristic(BluetoothGattCharacteristic characteristic){
//        GattOrder sC=new GattChracteristicOrder(characteristic,GATT_MODE_WRITE);
//        if(mGattFlag){
//            mGattFlag=false;
//            sC.gattAction();
//        }
//    }
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic,byte[] data){
        GattOrder sC=new GattChracteristicOrder(characteristic,data);
        synchronized (mGattOrderQueue){
            mGattOrderQueue.add(sC);
            if(mGattFlag){
                mGattFlag=false;
                mGattOrderQueue.poll().gattAction();
            }
        }

    }

    public void writeCharacteristic(byte[] data){
//        mWriteCharacteristic.setValue(data);
        writeCharacteristic(mWriteCharacteristic,data);
    }
    public void writeDescriptor(BluetoothGattDescriptor d,byte[] data){
        GattOrder sD=new GattDescriptorOrder(d,data);
        synchronized (mGattOrderQueue){
            mGattOrderQueue.add(sD);
            if(mGattFlag){
                mGattFlag=false;
                mGattOrderQueue.poll().gattAction();
            }
        }

    }
    private synchronized void sendGattOrder(){
        GattOrder go=mGattOrderQueue.poll();
        if(go!=null){
            go.gattAction();
            mGattFlag=false;
        }else{
            mGattFlag=true;
        }

    }
//    public void writeDescriptor(byte[] data){
//        writeDescriptor(mD);
//    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    protected  void scanLeDevice(final boolean enable){
        this.scanLeDevice(enable,mScanCallback);
    }

    private ScanCallback mScanCallback=new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord sRecord=result.getScanRecord();
            byte[] advert=sRecord.getBytes();
            String mark=new String(Tool.getSubByteArray(advert,9,5));
            if(mark.equals("SPAV0")){
                D.i("device found");
                scanLeDevice(false);
                D.i("stopScan because find");
            }else{

                D.i("not found");
            }



//            if((advert[9]&0xff)==53&&(advert[10]&0xff)==50&&(advert[11]&0xff)==41&&(advert[12]&0xff)==56){
//                D.i("match");
//            }

//            D.i("scanRecord:"+Tool.bytesToHexString(sRecord.getBytes()));
//            D.i("Device Address:"+sDevice.getAddress());
            Message msg=mHandler.obtainMessage(SCAN_SUCCESS,result);
            mHandler.sendMessage(msg);
//            D.i("scanRecord"+)
//            D.i("name:"+result.getScanRecord().getDeviceName());
//            D.i("advertiseFlag:"+result.getScanRecord().getAdvertiseFlags());
            super.onScanResult(callbackType, result);
        }
    };


    private BluetoothGattCallback mBleGattCallback=new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            sendGattOrder();
            mDataAvailableListener.onCharacteristicNoticefy(gatt,characteristic);
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            mConnectionListener.onConnectionChange(gatt,status,newState);
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            sendGattOrder();
            mDataAvailableListener.onCharacteristicRead(gatt,characteristic,status);
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            sendGattOrder();
            mDataAvailableListener.onCharacteristicWrite(gatt, characteristic,status);
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            sendGattOrder();
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mServiceDiscoverListener.onServiceDiscover(gatt,status);
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            sendGattOrder();
            D.i("DescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };
    public interface OnConnectionListener{
        public void onConnectionChange(BluetoothGatt gett,int status,int newState);
    }
    public interface OnServiceDiscoverListener {
        public void onServiceDiscover(BluetoothGatt gatt,int status);
    }
    public interface OnDataAvailableListener {

        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status);
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,int status);
        public void onCharacteristicNoticefy(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic);

    }
    private OnConnectionListener mConnectionListener=new OnConnectionListener() {
        @Override
        public void onConnectionChange(BluetoothGatt gett,int status,int newState) {
            D.i("ConnectionStateChange status: "+status+" newStatus:"+newState);
            if(2==newState){
                D.i("connected");
                boolean hasSearvice=mBluetoothGatt.discoverServices();
                D.i("have Service? "+hasSearvice);
//                BluetoothGattCharacteristic sC=new BluetoothGattCharacteristic(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),)
            }
        }
    };
    private OnDataAvailableListener mDataAvailableListener=new OnDataAvailableListener() {
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
            Message msg= mHandler.obtainMessage(RECEIVE_NOTIFY,sMap);
            mHandler.sendMessage(msg);

        }
    };

    private OnServiceDiscoverListener mServiceDiscoverListener=new OnServiceDiscoverListener() {
        @Override
        public void onServiceDiscover(BluetoothGatt gatt,int status) {
            D.i("servicesDiscovered");
            List<BluetoothGattService> sServerList= mBluetoothGatt.getServices();
            if(sServerList!=null){
//                D.i("service number="+sServerList.size());
                for(BluetoothGattService bgs:sServerList){
//                    UUID uuid=bgs.getUuid();
//
//                    D.i("findService:"+uuid.toString()+" least:"+uuid.getLeastSignificantBits()+" most:"+uuid.getMostSignificantBits()+" node:"+uuid.node()+" timeStamp:"+uuid.timestamp());
                    List<BluetoothGattCharacteristic> sCharacteristics=bgs.getCharacteristics();
                    for(BluetoothGattCharacteristic bgc: sCharacteristics){
                        UUID cUuid=bgc.getUuid();
                        String uuidStr=cUuid.toString();
//                        D.i("    findCharacteristics:"+uuidStr);
//                        List<BluetoothGattDescriptor> sDescriptorList=bgc.getDescriptors();
//                        D.i("DescriptorNumber:"+sDescriptorList.size());
//                        for(BluetoothGattDescriptor bgd:sDescriptorList){
//                            D.i("        findDescribe:"+bgd.getUuid().toString()+" value:"+Tool.bytesToHexString(bgd.getValue()));
//                        }
                        if(uuidStr.equals(CHARACTERISTIC_WRITE_UUID)){//发现
                            D.i("UUid ok");
//                            bgc.setValue(new byte[]{(byte)0x68,0x04,0x34,0x45,(byte)0x98,(byte)0xa1,(byte)0xee});
//                            mBluetoothGatt.writeCharacteristic(bgc);
                            mWriteCharacteristic=bgc;
                            writeCharacteristic(bgc,new byte[]{(byte)0x68,0x04,0x34,0x45,(byte)0x98,(byte)0xa1,(byte)0xee});//确认联结，防止设备认为误连而断开
                        }
                        if(uuidStr.equals(CHARACTERISTIC_NOTIFICATION_UUID)){
                            D.i("set notification");
                            if(mBluetoothGatt.setCharacteristicNotification(bgc,true)){
//                                D.i("set notification ok");
                                //设置通知接收
                                mReadChracteristic=bgc;
                                BluetoothGattDescriptor descriptor = bgc.getDescriptor(UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
                                writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                mBluetoothGatt.writeDescriptor(descriptor);
                                D.i("descriptor set ok");
                            }else{
                                D.i("set notification fail");
                            }

                        }

                    }
                }
            }else{
                D.i("can't find service");
            }
        }
    };

    public interface GattOrder{
        public void gattAction();
    }

    private class GattChracteristicOrder implements GattOrder{
        private int mMode;
        private byte[] mData;
        private BluetoothGattCharacteristic mC;
        public GattChracteristicOrder(BluetoothGattCharacteristic c, int mode){
            mMode=mode;
            mC=c;
        }
        public GattChracteristicOrder(BluetoothGattCharacteristic c,byte[] data){
            this(c,GATT_MODE_WRITE);
            mData=data;
            mC.setValue(data);
        }
        public void setValue(byte[]data){
            mData=data;
            mC.setValue(data);
        }
        @Override
        public void gattAction(){
            switch(mMode){
                case GATT_MODE_WRITE:
                    mBluetoothGatt.writeCharacteristic(mC);
                    break;
                case GATT_MODE_READ:
                    mBluetoothGatt.readCharacteristic(mC);
                    break;
                default:
                    break;
            }
        }
     }
    private class GattDescriptorOrder implements GattOrder{
        private int mMode;
        private byte[] mData;
        private BluetoothGattDescriptor mD;
        public GattDescriptorOrder(BluetoothGattDescriptor d, int mode){
            mMode=mode;
            mD=d;
        }
        public GattDescriptorOrder(BluetoothGattDescriptor d,byte[] data){
            this(d,GATT_MODE_WRITE);
            mData=data;
            mD.setValue(data);

        }
        public void setValue(byte[] data){
            mData=data;
            mD.setValue(data);
        }
        @Override
        public void gattAction() {
            switch(mMode){
                case GATT_MODE_WRITE:
                    mBluetoothGatt.writeDescriptor(mD);
                    break;
                case GATT_MODE_READ:
                    mBluetoothGatt.readDescriptor(mD);
                    break;
                default:
                    break;
            }
        }
    }





}
