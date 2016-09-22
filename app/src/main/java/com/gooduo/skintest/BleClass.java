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

import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2016/9/13.
 */
public class BleClass {

    public static final long SCAN_PERIOD=10000;
    public static final int SCAN_SUCCESS=0xabc;
    public static final String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID="00002902-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_WRITE_UUID="0000fff1-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_NOTIFICATION_UUID="0000fff4-0000-1000-8000-00805f9b34fb";
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mWriteCharacteristic,mReadChracteristic;
    private Handler mHandler;
    private boolean mScanning;
    private Context mContext;
    private OnConnectionListener mConnectionListener;
    private OnDataAvailableListener mDataAvailableListener;
    private OnServiceDiscoverListener mServiceDiscoverListener;





    public BleClass(Context context,BluetoothAdapter adapter,Handler handler){
        mBluetoothAdapter=adapter;
        mHandler=handler;
        mContext=context;
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
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false, mBleGattCallback);
       D.i( "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }
//    public void disconnect(){
//
//    }

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
//            D.i("stopScan2");
            sScanner.stopScan(mScanCallback);
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
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
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
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
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic){
        mBluetoothGatt.writeCharacteristic(characteristic);
    }
    public void writeCharacteristic(byte[] data){
        mWriteCharacteristic.setValue(data);
        writeCharacteristic(mWriteCharacteristic);
    }

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


    BluetoothGattCallback mBleGattCallback=new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            D.i("CharacteristicChanged");
            D.i("getChacteristic:"+Tool.bytesToHexString(characteristic.getValue()));
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            D.i("ConnectionStateChange status: "+status+" newStatus:"+newState);
            if(2==newState){
                D.i("connected");
                boolean hasSearvice=mBluetoothGatt.discoverServices();
                D.i("have Searvice? "+hasSearvice);


//                BluetoothGattCharacteristic sC=new BluetoothGattCharacteristic(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),)
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            D.i("CharacteristicRead");
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            D.i("CharacteristicWritehahaha");
            D.i("CharacteristicValue:"+characteristic.getStringValue(10));
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            D.i("DescriptorRead");
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            D.i("servicesDiscovered");
            List<BluetoothGattService> sServerList= mBluetoothGatt.getServices();
            if(sServerList!=null){
                D.i("service number="+sServerList.size());
                for(BluetoothGattService bgs:sServerList){
                    UUID uuid=bgs.getUuid();

                    D.i("findService:"+uuid.toString()+" least:"+uuid.getLeastSignificantBits()+" most:"+uuid.getMostSignificantBits()+" node:"+uuid.node()+" timeStamp:"+uuid.timestamp());
                    List<BluetoothGattCharacteristic> sCharacteristics=bgs.getCharacteristics();
                    for(BluetoothGattCharacteristic bgc: sCharacteristics){
                       UUID cUuid=bgc.getUuid();
                        String uuidStr=cUuid.toString();
                        D.i("    findCharacteristics:"+uuidStr);
                        List<BluetoothGattDescriptor> sDescriptorList=bgc.getDescriptors();
                        D.i("DescriptorNumber:"+sDescriptorList.size());
                        for(BluetoothGattDescriptor bgd:sDescriptorList){
                            D.i("        findDescribe:"+bgd.getUuid().toString()+" value:"+Tool.bytesToHexString(bgd.getValue()));
                        }
                        if(uuidStr.equals(CHARACTERISTIC_WRITE_UUID)){
                            D.i("UUid ok");
//                            bgc.setValue(new byte[]{(byte)0x68,0x04,0x34,0x45,(byte)0x98,(byte)0xa1,(byte)0xee});
//                            mBluetoothGatt.writeCharacteristic(bgc);
                            mWriteCharacteristic=bgc;
                        }
                        if(uuidStr.equals(CHARACTERISTIC_NOTIFICATION_UUID)){
                            D.i("set notification");
                            if(mBluetoothGatt.setCharacteristicNotification(bgc,true)){
//                                D.i("set notification ok");
                                mReadChracteristic=bgc;
                                BluetoothGattDescriptor descriptor = bgc.getDescriptor(UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                mBluetoothGatt.writeDescriptor(descriptor);
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
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            D.i("DescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };
    public interface OnConnectionListener{
        public void onConnectionChange(BluetoothGatt gett);
    }
    public interface OnServiceDiscoverListener {
        public void onServiceDiscover(BluetoothGatt gatt);
    }
    public interface OnDataAvailableListener {

        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status);
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic);
        public void onCharacteristicNoticefy(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic);

    }



}
