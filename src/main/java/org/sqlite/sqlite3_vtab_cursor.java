package org.sqlite;

import java.lang.foreign.*;
import java.util.function.Consumer;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.SQLite.C_POINTER;

public class sqlite3_vtab_cursor {
	static final GroupLayout layout = MemoryLayout.structLayout(
		C_POINTER.withName("pVtab")
	).withName("sqlite3_vtab_cursor");

	private static final AddressLayout pVtab = (AddressLayout) layout.select(groupElement("pVtab"));
	public static MemorySegment pVtab(MemorySegment struct) {
		return struct.get(pVtab, 0);
	}
	public static MemorySegment allocate(SegmentAllocator allocator) {
		return allocator.allocate(layout);
	}
}
