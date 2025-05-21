package org.sqlite;

import java.lang.foreign.*;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.SQLite.*;

public class sqlite3_vtab {
	static final GroupLayout layout = MemoryLayout.structLayout(
		C_POINTER.withName("pModule"),
		C_INT.withName("nRef"),
		MemoryLayout.paddingLayout(4),
		C_POINTER.withName("zErrMsg")
	).withName("sqlite3_vtab");

	private static final AddressLayout zErrMsg = (AddressLayout)layout.select(groupElement("zErrMsg"));
	private static final int zErrMsgOffset = 16;
	public static MemorySegment zErrMsg(MemorySegment struct) {
		return struct.get(zErrMsg, zErrMsgOffset);
	}
	public static void zErrMsg(MemorySegment struct, String errMsg) {
		final MemorySegment old = zErrMsg(struct);
		if (!isNull(old)) {
			sqlite3_free(old);
		}
		final byte[] bytes = errMsg.getBytes(UTF_8);
		MemorySegment ms = sqlite3_malloc(bytes.length + 1);
		ms = ms.reinterpret(bytes.length + 1);
		MemorySegment.copy(bytes, 0, ms, ValueLayout.JAVA_BYTE, 0, bytes.length);
		ms.set(ValueLayout.JAVA_BYTE, bytes.length, (byte)0);
		struct.set(zErrMsg, zErrMsgOffset, ms);
	}
}
