/* 
  Copyright 2016- Nordic ID 
  NORDIC ID DEMO SOFTWARE DISCLAIMER

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

package com.nordicid.nuraccessory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiErrors;
import com.nordicid.nurapi.NurApiException;
import com.nordicid.nurapi.NurApiUnknownEventListener;
import com.nordicid.nurapi.NurEventUnknown;
import com.nordicid.nurapi.NurPacket;

import android.util.Log;

/**
 * NUR accessory extension.
 */
public class NurAccessoryExtension implements NurApiUnknownEventListener {
	public static final String TAG = "AccessoryExtension";

	/** The API instance of that the extension uses. */
	private NurApi mApi = null;

	/** The event number associated with barcode scan event (Previous version). */
	private static final int EVENT_ACCESSORY_BARCODE_OLD = 0x83;

	/** The event number associated with barcode scan event. */
	private static final int EVENT_ACCESSORY_BARCODE = 0x90;

	/** Barcode event value. */
	private static final byte EVENT_BARCODE_ID = 1;

	/** For handling the asynchronous barcode scan result. */
	private AccessoryBarcodeResultListener mBarcodeResultListener;

	/** An error code from the accessory stating that the barcode reader is not present (13 = "not ready"). */
	public static final int BARCODE_READER_NOT_PRESENT_ERROR = NurApiErrors.NOT_READY;

	/** Trigger source number of I/O change. */
	public static final int TRIGGER_SOURCE = 100;

	/** A configuration (or any other class') value indicating that this integer value is either not initialized, not used or both. */
	public static final int INTVALUE_NOT_VALID = -1;
	
	/** A configuration (or any other class') value indicating that this short value is either not initialized, not used or both. */
	public static final short SHORTVALUE_NOT_VALID = -1;	

	/** The protocol's value that the extension handler captures. */
	public static final int NUR_CMD_ACC_EXT = 0x55;

	/** BLE FW version. */
	public static final int ACC_EXT_GET_FWVERSION = 0;
	/** System restart command. */
	public static final int ACC_EXT_RESTART = 5;
	/** Asynchronous barcode scan (non-blocking). */
	public static final int ACC_EXT_READ_BARCODE_ASYNC = 6;
	/** Set external LED. */
	public static final int ACC_EXT_SET_LED_OP = 7;
	/** Asynchronous beep operation. */
	public static final int ACC_EXT_BEEP_ASYNC = 8;
	
	/** Constant indicating battery level being "good". */
	public static final int BATT_GOOD_mA = 3900;
	/** Constant indicating battery level being "moderate". */
	public static final int BATT_MODERATE_mA = 3700;
	/** Constant indicating battery level being "low". Under this value the level is "critical". */
	public static final int BATT_LOW_mA = 3500;

	/** Character set used to interpret the barcode. */
	private String mBarcodeCharSet = "UTF-8";

	/** Indicates whether the barcode scan was canceled during the response wait. */
	// private boolean mBarcodeCanceled = false;

	/**
	 * Basic constructor.
	 * 
	 * @param api The parameter is a NUR API object. It can also be set to null.
	 * 
	 * @see #setNurApi(NurApi)
	 */
	public NurAccessoryExtension(NurApi api) {
		mApi = api;
		// This instance is about to receive some "unknown" events from the API.
		mApi.addUnknownEventListener(this);
	}

	/**
	 * Set or change the API object of this instance.
	 * 
	 * @param api The parameter is a NUR API object. It can also be set to null.
	 */	
	public void setNurApi(NurApi api)
	{
		mApi = api;
		// This instance is about to receive some "unknown" events from the API.
		mApi.addUnknownEventListener(this);
	}

	/**
	 * Set the barcode interpretation character set.
	 *
	 * @param charsetToUse Character set as a string e.g. "UTF-8", "Shift_JIS", "IBM855" (IBM Cyrillic) etc.
	 *
	 * @see #getBarcodeDecodingScheme()
	 * @see <a href=https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html></a>
	 */
	public void setBarcodeDecodingScheme(String charsetToUse)
	{
		mBarcodeCharSet = charsetToUse;
	}

	/**
	 * Get the current character set used for barcode decoding.
	 *
	 * @return Return the character set as a string such as "UTF-8", "Shift_JS" etc.
	 *
	 * @see #setBarcodeDecodingScheme(String)
	 * @see <a href=https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html></a>
     */
	public String getBarcodeDecodingScheme()
	{
		return mBarcodeCharSet;
	}

	/**
	 * Commands the remote BLE module to restart.
	 *
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
	 */
	public void restartBLEModule() throws Exception
	{		
		doCustomCommand(new byte [] { ACC_EXT_RESTART });	
	}

	/**
	 * Set asynchronous barcode scan result listener.
	 *
	 * @param newListener The receiveing class that implements the interface.
	 *
	 * @see AccessoryBarcodeResultListener
	 * @see #readBarcodeAsync(int)
     */
	public void registerBarcodeResultListener(AccessoryBarcodeResultListener newListener)
	{
		mBarcodeResultListener = newListener;
		// Log.e(TAG, "New listener: " + newListener);
	}

	/**
	 * Can be used to detect whether the accessory module reported the barcodereader not being present.
	 *
	 * @param error The error code received from the barcode result event.
	 *
	 * @return Return true if the error code indicates the barcode reader not being present.
	 *
	 * @see #BARCODE_READER_NOT_PRESENT_ERROR
	 *
	 * @see AccessoryBarcodeResult
     */
	public boolean isNotSupportedError(int error)
	{
		return (error == BARCODE_READER_NOT_PRESENT_ERROR);
	}

	/**
	 * Cleanup. Always call before disposing the class so that no useless event listeners are left hanging around in the NUR API.
	 */
	public void unregisterBarcodeResultListener()
	{
		mApi.removeUnknownEventListener(this);
	}

	/**
	 * Get current accessory extension configuration.
	 *
	 * @return Return the current configuration as a extension configuration class.
	 * 
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
	 * 
	 * @see {@link NurAccessoryBattery}
	 */
	public NurAccessoryConfig getConfig() throws Exception
	{
		// Get payload from the configuration class.
		byte []payload = NurAccessoryConfig.getQueryCommand();
		byte []reply = null;
		
		// Get response byte by executing this as a "custom command". 
		reply = mApi.customCmd(NUR_CMD_ACC_EXT, payload);
		
		return NurAccessoryConfig.deserializeConfigurationReply(reply);
	}
	
	/**
	 * Set accessory configuration. 
	 * 
	 * @param cfg A valid accessory configuration.
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
	 */
	public void setConfig(NurAccessoryConfig cfg) throws Exception
	{
		byte []payload = NurAccessoryConfig.serializeConfiguration(cfg);
		
		// The call will also throw an exception if something goes wrong.
		mApi.customCmd(NUR_CMD_ACC_EXT, payload);
	}
	
	/**
	 * Battery state as a string.
	 * 
	 * @return Returns string describing the battery condition as "good", "moderate", "poor" or "critical"
	 *
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
	 *
	 */
	public String getBattState() throws Exception
	{
		int volt = getBattVoltage();
		// > 3900
		if (volt > BATT_GOOD_mA) {
			return "Good";
		}
		// 3900 - 3700
		else if (volt > BATT_MODERATE_mA) {
			return "Moderate";
		}
		// 3700 - 3500
		else if (volt > BATT_LOW_mA) {
			return "Low";
		}
		// < 3500
		return "Critical";
	}
	
	/**
	 * Get battery voltage.
	 *
	 * @return Return the battery voltage in mV.
	 *
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
	 */
	public int getBattVoltage() throws Exception
	{
		byte []payload = new byte[1];
		payload[0] = (byte)NurAccessoryBattery.ACC_EXT_GET_BATT;
		byte []resp = mApi.customCmd(NUR_CMD_ACC_EXT, payload);
		return NurPacket.BytesToWord(resp, 0);
	}
	
	/**
	 * Get the (extended) battery information.
	 * 
	 * @return Returns the battery information class with appropriate values set if successful.
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error. 
	 */
	public NurAccessoryBattery getBatteryInfo() throws Exception
	{
		byte []payload;
		byte []reply;
		
		payload = NurAccessoryBattery.getQueryCommand();
		reply = doCustomCommand(payload);
		
		return NurAccessoryBattery.deserializeBatteryReply(reply);		
	}
	
	// Simply does the data exchange between the host and accessory/reader.
	private byte []doCustomCommand(byte []commandParameters) throws Exception
	{
		if (mApi == null)
			throw new NurApiException("Accessory extension: API is invalid");
		
		return mApi.customCmd(NUR_CMD_ACC_EXT, commandParameters);
	}

	/**
	 * Get  whether the accessory extension is supported or not.
	 *
	 * @return Returns true if accessory extension is supported. Internally the supported is detected by getting the battery voltage.
     */
	public boolean isSupported()
	{
		String strVersion;
		try {
			// getBattVoltage();
			strVersion = getFwVersion();
			Log.d(TAG, "isSupported: version = \"" + strVersion + "\".");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Sets the LED operation mode.
	 *
	 * @param mode Mode to set.
	 *
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
     */
	public void setLedOpMode(int mode) throws Exception
	{
		doCustomCommand(new byte [] { ACC_EXT_SET_LED_OP, (byte)mode });
	}

	/**
	 * Non-blocking beep call.
	 *
	 * @param timeout
	 *
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
     */
	public void beepAsync(int timeout) throws Exception
	{
		byte []payload = new byte[3];
		payload[0] = (byte)ACC_EXT_BEEP_ASYNC;
		NurPacket.PacketWord(payload, 1, timeout);
		mApi.customCmd(NUR_CMD_ACC_EXT, payload);		
	}

	/**
	 * Asynchronous (non-blocking) barcode scan. NOTE: calling this effectively prohibits other operations at the same time.
	 *
	 * @param timeout Scan timeout in milliseconds.
	 *
	 * @throws Exception Can throw exception if an error occurred int APi's transport.
	 *
	 * @see #cancelBarcodeAsync()
     */
	public void readBarcodeAsync(int timeout) throws Exception
	{
		// Log.e(TAG, "Read barcode, listener: " + mBarcodeResultListener);

		// Command + 16-bit timeout.
		byte []payload = new byte[3];
		payload[0] = (byte)ACC_EXT_READ_BARCODE_ASYNC;
		NurPacket.PacketWord(payload, 1, timeout);

		// mBarcodeCanceled = false;
		doCustomCommand(payload);
	}

	/**
	 * Get the BLE module's FW version.
	 *
	 * @return When successful, returns the 3-digit FW version dot separted as in A.B.C.
	 *
	 * @throws Exception Can throw I/O, timeout or API related exception based on the occurred error.
	 *
	 * @see #makeIntegerVersion(String)
     */
	public String getFwVersion() throws Exception
	{
		byte []reply;
		String strVersion = "";
		reply = doCustomCommand(new byte [] { ACC_EXT_GET_FWVERSION });

		strVersion = new String(reply, StandardCharsets.UTF_8);

		return strVersion;
	}
	
	/**
     * Split a string to a string array based on give character.
     * Can be used e.g. to split comma or semicolon separated string to multiple fields.
     *
     * @param stringToSplit The separated string
     * @param separator Separating character e.g. ',', ';', ':' etc.
     * @param removeEmpty If true then the empty fields are removed.
     *
     * @return Returns an arrays of strings split from the single string. The strings are trimmed i.e. whitespace is removed.
     */
    public static String []splitByChar(String stringToSplit, char separator, boolean removeEmpty)
    {
        String expression;
        ArrayList<String> strList = new ArrayList<String>();
        String []arr;
        String tmp;

        expression = String.format("\\%c", separator);
        arr = stringToSplit.split(expression);

        for (String s : arr)
        {
            tmp = s.trim();
            if (removeEmpty && tmp.isEmpty())
                continue;;
            strList.add(tmp);
        }

        return strList.toArray(new String[0]);
    }

    /**
     * Split a string to a string array based on give character.
     * Can be used e.g. to split comma or semicolon separated string to multiple fields.
     * Empty string are removed.
     *
     * @param stringToSplit The separated string
     * @param separator Separating character e.g. ',', ';', ':' etc.
     *
     * @return Returns an arrays of strings split from the single string. The strings are trimmed i.e. whitespace is removed.
     */
    public static String []splitByChar(String stringToSplit, char separator)
    {
        return splitByChar(stringToSplit, separator, true);
    }
    
	/**
	 * Converts the FW version from format A.B.C to an integer that can be used to compare versions as numbers.
	 *
	 * @param strVersion Version in correct format like 1.2.3.
	 *
	 * @return When successful, an integer reprentation of the version string is returned. In case of parsing errors -1 is returned.
     */
	public int makeIntegerVersion(String strVersion)
	{
		int intVersion = NurAccessoryExtension.INTVALUE_NOT_VALID;
		String []strArr;
		int a, b, c;

		if (strVersion == null)
			return intVersion;

		intVersion = NurAccessoryExtension.INTVALUE_NOT_VALID;

		strArr = splitByChar(strVersion, '.');
		if (strArr == null || strArr.length != 3)
			return intVersion;

		try {
			a = Integer.parseInt(strArr[0], 10);
			b = Integer.parseInt(strArr[1], 10);
			c = Integer.parseInt(strArr[2], 10);

			if (a >= 0 && a <= 255 && b >= 0 && b <= 255 && c >= 0 && c <= 255) {
				intVersion = a;
				intVersion <<= 8;
				intVersion |= b;
				intVersion <<= 8;
				intVersion |= c;
			}
		}
		catch (Exception ex) { }

		return intVersion;
	}
	
	/**
	 * Cancels an ongoing asynchronous barcode scan. NOTE: the scan is canceled by a single byte sent to the extension module.
	 *
	 * @throws Exception Can throw exception if an error occurred int APi's transport.
	 *
	 * @see #readBarcodeAsync(int)
	 */
	public void cancelBarcodeAsync() throws Exception
	{
		byte []payload = new byte[1];
		payload[0] = (byte)0xFF;
		mApi.getTransport().writeData(payload, 1);

		// mBarcodeCanceled = true;
	}

	/**
	 * Interpret unknown event data. Data is expected to be decoded barcode scan result.
	 *
	 * @param status This is the status from the NUR API. It is 0 when successful.
	 * @param data Byte data received from the accessory / reader module.
	 * @param result Barcode result to be filled.
	 *
     * @return Returns true if the event data could be interpreted. The return value is also true if the decoding falls back to UTF-8 instead of the currently set character set.
	 *
	 * @see AccessoryBarcodeResult
	 * @see AccessoryBarcodeResultListener
	 * @see #setBarcodeDecodingScheme(String)
     */
	public boolean interpretEventData(int status, byte []data, AccessoryBarcodeResult result)
	{
		boolean retry = false;
		/* if (mBarcodeCanceled)
		{
			// This causes no event.
			mBarcodeCanceled = false;
			return false;
		} */

		// All others cause an event so that errors such as timeout can be detected.
		result.status = status;

		if (status == NurApiErrors.NUR_SUCCESS && data != null && data[0] == EVENT_BARCODE_ID) {
			try {
				result.strBarcode = new String(data, 1, data.length - 1, mBarcodeCharSet);
			} catch (Exception e) {
				retry = true;
			}

			if (retry)
			{
				try {
					result.strBarcode = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
				} catch (Exception ex) {
					result.strBarcode = "";
				}
			}
		}
		else
			result.strBarcode = "";

		return true;
	}

	/**
	 * Handle the "unknown" event i.e. an event which is not supported by the API.
	 *
	 * @param event The unknown event data. This data is tried to interpreted as a asynchronous scan result from the barcode scan.
	 *
     */
	@Override
	public void handleUnknownEvent(NurEventUnknown event) {

		int eventCmd = event.getCommand();

		// Log.i(TAG, "*** Unknown event: " + this + ", " + mBarcodeResultListener + ", event = 0x" + Integer.toHexString(eventCmd));

		if (eventCmd == EVENT_ACCESSORY_BARCODE || eventCmd == EVENT_ACCESSORY_BARCODE_OLD)
		{
			if (mBarcodeResultListener != null)
			{
				AccessoryBarcodeResult br = new AccessoryBarcodeResult();
	
				if (interpretEventData(event.getStatus(), event.getData(), br))
					mBarcodeResultListener.onBarcodeResult(br);
				// Else interpret error.
			}
		}
		else
			Log.e(TAG, "*** invalid event " + String.format("0x%08X (%d)", eventCmd, eventCmd) + " ***");
	}
}
