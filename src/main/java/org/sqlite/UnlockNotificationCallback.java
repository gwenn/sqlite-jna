package org.sqlite;

import java.util.Map;
import java.util.WeakHashMap;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.sqlite.SQLite.sqlite3;

class UnlockNotificationCallback extends UnlockNotifyCallback {
	static final UnlockNotificationCallback INSTANCE = new UnlockNotificationCallback();

	private final Map<Pointer, UnlockNotification> unlockNotifications = new WeakHashMap<>();

	private UnlockNotificationCallback() {
	}

	synchronized UnlockNotification add(sqlite3 db) {
		return unlockNotifications.computeIfAbsent(db, k -> new UnlockNotification());
	}

	@Override
	public void notify(PointerPointer args, int nArg) {
		for (int i = 0; i < nArg; i++) {
			UnlockNotification notif = unlockNotifications.get(args.get(i));
			notif.fire();
		}
	}
}
