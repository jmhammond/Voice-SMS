package com.jclmsoft.voicesms.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.jclmsoft.voicesms.R;

public class WizardSplash extends SetupActivity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.setup_splash);

		m_backButton.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void moveToNext() {
		startActivity(new Intent(this, LoginInformation.class));
	}
}
