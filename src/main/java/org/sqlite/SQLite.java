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
import java.nio.file.Path;

// TODO JNA/Bridj/JNR/JNI and native libs embedded in JAR.
public final class SQLite {
	private static final Logger log = LoggerFactory.getLogger(SQLite.class);
	private static final String JNA_LIBRARY_NAME = "sqlite3";

	static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
	static final ValueLayout.OfDouble C_DOUBLE = ValueLayout.JAVA_DOUBLE;
	static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;
	static final ValueLayout.OfLong C_LONG_LONG = ValueLayout.JAVA_LONG;
	static final AddressLayout C_POINTER = ValueLayout.ADDRESS;

	static final FunctionDescriptor IP = FunctionDescriptor.of(C_INT, C_POINTER);
	static final FunctionDescriptor IPI = FunctionDescriptor.of(C_INT, C_POINTER, C_INT);
	static final FunctionDescriptor IPII = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT);
	static final FunctionDescriptor IPIPIP = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_INT, C_POINTER);
	static final FunctionDescriptor IPP = FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER);
	static final FunctionDescriptor LP = FunctionDescriptor.of(C_LONG_LONG, C_POINTER);
	static final FunctionDescriptor PP = FunctionDescriptor.of(C_POINTER, C_POINTER);
	static final FunctionDescriptor PPP = FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER);
	static final FunctionDescriptor PPI = FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT);
	static final FunctionDescriptor VP = FunctionDescriptor.ofVoid(C_POINTER);
	static final FunctionDescriptor VPI = FunctionDescriptor.ofVoid(C_POINTER, C_INT);
	static final FunctionDescriptor VPIP = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER);

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
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
	static MemorySegment upcallStub(MethodHandle mh, Object x, FunctionDescriptor fd, Arena arena) {
		if (x == null) {
			return MemorySegment.NULL;
		}
		return LINKER.upcallStub(mh.bindTo(x), fd, arena);
	}
	static MemorySegment upcallStub(Class<?> clz, String name, FunctionDescriptor fd) {
		try {
			MethodHandle handle = MH_LOOKUP.findStatic(clz, name, fd.toMethodType());
			return LINKER.upcallStub(handle, fd, Arena.global());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
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
			MemorySegment xLog = upcallStub(clz, name, VPIP);
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

	// https://sqlite.org/c3ref/c_limit_attached.html
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
		SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
		SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
		SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10, SQLITE_LIMIT_WORKER_THREADS = 11;

	private static final MethodHandle sqlite3_free = downcallHandle(
		"sqlite3_free", VP);
	static final MemorySegment xFree = findOrThrow("sqlite3_free");
	public static void sqlite3_free(MemorySegment p) {
		try {
			sqlite3_free.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	static final MethodHandle sqlite3_malloc = downcallHandle(
		"sqlite3_malloc", FunctionDescriptor.of(C_POINTER, C_INT));
	public static MemorySegment sqlite3_malloc(MemoryLayout ml) {
		return sqlite3_malloc(ml.byteSize());
	}
	public static MemorySegment sqlite3_malloc(long l) {
		try {
			MemorySegment ms = (MemorySegment) sqlite3_malloc.invokeExact(Math.toIntExact(l));
			if (ms != null) {
				ms = ms.reinterpret(l);
				memset(ms, 0, l);
			}
			return ms;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle memset = LINKER.downcallHandle(LINKER.defaultLookup()
		.find("memset").orElseThrow(), FunctionDescriptor.of(C_POINTER, C_POINTER, C_INT, C_LONG_LONG));
	private static MemorySegment memset(MemorySegment ms, int c, long n) {
		try {
			return (MemorySegment) memset.invokeExact(ms, c, n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	public static final String UTF_8_ECONDING = UTF_8.name();
	static MemorySegment nativeString(Arena arena, String s) {
		if (s == null) {
			return MemorySegment.NULL;
		}
		return arena.allocateFrom(s);
	}
	static MemorySegment sqlite3OwnedString(String s) {
		if (s == null) {
			return MemorySegment.NULL;
		}
		byte[] bytes = s.getBytes(UTF_8);
		MemorySegment ms = sqlite3_malloc(bytes.length + 1);
		if (isNull(ms)) {
			return ms;
		}
		MemorySegment.copy(bytes, 0, ms, C_CHAR, 0, bytes.length);
		ms.set(C_CHAR, bytes.length, (byte)0);
		return ms;
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

	@SuppressWarnings("unused")
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
			int res = sqlite3_config(SQLITE_CONFIG_LOG, SQLite.class, "log_callback", MemorySegment.NULL);
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
}
