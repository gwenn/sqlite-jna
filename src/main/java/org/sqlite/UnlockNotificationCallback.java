package org.sqlite;

import com.sun.jna.Pointer;

class UnlockNotificationCallback implements UnlockNotifyCallback {
	@Override
	public void notify(Pointer[] args) {
		for (Pointer arg : args) {
			UnlockNotification notif = null; // FIXME
			notif.fire();
		}
	}
}
