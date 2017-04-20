package com.nordicid.rfiddemo;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.nuraccessory.NurAccessoryConfig;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurEventAutotune;
import com.nordicid.nurapi.NurEventClientInfo;
import com.nordicid.nurapi.NurEventDeviceInfo;
import com.nordicid.nurapi.NurEventEpcEnum;
import com.nordicid.nurapi.NurEventFrequencyHop;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurEventInventory;
import com.nordicid.nurapi.NurEventNxpAlarm;
import com.nordicid.nurapi.NurEventProgrammingProgress;
import com.nordicid.nurapi.NurEventTagTrackingChange;
import com.nordicid.nurapi.NurEventTagTrackingData;
import com.nordicid.nurapi.NurEventTraceTag;
import com.nordicid.nurapi.NurEventTriggeredRead;

import android.nfc.Tag;
import android.opengl.Visibility;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsAppHidTab extends Fragment {
    SettingsAppTabbed mOwner;

    NurApi mApi;
    NurAccessoryExtension mExt;

    CheckBox mHidBarcodeCheckBox;
    CheckBox mHidRFIDCheckBox;
    CheckBox mWirelessChargingCheckBox;

    TextView mWirelessChargingLabel;

    private NurApiListener mThisClassListener = null;

    public NurApiListener getNurApiListener() {
        return mThisClassListener;
    }

    public SettingsAppHidTab() {
        mOwner = SettingsAppTabbed.getInstance();
        mApi = mOwner.getNurApi();
        mExt = AppTemplate.getAppTemplate().getAccessoryApi();

        mThisClassListener = new NurApiListener() {
            @Override
            public void connectedEvent() {
                if (isAdded()) {
                    enableItems(true);
                    readCurrentSetup();
                }

            }

            @Override
            public void disconnectedEvent() {
                if (isAdded()) {
                    enableItems(false);
                }

            }

            @Override
            public void logEvent(int level, String txt) {
            }

            @Override
            public void bootEvent(String event) {
            }

            @Override
            public void inventoryStreamEvent(NurEventInventory event) {
            }

            @Override
            public void IOChangeEvent(NurEventIOChange event) {
            }

            @Override
            public void traceTagEvent(NurEventTraceTag event) {
            }

            @Override
            public void triggeredReadEvent(NurEventTriggeredRead event) {
            }

            @Override
            public void frequencyHopEvent(NurEventFrequencyHop event) {
            }

            @Override
            public void debugMessageEvent(String event) {
            }

            @Override
            public void inventoryExtendedStreamEvent(NurEventInventory event) {
            }

            @Override
            public void programmingProgressEvent(NurEventProgrammingProgress event) {
            }

            @Override
            public void deviceSearchEvent(NurEventDeviceInfo event) {
            }

            @Override
            public void clientConnectedEvent(NurEventClientInfo event) {
            }

            @Override
            public void clientDisconnectedEvent(NurEventClientInfo event) {
            }

            @Override
            public void nxpEasAlarmEvent(NurEventNxpAlarm event) {
            }

            @Override
            public void epcEnumEvent(NurEventEpcEnum event) {
            }

            @Override
            public void autotuneEvent(NurEventAutotune event) {
            }

            @Override
            public void tagTrackingScanEvent(NurEventTagTrackingData event) {
                // TODO Auto-generated method stub

            }

            @Override
            public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {
                // TODO Auto-generated method stub

            }
        };
    }

    public void onVisibility(boolean val) {
        if (val) {
            if (isAdded()) {
                enableItems(mApi.isConnected());
                if (mApi.isConnected())
                    readCurrentSetup();
            }
        }
    }

    private void enableItems(boolean v) {
        mHidBarcodeCheckBox.setEnabled(v);
        mHidRFIDCheckBox.setEnabled(v);
        mWirelessChargingCheckBox.setEnabled(v);
    }
	
	private void readCurrentSetup() 
	{
		mHidBarcodeCheckBox.setOnCheckedChangeListener(null);
		mHidRFIDCheckBox.setOnCheckedChangeListener(null);
        mWirelessChargingCheckBox.setOnCheckedChangeListener(null);
        try {
            NurAccessoryConfig cfg = mExt.getConfig();
            mHidBarcodeCheckBox.setChecked(cfg.getHidBarCode());
            mHidRFIDCheckBox.setChecked(cfg.getHidRFID());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            enableItems(false);
        }
        try {
            mWirelessChargingCheckBox.setChecked(mExt.isWirelessChargingOn());
            mWirelessChargingCheckBox.setEnabled(true);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            mWirelessChargingCheckBox.setEnabled(true);
        }
		mHidBarcodeCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
		mHidRFIDCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mWirelessChargingCheckBox.setOnCheckedChangeListener(mWirelessChargingChangeListener);

	}
	
	void setNewHidConfig()
	{
		try {
			NurAccessoryConfig cfg = mExt.getConfig();
			cfg.setHidBarcode(mHidBarcodeCheckBox.isChecked());
			cfg.setHidRFID(mHidRFIDCheckBox.isChecked());
            mExt.setConfig(cfg);
			readCurrentSetup();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			enableItems(false);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_settings_hid, container, false);
	}
	
	OnCheckedChangeListener mOnCheckedChangeListener = new OnCheckedChangeListener() {		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			setNewHidConfig();
		}
	};

    OnCheckedChangeListener mWirelessChargingChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            try{
                int i = mExt.setWirelessChargingOn(mWirelessChargingCheckBox.isChecked());
                if (i != (mWirelessChargingCheckBox.isChecked() ? 1 : 0)){
                    Toast.makeText(AppTemplate.getAppTemplate(),"Error setting wireless charging :" + i,Toast.LENGTH_SHORT).show();
                }
                mWirelessChargingCheckBox.setOnCheckedChangeListener(null);
                mWirelessChargingCheckBox.setChecked(mExt.isWirelessChargingOn());
                mWirelessChargingCheckBox.setOnCheckedChangeListener(mWirelessChargingChangeListener);
            } catch (Exception e)
            {

            }
        }
    };
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mHidBarcodeCheckBox = (CheckBox)view.findViewById(R.id.hid_barcode_checkbox);
		mHidRFIDCheckBox = (CheckBox)view.findViewById(R.id.hid_rfid_checkbox);
        mWirelessChargingCheckBox = (CheckBox)view.findViewById(R.id.hid_wireless_charging_checkbox);
        mWirelessChargingLabel = (TextView)view.findViewById(R.id.hid_wireless_charging_label);

		mHidBarcodeCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
		mHidRFIDCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mWirelessChargingCheckBox.setOnCheckedChangeListener(mWirelessChargingChangeListener);

		enableItems(mApi.isConnected());
		if (mApi.isConnected()) {
            readCurrentSetup();
        }
	}
}
