/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLFeatureNotSupportedException;
import java.nio.file.Path;

import static org.sqlite.SQLite3Stmt.*;

// TODO JNA/Bridj/JNR/JNI and native libs embedded in JAR.
public final class SQLite {
	private static final Logger log = LoggerFactory.getLogger(SQLite.class);
	private static final String JNA_LIBRARY_NAME = "sqlite3";

	static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
	static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
	static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
	static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
	static final AddressLayout C_POINTER = ValueLayout.ADDRESS;

	public static final FunctionDescriptor IP = FunctionDescriptor.of(C_INT, C_POINTER);
	static final FunctionDescriptor IPI = FunctionDescriptor.of(C_INT, C_POINTER, C_INT);
	static final FunctionDescriptor IPII = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT);
	public static final FunctionDescriptor IPIPIP = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_INT, C_POINTER);
	public static final FunctionDescriptor IPP = FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER);
	static final FunctionDescriptor LP = FunctionDescriptor.of(C_LONG_LONG, C_POINTER);
	static final FunctionDescriptor PP = FunctionDescriptor.of(C_POINTER, C_POINTER);
	static final FunctionDescriptor PPI = FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT);
	static final FunctionDescriptor VP = FunctionDescriptor.ofVoid(C_POINTER);
	static final FunctionDescriptor VPI = FunctionDescriptor.ofVoid(C_POINTER, C_INT);
	private static final FunctionDescriptor VPIP = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER);

	private static final Arena LIBRARY_ARENA = Arena.ofAuto();
	private static final SymbolLookup SYMBOL_LOOKUP = System.getProperty("sqlite3.library.path", "").isEmpty() ?
		SymbolLookup.libraryLookup(System.mapLibraryName(System.getProperty("sqlite3.library.name", JNA_LIBRARY_NAME)), LIBRARY_ARENA)
		:
		SymbolLookup.libraryLookup(Path.of(System.getProperty("sqlite3.library.path")), LIBRARY_ARENA);

	public static final int SQLITE_OK = 0;

	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;

	static final MemorySegment SQLITE_TRANSIENT = MemorySegment.ofAddress(-1L);
	static final MemorySegment SQLITE_STATIC = MemorySegment.ofAddress(0L);
	static final Cleaner cleaner = Cleaner.create();
	static final Runnable NO_OP = () -> {};

	private static MemorySegment findOrThrow(String symbol) {
		return SYMBOL_LOOKUP.find(symbol)
			.orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
	}
	private static final Linker LINKER = Linker.nativeLinker();
	static final Linker.Option CRITICAL = Linker.Option.critical(true);
	static MethodHandle downcallHandle(String symbol, FunctionDescriptor desc,
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
		if (isNull(ms)) {
			return null;
		}
		return ms.reinterpret(Integer.MAX_VALUE).getString(0);
	}

	static boolean isNull(MemorySegment ms) {
		return MemorySegment.NULL.equals(ms);
	}

	static void checkActivated(MethodHandle mh, String msg) {
		if (mh == null) {
			throw new UnsupportedOperationException(msg);
		}
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
	private static int SQLITE_VERSION_NUMBER;
	static int sqlite3_libversion_number() {
		if (SQLITE_VERSION_NUMBER > 0) {
			log.debug("Using SQLite version {}", SQLITE_VERSION_NUMBER);
			return SQLITE_VERSION_NUMBER;
		}
		try {
			SQLITE_VERSION_NUMBER = (int) sqlite3_libversion_number.invokeExact();
			return SQLITE_VERSION_NUMBER;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	public static boolean versionAtLeast(int min) {
		return sqlite3_libversion_number() >= min;
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
		"sqlite3_compileoption_used", IP);
	public static boolean sqlite3_compileoption_used(String optName) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment ms = arena.allocateFrom(optName);
			return ((int) sqlite3_compileoption_used.invokeExact(ms)) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	public static final boolean ENABLE_UNLOCK_NOTIFY = sqlite3_compileoption_used("ENABLE_UNLOCK_NOTIFY");
	public static final boolean OMIT_LOAD_EXTENSION = sqlite3_compileoption_used("OMIT_LOAD_EXTENSION");
	public static final boolean OMIT_TRACE = sqlite3_compileoption_used("OMIT_TRACE");
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

	//sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
	public static int sqlite3_config(int op, Class<?> clz, String name, MemorySegment udp) {
		try {
			MethodHandle xLogHandle = MH_LOOKUP.findStatic(clz, name, VPIP.toMethodType());
			MemorySegment xLog = LINKER.upcallStub(xLogHandle, VPIP, Arena.global());
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
		"sqlite3_errmsg", PP);
	static String sqlite3_errmsg(SQLite3 pDb) { // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
		try {
			return getString((MemorySegment) sqlite3_errmsg.invokeExact(pDb.getPointer()));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_errcode = downcallHandle(
		"sqlite3_errcode", IP);
	static int sqlite3_errcode(SQLite3 pDb) {
		try {
			return (int) sqlite3_errcode.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_extended_result_codes = downcallHandle(
		"sqlite3_extended_result_codes", IPI);
	static int sqlite3_extended_result_codes(SQLite3 pDb, boolean onoff) {
		try {
			return (int) sqlite3_extended_result_codes.invokeExact(pDb.getPointer(), onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_extended_errcode = downcallHandle(
		"sqlite3_extended_errcode", IP);
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
		"sqlite3_close", IP);
	static int sqlite3_close(SQLite3 pDb) {
		try {
			return (int) sqlite3_close.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_close_v2 = downcallHandle(
		"sqlite3_close_v2", IP);
	private static int sqlite3_close_v2(SQLite3 pDb) { // since 3.7.14
		try {
			return (int) sqlite3_close_v2.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_interrupt = downcallHandle(
		"sqlite3_interrupt", VP);
	static void sqlite3_interrupt(SQLite3 pDb) {
		try {
			sqlite3_interrupt.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_busy_handler = downcallHandle(
		"sqlite3_busy_handler", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final MethodHandle busy_handler = upcallHandle(BusyHandler.class, "callback",
		IPI);
	static int sqlite3_busy_handler(SQLite3 pDb, BusyHandler bh, MemorySegment pArg) {
		try {
			// FIXME previous busyHandler will not be freed until pDb is closed & gced
			MemorySegment busyHandler = upcallStub(busy_handler, bh, IPI, pDb.getArena());
			return (int) sqlite3_busy_handler.invokeExact(pDb.getPointer(), busyHandler, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_busy_timeout = downcallHandle(
		"sqlite3_busy_timeout", IPI);
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

	private static final MethodHandle sqlite3_enable_load_extension = OMIT_LOAD_EXTENSION ? null : downcallHandle(
	"sqlite3_enable_load_extension", IPI);
	static int sqlite3_enable_load_extension(SQLite3 pDb, boolean onoff) {
		checkActivated(sqlite3_enable_load_extension, "SQLITE_OMIT_LOAD_EXTENSION activated");
		try {
			return (int) sqlite3_enable_load_extension.invokeExact(pDb.getPointer(), onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_load_extension = OMIT_LOAD_EXTENSION ? null : downcallHandle(
		"sqlite3_load_extension", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_load_extension(SQLite3 pDb, String file, String proc, MemorySegment errMsg) {
		checkActivated(sqlite3_load_extension, "SQLITE_OMIT_LOAD_EXTENSION activated");
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_load_extension.invokeExact(pDb.getPointer(), nativeString(arena, file), nativeString(arena, proc), errMsg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	// https://sqlite.org/c3ref/c_limit_attached.html
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
			SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
			SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
			SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10, SQLITE_LIMIT_WORKER_THREADS = 11;
	private static final MethodHandle sqlite3_limit = downcallHandle(
		"sqlite3_limit", IPII);
	static int sqlite3_limit(SQLite3 pDb, int id, int newVal) {
		try {
			return (int) sqlite3_limit.invokeExact(pDb.getPointer(), id, newVal);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_get_autocommit = downcallHandle(
		"sqlite3_get_autocommit", IP);
	static boolean sqlite3_get_autocommit(SQLite3 pDb) {
		try {
			return ((int) sqlite3_get_autocommit.invokeExact(pDb.getPointer())) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_changes = downcallHandle(
		"sqlite3_changes", IP);
	static int sqlite3_changes(SQLite3 pDb) {
		try {
			return (int) sqlite3_changes.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_changes64 = versionAtLeast(3037000) ? downcallHandle(
	"sqlite3_changes64", LP) : null;
	static long sqlite3_changes64(SQLite3 pDb) throws SQLFeatureNotSupportedException { // 3.37.0
		if (sqlite3_changes64 == null) {
			throw new SQLFeatureNotSupportedException("LargeUpdate not implemented for " + sqlite3_libversion());
		}
		try {
			return (long) sqlite3_changes64.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_total_changes = downcallHandle(
	"sqlite3_total_changes", IP);
	static int sqlite3_total_changes(SQLite3 pDb) {
		try {
			return (int) sqlite3_total_changes.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_total_changes64 = versionAtLeast(3037000) ? downcallHandle(
	"sqlite3_total_changes64", LP) : null;
	static long sqlite3_total_changes64(SQLite3 pDb) throws SQLFeatureNotSupportedException { // 3.37.0
		if (sqlite3_total_changes64 == null) {
			throw new SQLFeatureNotSupportedException("LargeUpdate not implemented for " + sqlite3_libversion());
		}
		try {
			return (long) sqlite3_total_changes64.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_last_insert_rowid = downcallHandle(
	"sqlite3_last_insert_rowid", LP);
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
		"sqlite3_db_readonly", IPP);
	static int sqlite3_db_readonly(SQLite3 pDb, String dbName) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_db_readonly.invokeExact(pDb.getPointer(), nativeString(arena, dbName));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_next_stmt = downcallHandle(
		"sqlite3_next_stmt", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER));
	private static MemorySegment sqlite3_next_stmt(SQLite3 pDb, MemorySegment stmt) {
		try {
			return (MemorySegment) sqlite3_next_stmt.invokeExact(pDb.getPointer(), stmt);
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

	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	private static final MethodHandle sqlite3_free = downcallHandle(
		"sqlite3_free", VP);
	static void sqlite3_free(MemorySegment p) {
		try {
			sqlite3_free.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	static final MethodHandle sqlite3_malloc = downcallHandle(
		"sqlite3_malloc", FunctionDescriptor.of(C_POINTER, C_INT));
	static MemorySegment sqlite3_malloc(int n) {
		try {
			return (MemorySegment) sqlite3_malloc.invokeExact(n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	private static final MethodHandle sqlite3_progress_handler = sqlite3_compileoption_used("OMIT_PROGRESS_CALLBACK") ? null :
		downcallHandle("sqlite3_progress_handler", FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER));
	private static final MethodHandle progress_callback = upcallHandle(ProgressCallback.class, "callback",
		IP);
	static void sqlite3_progress_handler(SQLite3 pDb, int nOps, ProgressCallback xProgress, MemorySegment pArg) {
		checkActivated(sqlite3_progress_handler, "SQLITE_OMIT_PROGRESS_CALLBACK activated");
		try {
			// FIXME previous pc will not be freed until pDb is closed & gced
			MemorySegment pc = upcallStub(progress_callback, xProgress, IP, pDb.getArena());
			sqlite3_progress_handler.invokeExact(pDb.getPointer(), nOps, pc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_trace = OMIT_TRACE ? null : downcallHandle(
		"sqlite3_trace", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor trace_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_POINTER);
	private static final MethodHandle trace_callback = upcallHandle(TraceCallback.class, "callback",
		trace_callback_desc);
	static void sqlite3_trace(SQLite3 pDb, TraceCallback xTrace, MemorySegment pArg) {
		checkActivated(sqlite3_trace, "SQLITE_OMIT_TRACE activated");
		try {
			// FIXME previous tc will not be freed until pDb is closed & gced
			MemorySegment tc = upcallStub(trace_callback, xTrace, trace_callback_desc, pDb.getArena());
			sqlite3_trace.invokeExact(pDb.getPointer(), tc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_profile = OMIT_TRACE ? null : downcallHandle(
		"sqlite3_profile", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor profile_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_LONG_LONG);
	private static final MethodHandle profile_callback = upcallHandle(ProfileCallback.class, "callback",
		profile_callback_desc);
	static void sqlite3_profile(SQLite3 pDb, ProfileCallback xProfile, MemorySegment pArg) {
		checkActivated(sqlite3_profile, "SQLITE_OMIT_TRACE activated");
		try {
			// FIXME previous pc will not be freed until pDb is closed & gced
			MemorySegment pc = upcallStub(profile_callback, xProfile, profile_callback_desc, pDb.getArena());
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
			// FIXME previous uh will not be freed until pDb is closed & gced
			MemorySegment uh = upcallStub(update_hook, xUpdate, update_hook_desc, pDb.getArena());
			return (MemorySegment)sqlite3_update_hook.invokeExact(pDb.getPointer(), uh, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_set_authorizer = sqlite3_compileoption_used("OMIT_AUTHORIZATION") ? null :
		downcallHandle("sqlite3_set_authorizer", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor authorizer_desc = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER);
	private static final MethodHandle authorizer_up = upcallHandle(Authorizer.class, "callback",
		authorizer_desc);
	static int sqlite3_set_authorizer(SQLite3 pDb, Authorizer authorizer, MemorySegment pUserData) {
		checkActivated(sqlite3_set_authorizer, "SQLITE_OMIT_AUTHORIZATION activated");
		try {
			// FIXME previous auth will not be freed until pDb is closed & gced
			MemorySegment auth = upcallStub(authorizer_up, authorizer, authorizer_desc, pDb.getArena());
			return (int)sqlite3_set_authorizer.invokeExact(pDb.getPointer(), auth, pUserData);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_unlock_notify = ENABLE_UNLOCK_NOTIFY ? downcallHandle(
	"sqlite3_unlock_notify", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER)) : null;
	private static final MethodHandle unlock_notify_up = upcallHandle(UnlockNotifyCallback.class, "callback",
		VPI);
	static int sqlite3_unlock_notify(SQLite3 pBlocked, UnlockNotifyCallback xNotify, MemorySegment pNotifyArg) {
		checkActivated(sqlite3_unlock_notify, "SQLITE_ENABLE_UNLOCK_NOTIFY not activated");
		try {
			// FIXME previous unc will not be freed until pDb is closed & gced
			MemorySegment unc = upcallStub(unlock_notify_up, xNotify, VPI, pBlocked.getArena());
			return (int)sqlite3_unlock_notify.invokeExact(pBlocked.getPointer(), unc, pNotifyArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	private static final MethodHandle sqlite3_create_function_v2 = downcallHandle(
		"sqlite3_create_function_v2", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	private static final MethodHandle scalar_callback = upcallHandle(ScalarCallback.class, "callback",
		VPIP);
	private static final MethodHandle aggregate_step_callback = upcallHandle(AggregateStepCallback.class, "callback",
		VPIP);
	private static final MethodHandle aggregate_final_callback = upcallHandle(AggregateFinalCallback.class, "callback",
		VP);
	static int sqlite3_create_function_v2(SQLite3 pDb, String functionName, int nArg, int eTextRep,
		MemorySegment pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateFinalCallback xFinal, MemorySegment xDestroy) {
		try (Arena arena = Arena.ofConfined()) {
			final MemorySegment xFu = upcallStub(scalar_callback, xFunc, VPIP, pDb.getArena());
			final MemorySegment xS = upcallStub(aggregate_step_callback, xStep, VPIP, pDb.getArena());
			final MemorySegment xFi = upcallStub(aggregate_final_callback, xFinal, VP, pDb.getArena());
			return (int)sqlite3_create_function_v2.invokeExact(pDb.getPointer(), nativeString(arena, functionName), nArg, eTextRep, pApp, xFu, xS, xFi, xDestroy);
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
		if (err == ErrCodes.WRAPPER_SPECIFIC) {
			log.error("{}: {}", err, getString(msg));
		} else if (err == ErrCodes.SQLITE_OK) {
			log.info("{}: {}", err, getString(msg));
		} else if ((err & ErrCodes.SQLITE_WARNING) == ErrCodes.SQLITE_WARNING) {
			log.warn("{}: {}", err, getString(msg));
		} else {
			log.error("{}: {}", err, getString(msg));
		}
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
	public static final class SQLite3 {
		private MemorySegment p;
		int res;
		// To avoid upcallStub(s) from being GCed before connection is closed
		private Arena arena;
		// Make sure a stmt is not finalized while current conn is being closed
		final Object lock = new Object();
		SQLite3(MemorySegment p) {
			this.p = p;
		}
		MemorySegment getPointer() {
			return p;
		}
		void close() {
			synchronized (lock) {
				if (isNull(p)) {
					return;
				}
				// Dangling statements
				MemorySegment stmt = sqlite3_next_stmt(this, MemorySegment.NULL);
				while (!isNull(stmt)) {
					if (sqlite3_stmt_busy(stmt)) {
						sqlite3_log(ErrCodes.SQLITE_MISUSE, "Dangling statement (not reset): \"" + sqlite3_sql(stmt) + "\"");
					} else {
						sqlite3_log(ErrCodes.SQLITE_MISUSE, "Dangling statement (not finalize): \"" + sqlite3_sql(stmt) + "\"");
					}
					stmt = sqlite3_next_stmt(this, stmt);
				}
				res = sqlite3_close_v2(this); // must be called only once
				p = MemorySegment.NULL;
			}
		}
		boolean isClosed() {
			return isNull(p);
		}
		private Arena getArena() {
			if (arena == null) { // lazy loading
				arena = Arena.ofAuto();
			}
			return arena;
		}
	}

}
