package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.In;

@FunctionalInterface
public interface UnlockNotifyCallback {
	@Delegate
	default void callback(@In Pointer args, int nArg) {
		if (nArg == 0) {
			notify(new Pointer[0]);
		}
		Pointer[] dst = new Pointer[nArg];
		args.get(0, dst, 0, nArg);
		notify(dst);
	}

	void notify(Pointer[] args);
}
