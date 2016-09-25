/* 
  Copyright 2016- Nordic ID
  NORDIC ID SOFTWARE DISCLAIMER

  You are about to use Nordic ID Demo Software ("Software"). 
  It is explicitly stated that Nordic ID does not give any kind of warranties, 
  expressed or implied, for this Software. Software is provided "as is" and with 
  all faults. Under no circumstances is Nordic ID liable for any direct, special, 
  incidental or indirect damages or for any economic consequential damages to you 
  or to any third party.

  The use of this software indicates your complete and unconditional understanding 
  of the terms of this disclaimer. 
  
  IF YOU DO NOT AGREE OF THE TERMS OF THIS DISCLAIMER, DO NOT USE THE SOFTWARE.  
*/
package com.nordicid.nurapi;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
//import android.util.Log;
import android.os.Handler;
import android.util.Log;

public class NurApiUsbAutoConnect implements NurApiAutoConnectTransport
{
	static final String TAG = "NurApiUsbAutoConnect";

	private NurApi mApi = null;
	private Context mContext = null;
	private UsbManager mUsbManager = null;
	private UsbDevice mUsbDevice = null;
	private IntentFilter mIntentFilter = null;
	private static final String ACTION_USB_PERMISSION = "com.nordicid.nurapi.USB_PERMISSION";
	private boolean mEnabled = false;
	private boolean mReceiverRegistered = false;
	private PendingIntent mPermissionIntent;
	private boolean mRequestingPermission = false;

	public NurApiUsbAutoConnect(Context c, NurApi na) 
	{
		this.mContext = c;
		this.mApi = na;
		this.mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
		
	    mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
			{
				synchronized (this) {
					Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED " + intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));
					mUsbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					{
						if (mUsbDevice != null) {
							NurApiUsbAutoConnect.this.setAddress(getAddress());
						}
					} 
					else {
						Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED permission denied for device " + mUsbDevice);
					}
				}
			} 
			else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				Log.d(TAG, "ACTION_USB_DEVICE_DETACHED");
				disconnect();
			}
			else if (ACTION_USB_PERMISSION.equals(action))
			{
				mRequestingPermission = false;
				Log.d(TAG, "ACTION_USB_PERMISSION " + intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false));

				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
					NurApiUsbAutoConnect.this.setAddress(getAddress());
			}
		}
	};

	private void connect()
	{
		if (!mEnabled)
			return;

		if (mUsbDevice != null && mUsbManager.hasPermission(mUsbDevice)) 
		{
			try {
				mApi.setTransport(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			NurApiUsbTransport tr = new NurApiUsbTransport(mUsbManager, mUsbDevice);
			try {
				mApi.setTransport(tr);
				mApi.connect();
				if (mReceiverRegistered)
					mContext.unregisterReceiver(mUsbReceiver);
				mIntentFilter = new IntentFilter();
				mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
				mContext.registerReceiver(mUsbReceiver, mIntentFilter);
				mReceiverRegistered = true;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		else if(mUsbDevice != null && !mUsbManager.hasPermission(mUsbDevice))
		{
			mRequestingPermission = true;
			mUsbManager.requestPermission(mUsbDevice,mPermissionIntent);
		}
	}

	private void disconnect() 
	{
		if (mApi.isConnected()) {
			try {
				if (mReceiverRegistered)
					mContext.unregisterReceiver(mUsbReceiver);
				mIntentFilter = new IntentFilter();
				mIntentFilter.addAction(ACTION_USB_PERMISSION);
				mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
				mContext.registerReceiver(mUsbReceiver, mIntentFilter);
				mReceiverRegistered = true;
				mApi.disconnect(); 
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onResume() 
	{
		if (mEnabled)
		{
			if (mApi.isConnected())
				return;

			if (!mRequestingPermission)
				this.setAddress(getAddress());
		}
	}
	
	@Override
	public void onDestroy() 
	{
		
	}

	@Override
	public void setAddress(String addr)
	{
		Log.d(TAG, "setAddress " + addr);

		this.mEnabled = !addr.toLowerCase().equals("disabled");
		if (mEnabled) 
		{
			this.mUsbDevice = null;
			for (UsbDevice device : mUsbManager.getDeviceList().values()) {
				if(device.getVendorId() == 1254 || device.getVendorId() == 3589) {
					this.mUsbDevice = device;
					break;
				}
			} 
			mIntentFilter = new IntentFilter();
			mIntentFilter.addAction(ACTION_USB_PERMISSION);
			if (mUsbDevice == null) {
				mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			} else {
				mIntentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			}
			mContext.registerReceiver(mUsbReceiver, mIntentFilter);
			mReceiverRegistered = true;

			if (mUsbDevice != null) {
				connect();
			}			
		} else {
			disconnect();
		}
	}

	@Override
	public void onPause() {
	}

	@Override
	public void onStop() {
		if (mApi.isConnected()) {
			try {
				mApi.disconnect(); 
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (mReceiverRegistered)
		{
			mReceiverRegistered = false;
			mContext.unregisterReceiver(mUsbReceiver);
		}
	}

	@Override
	public void dispose() {
		onStop();
	}

	@Override
	public String getType() {
		return "USB";
	}

	@Override
	public String getAddress() {
		return mEnabled ? "OTG" : "Disabled";
	}

	@Override
	public String getDetails() {
		return "";
	}
}
