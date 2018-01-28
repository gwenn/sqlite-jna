package org.sqlite;

import com.sun.jna.Pointer;
import org.sqlite.SQLite.SQLite3;

import java.util.Map;
import java.util.WeakHashMap;

class UnlockNotificationCallback implements UnlockNotifyCallback {
	static final UnlockNotificationCallback INSTANCE = new UnlockNotificationCallback();

	private final Map<Pointer, UnlockNotification> unlockNotifications = new WeakHashMap<>();

	private UnlockNotificationCallback() {
	}

	synchronized UnlockNotification add(SQLite3 db) {
		return unlockNotifications.computeIfAbsent(db.getPointer(), k -> new UnlockNotification());
	}

	@Override
	public void notify(Pointer[] args) {
		for (Pointer arg : args) {
			UnlockNotification notif = unlockNotifications.get(arg);
			notif.fire();
		}
	}
}
