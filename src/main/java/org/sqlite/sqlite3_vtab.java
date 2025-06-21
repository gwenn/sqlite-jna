package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.foreign.*;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.SQLite.*;

public class sqlite3_vtab {
	public static final GroupLayout layout = MemoryLayout.structLayout(
		C_POINTER.withName("pModule"),
		C_INT.withName("nRef"),
		MemoryLayout.paddingLayout(4),
		C_POINTER.withName("zErrMsg")
	).withName("sqlite3_vtab");

	private static final AddressLayout zErrMsg = (AddressLayout)layout.select(groupElement("zErrMsg"));
	private static final int zErrMsgOffset = 16;
	@NonNull
	public static MemorySegment zErrMsg(MemorySegment struct) {
		return struct.get(zErrMsg, zErrMsgOffset);
	}
	public static void zErrMsg(@NonNull MemorySegment struct, @Nullable String errMsg) {
		MemorySegment old = zErrMsg(struct);
		if (!isNull(old)) {
			sqlite3_free(old);
		}
		MemorySegment ms = sqlite3OwnedString(errMsg);
		struct.set(zErrMsg, zErrMsgOffset, ms);
	}
}
