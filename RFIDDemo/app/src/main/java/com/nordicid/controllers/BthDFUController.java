package com.nordicid.controllers;

import android.content.Context;
import android.util.Log;

import com.nordicid.helpers.UpdateContainer;
import com.nordicid.rfiddemo.DfuService;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * DFU Controller Used to update bluetooth firmware
 * https://github.com/NordicSemiconductor/Android-DFU-Library
 */

public class BthDFUController extends UpdateController{

    private final String TAG = "BTHDFUCONTROLLER";
    private String mDFUDeviceAddress = null;
    private BthFirmwareControllerListener mBthFwControllerListener = null;
    private Context mContext = null;
    private DfuServiceController mDFUServiceController = null;

    public interface BthFirmwareControllerListener {
        void onDeviceConnected(String address);
        void onDeviceConnecting(String address);
        void onDeviceDisconnected(String address);
        void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal);
        void onDfuAborted(String deviceAddress);
        void onUpdateError(String deviceAddress, int error, int errorType, String message);
        void onFirmwareValidating(String deviceAddress);
        void onDfuProcessStarting(String deviceAddress);
        void onDfuProcessStarted(String deviceAddress);
        void onEnablingDfuMode(String deviceAddress);
        void onDeviceDisconnecting(String deviceAddress);
        void onDfuCompleted(String deviceAddress);
    }

    public String getTargerAddress() { return mDFUDeviceAddress; }

    public void setBthFwControllerListener(BthFirmwareControllerListener l){
        mBthFwControllerListener = l;
    }

    public BthDFUController(Context context, String appSource, String bldrSource){
        mContext = context;
        mAppUpdateSource = appSource;
        mBldrUpdateSource = bldrSource;
    }

    public void setTargetAddress(String address){
        mDFUDeviceAddress = address;
    }

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            mBthFwControllerListener.onDeviceConnecting(deviceAddress);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            mBthFwControllerListener.onDeviceConnected(deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mBthFwControllerListener.onDfuProcessStarting(deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            mBthFwControllerListener.onDfuProcessStarted(deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mBthFwControllerListener.onEnablingDfuMode(deviceAddress);
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mBthFwControllerListener.onProgressChanged(deviceAddress,percent,speed,avgSpeed,currentPart,partsTotal);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mBthFwControllerListener.onFirmwareValidating(deviceAddress);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            mBthFwControllerListener.onDeviceDisconnecting(deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            mBthFwControllerListener.onDeviceDisconnected(deviceAddress);
        }

        /** only called when operation succeeded **/
        @Override
        public void onDfuCompleted(String deviceAddress) {
            mBthFwControllerListener.onDfuCompleted(deviceAddress);
            mDFUServiceController = null;
        }
        /** called when the operation was aborted **/
        @Override
        public void onDfuAborted(String deviceAddress) {
            mBthFwControllerListener.onDfuAborted(deviceAddress);
            mDFUServiceController = null;
        }
        /** on any error this will be triggered **/
        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            mBthFwControllerListener.onUpdateError(deviceAddress,error,errorType,message);
            mDFUServiceController = null;
        }
    };

    public void registerListener(){
        DfuServiceListenerHelper.registerProgressListener(mContext,mDfuProgressListener);
    }

    public void unregisterListener(){
        DfuServiceListenerHelper.unregisterProgressListener(mContext,mDfuProgressListener);
    }

    /**
     *  Takes device address as input and calculates DFU Target address
     *  Dfu targer @ = device @ + 1
     *  ToUpperCase is necessary android does not accept lower case addresses.
     */
    public String getDfuTargetAddress(String deviceAddress){
        return Long.toHexString(Long.parseLong(deviceAddress.replace(":",""),16)+1).replaceAll("(.{2})", "$1"+':').substring(0,17).toUpperCase();
    }

    public boolean startUpdate(){
        DfuServiceListenerHelper.registerProgressListener(mContext,mDfuProgressListener);
        if(mDFUDeviceAddress != null && mFilePath != null && mContext != null){
            final DfuServiceInitiator dfuStarter = new DfuServiceInitiator(mDFUDeviceAddress)
                    .setDisableNotification(true)
                    .setForceDfu(true)
                    .setZip(mFilePath);
            mDFUServiceController = dfuStarter.start(mContext, DfuService.class);
        }
        return false;
    }

    public void abortUpdate(){
        mDFUServiceController.abort();
    }

    public void pauseUpdate(){
        mDFUServiceController.pause();
    }

    public void resumeUpdate(){
        mDFUServiceController.resume();
    }

    /**
     * Compares remote and current versions
     * @param currentVersion
     * @param remoteVersion
     * @return true if remote is newer false if not
     */
    public boolean checkVersion(String currentVersion, String remoteVersion){
        String[] currentSplits = currentVersion.split("\\.");
        String[] remoteSplits = remoteVersion.split("\\.");
        int i = 0;
        while (i < currentSplits.length && i < remoteSplits.length && currentSplits[i].equals(remoteSplits[i])){
            i++;
        }
        if (i < currentSplits.length && i < remoteSplits.length) {
            return Integer.valueOf(currentSplits[i]).compareTo(Integer.valueOf(remoteSplits[i])) < 0;
        }
        return currentSplits.length - remoteSplits.length < 0;
    }
}
