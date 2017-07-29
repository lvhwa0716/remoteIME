package com.lvh.remoteime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
//import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

public class ADBStatus extends BroadcastReceiver {
	private static final String TAG = "ADBStatus";

	@Override
	public void onReceive(final Context context, Intent intent) {
		Boolean adb = intent.getBooleanExtra(
				"adb"/* UsbManager.USB_FUNCTION_ADB */, false);

		if (com.lvh.remoteime.Debug.DebugFlags) {
			Log.d(TAG, intent.toString());
		}
		if (adb == false) {
			return;
		}
		InputMethodManager imeManager = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imeManager == null) {
			Toast.makeText(context,
					"adb usb changed ,but get ime manager error",
					Toast.LENGTH_LONG).show();
			return;
		}

		String ime_name = Settings.Secure.getString(
				context.getContentResolver(),
				Settings.Secure.DEFAULT_INPUT_METHOD);
		Log.d(TAG, "ime_name=" + ime_name);

		boolean needChangeIME = false;
		if (intent.getBooleanExtra("connected"/* UsbManager.USB_CONNECTED */,
				false) == false) {
			Log.d(TAG, "adb disconnected");
			if (isMyIme(ime_name)) {
				needChangeIME = true;
			}
		} else {
			Log.d(TAG, "adb connected");
			if (false == isMyIme(ime_name)) {
				needChangeIME = true;
			}
		}

		if (needChangeIME) {
			/*
			 * Intent ime_select = new Intent(context, MainActivity.class);
			 * ime_select.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			 * context.startActivity(ime_select);
			 */
			imeManager.showInputMethodPicker();
		} else {
			// hide IME
		}
	}

	private boolean isMyIme(String name) {
		if (name != null && name.contains("com.lvh.remoteime")) {
			return true;
		}
		return false;
	}
}
