package org.sqlite;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static org.sqlite.SQLite.*;

/**
 * Prepared statement object
 *
 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt</a>
 */
final class sqlite3_stmt {
	@NonNull
	private MemorySegment p;
	int res;
	// To avoid copying text twice in sqlite3_bind_text
	private Arena arena;
	// Make sure a stmt is not finalized while current conn is being closed
	@NonNull
	final Object lock;

	sqlite3_stmt(@NonNull Object lock, MemorySegment p) {
		this.lock = lock;
		this.p = p.asReadOnly();
	}

	private static final MethodHandle sqlite3_sql = downcallHandle(
		"sqlite3_sql", PP);
	@NonNull
	static String sqlite3_sql(@NonNull sqlite3_stmt pStmt) {
		return sqlite3_sql(pStmt.p); // no copy needed
	}
	@NonNull
	static String sqlite3_sql(@NonNull MemorySegment stmt) { // no copy needed
		try {
			return getString((MemorySegment) sqlite3_sql.invokeExact(stmt));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_expanded_sql = downcallHandle(
		"sqlite3_expanded_sql", PP);
	@NonNull
	static MemorySegment sqlite3_expanded_sql(@NonNull sqlite3_stmt pStmt) { // sqlite3_free
		try {
			return (MemorySegment) sqlite3_expanded_sql.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_finalize = downcallHandle(
		"sqlite3_finalize", IP);
	private static int sqlite3_finalize(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_finalize.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_step = downcallHandle(
		"sqlite3_step", IP);
	static int sqlite3_step(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_step.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_reset = downcallHandle(
		"sqlite3_reset", IP);
	static int sqlite3_reset(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_reset.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_clear_bindings = downcallHandle(
		"sqlite3_clear_bindings", IP);
	static int sqlite3_clear_bindings(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_clear_bindings.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		} finally {
			pStmt.clear();
		}
	}

	private static final MethodHandle sqlite3_stmt_busy = downcallHandle(
		"sqlite3_stmt_busy", IP);
	static boolean sqlite3_stmt_busy(@NonNull sqlite3_stmt pStmt) {
		return sqlite3_stmt_busy(pStmt.p);
	}
	static boolean sqlite3_stmt_busy(@NonNull MemorySegment stmt) {
		try {
			return ((int) sqlite3_stmt_busy.invokeExact(stmt)) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_stmt_readonly = downcallHandle(
		"sqlite3_stmt_readonly", IP);
	static boolean sqlite3_stmt_readonly(@NonNull sqlite3_stmt pStmt) {
		try {
			return ((int) sqlite3_stmt_readonly.invokeExact(pStmt.p)) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_count = downcallHandle(
		"sqlite3_column_count", IP);
	static int sqlite3_column_count(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_column_count.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_data_count = downcallHandle(
		"sqlite3_data_count", IP);
	static int sqlite3_data_count(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_data_count.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_type = downcallHandle(
		"sqlite3_column_type", IPI);
	static int sqlite3_column_type(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) {
		try {
			return (int) sqlite3_column_type.invokeExact(pStmt.p, iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_name = downcallHandle(
		"sqlite3_column_name", PPI);
	static String sqlite3_column_name(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
		try {
			return getString((MemorySegment) sqlite3_column_name.invokeExact(pStmt.p, iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	public static final boolean ENABLE_COLUMN_METADATA = sqlite3_compileoption_used("ENABLE_COLUMN_METADATA");
	private static final MethodHandle sqlite3_column_origin_name = ENABLE_COLUMN_METADATA ? downcallHandle(
		"sqlite3_column_origin_name", PPI) : null;
	static String sqlite3_column_origin_name(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed
		checkActivated(sqlite3_column_origin_name, "SQLITE_ENABLE_COLUMN_METADATA not activated");
		try {
			return getString((MemorySegment) sqlite3_column_origin_name.invokeExact(pStmt.p, iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_table_name = ENABLE_COLUMN_METADATA ? downcallHandle(
		"sqlite3_column_table_name", PPI) : null;
	@Nullable
	static String sqlite3_column_table_name(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed
		checkActivated(sqlite3_column_table_name, "SQLITE_ENABLE_COLUMN_METADATA not activated");
		try {
			return getString((MemorySegment) sqlite3_column_table_name.invokeExact(pStmt.p, iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_database_name = ENABLE_COLUMN_METADATA ? downcallHandle(
		"sqlite3_column_database_name", PPI) : null;
	@Nullable
	static String sqlite3_column_database_name(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed
		checkActivated(sqlite3_column_database_name, "SQLITE_ENABLE_COLUMN_METADATA not activated");
		try {
			return getString((MemorySegment) sqlite3_column_database_name.invokeExact(pStmt.p, iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_decltype = sqlite3_compileoption_used("OMIT_DECLTYPE") ? null :
		downcallHandle("sqlite3_column_decltype", PPI);
	@Nullable
	static String sqlite3_column_decltype(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed
		checkActivated(sqlite3_column_decltype, "SQLITE_OMIT_DECLTYPE activated");
		try {
			return getString((MemorySegment) sqlite3_column_decltype.invokeExact(pStmt.p, iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_blob = downcallHandle(
		"sqlite3_column_blob", PPI);
	@NonNull
	static MemorySegment sqlite3_column_blob(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		try {
			return (MemorySegment) sqlite3_column_blob.invokeExact(pStmt.p, iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_bytes = downcallHandle(
		"sqlite3_column_bytes", IPI);
	static int sqlite3_column_bytes(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) {
		try {
			return (int) sqlite3_column_bytes.invokeExact(pStmt.p, iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_double = downcallHandle(
		"sqlite3_column_double", FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_INT));
	static double sqlite3_column_double(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) {
		try {
			return (double) sqlite3_column_double.invokeExact(pStmt.p, iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_int = downcallHandle(
		"sqlite3_column_int", IPI);
	static int sqlite3_column_int(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) {
		try {
			return (int) sqlite3_column_int.invokeExact(pStmt.p, iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_int64 = downcallHandle(
		"sqlite3_column_int64", FunctionDescriptor.of(C_LONG_LONG, C_POINTER, C_INT));
	static long sqlite3_column_int64(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) {
		try {
			return (long) sqlite3_column_int64.invokeExact(pStmt.p, iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_text = downcallHandle(
		"sqlite3_column_text", PPI);
	static String sqlite3_column_text(@NonNull sqlite3_stmt pStmt, @NonNegative int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		try {
			return getString((MemorySegment) sqlite3_column_text.invokeExact(pStmt.p, iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	private static final MethodHandle sqlite3_bind_parameter_count = downcallHandle(
		"sqlite3_bind_parameter_count", IP);
	static int sqlite3_bind_parameter_count(@NonNull sqlite3_stmt pStmt) {
		try {
			return (int) sqlite3_bind_parameter_count.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_parameter_index = downcallHandle(
		"sqlite3_bind_parameter_index", IPP);
	static int sqlite3_bind_parameter_index(@NonNull sqlite3_stmt pStmt, @NonNull String name) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_bind_parameter_index.invokeExact(pStmt.p, nativeString(arena, name));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_parameter_name = downcallHandle(
		"sqlite3_bind_parameter_name", PPI);
	@Nullable
	static String sqlite3_bind_parameter_name(@NonNull sqlite3_stmt pStmt, @Positive int i) { // copy needed
		try {
			return getString((MemorySegment) sqlite3_bind_parameter_name.invokeExact(pStmt.p, i));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_blob = downcallHandle(
		"sqlite3_bind_blob", IPIPIP,
		CRITICAL);
	static int sqlite3_bind_blob(@NonNull sqlite3_stmt pStmt, @Positive int i, byte @NonNull [] value, int n, @NonNull MemorySegment xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		try {
			MemorySegment ms = MemorySegment.ofArray(value);
			return (int) sqlite3_bind_blob.invokeExact(pStmt.p, i, ms, n, xDel);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_double = downcallHandle(
		"sqlite3_bind_double", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_DOUBLE));
	static int sqlite3_bind_double(@NonNull sqlite3_stmt pStmt, @Positive int i, double value) {
		try {
			return (int) sqlite3_bind_double.invokeExact(pStmt.p, i, value);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_int = downcallHandle(
		"sqlite3_bind_int", IPII);
	static int sqlite3_bind_int(@NonNull sqlite3_stmt pStmt, @Positive int i, int value) {
		try {
			return (int) sqlite3_bind_int.invokeExact(pStmt.p, i, value);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_int64 = downcallHandle(
		"sqlite3_bind_int64", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_LONG_LONG));
	static int sqlite3_bind_int64(@NonNull sqlite3_stmt pStmt, @Positive int i, long value) {
		try {
			return (int) sqlite3_bind_int64.invokeExact(pStmt.p, i, value);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_null = downcallHandle(
		"sqlite3_bind_null", IPI);
	static int sqlite3_bind_null(@NonNull sqlite3_stmt pStmt, @Positive int i) {
		try {
			return (int) sqlite3_bind_null.invokeExact(pStmt.p, i);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_text = downcallHandle(
		"sqlite3_bind_text", IPIPIP);
	//static int sqlite3_bind_text16(SQLite3Stmt pStmt, int i, const void*, int, void(*)(void*));
	//static int sqlite3_bind_value(SQLite3Stmt pStmt, int i, const sqlite3_value*);
	static int sqlite3_bind_text(@NonNull sqlite3_stmt pStmt, @Positive int i, @NonNull String value, int n, @NonNull MemorySegment xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		try {
			return (int) sqlite3_bind_text.invokeExact(pStmt.p, i, nativeString(pStmt.getArena(), value), n, SQLITE_STATIC);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_zeroblob = downcallHandle(
		"sqlite3_bind_zeroblob", IPII);
	static int sqlite3_bind_zeroblob(@NonNull sqlite3_stmt pStmt, @Positive int i, int n) {
		try {
			return (int) sqlite3_bind_zeroblob.invokeExact(pStmt.p, i, n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_pointer = downcallHandle(
		"sqlite3_bind_pointer", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_bind_pointer(@NonNull sqlite3_stmt pStmt, @Positive int i, @NonNull MemorySegment value, @NonNull MemorySegment name, MemorySegment xDel) {
		try {
			return (int) sqlite3_bind_pointer.invokeExact(pStmt.p, i, value, name, xDel);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_stmt_status = downcallHandle(
		"sqlite3_stmt_status", IPII);
	static int sqlite3_stmt_status(@NonNull sqlite3_stmt pStmt, int op, boolean reset) {
		try {
			return (int) sqlite3_stmt_status.invokeExact(pStmt.p, op, reset ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	public static final boolean ENABLE_STMT_SCANSTATUS = sqlite3_compileoption_used("ENABLE_STMT_SCANSTATUS");
	// TODO https://sqlite.org/c3ref/c_scanstat_est.html constants
	private static final MethodHandle sqlite3_stmt_scanstatus = ENABLE_STMT_SCANSTATUS ? downcallHandle(
		"sqlite3_stmt_scanstatus", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT, C_POINTER)) : null;
	static int sqlite3_stmt_scanstatus(@NonNull sqlite3_stmt pStmt, int idx, int iScanStatusOp, MemorySegment pOut) {
		checkActivated(sqlite3_stmt_scanstatus, "SQLITE_ENABLE_STMT_SCANSTATUS not activated");
		try {
			return (int) sqlite3_stmt_scanstatus.invokeExact(pStmt.p, idx, iScanStatusOp, pOut);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_stmt_scanstatus_reset = ENABLE_STMT_SCANSTATUS ? downcallHandle(
		"sqlite3_stmt_scanstatus_reset", VP) : null;
	static void sqlite3_stmt_scanstatus_reset(@NonNull sqlite3_stmt pStmt) {
		checkActivated(sqlite3_stmt_scanstatus_reset, "SQLITE_ENABLE_STMT_SCANSTATUS not activated");
		try {
			sqlite3_stmt_scanstatus_reset.invokeExact(pStmt.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	void close() {
		synchronized (lock) {
			if (isNull(p)) {
				return;
			}
			res = sqlite3_finalize(this); // must be called only once
			p = MemorySegment.NULL;
		}
	}

	boolean isClosed() {
		return isNull(p);
	}

	private void clear() {
		arena = null;
	}

	private Arena getArena() {
		if (arena == null) { // lazy loading
			arena = Arena.ofAuto();
		}
		return arena;
	}
}
