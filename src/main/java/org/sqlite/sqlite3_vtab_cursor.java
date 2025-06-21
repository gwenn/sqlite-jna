package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.foreign.*;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.SQLite.C_POINTER;

public class sqlite3_vtab_cursor {
	public static final GroupLayout layout = MemoryLayout.structLayout(
		C_POINTER.withName("pVtab")
	).withName("sqlite3_vtab_cursor");

	private static final AddressLayout pVtab = (AddressLayout) layout.select(groupElement("pVtab"));
	@NonNull
	public static MemorySegment pVtab(@NonNull MemorySegment struct, @NonNull MemoryLayout layout) {
		MemorySegment vtab = struct.get(pVtab, 0);
		return vtab.reinterpret(layout.byteSize());
	}
}
