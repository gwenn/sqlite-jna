package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

public interface Destructor {
	@Delegate
	void callback(Pointer p);
}
