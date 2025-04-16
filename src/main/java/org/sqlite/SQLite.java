/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// TODO JNA/Bridj/JNR/JNI and native libs embedded in JAR.
public final class SQLite {
	private static final String JNA_LIBRARY_NAME = "sqlite3";
	private static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
	private static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
	private static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
	private static final AddressLayout C_POINTER = ValueLayout.ADDRESS;

	private static final Arena LIBRARY_ARENA = Arena.ofAuto();
	private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.libraryLookup(
		System.mapLibraryName(System.getProperty("sqlite3.library.name", JNA_LIBRARY_NAME)), LIBRARY_ARENA);

	public static final int SQLITE_OK = 0;

	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;

	static final MemorySegment SQLITE_TRANSIENT = MemorySegment.ofAddress(-1L);

	private static MemorySegment findOrThrow(String symbol) {
		return SYMBOL_LOOKUP.find(symbol)
			.orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
	}
	private static final Linker LINKER = Linker.nativeLinker();
	private static final Linker.Option CRITICAL = Linker.Option.critical(true);
	private static MethodHandle downcallHandle(String symbol, FunctionDescriptor desc,
											   Linker.Option... options) {
		return LINKER.downcallHandle(findOrThrow(symbol), desc, options);
	}
	private static final MethodHandles.Lookup MH_LOOKUP = MethodHandles.lookup();
	static MethodHandle upcallHandle(Class<?> fi, String name, FunctionDescriptor fdesc) {
		try {
			return MH_LOOKUP.findVirtual(fi, name, fdesc.toMethodType());
		} catch (ReflectiveOperationException ex) {
			throw new AssertionError(ex);
		}
	}
	static MemorySegment upcallStub(MethodHandle mh, Object x, FunctionDescriptor fd, Arena arena) {
		if (x == null) {
			return MemorySegment.NULL;
		}
		return LINKER.upcallStub(mh.bindTo(x), fd, arena);
	}

	static String getString(MemorySegment ms) {
		if (MemorySegment.NULL.equals(ms)) {
			return null;
		}
		return ms.reinterpret(Integer.MAX_VALUE).getString(0);
	}

	private static final MethodHandle sqlite3_libversion = downcallHandle(
		"sqlite3_libversion", FunctionDescriptor.of(C_POINTER));
	static String sqlite3_libversion() { // no copy needed
		try {
			return getString((MemorySegment) sqlite3_libversion.invokeExact());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_libversion_number = downcallHandle(
		"sqlite3_libversion_number", FunctionDescriptor.of(C_INT));
	static int sqlite3_libversion_number() {
		try {
			return (int) sqlite3_libversion_number.invokeExact();
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_threadsafe = downcallHandle(
		"sqlite3_threadsafe", FunctionDescriptor.of(C_INT));
	static boolean sqlite3_threadsafe() {
		try {
			return ((int) sqlite3_threadsafe.invokeExact()) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_compileoption_used = downcallHandle(
		"sqlite3_compileoption_used", FunctionDescriptor.of(C_INT, C_POINTER));
	static boolean sqlite3_compileoption_used(String optName) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment ms = arena.allocateFrom(optName);
			return ((int) sqlite3_compileoption_used.invokeExact(ms)) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_compileoption_get = downcallHandle(
		"sqlite3_compileoption_get", FunctionDescriptor.of(C_POINTER, C_INT));
	static String sqlite3_compileoption_get(int n) {
		try {
			return getString((MemorySegment) sqlite3_compileoption_get.invokeExact(n));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	// https://sqlite.org/c3ref/c_config_covering_index_scan.html
	public static final int SQLITE_CONFIG_SINGLETHREAD = 1,
			SQLITE_CONFIG_MULTITHREAD = 2, SQLITE_CONFIG_SERIALIZED = 3,
			SQLITE_CONFIG_MEMSTATUS = 9,
			SQLITE_CONFIG_LOG = 16,
			SQLITE_CONFIG_URI = 17,
			SQLITE_CONFIG_COVERING_INDEX_SCAN = 20,
			SQLITE_CONFIG_STMTJRNL_SPILL = 26,
			SQLITE_CONFIG_SORTERREF_SIZE = 28;
	private static final FunctionDescriptor sqlite3_config_base_desc = FunctionDescriptor.of(C_INT, C_INT);
	private static final Linker.Option sqlite3_config_option = Linker.Option.firstVariadicArg(sqlite3_config_base_desc.argumentLayouts().size());
	private static final MethodHandle sqlite3_config1 = downcallHandle(
		"sqlite3_config",
		sqlite3_config_base_desc);
	//sqlite3_config(SQLITE_CONFIG_SINGLETHREAD|SQLITE_CONFIG_MULTITHREAD|SQLITE_CONFIG_SERIALIZED|SQLITE_CONFIG_COVERING_INDEX_SCAN|SQLITE_CONFIG_STMTJRNL_SPILL|SQLITE_CONFIG_SORTERREF_SIZE)
	static int sqlite3_config(int op) {
		try {
			return (int) sqlite3_config1.invokeExact(op);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_config2 = downcallHandle(
		"sqlite3_config",
		sqlite3_config_base_desc.appendArgumentLayouts(C_INT),
		sqlite3_config_option);
	//sqlite3_config(SQLITE_CONFIG_URI, int onoff)
	//sqlite3_config(SQLITE_CONFIG_MEMSTATUS, int onoff)
	static int sqlite3_config(int op, boolean onoff) {
		try {
			return (int) sqlite3_config2.invokeExact(op, onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_config_log = downcallHandle(
		"sqlite3_config",
		sqlite3_config_base_desc.appendArgumentLayouts(C_POINTER, C_POINTER),
		sqlite3_config_option);
	private static final FunctionDescriptor log_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER);
	//sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
	public static int sqlite3_config(int op, Class<?> clz, String name, MemorySegment udp) {
		try {
			MethodHandle xLogHandle = MH_LOOKUP.findStatic(clz, name, log_callback_desc.toMethodType());
			MemorySegment xLog = LINKER.upcallStub(xLogHandle, log_callback_desc, Arena.global());
			return (int) sqlite3_config_log.invokeExact(op, xLog, udp);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	// Applications can use the sqlite3_log(E,F,..) API to send new messages to the log, if desired, but this is discouraged.
	private static final MethodHandle sqlite3_log = downcallHandle(
		"sqlite3_log", FunctionDescriptor.ofVoid(C_INT, C_POINTER));
	public static void sqlite3_log(int iErrCode, String msg) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment ms = arena.allocateFrom(msg);
			sqlite3_log.invokeExact(iErrCode, ms);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_errstr = downcallHandle(
		"sqlite3_errstr", FunctionDescriptor.of(C_POINTER, C_INT));
	static String sqlite3_errstr(int rc) {
		try {
			return getString((MemorySegment) sqlite3_errstr.invokeExact(rc));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_errmsg = downcallHandle(
		"sqlite3_errmsg", FunctionDescriptor.of(C_POINTER, C_POINTER));
	static String sqlite3_errmsg(SQLite3 pDb) { // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
		try {
			return getString((MemorySegment) sqlite3_errmsg.invokeExact(pDb.getPointer()));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_errcode = downcallHandle(
		"sqlite3_errcode", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_errcode(SQLite3 pDb) {
		try {
			return (int) sqlite3_errcode.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_extended_result_codes = downcallHandle(
		"sqlite3_extended_result_codes", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_extended_result_codes(SQLite3 pDb, boolean onoff) {
		try {
			return (int) sqlite3_extended_result_codes.invokeExact(pDb.getPointer(), onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_extended_errcode = downcallHandle(
		"sqlite3_extended_errcode", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_extended_errcode(SQLite3 pDb) {
		try {
			return (int) sqlite3_extended_errcode.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_initialize = downcallHandle(
		"sqlite3_initialize", FunctionDescriptor.of(C_INT));
	static int sqlite3_initialize() {
		try {
			return (int) sqlite3_initialize.invokeExact();
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_shutdown = downcallHandle(
		"sqlite3_shutdown", FunctionDescriptor.of(C_INT));
	static int sqlite3_shutdown() {
		try {
			return (int) sqlite3_shutdown.invokeExact();
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_open_v2 = downcallHandle(
		"sqlite3_open_v2", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_POINTER));
	static int sqlite3_open_v2(String filename, MemorySegment ppDb, int flags, String vfs) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment filenameSegment = arena.allocateFrom(filename);
			MemorySegment vfsSegment = vfs == null ? MemorySegment.NULL : arena.allocateFrom(vfs);
			return (int) sqlite3_open_v2.invokeExact(filenameSegment, ppDb, flags, vfsSegment);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_close = downcallHandle(
		"sqlite3_close", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_close(SQLite3 pDb) {
		try {
			return (int) sqlite3_close.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_close_v2 = downcallHandle(
		"sqlite3_close_v2", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_close_v2(SQLite3 pDb) { // since 3.7.14
		try {
			return (int) sqlite3_close_v2.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_interrupt = downcallHandle(
		"sqlite3_interrupt", FunctionDescriptor.ofVoid(C_POINTER));
	static void sqlite3_interrupt(SQLite3 pDb) {
		try {
			sqlite3_interrupt.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_busy_handler = downcallHandle(
		"sqlite3_busy_handler", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor busy_handler_desc = FunctionDescriptor.of(C_INT, C_POINTER, C_INT);
	private static final MethodHandle busy_handler = upcallHandle(BusyHandler.class, "callback",
		busy_handler_desc);
	static int sqlite3_busy_handler(SQLite3 pDb, BusyHandler bh, MemorySegment pArg) {
		try {
			MemorySegment busyHandler = upcallStub(busy_handler, bh, busy_handler_desc, pDb.arena);
			return (int) sqlite3_busy_handler.invokeExact(pDb.getPointer(), busyHandler, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_busy_timeout = downcallHandle(
		"sqlite3_busy_timeout", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_busy_timeout(SQLite3 pDb, int ms) {
		try {
			// TODO How to free previous busyHandler ?
			return (int) sqlite3_busy_timeout.invokeExact(pDb.getPointer(), ms);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_db_status = downcallHandle(
		"sqlite3_db_status", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_POINTER, C_INT));
	static int sqlite3_db_status(SQLite3 pDb, int op, MemorySegment pCur, MemorySegment pHiwtr, boolean resetFlg) {
		try {
			return (int) sqlite3_db_status.invokeExact(pDb.getPointer(), op, pCur, pCur, resetFlg ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	// TODO https://sqlite.org/c3ref/c_dbconfig_defensive.html#sqlitedbconfiglookaside constants
	private static final MethodHandle sqlite3_db_config = downcallHandle(
		"sqlite3_db_config", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT, C_POINTER));
	static int sqlite3_db_config(SQLite3 pDb, int op, int v, MemorySegment pOk) {
		try {
			return (int) sqlite3_db_config.invokeExact(pDb.getPointer(), op, v, pOk);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#if sqlite.omit.load.extension == "true"
	static int sqlite3_enable_load_extension(Object pDb, boolean onoff) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	static int sqlite3_load_extension(Object pDb, String file, String proc, MemorySegment errMsg) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
#else
	private static final MethodHandle sqlite3_enable_load_extension = downcallHandle(
	"sqlite3_enable_load_extension", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_enable_load_extension(SQLite3 pDb, boolean onoff) {
		try {
			return (int) sqlite3_enable_load_extension.invokeExact(pDb.getPointer(), onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_load_extension = downcallHandle(
		"sqlite3_load_extension", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_load_extension(SQLite3 pDb, String file, String proc, MemorySegment errMsg) {
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_load_extension.invokeExact(pDb.getPointer(), nativeString(arena, file), nativeString(arena, proc), errMsg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#endif
	// https://sqlite.org/c3ref/c_limit_attached.html
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
			SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
			SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
			SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10, SQLITE_LIMIT_WORKER_THREADS = 11;
	private static final MethodHandle sqlite3_limit = downcallHandle(
		"sqlite3_limit", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT));
	static int sqlite3_limit(SQLite3 pDb, int id, int newVal) {
		try {
			return (int) sqlite3_limit.invokeExact(pDb.getPointer(), id, newVal);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_get_autocommit = downcallHandle(
		"sqlite3_get_autocommit", FunctionDescriptor.of(C_INT, C_POINTER));
	static boolean sqlite3_get_autocommit(SQLite3 pDb) {
		try {
			return ((int) sqlite3_get_autocommit.invokeExact(pDb.getPointer())) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_changes = downcallHandle(
		"sqlite3_changes", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_changes(SQLite3 pDb) {
		try {
			return (int) sqlite3_changes.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#if large.update == "true"
	private static final MethodHandle sqlite3_changes64 = downcallHandle(
	"sqlite3_changes64", FunctionDescriptor.of(C_LONG_LONG, C_POINTER));
	static long sqlite3_changes64(SQLite3 pDb) { // 3.37.0
		try {
			return (long) sqlite3_changes64.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#endif
	private static final MethodHandle sqlite3_total_changes = downcallHandle(
	"sqlite3_total_changes", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_total_changes(SQLite3 pDb) {
		try {
			return (int) sqlite3_total_changes.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#if large.update == "true"
	private static final MethodHandle sqlite3_total_changes64 = downcallHandle(
	"sqlite3_total_changes64", FunctionDescriptor.of(C_INT, C_POINTER));
	static long sqlite3_total_changes64(SQLite3 pDb) { // 3.37.0
		try {
			return (long) sqlite3_total_changes64.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#endif
	private static final MethodHandle sqlite3_last_insert_rowid = downcallHandle(
	"sqlite3_last_insert_rowid", FunctionDescriptor.of(C_LONG_LONG, C_POINTER));
	static long sqlite3_last_insert_rowid(SQLite3 pDb) {
		try {
			return (long) sqlite3_last_insert_rowid.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_db_filename = downcallHandle(
		"sqlite3_db_filename", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER));
	static String sqlite3_db_filename(SQLite3 pDb, String dbName) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return getString((MemorySegment) sqlite3_db_filename.invokeExact(pDb.getPointer(), nativeString(arena, dbName)));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_db_readonly = downcallHandle(
		"sqlite3_db_readonly", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));
	static int sqlite3_db_readonly(SQLite3 pDb, String dbName) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_db_readonly.invokeExact(pDb.getPointer(), nativeString(arena, dbName));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_next_stmt = downcallHandle(
		"sqlite3_next_stmt", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER));
	static SQLite3Stmt sqlite3_next_stmt(SQLite3 pDb, SQLite3Stmt pStmt) {
		try {
			final MemorySegment stmt = pStmt == null ? MemorySegment.NULL : pStmt.getPointer();
			final MemorySegment ms = (MemorySegment) sqlite3_next_stmt.invokeExact(pDb.getPointer(), stmt);
			return MemorySegment.NULL.equals(ms) ? null : new SQLite3Stmt(ms);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_table_column_metadata = downcallHandle(
		"sqlite3_table_column_metadata", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER,
			C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_table_column_metadata(SQLite3 pDb, String dbName, String tableName, String columnName,
													MemorySegment pzDataType, MemorySegment pzCollSeq,
													MemorySegment pNotNull, MemorySegment pPrimaryKey, MemorySegment pAutoinc) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_table_column_metadata.invokeExact(pDb.getPointer(),
				nativeString(arena, dbName), nativeString(arena, tableName), nativeString(arena, columnName),
				pzDataType, pzCollSeq, pNotNull, pPrimaryKey, pAutoinc);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_exec = downcallHandle(
		"sqlite3_exec", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_exec(SQLite3 pDb, String cmd, MemorySegment c, MemorySegment udp, MemorySegment errMsg) {
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_exec.invokeExact(pDb.getPointer(), nativeString(arena, cmd), c, udp, errMsg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	// https://sqlite.org/c3ref/c_prepare_normalize.html
	public static final int SQLITE_PREPARE_PERSISTENT = 0x01/*, SQLITE_PREPARE_NORMALIZE = 0x02*/, SQLITE_PREPARE_NO_VTAB = 0x04;
	private static final MethodHandle sqlite3_prepare_v3 = downcallHandle(
		"sqlite3_prepare_v3", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT, C_POINTER, C_POINTER));
	static int sqlite3_prepare_v3(SQLite3 pDb, MemorySegment sql, int nByte, int prepFlags, MemorySegment ppStmt,
										 MemorySegment pTail) {
		try {
			return (int)sqlite3_prepare_v3.invokeExact(pDb.getPointer(), sql, nByte, prepFlags, ppStmt, pTail);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_sql = downcallHandle(
		"sqlite3_sql", FunctionDescriptor.of(C_POINTER, C_POINTER));
	static String sqlite3_sql(SQLite3Stmt pStmt) { // no copy needed
		try {
			return getString((MemorySegment) sqlite3_sql.invokeExact(pStmt.getPointer()));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_expanded_sql = downcallHandle(
		"sqlite3_expanded_sql", FunctionDescriptor.of(C_POINTER, C_POINTER));
	static MemorySegment sqlite3_expanded_sql(SQLite3Stmt pStmt) { // sqlite3_free
		try {
			return (MemorySegment) sqlite3_expanded_sql.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_finalize = downcallHandle(
		"sqlite3_finalize", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_finalize(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_finalize.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_step = downcallHandle(
		"sqlite3_step", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_step(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_step.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_reset = downcallHandle(
		"sqlite3_reset", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_reset(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_reset.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_clear_bindings = downcallHandle(
		"sqlite3_clear_bindings", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_clear_bindings(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_clear_bindings.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_stmt_busy = downcallHandle(
		"sqlite3_stmt_busy", FunctionDescriptor.of(C_INT, C_POINTER));
	static boolean sqlite3_stmt_busy(SQLite3Stmt pStmt) {
		try {
			return ((int) sqlite3_stmt_busy.invokeExact(pStmt.getPointer())) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_stmt_readonly = downcallHandle(
		"sqlite3_stmt_readonly", FunctionDescriptor.of(C_INT, C_POINTER));
	static boolean sqlite3_stmt_readonly(SQLite3Stmt pStmt) {
		try {
			return ((int) sqlite3_stmt_readonly.invokeExact(pStmt.getPointer())) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_column_count = downcallHandle(
		"sqlite3_column_count", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_column_count(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_column_count.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_data_count = downcallHandle(
		"sqlite3_data_count", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_data_count(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_data_count.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_type = downcallHandle(
		"sqlite3_column_type", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_column_type(SQLite3Stmt pStmt, int iCol) {
		try {
			return (int) sqlite3_column_type.invokeExact(pStmt.getPointer(), iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_name = downcallHandle(
		"sqlite3_column_name", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_column_name(SQLite3Stmt pStmt, int iCol) { // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
		try {
			return getString((MemorySegment) sqlite3_column_name.invokeExact(pStmt.getPointer(), iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#if sqlite.enable.column.metadata == "true"
	private static final MethodHandle sqlite3_column_origin_name = downcallHandle(
	"sqlite3_column_origin_name", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_column_origin_name(SQLite3Stmt pStmt, int iCol) { // copy needed
		try {
			return getString((MemorySegment) sqlite3_column_origin_name.invokeExact(pStmt.getPointer(), iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_table_name = downcallHandle(
		"sqlite3_column_table_name", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_column_table_name(SQLite3Stmt pStmt, int iCol) { // copy needed
		try {
			return getString((MemorySegment) sqlite3_column_table_name.invokeExact(pStmt.getPointer(), iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_database_name = downcallHandle(
		"sqlite3_column_database_name", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_column_database_name(SQLite3Stmt pStmt, int iCol) { // copy needed
		try {
			return getString((MemorySegment) sqlite3_column_database_name.invokeExact(pStmt.getPointer(), iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_decltype = downcallHandle(
		"sqlite3_column_decltype", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_column_decltype(SQLite3Stmt pStmt, int iCol) { // copy needed
		try {
			return getString((MemorySegment) sqlite3_column_decltype.invokeExact(pStmt.getPointer(), iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#else
	static String sqlite3_column_origin_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static String sqlite3_column_table_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static String sqlite3_column_database_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static String sqlite3_column_decltype(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
#endif

	private static final MethodHandle sqlite3_column_blob = downcallHandle(
		"sqlite3_column_blob", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static MemorySegment sqlite3_column_blob(SQLite3Stmt pStmt, int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		try {
			return (MemorySegment) sqlite3_column_blob.invokeExact(pStmt.getPointer(), iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_bytes = downcallHandle(
		"sqlite3_column_bytes", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_column_bytes(SQLite3Stmt pStmt, int iCol) {
		try {
			return (int) sqlite3_column_bytes.invokeExact(pStmt.getPointer(), iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_double = downcallHandle(
		"sqlite3_column_double", FunctionDescriptor.of(C_DOUBLE, C_POINTER, C_INT));
	static double sqlite3_column_double(SQLite3Stmt pStmt, int iCol) {
		try {
			return (double) sqlite3_column_double.invokeExact(pStmt.getPointer(), iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_int = downcallHandle(
		"sqlite3_column_int", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_column_int(SQLite3Stmt pStmt, int iCol) {
		try {
			return (int) sqlite3_column_int.invokeExact(pStmt.getPointer(), iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_int64 = downcallHandle(
		"sqlite3_column_int64", FunctionDescriptor.of(C_LONG_LONG, C_POINTER, C_INT));
	static long sqlite3_column_int64(SQLite3Stmt pStmt, int iCol) {
		try {
			return (long) sqlite3_column_int64.invokeExact(pStmt.getPointer(), iCol);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_column_text = downcallHandle(
		"sqlite3_column_text", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_column_text(SQLite3Stmt pStmt, int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		try {
			return getString((MemorySegment) sqlite3_column_text.invokeExact(pStmt.getPointer(), iCol));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	private static final MethodHandle sqlite3_bind_parameter_count = downcallHandle(
		"sqlite3_bind_parameter_count", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_bind_parameter_count(SQLite3Stmt pStmt) {
		try {
			return (int) sqlite3_bind_parameter_count.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_parameter_index = downcallHandle(
		"sqlite3_bind_parameter_index", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER));
	static int sqlite3_bind_parameter_index(SQLite3Stmt pStmt, String name) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_bind_parameter_index.invokeExact(pStmt.getPointer(), nativeString(arena, name));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_parameter_name = downcallHandle(
		"sqlite3_bind_parameter_name", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static String sqlite3_bind_parameter_name(SQLite3Stmt pStmt, int i) { // copy needed
		try {
			return getString((MemorySegment) sqlite3_bind_parameter_name.invokeExact(pStmt.getPointer(), i));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_bind_blob = downcallHandle(
		"sqlite3_bind_blob", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_INT, C_POINTER),
		CRITICAL);
	static int sqlite3_bind_blob(SQLite3Stmt pStmt, int i, byte[] value, int n, MemorySegment xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		try {
			MemorySegment ms = MemorySegment.ofArray(value);
			return (int) sqlite3_bind_blob.invokeExact(pStmt.getPointer(), i, ms, n, xDel);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_double = downcallHandle(
		"sqlite3_bind_double", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_DOUBLE));
	static int sqlite3_bind_double(SQLite3Stmt pStmt, int i, double value) {
		try {
			return (int) sqlite3_bind_double.invokeExact(pStmt.getPointer(), i, value);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_int = downcallHandle(
		"sqlite3_bind_int", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT));
	static int sqlite3_bind_int(SQLite3Stmt pStmt, int i, int value) {
		try {
			return (int) sqlite3_bind_int.invokeExact(pStmt.getPointer(), i, value);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_int64 = downcallHandle(
		"sqlite3_bind_int64", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_LONG_LONG));
	static int sqlite3_bind_int64(SQLite3Stmt pStmt, int i, long value) {
		try {
			return (int) sqlite3_bind_int64.invokeExact(pStmt.getPointer(), i, value);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_null = downcallHandle(
		"sqlite3_bind_null", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_bind_null(SQLite3Stmt pStmt, int i) {
		try {
			return (int) sqlite3_bind_null.invokeExact(pStmt.getPointer(), i);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_bind_text = downcallHandle(
		"sqlite3_bind_text", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_INT, C_POINTER));
	static int sqlite3_bind_text(SQLite3Stmt pStmt, int i, String value, int n, MemorySegment xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		try (Arena arena = Arena.ofConfined()) {
			// How to avoid copying twice ? nativeString + SQLite
			return (int) sqlite3_bind_text.invokeExact(pStmt.getPointer(), i, nativeString(arena, value), n, xDel);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	//static int sqlite3_bind_text16(SQLite3Stmt pStmt, int i, const void*, int, void(*)(void*));
	//static int sqlite3_bind_value(SQLite3Stmt pStmt, int i, const sqlite3_value*);
	private static final MethodHandle sqlite3_bind_zeroblob = downcallHandle(
		"sqlite3_bind_zeroblob", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT));
	static int sqlite3_bind_zeroblob(SQLite3Stmt pStmt, int i, int n) {
		try {
			return (int) sqlite3_bind_zeroblob.invokeExact(pStmt.getPointer(), i, n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_stmt_status = downcallHandle(
		"sqlite3_stmt_status", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT));
	static int sqlite3_stmt_status(SQLite3Stmt pStmt, int op, boolean reset) {
		try {
			return (int) sqlite3_stmt_status.invokeExact(pStmt.getPointer(), op, reset ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#if sqlite.enable.stmt.scanstatus == "true"
	// TODO https://sqlite.org/c3ref/c_scanstat_est.html constants
	private static final MethodHandle sqlite3_stmt_scanstatus = downcallHandle(
	"sqlite3_stmt_scanstatus", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT, C_POINTER));
	static int sqlite3_stmt_scanstatus(SQLite3Stmt pStmt, int idx, int iScanStatusOp, MemorySegment pOut) {
		try {
			return (int) sqlite3_stmt_scanstatus.invokeExact(pStmt.getPointer(), idx, iScanStatusOp, pOut);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_stmt_scanstatus_reset = downcallHandle(
		"sqlite3_stmt_scanstatus_reset", FunctionDescriptor.ofVoid(C_POINTER));
	static void sqlite3_stmt_scanstatus_reset(SQLite3Stmt pStmt) {
		try {
			sqlite3_stmt_scanstatus_reset.invokeExact(pStmt.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#endif

	private static final MethodHandle sqlite3_free = downcallHandle(
		"sqlite3_free", FunctionDescriptor.ofVoid(C_POINTER));
	static void sqlite3_free(MemorySegment p) {
		try {
			sqlite3_free.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_blob_open = downcallHandle(
		"sqlite3_blob_open", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_LONG_LONG, C_INT, C_POINTER));
	static int sqlite3_blob_open(SQLite3 pDb, String dbName, String tableName, String columnName,
			long iRow, boolean flags, MemorySegment ppBlob) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_blob_open.invokeExact(pDb.getPointer(),
				nativeString(arena, dbName), nativeString(arena, tableName), nativeString(arena, columnName),
				iRow, flags ? 1 : 0, ppBlob);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_blob_reopen = downcallHandle(
		"sqlite3_blob_reopen", FunctionDescriptor.of(C_INT, C_POINTER, C_LONG_LONG));
	static int sqlite3_blob_reopen(SQLite3Blob pBlob, long iRow) {
		try {
			return (int)sqlite3_blob_reopen.invokeExact(pBlob.getPointer(), iRow);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_blob_bytes = downcallHandle(
		"sqlite3_blob_bytes", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_blob_bytes(SQLite3Blob pBlob) {
		try {
			return (int)sqlite3_blob_bytes.invokeExact(pBlob.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_blob_read = downcallHandle(
		"sqlite3_blob_read", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT),
		CRITICAL);
	static int sqlite3_blob_read(SQLite3Blob pBlob, byte[] z,  int off, int len, int iOffset) {
		try {
			int n = len - off;
			MemorySegment ms = MemorySegment.ofArray(z);
			return (int) sqlite3_blob_read.invokeExact(pBlob.getPointer(), ms.asSlice(off), n, iOffset);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_blob_write = downcallHandle(
		"sqlite3_blob_write", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT),
		CRITICAL);
	static int sqlite3_blob_write(SQLite3Blob pBlob, byte[] z, int off, int len, int iOffset) {
		try {
			int n = len - off;
			MemorySegment ms = MemorySegment.ofArray(z);
			return (int)sqlite3_blob_write.invokeExact(pBlob.getPointer(), ms.asSlice(off), n, iOffset);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_blob_close = downcallHandle(
		"sqlite3_blob_close", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_blob_close(SQLite3Blob pBlob) {
		try {
			return (int)sqlite3_blob_close.invokeExact(pBlob.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_init = downcallHandle(
		"sqlite3_backup_init", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static SQLite3Backup sqlite3_backup_init(SQLite3 pDst, String dstName, SQLite3 pSrc, String srcName) {
		try (Arena arena = Arena.ofConfined()) {
			return new SQLite3Backup((MemorySegment)sqlite3_backup_init.invokeExact(pDst.getPointer(),
				nativeString(arena, dstName), pSrc.getPointer(), nativeString(arena, srcName)));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_backup_step = downcallHandle(
		"sqlite3_backup_step", FunctionDescriptor.of(C_INT, C_POINTER, C_INT));
	static int sqlite3_backup_step(SQLite3Backup pBackup, int nPage) {
		try {
			return (int)sqlite3_backup_step.invokeExact(pBackup.getPointer(), nPage);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_backup_remaining = downcallHandle(
		"sqlite3_backup_remaining", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_backup_remaining(SQLite3Backup pBackup) {
		try {
			return (int)sqlite3_backup_remaining.invokeExact(pBackup.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_backup_pagecount = downcallHandle(
		"sqlite3_backup_pagecount", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_backup_pagecount(SQLite3Backup pBackup) {
		try {
			return (int)sqlite3_backup_pagecount.invokeExact(pBackup.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_backup_finish = downcallHandle(
		"sqlite3_backup_finish", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_backup_finish(SQLite3Backup pBackup) {
		try {
			return (int)sqlite3_backup_finish.invokeExact(pBackup.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	private static final MethodHandle sqlite3_progress_handler = downcallHandle(
		"sqlite3_progress_handler", FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER));
	private static final FunctionDescriptor progress_callback_desc = FunctionDescriptor.of(C_INT, C_POINTER);
	private static final MethodHandle progress_callback = upcallHandle(ProgressCallback.class, "callback",
		progress_callback_desc);
	static void sqlite3_progress_handler(SQLite3 pDb, int nOps, ProgressCallback xProgress, MemorySegment pArg) {
		try {
			MemorySegment pc = upcallStub(progress_callback, xProgress, progress_callback_desc, pDb.arena);
			sqlite3_progress_handler.invokeExact(pDb.getPointer(), nOps, pc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_trace = downcallHandle(
		"sqlite3_trace", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor trace_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_POINTER);
	private static final MethodHandle trace_callback = upcallHandle(TraceCallback.class, "callback",
		trace_callback_desc);
	static void sqlite3_trace(SQLite3 pDb, TraceCallback xTrace, MemorySegment pArg) {
		try {
			MemorySegment tc = upcallStub(trace_callback, xTrace, trace_callback_desc, pDb.arena);
			sqlite3_trace.invokeExact(pDb.getPointer(), tc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_profile = downcallHandle(
		"sqlite3_profile", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor profile_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_LONG_LONG);
	private static final MethodHandle profile_callback = upcallHandle(ProfileCallback.class, "callback",
		profile_callback_desc);
	static void sqlite3_profile(SQLite3 pDb, ProfileCallback xProfile, MemorySegment pArg) {
		try {
			MemorySegment pc = upcallStub(profile_callback, xProfile, profile_callback_desc, pDb.arena);
			sqlite3_profile.invokeExact(pDb.getPointer(), pc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	// TODO sqlite3_commit_hook, sqlite3_rollback_hook
	private static final MethodHandle sqlite3_update_hook = downcallHandle(
		"sqlite3_update_hook", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor update_hook_desc = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER, C_LONG_LONG);
	private static final MethodHandle update_hook = upcallHandle(UpdateHook.class, "callback",
		update_hook_desc);
	static MemorySegment sqlite3_update_hook(SQLite3 pDb, UpdateHook xUpdate, MemorySegment pArg) {
		try {
			MemorySegment uh = upcallStub(update_hook, xUpdate, update_hook_desc, pDb.arena);
			return (MemorySegment)sqlite3_update_hook.invokeExact(pDb.getPointer(), uh, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_set_authorizer = downcallHandle(
		"sqlite3_set_authorizer", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor authorizer_desc = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER);
	private static final MethodHandle authorizer_up = upcallHandle(Authorizer.class, "callback",
		authorizer_desc);
	static int sqlite3_set_authorizer(SQLite3 pDb, Authorizer authorizer, MemorySegment pUserData) {
		try {
			MemorySegment auth = upcallStub(authorizer_up, authorizer, authorizer_desc, pDb.arena);
			return (int)sqlite3_set_authorizer.invokeExact(pDb.getPointer(), auth, pUserData);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#if sqlite.enable.unlock.notify == "true"
	private static final MethodHandle sqlite3_unlock_notify = downcallHandle(
	"sqlite3_unlock_notify", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor unlock_notify_desc = FunctionDescriptor.ofVoid(C_POINTER, C_INT);
	private static final MethodHandle unlock_notify_up = upcallHandle(UnlockNotifyCallback.class, "callback",
		unlock_notify_desc);
	static int sqlite3_unlock_notify(SQLite3 pBlocked, UnlockNotifyCallback xNotify, MemorySegment pNotifyArg) {
		try {
			MemorySegment unc = upcallStub(unlock_notify_up, xNotify, unlock_notify_desc, pBlocked.arena);
			return (int)sqlite3_unlock_notify.invokeExact(pBlocked.getPointer(), unc, pNotifyArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
#endif

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	private static final MethodHandle sqlite3_create_function_v2 = downcallHandle(
		"sqlite3_create_function_v2", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor scalar_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER);
	private static final MethodHandle scalar_callback = upcallHandle(ScalarCallback.class, "callback",
		scalar_callback_desc);
	private static final FunctionDescriptor aggregate_step_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER);
	private static final MethodHandle aggregate_step_callback = upcallHandle(AggregateStepCallback.class, "callback",
		aggregate_step_callback_desc);
	private static final FunctionDescriptor aggregate_final_callback_desc = FunctionDescriptor.ofVoid(C_POINTER);
	private static final MethodHandle aggregate_final_callback = upcallHandle(AggregateFinalCallback.class, "callback",
		aggregate_final_callback_desc);
	static int sqlite3_create_function_v2(SQLite3 pDb, String functionName, int nArg, int eTextRep,
		MemorySegment pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateFinalCallback xFinal, MemorySegment xDestroy) {
		try (Arena arena = Arena.ofConfined()) {
			final MemorySegment xFu = upcallStub(scalar_callback, xFunc, scalar_callback_desc, pDb.arena);
			final MemorySegment xS = upcallStub(aggregate_step_callback, xStep, aggregate_step_callback_desc, pDb.arena);
			final MemorySegment xFi = upcallStub(aggregate_final_callback, xFinal, aggregate_final_callback_desc, pDb.arena);
			return (int)sqlite3_create_function_v2.invokeExact(pDb.getPointer(), nativeString(arena, functionName), nArg, eTextRep, pApp, xFu, xS, xFi, xDestroy);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_null = downcallHandle(
		"sqlite3_result_null", FunctionDescriptor.ofVoid(C_POINTER));
	static void sqlite3_result_null(SQLite3Context pCtx) {
		try {
			sqlite3_result_null.invokeExact(pCtx.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_int = downcallHandle(
		"sqlite3_result_int", FunctionDescriptor.ofVoid(C_POINTER, C_INT));
	static void sqlite3_result_int(SQLite3Context pCtx, int i) {
		try {
			sqlite3_result_int.invokeExact(pCtx.p, i);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_double = downcallHandle(
		"sqlite3_result_double", FunctionDescriptor.ofVoid(C_POINTER, C_DOUBLE));
	static void sqlite3_result_double(SQLite3Context pCtx, double d) {
		try {
			sqlite3_result_double.invokeExact(pCtx.p, d);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_text = downcallHandle(
		"sqlite3_result_text", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_INT, C_POINTER));
	static void sqlite3_result_text(SQLite3Context pCtx, String text, int n, MemorySegment xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		try (Arena arena = Arena.ofConfined()) {
			// How to avoid copying twice ? nativeString + SQLite
			sqlite3_result_text.invokeExact(pCtx.p, nativeString(arena, text), n, xDel);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_blob = downcallHandle(
		"sqlite3_result_blob", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_INT, C_POINTER),
		CRITICAL);
	static void sqlite3_result_blob(SQLite3Context pCtx, byte[] blob, int n, MemorySegment xDel) {
		try {
			MemorySegment ms = MemorySegment.ofArray(blob);
			sqlite3_result_blob.invokeExact(pCtx.p, ms, n, xDel);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_int64 = downcallHandle(
		"sqlite3_result_int64", FunctionDescriptor.ofVoid(C_POINTER, C_LONG_LONG));
	static void sqlite3_result_int64(SQLite3Context pCtx, long l) {
		try {
			sqlite3_result_int64.invokeExact(pCtx.p, l);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_zeroblob = downcallHandle(
		"sqlite3_result_zeroblob", FunctionDescriptor.ofVoid(C_POINTER, C_INT));
	static void sqlite3_result_zeroblob(SQLite3Context pCtx, int n) {
		try {
			sqlite3_result_zeroblob.invokeExact(pCtx.p, n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_error = downcallHandle(
		"sqlite3_result_error", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_INT));
	static void sqlite3_result_error(SQLite3Context pCtx, String err, int length) {
		try (Arena arena = Arena.ofConfined()) {
			sqlite3_result_error.invokeExact(pCtx.p, nativeString(arena, err), length);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_error_code = downcallHandle(
		"sqlite3_result_error_code", FunctionDescriptor.ofVoid(C_POINTER, C_INT));
	static void sqlite3_result_error_code(SQLite3Context pCtx, int errCode) {
		try {
			sqlite3_result_error_code.invokeExact(pCtx.p, errCode);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_error_nomem = downcallHandle(
		"sqlite3_result_error_nomem", FunctionDescriptor.ofVoid(C_POINTER));
	static void sqlite3_result_error_nomem(SQLite3Context pCtx) {
		try {
			sqlite3_result_error_nomem.invokeExact(pCtx.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_result_error_toobig = downcallHandle(
		"sqlite3_result_error_toobig", FunctionDescriptor.ofVoid(C_POINTER));
	static void sqlite3_result_error_toobig(SQLite3Context pCtx) {
		try {
			sqlite3_result_error_toobig.invokeExact(pCtx.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	//static void sqlite3_result_subtype(SQLite3Context pCtx, /*unsigned*/ int subtype);

	private static final MethodHandle sqlite3_value_blob = downcallHandle(
		"sqlite3_value_blob", FunctionDescriptor.of(C_POINTER, C_POINTER));
	static MemorySegment sqlite3_value_blob(MemorySegment pValue) {
		try {
			return (MemorySegment)sqlite3_value_blob.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_bytes = downcallHandle(
		"sqlite3_value_bytes", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_value_bytes(MemorySegment pValue) {
		try {
			return (int)sqlite3_value_bytes.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_double = downcallHandle(
		"sqlite3_value_double", FunctionDescriptor.of(C_LONG_LONG, C_POINTER));
	static double sqlite3_value_double(MemorySegment pValue) {
		try {
			return (double)sqlite3_value_double.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_int = downcallHandle(
		"sqlite3_value_int", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_value_int(MemorySegment pValue) {
		try {
			return (int)sqlite3_value_int.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_int64 = downcallHandle(
		"sqlite3_value_int64", FunctionDescriptor.of(C_LONG_LONG, C_POINTER));
	static long sqlite3_value_int64(MemorySegment pValue) {
		try {
			return (long)sqlite3_value_int64.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_text = downcallHandle(
		"sqlite3_value_text", FunctionDescriptor.of(C_POINTER, C_POINTER));
	static String sqlite3_value_text(MemorySegment pValue){
		try {
			return getString((MemorySegment)sqlite3_value_text.invokeExact(pValue));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_type = downcallHandle(
		"sqlite3_value_type", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_value_type(MemorySegment pValue){
		try {
			return (int)sqlite3_value_type.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_value_numeric_type = downcallHandle(
		"sqlite3_value_numeric_type", FunctionDescriptor.of(C_INT, C_POINTER));
	static int sqlite3_value_numeric_type(MemorySegment pValue){
		try {
			return (int)sqlite3_value_numeric_type.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_get_auxdata = downcallHandle(
		"sqlite3_get_auxdata", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static MemorySegment sqlite3_get_auxdata(SQLite3Context pCtx, int n) {
		try {
			return (MemorySegment)sqlite3_get_auxdata.invokeExact(pCtx.p, n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_set_auxdata = downcallHandle(
		"sqlite3_set_auxdata", FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER));
	static void sqlite3_set_auxdata(SQLite3Context pCtx, int n, MemorySegment p, Destructor free) {
		try {
			sqlite3_set_auxdata.invokeExact(pCtx.p, n, p, free);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_aggregate_context = downcallHandle(
		"sqlite3_aggregate_context", FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT));
	static MemorySegment sqlite3_aggregate_context(SQLite3Context pCtx, int nBytes) {
		try {
			return (MemorySegment)sqlite3_aggregate_context.invokeExact(pCtx.p, nBytes);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_context_db_handle = downcallHandle(
		"sqlite3_context_db_handle", FunctionDescriptor.of(C_POINTER, C_POINTER));
	static SQLite3 sqlite3_context_db_handle(SQLite3Context pCtx) {
		try {
			return new SQLite3((MemorySegment)sqlite3_context_db_handle.invokeExact(pCtx.p));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	public static final String UTF_8_ECONDING = UTF_8.name();
	static MemorySegment nativeString(Arena arena, String sql) {
		if (sql == null) {
			return MemorySegment.NULL;
		}
		return arena.allocateFrom(sql);
	}

	// http://sqlite.org/datatype3.html
	public static int getAffinity(String declType) {
		if (declType == null || declType.isEmpty()) {
			return ColAffinities.NONE;
		}
		declType = declType.toUpperCase();
		if (declType.contains("INT")) {
			return ColAffinities.INTEGER;
		} else if (declType.contains("TEXT") || declType.contains("CHAR") || declType.contains("CLOB")) {
			return ColAffinities.TEXT;
		} else if (declType.contains("BLOB")) {
			return ColAffinities.NONE;
		} else if (declType.contains("REAL") || declType.contains("FLOA") || declType.contains("DOUB")) {
			return ColAffinities.REAL;
		} else {
			return ColAffinities.NUMERIC;
		}
	}

	private SQLite() {
	}

	public static String escapeIdentifier(String identifier) {
		if (identifier == null) {
			return "";
		}
		if (identifier.indexOf('"') >= 0) { // escape quote by doubling them
			identifier = identifier.replaceAll("\"", "\"\"");
		}
		return identifier;
	}

	private static void log_callback(MemorySegment udp, int err, MemorySegment msg) {
		System.out.printf("%d: %s%n", err, getString(msg));
	}

	static {
		if (!System.getProperty("sqlite.config.log", "").isEmpty()) {
			// DriverManager.getLogWriter();
			final int res = sqlite3_config(SQLITE_CONFIG_LOG, SQLite.class, "log_callback", MemorySegment.NULL);
			if (res != SQLITE_OK) {
				throw new ExceptionInInitializerError("sqlite3_config(SQLITE_CONFIG_LOG, ...) = " + res);
			}
		}
	}

	/**
	 * Query Progress Callback.
	 * @see <a href="http://sqlite.org/c3ref/progress_handler.html">sqlite3_progress_handler</a>
	 */
	@FunctionalInterface
	public interface ProgressCallback {
		/**
		 * @return <code>true</code> to interrupt
		 */
		@SuppressWarnings("unused")
		default int callback(MemorySegment arg) {
			return progress() ? 1 : 0;
		}

		boolean progress();
	}

	/**
	 * Database connection handle
	 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
	 */
	public static class SQLite3 {
		private final MemorySegment p;
		private final Arena arena = Arena.ofAuto();
		SQLite3(MemorySegment p) {
			this.p = p;
		}
		MemorySegment getPointer() {
			return p;
		}
	}

	/**
	 * Prepared statement object
	 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt</a>
	 */
	public static class SQLite3Stmt {
		private final MemorySegment p;
		SQLite3Stmt(MemorySegment p) {
			this.p = p;
		}
		MemorySegment getPointer() {
			return p;
		}
	}

	/**
	 * A handle to an open BLOB
	 * @see <a href="http://sqlite.org/c3ref/blob.html">sqlite3_blob</a>
	 */
	public static class SQLite3Blob {
		private final MemorySegment p;
		SQLite3Blob(MemorySegment p) {
			this.p = p;
		}
		MemorySegment getPointer() {
			return p;
		}
	}

	/**
	 * Online backup object
	 * @see <a href="http://sqlite.org/c3ref/backup.html">sqlite3_backup</a>
	 */
	public static class SQLite3Backup {
		private final MemorySegment p;
		SQLite3Backup(MemorySegment p) {
			this.p = p;
		}
		MemorySegment getPointer() {
			return p;
		}
	}

	/**
	 * SQL function context object
	 * @see <a href="http://sqlite.org/c3ref/context.html">sqlite3_context</a>
	 */
	public static class SQLite3Context {
		private final MemorySegment p;
		SQLite3Context(MemorySegment p) {
			this.p = p;
		}

		/**
		 * @return a copy of the pointer to the database connection (the 1st parameter) of
		 * {@link SQLite#sqlite3_create_function_v2(SQLite3, String, int, int, MemorySegment, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, MemorySegment)}
		 * @see <a href="http://sqlite.org/c3ref/context_db_handle.html">sqlite3_context_db_handle</a>
         */
		public SQLite3 getDbHandle() {
			return sqlite3_context_db_handle(this);
		}

		/**
		 * Sets the return value of the application-defined function to be the BLOB value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_blob</a>
		 */
		public void setResultBlob(byte[] result) {
			sqlite3_result_blob(this, result, result.length, SQLITE_TRANSIENT);
		}
		/**
		 * Sets the return value of the application-defined function to be the floating point value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_double</a>
		 */
		public void setResultDouble(double result) {
			sqlite3_result_double(this, result);
		}
		/**
		 * Sets the return value of the application-defined function to be the 32-bit signed integer value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int</a>
		 */
		public void setResultInt(int result) {
			sqlite3_result_int(this, result);
		}
		/**
		 * Sets the return value of the application-defined function to be the 64-bit signed integer value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int64</a>
		 */
		public void setResultLong(long result) {
			sqlite3_result_int64(this, result);
		}
		/**
		 * Sets the return value of the application-defined function to be NULL.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_null</a>
		 */
		public void setResultNull() {
			sqlite3_result_null(this);
		}
		/**
		 * Sets the return value of the application-defined function to be the text string given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_text</a>
		 */
		public void setResultText(String result) {
			sqlite3_result_text(this, result, -1, SQLITE_TRANSIENT);
		}
		/**
		 * Sets the return value of the application-defined function to be a BLOB containing all zero bytes and N bytes in size.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_zeroblob</a>
		 */
		public void setResultZeroBlob(ZeroBlob result) {
			sqlite3_result_zeroblob(this, result.n);
		}

		/*
		 * Causes the subtype of the result from the application-defined SQL function to be the value given.
		 * @see <a href="http://sqlite.org/c3ref/result_subtype.html">sqlite3_result_subtype</a>
		 */
		/*public void setResultSubType(int subtype) {
			sqlite3_result_subtype(this, subtype);
		}*/

		/**
		 * Causes the implemented SQL function to throw an exception.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error</a>
		 */
		public void setResultError(String errMsg) {
			sqlite3_result_error(this, errMsg, -1);
		}
		/**
		 * Causes the implemented SQL function to throw an exception.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_code</a>
		 */
		public void setResultErrorCode(int errCode) {
			sqlite3_result_error_code(this, errCode);
		}
		/**
		 * Causes SQLite to throw an error indicating that a memory allocation failed.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_nomem</a>
		 */
		public void setResultErrorNoMem() {
			sqlite3_result_error_nomem(this);
		}
		/**
		 * Causes SQLite to throw an error indicating that a string or BLOB is too long to represent.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_toobig</a>
		 */
		public void setResultErrorTooBig() {
			sqlite3_result_error_toobig(this);
		}
	}

	/**
	 * Dynamically typed value objects
	 * @see <a href="http://sqlite.org/c3ref/value.html">sqlite3_value</a>
	 */
	public static class SQLite3Values {
		private static final SQLite3Values NO_ARG = new SQLite3Values(MemorySegment.NULL, 0);
		private final MemorySegment args;
		private final int nArg;

		public static SQLite3Values build(int nArg, MemorySegment args) {
			if (nArg == 0) {
				return NO_ARG;
			}
			return new SQLite3Values(args, nArg);
		}

		private SQLite3Values(MemorySegment args, int nArg) {
			this.args = args.reinterpret(nArg * ValueLayout.ADDRESS.byteSize());
			this.nArg = nArg;
		}

		/**
		 * @return arg count
		 */
		public int getCount() {
			return nArg;
		}

		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_blob</a>
		 */
		public byte[] getBlob(int i) {
			MemorySegment arg = arg(i);
			MemorySegment blob = sqlite3_value_blob(arg);
			if (MemorySegment.NULL.equals(blob)) {
				return null;
			} else {
				return blob.reinterpret(sqlite3_value_bytes(arg)).toArray(ValueLayout.JAVA_BYTE); // a copy is made...
			}
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_double</a>
		 */
		public double getDouble(int i) {
			return sqlite3_value_double(arg(i));
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int</a>
		 */
		public int getInt(int i) {
			return sqlite3_value_int(arg(i));
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int64</a>
		 */
		public long getLong(int i) {
			return sqlite3_value_int64(arg(i));
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_text</a>
		 */
		public String getText(int i) {
			return sqlite3_value_text(arg(i));
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_type</a>
		 */
		public int getType(int i) {
			return sqlite3_value_type(arg(i));
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_numeric_type</a>
		 */
		public int getNumericType(int i) {
			return sqlite3_value_numeric_type(arg(i));
		}

		private MemorySegment arg(int i) {
			return args.getAtIndex(ValueLayout.ADDRESS, i);
		}
	}
}
