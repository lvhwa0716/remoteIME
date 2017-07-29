package com.lvh.remoteime;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		InputMethodManager imeManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imeManager != null) {
			imeManager.showInputMethodPicker();
		} else {
			Toast.makeText(getApplicationContext(), "Not possible", Toast.LENGTH_LONG).show();
		}
	}

}
