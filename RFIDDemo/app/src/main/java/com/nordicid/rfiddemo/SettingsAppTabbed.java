package com.nordicid.rfiddemo;

import java.util.ArrayList;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubAppTabbed;
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

public class SettingsAppTabbed extends SubAppTabbed { 
	private static String mPreferredTab = "";

	private SettingsAppSettingsTab mSettingsTab;
	private SettingsAppTuneTab mSettingsTuneTab;
	private SettingsAppHidTab mSettingsHidTab;
	private SettingsAppAuthTab mSettingsAuthTab;
    private SettingsAppTab mSettingsAppTab;
    private SettingsUpdateTab mSettingsUpdateTab;

	private NurApiListener mThisClassListener = null;

	private static SettingsAppTabbed gInstance = null;
	public static SettingsAppTabbed getInstance()
	{
		return gInstance;
	}

	public static void setPreferredTab(String preferredTab)
	{
		mPreferredTab = preferredTab;
	}

	@Override
	protected String onGetPreferredTab()
	{
		String preferredTab = mPreferredTab;
		mPreferredTab = "";

		return preferredTab;
	}

	@Override
    public void onAttach(Activity context){
        super.onAttach(context);
    }

    @Override
	public NurApiListener getNurApiListener()
	{
		return mThisClassListener;
	}
	
	public SettingsAppTabbed() {
		super();
		gInstance = this;
		
		mSettingsTab = new SettingsAppSettingsTab();
		mSettingsTuneTab = new SettingsAppTuneTab();
		mSettingsHidTab = new SettingsAppHidTab();
		mSettingsAuthTab = new SettingsAppAuthTab();
        mSettingsAppTab = new SettingsAppTab();
        mSettingsUpdateTab = new SettingsUpdateTab();
		
		mThisClassListener =  new NurApiListener() {
			@Override
			public void connectedEvent() {
				if (isAdded()) {
					mSettingsTab.getNurApiListener().connectedEvent();
					mSettingsHidTab.getNurApiListener().connectedEvent();
				}
			}

			@Override
			public void disconnectedEvent() {
				if (isAdded()) {
					mSettingsTab.getNurApiListener().disconnectedEvent();
					mSettingsHidTab.getNurApiListener().disconnectedEvent();
				}
			}

			@Override public void logEvent(int level, String txt) {}
			@Override public void bootEvent(String event) {}
			@Override public void inventoryStreamEvent(NurEventInventory event) { } 
			@Override public void IOChangeEvent(NurEventIOChange event) {}
			@Override public void traceTagEvent(NurEventTraceTag event) { } 
			@Override public void triggeredReadEvent(NurEventTriggeredRead event) {}
			@Override public void frequencyHopEvent(NurEventFrequencyHop event) {}
			@Override public void debugMessageEvent(String event) {}
			@Override public void inventoryExtendedStreamEvent(NurEventInventory event) { } 
			@Override public void programmingProgressEvent(NurEventProgrammingProgress event) {}
			@Override public void deviceSearchEvent(NurEventDeviceInfo event) {}
			@Override public void clientConnectedEvent(NurEventClientInfo event) {}
			@Override public void clientDisconnectedEvent(NurEventClientInfo event) {}
			@Override public void nxpEasAlarmEvent(NurEventNxpAlarm event) {}
			@Override public void epcEnumEvent(NurEventEpcEnum event) {}
			@Override public void autotuneEvent(NurEventAutotune event) {}

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

	@Override
	protected int onGetFragments(ArrayList<Fragment> fragments, ArrayList<String> fragmentNames) throws Exception
	{
        fragmentNames.add("Application settings");
        fragments.add(mSettingsAppTab);

		fragmentNames.add("Reader settings");
		fragments.add(mSettingsTab);

		fragmentNames.add("Antenna tuning");
		fragments.add(mSettingsTuneTab);

		fragmentNames.add("Accessory HID");
		fragments.add(mSettingsHidTab);

		fragmentNames.add("Authentication");
		fragments.add(mSettingsAuthTab);

        fragmentNames.add("Updates");
        fragments.add(mSettingsUpdateTab);

		return R.id.pager;
	}

	@Override
	public void onVisibility(boolean val)
	{
		mSettingsTab.onVisibility(val);
		mSettingsHidTab.onVisibility(val);
	}
	
	//main layout
	@Override
	public int getLayout() {
		return R.layout.app_settings_tabbed;
	}

	@Override
	public String getAppName() {
		return "Settings";
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_settings;
	}
}
