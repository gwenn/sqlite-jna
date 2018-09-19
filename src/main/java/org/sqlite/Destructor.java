package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

@FunctionalInterface
public interface Destructor {
	@Delegate
	void callback(Pointer p);
}
