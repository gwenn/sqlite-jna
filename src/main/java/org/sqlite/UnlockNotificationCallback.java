package org.sqlite;

import org.sqlite.SQLite.SQLite3;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.WeakHashMap;

final class UnlockNotificationCallback implements UnlockNotifyCallback {
	static final UnlockNotificationCallback INSTANCE = new UnlockNotificationCallback();

	private final Map<MemorySegment, UnlockNotification> unlockNotifications = new WeakHashMap<>();

	private UnlockNotificationCallback() {
	}

	synchronized UnlockNotification add(SQLite3 db) {
		return unlockNotifications.computeIfAbsent(db.getPointer(), k -> new UnlockNotification());
	}

	@Override
	public void notify(MemorySegment args, int nArg) {
		for (int i = 0; i < nArg; i++) {
			UnlockNotification notif = unlockNotifications.get(args.getAtIndex(ValueLayout.ADDRESS, i));
			notif.fire();
		}
	}
}
