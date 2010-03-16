package com.jclmsoft.voicesms.services;

import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.jclmsoft.voicesms.R;
import com.jclmsoft.voicesms.VoiceSMS;
import com.jclmsoft.voicesms.PreferencesProvider;
import com.jclmsoft.voicesms.net.GVCommunicator;
import com.jclmsoft.voicesms.ui.SMSThreads;

public class UnreadService extends Service {
	public static final String ACTION_CHECK_MAIL = "com.jclmsoft.voicesms.intent.action.VOICEMAIL_SERVICE_CHECK";
	public static final String ACTION_RESCHEDULE = "com.jclmsoft.voicesms.intent.action.VOICEMAIL_SERVICE_RESCHEDULE";
	public static final String ACTION_CANCEL = "com.jclmsoft.voicesms.intent.action.VOICEMAIL_SERVICE_CANCEL";

	private static final long LONG = 500;
	private static final long SHORT = 250;
	private static final long DELAY = 250;

	private WakeLock wakeLock;

	public static void actionReschedule(Context context) {
		PreferencesProvider prefs = PreferencesProvider.getInstance(context);
		if (prefs.getBoolean(PreferencesProvider.SETUP_COMPLETED, false)) {
			Intent i = new Intent();
			i.setClass(context, UnreadService.class);
			i.setAction(UnreadService.ACTION_RESCHEDULE);
			context.startService(i);
		}
	}

	public static void actionCancel(Context context) {
		Intent i = new Intent();
		i.setClass(context, UnreadService.class);
		i.setAction(UnreadService.ACTION_CANCEL);
		context.startService(i);
	}

	private void cancel() {
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent();
		i.setClassName(getApplication().getPackageName(), "com.android.email.service.MailService");
		i.setAction(ACTION_CHECK_MAIL);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		alarmMgr.cancel(pi);
	}

	private void reschedule() {
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent();
		i.setClassName(getApplication().getPackageName(), "com.jclmsoft.voicesms.services.UnreadService");
		i.setAction(ACTION_CHECK_MAIL);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

		String delayPref = PreferencesProvider.getInstance(this).getString(PreferencesProvider.VOICEMAIL_CHECK_INTERVAL, "30");
		long delay = (Long.parseLong(delayPref) * 1000L * 60L);
		if (delay > 0) {
			long nextTime = System.currentTimeMillis() + delay;
			alarmMgr.set(AlarmManager.RTC_WAKEUP, nextTime, pi);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public void onStart(Intent intent, int startId) {
		setForeground(true); // if it gets killed once, it'll never restart
		super.onStart(intent, startId);

		if (ACTION_CHECK_MAIL.equals(intent.getAction())) {
			acquireWakeLock();

			new CheckUnreadTask().execute();

			reschedule();
		} else if (ACTION_CANCEL.equals(intent.getAction())) {
			cancel();
		} else if (ACTION_RESCHEDULE.equals(intent.getAction())) {
			reschedule();
		}
		// always release the wakelock, even if we didn't acquire one.
		releaseWakeLock();
	}

	private void createSMSNotification(Long numNew) {
		NotificationManager nm = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
		if (nm != null) {
			if (numNew == null || numNew == 0) {
				nm.cancel(VoiceSMS.NOTIFICATION_SMS);
				return;
			}
			Notification notification = new Notification(R.drawable.contacticon, "New Google Voice SMS!", System.currentTimeMillis());
			notification.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;
			// the letter "S" in Morse code :)
			notification.vibrate = new long[] {
					DELAY,
					SHORT,
					DELAY,
					SHORT,
					DELAY,
					SHORT,
			};
			notification.ledARGB = 0xFF005FC8;
			notification.ledOnMS = 500;
			notification.ledOffMS = 250;
			Intent i = new Intent(this, SMSThreads.class);
			PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
			notification.setLatestEventInfo(getApplication(), "New Voice SMS", String.format("You have %d new Voice SMS message%s!", numNew, (numNew == 1 ? "" : "s")), pi);
			nm.notify(VoiceSMS.NOTIFICATION_SMS, notification);
		}
	}

	private void acquireWakeLock() {
		WakeLock oldWakeLock = wakeLock;

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GV-Voicemail-Check");
		wakeLock.setReferenceCounted(false);
		// if I'm not done checking in 20 seconds, then to hell with it
		wakeLock.acquire(20 * 1000);

		if (oldWakeLock != null) {
			oldWakeLock.release();
		}
	}

	private void releaseWakeLock() {
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private class CheckUnreadTask extends AsyncTask<String, Integer, Map<String, Long>> {
		@Override
		protected Map<String, Long> doInBackground(String... params) {
			try {
				GVCommunicator api = GVCommunicator.getInstance(UnreadService.this);
				return api.getUnreadCounts();
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Map<String, Long> unread) {
			if (unread == null) {
				reschedule();
			} else {
				createSMSNotification(unread.get("sms"));
			}
		}
	}
}
