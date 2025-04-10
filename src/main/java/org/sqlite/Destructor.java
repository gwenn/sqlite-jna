package org.sqlite;

import java.lang.foreign.MemorySegment;

@FunctionalInterface
public interface Destructor {
	void callback(MemorySegment p);
}
