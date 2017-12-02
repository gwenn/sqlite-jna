package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

@FunctionalInterface
public interface Destructor extends Callback {
	void callback(Pointer p);
}
