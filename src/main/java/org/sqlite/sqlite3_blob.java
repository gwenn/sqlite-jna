package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static org.sqlite.SQLite.*;

/**
 * A handle to an open BLOB
 *
 * @see <a href="http://sqlite.org/c3ref/blob.html">sqlite3_blob</a>
 */
final class sqlite3_blob {
	private static final boolean OMIT_INCRBLOB = sqlite3_compileoption_used("OMIT_INCRBLOB");
	private MemorySegment p;
	int res;

	sqlite3_blob(MemorySegment p) {
		this.p = p.asReadOnly();
	}

	private static final MethodHandle sqlite3_blob_open = OMIT_INCRBLOB ? null : downcallHandle(
		"sqlite3_blob_open", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_LONG_LONG, C_INT, C_POINTER));
	static int sqlite3_blob_open(@NonNull sqlite3 pDb, String dbName, String tableName, String columnName,
								 long iRow, boolean flags, MemorySegment ppBlob) { // no copy needed
		checkActivated(sqlite3_blob_open, "SQLITE_OMIT_INCRBLOB activated");
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_blob_open.invokeExact(pDb.getPointer(),
				nativeString(arena, dbName), nativeString(arena, tableName), nativeString(arena, columnName),
				iRow, flags ? 1 : 0, ppBlob);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_blob_reopen = OMIT_INCRBLOB ? null : downcallHandle(
		"sqlite3_blob_reopen", FunctionDescriptor.of(C_INT, C_POINTER, C_LONG_LONG));
	static int sqlite3_blob_reopen(@NonNull sqlite3_blob pBlob, long iRow) {
		checkActivated(sqlite3_blob_reopen, "SQLITE_OMIT_INCRBLOB activated");
		try {
			return (int)sqlite3_blob_reopen.invokeExact(pBlob.p, iRow);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_blob_bytes = OMIT_INCRBLOB ? null : downcallHandle(
		"sqlite3_blob_bytes", IP);
	static int sqlite3_blob_bytes(@NonNull sqlite3_blob pBlob) {
		checkActivated(sqlite3_blob_bytes, "SQLITE_OMIT_INCRBLOB activated");
		try {
			return (int)sqlite3_blob_bytes.invokeExact(pBlob.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_blob_read = OMIT_INCRBLOB ? null : downcallHandle(
		"sqlite3_blob_read", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT),
		CRITICAL);
	static int sqlite3_blob_read(@NonNull sqlite3_blob pBlob, byte @NonNull [] z, int off, int len, int iOffset) {
		checkActivated(sqlite3_blob_read, "SQLITE_OMIT_INCRBLOB activated");
		try {
			int n = len - off;
			MemorySegment ms = MemorySegment.ofArray(z);
			return (int) sqlite3_blob_read.invokeExact(pBlob.p, ms.asSlice(off), n, iOffset);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_blob_write = OMIT_INCRBLOB ? null : downcallHandle(
		"sqlite3_blob_write", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT),
		CRITICAL);
	static int sqlite3_blob_write(@NonNull sqlite3_blob pBlob, byte @NonNull[] z, int off, int len, int iOffset) {
		checkActivated(sqlite3_blob_write, "SQLITE_OMIT_INCRBLOB activated");
		try {
			int n = len - off;
			MemorySegment ms = MemorySegment.ofArray(z);
			return (int)sqlite3_blob_write.invokeExact(pBlob.p, ms.asSlice(off), n, iOffset);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_blob_close = OMIT_INCRBLOB ? null : downcallHandle(
		"sqlite3_blob_close", IP);
	static int sqlite3_blob_close(@NonNull sqlite3_blob pBlob) {
		checkActivated(sqlite3_blob_close, "SQLITE_OMIT_INCRBLOB activated");
		try {
			return (int)sqlite3_blob_close.invokeExact(pBlob.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	void close() {
		if (isNull(p)) {
			return;
		}
		res = sqlite3_blob_close(this); // must be called only once
		p = MemorySegment.NULL;
	}

	boolean isClosed() {
		return isNull(p);
	}
}
