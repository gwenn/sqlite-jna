package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface UnlockNotifyCallback extends Callback {
	default void callback(Pointer args, int nArg) {
		if (nArg == 0) {
			notify(new Pointer[0]);
		}
		notify(args.getPointerArray(0, nArg));
	}

	void notify(Pointer[] args);
}
