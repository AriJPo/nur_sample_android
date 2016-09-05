package com.nordicid.rfiddemo;

import com.nordicid.apptemplate.SubAppTabbed;
import com.nordicid.controllers.AuthenticationController;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurTag;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class AuthenticationAppTabbed extends SubAppTabbed {

	private Button mStartStopBtn;
	private Handler mHandler;

	// Main view: processed , OK and failed tags; key set to use text.
	private AuthenticationTab mAuthTab;
	// List for OK and filed tags; use for locating
	private AuthenticationAppFoundTab mOkTagsTab;
	private AuthenticationAppFoundTab mFailedTagsTab;

	// controller does the actual authentication.
	private AuthenticationController mAuthController = null;
	private AuthenticationController.AuthenticationControllerListener mAuthListener = null;

	// This view
	private View mView;

	// Instance of this sub app.
	private static AuthenticationAppTabbed gInstance;
	public static AuthenticationAppTabbed getInstance() { return gInstance; }

	// Tag hashes for adapters.
	private final ArrayList<HashMap<String,String>> mOkTagHash = new ArrayList<HashMap<String,String>>();
	private final ArrayList<HashMap<String,String>> mFailedTagHash= new ArrayList<HashMap<String,String>>();

	@Override
	public NurApiListener getNurApiListener()
	{
		if (mAuthController != null)
			mAuthController.getNurApiListener();
		return null;
	}

	public AuthenticationAppTabbed() {
		super();

		mHandler = new Handler(Looper.getMainLooper());
		gInstance = this;
		mAuthController = new AuthenticationController(getActivity(), getNurApi());


		mAuthListener = new AuthenticationController.AuthenticationControllerListener() {
			@Override
			public void processedCountChanged(int newCount)
			{
				mAuthTab.updateTotalCount(newCount);
			}

			@Override
			public void onNewOkTag(NurTag newTag) {
				tagFound(newTag, mOkTagHash, true);
				mAuthTab.updateOkCount(mOkTagHash.size());
				mOkTagsTab.updateAll();
			}

			@Override
			public void onNewFailedTag(NurTag newTag) {
				tagFound(newTag, mFailedTagHash, false);
				mAuthTab.updateFailCount(mFailedTagHash.size());
				mFailedTagsTab.updateAll();
			}

			@Override
			public void readerDisconnected() {
				handleReaderDisconnect();
			}

			@Override
			public void readerConnected() {
				handleReaderConnect();
			}

			@Override
			public void authenticationStateChanged(boolean executing) {
				if (isVisible() && mStartStopBtn != null) {
					keepScreenOn(executing);
					mStartStopBtn.setText(executing ? "Stop" : "Start");
				}
			}

			@Override
			public void resetAll()
			{
				mOkTagHash.clear();
				mFailedTagHash.clear();
				mAuthTab.updateTotalCount(0);
				mAuthTab.updateOkCount(0);
				mAuthTab.updateFailCount(0);
			}
		};

		mAuthController.setListener(mAuthListener);
	}

	private void handleReaderConnect()
	{
		if (!isVisible())
			return;
		mStartStopBtn.setEnabled(false);
		mStartStopBtn.setText("Start");

		try {
			if (mAuthController.getTotalKeyCount() > 0) {
				// This would cause an exception...
				mAuthController.getAuthKeyNumber();
				// ...if not, then enable the start/stop button.
				mStartStopBtn.setEnabled(true);
			}
		}
		catch (Exception ex) { }
	}

	private void handleReaderDisconnect()
	{
		if (!isVisible())
			return;

		mStartStopBtn.setEnabled(false);
		mStartStopBtn.setText("Start");
	}

	/**
	 * A tag was authenticated or failed to authenticate.
	 * @param tag Tag to process/add.
	 * @param target The target hash where the tag is added.
	 * @param authenticationOk True if the tag authenticated OK.
     */
	private void tagFound(NurTag tag, ArrayList<HashMap<String, String>> target, boolean authenticationOk) {

		HashMap<String, String> tmp;

		tmp = new HashMap<String, String>();
		tmp.put(AuthenticationAppFoundTab.DATA_TAG_EPC, tag.getEpcString());

		if (authenticationOk)
			tmp.put(AuthenticationAppFoundTab.DATA_TAG_AUTH_OK, "YES");
		else
			tmp.put(AuthenticationAppFoundTab.DATA_TAG_AUTH_OK, "NO");
		tag.setUserdata(tmp);

		target.add(tmp);
	}

	@Override
	public String getAppName() {
		return "Authentication";
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_tagauth;
	}

	@Override
	public int getLayout() {
		return R.layout.app_tagauth_tabbed;
	}

	@Override
	public void onVisibility(boolean val) {

	}

	private void keepScreenOn(boolean value) {
		if (mView != null)
			mView.setKeepScreenOn(value);
	}

	private void handleStartStop() {
		if (mAuthController.isAuthenticationRunning())
			mAuthController.stopAuthentication();
		else {
			if (!getNurApi().isConnected())
			{
				Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
				return;
			}
			try
			{
				mAuthController.setAuthKeyNumber(Main.getInstance().getUsedKeyNumber());
				mAuthController.startTAM1Authentication();
			}
			catch (Exception ex) { }
		}
	}

	private void handleStorageClear() {
		if (mAuthController.isAuthenticationRunning())
			return;

		mAuthController.clearAllTags();
		mOkTagHash.clear();
		mFailedTagHash.clear();
		mOkTagsTab.updateAll();
		mFailedTagsTab.updateAll();
	}

	@Override
	protected int onGetFragments(ArrayList<Fragment> fragments, ArrayList<String> fragmentNames) throws Exception {
		//create instances to fragments and pager.
		mAuthTab = new AuthenticationTab();

		mOkTagsTab = new AuthenticationAppFoundTab();
		mOkTagsTab.setTagHashSource(mOkTagHash, true);

		mFailedTagsTab = new AuthenticationAppFoundTab();
		mFailedTagsTab.setTagHashSource(mFailedTagHash, false);

		fragmentNames.add(getString(R.string.text_authenticating));
		fragments.add(mAuthTab);

		fragmentNames.add(getString(R.string.text_tags_auth_ok));
		fragments.add(mOkTagsTab);

		fragmentNames.add(getString(R.string.text_tags_auth_failed));
		fragments.add(mFailedTagsTab);

		return R.id.auth_pager;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mView = view;

		mStartStopBtn = addButtonBarButton("Start", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleStartStop();
			}
		});
		addButtonBarButton("Clear tags", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleStorageClear();
			}
		});

		mStartStopBtn.setEnabled(getNurApi().isConnected());
		super.onViewCreated(view, savedInstanceState);
	}

	private void stopAuthentication()
	{
		if (mAuthController != null)
			mAuthController.stopAuthentication();
	}

	// Set up keys from a file.
	// Format of the file can be found in the AuthenticationController.java.
	private void tryKeySetup()
	{
		String keyFileName;
		int keyNumber = -1;
		int keyCount = 0;
		boolean ok = false;

		mStartStopBtn.setEnabled(false);
		keyFileName = Main.getInstance().getKeyFileName();
		keyNumber = Main.getInstance().getUsedKeyNumber();

		try
		{
			int result;
			result = mAuthController.readKeysFromFile(keyFileName);
			if (result == AuthenticationController.KEYS_OK) {
				keyCount = mAuthController.getTotalKeyCount();
				mAuthController.setAuthKeyNumber(keyNumber);
				ok = true;
			}
			else {
				Toast.makeText(getActivity(), "Key read error:\n" + AuthenticationController.keyErrorToString(result), Toast.LENGTH_SHORT).show();
			}
		}
		catch (Exception ex) { }

		if (!ok)
			Toast.makeText(getActivity(), "Authentication key setup error.", Toast.LENGTH_SHORT).show();
		else {
			Toast.makeText(getActivity(), "Loaded " + keyCount + " keys, using set " + keyNumber, Toast.LENGTH_SHORT).show();
		}

		mStartStopBtn.setEnabled(ok);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mAuthController != null && !mAuthController.isAuthenticationRunning())
			tryKeySetup();
	}

	@Override
	public void onPause() {

		super.onPause();
		stopAuthentication();
	}

	@Override
	public void onStop() {

		super.onStop();
		stopAuthentication();
	}
}
