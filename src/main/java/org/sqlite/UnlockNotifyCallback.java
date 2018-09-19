package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

@FunctionalInterface
public interface UnlockNotifyCallback {
	@Delegate
	default void callback(Pointer args, int nArg) {
		if (nArg == 0) {
			notify(new Pointer[0]);
		}
		Pointer[] dst = new Pointer[nArg];
		args.get(0, dst, 0, nArg);
		notify(dst);
	}

	void notify(Pointer[] args);
}
