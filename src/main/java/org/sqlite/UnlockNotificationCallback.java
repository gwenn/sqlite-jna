package org.sqlite;

import java.lang.foreign.MemorySegment;
import java.util.Map;
import java.util.WeakHashMap;

import static org.sqlite.SQLite.C_POINTER;

final class UnlockNotificationCallback implements UnlockNotifyCallback {
	static final UnlockNotificationCallback INSTANCE = new UnlockNotificationCallback();

	private final Map<MemorySegment, UnlockNotification> unlockNotifications = new WeakHashMap<>();

	private UnlockNotificationCallback() {
	}

	synchronized UnlockNotification add(sqlite3 db) {
		return unlockNotifications.computeIfAbsent(db.getPointer(), k -> new UnlockNotification());
	}

	@Override
	public void notify(MemorySegment args, int nArg) {
		for (int i = 0; i < nArg; i++) {
			UnlockNotification notif = unlockNotifications.get(args.getAtIndex(C_POINTER, i));
			notif.fire();
		}
	}
}
