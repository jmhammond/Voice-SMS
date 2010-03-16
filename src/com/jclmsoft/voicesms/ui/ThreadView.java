package com.jclmsoft.voicesms.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.jclmsoft.voicesms.R;
import com.jclmsoft.voicesms.net.GVCommunicator;
import com.jclmsoft.voicesms.objects.SMS;
import com.jclmsoft.voicesms.objects.SMSThread;

public class ThreadView extends SMSCompose {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.sms_thread);

		Intent i = getIntent();
		m_thread = (SMSThread) i.getExtras().getSerializable(SMSThread.EXTRA);
		m_threadId = m_thread.getId();
		m_number = m_thread.getNumber();

		renderMessages();
		setResult(RESULT_CANCELED);
		setTitle(String.format("Conversation with %s", m_thread.getSender()));

		new ReadTask().execute();

		initUI();

		m_task.activity = this;
	}

	private void renderMessages() {
		LinearLayout container = (LinearLayout) findViewById(R.id.messages);
		container.removeAllViews();
		for (SMS s : m_thread.getMessages()) {
			s.render(container);
		}
		final ScrollView sv = (ScrollView) findViewById(R.id.thread);
		sv.post(new Runnable() {
			public void run() {
				sv.computeScroll();
				sv.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});
	}

	@Override
	protected void messageSent() {
		SMS sms = new SMS("Me:", m_message.getText().toString(), "(Just now)");
		m_thread.addMessage(sms);
		renderMessages();
		m_message.setText("");
		m_message.setEnabled(true);
		m_send.setText("Send");
		m_send.setEnabled(true);
		setResult(RESULT_OK);
		m_task = new SendSMSTask();
		m_task.activity = this;
	}

	private class ReadTask extends AsyncTask<String, Integer, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			return GVCommunicator.getInstance(ThreadView.this).messageMarkRead(m_thread.getId(), true);
		}
	}
}
