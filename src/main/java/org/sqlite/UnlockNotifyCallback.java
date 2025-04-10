package org.sqlite;

import java.lang.foreign.MemorySegment;

@FunctionalInterface
public interface UnlockNotifyCallback {
	default void callback(MemorySegment args, int nArg) {
		notify(args, nArg);
	}

	void notify(MemorySegment args, int nArg);
}
