package org.sqlite;

import jnr.ffi.Pointer;

import java.util.Map;
import java.util.WeakHashMap;

class UnlockNotificationCallback implements UnlockNotifyCallback {
	static final UnlockNotificationCallback INSTANCE = new UnlockNotificationCallback();

	private final Map<Pointer, UnlockNotification> unlockNotifications = new WeakHashMap<>();

	private UnlockNotificationCallback() {
	}

	synchronized UnlockNotification add(Pointer db) {
		return unlockNotifications.computeIfAbsent(db, k -> new UnlockNotification());
	}

	@Override
	public void notify(Pointer[] args) {
		for (Pointer arg : args) {
			UnlockNotification notif = unlockNotifications.get(arg);
			notif.fire();
		}
	}
}
