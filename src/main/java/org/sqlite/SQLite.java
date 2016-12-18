/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import jnr.ffi.LibraryLoader;
import jnr.ffi.LibraryOption;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.Encoding;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class SQLite {
	private static final String LIBRARY_NAME = "sqlite3";
	private static final LibSQLite library;

	static {
		library = LibraryLoader.create(LibSQLite.class).option(LibraryOption.IgnoreError, true).load(LIBRARY_NAME);
	}

	public static final int SQLITE_OK = 0;

	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;

	static final int SQLITE_TRANSIENT = -1;

	static String sqlite3_libversion() { // no copy needed
		return library.sqlite3_libversion();
	}
	static int sqlite3_libversion_number() {
		return library.sqlite3_libversion_number();
	}
	static boolean sqlite3_threadsafe() {
		return library.sqlite3_threadsafe();
	}
	static boolean sqlite3_compileoption_used(String optName) {
		return library.sqlite3_compileoption_used(optName);
	}

	public static final int SQLITE_CONFIG_SINGLETHREAD = 1,
			SQLITE_CONFIG_MULTITHREAD = 2, SQLITE_CONFIG_SERIALIZED = 3,
			SQLITE_CONFIG_MEMSTATUS = 9,
			SQLITE_CONFIG_LOG = 16,
			SQLITE_CONFIG_URI = 17;
	//sqlite3_config(SQLITE_CONFIG_SINGLETHREAD|SQLITE_CONFIG_MULTITHREAD|SQLITE_CONFIG_SERIALIZED)
	static int sqlite3_config(int op) {
		return library.sqlite3_config(op);
	}
	//sqlite3_config(SQLITE_CONFIG_URI, int onoff)
	//sqlite3_config(SQLITE_CONFIG_MEMSTATUS, int onoff)
	static int sqlite3_config(int op, boolean onoff) {
		return library.sqlite3_config(op, onoff);
	}
	//sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
	public static int sqlite3_config(int op, SQLite.LogCallback xLog, Pointer udp) {
		return library.sqlite3_config(op, xLog, udp);
	}
	// Applications can use the sqlite3_log(E,F,..) API to send new messages to the log, if desired, but this is discouraged.
	public static void sqlite3_log(int iErrCode, String msg) {
		library.sqlite3_log(iErrCode, msg);
	}

	static String sqlite3_errmsg(Pointer pDb) { // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
		return library.sqlite3_errmsg(pDb);
	}
	static int sqlite3_errcode(Pointer pDb) {
		return library.sqlite3_errcode(pDb);
	}

	static int sqlite3_extended_result_codes(Pointer pDb, boolean onoff) {
		return library.sqlite3_extended_result_codes(pDb, onoff);
	}
	static int sqlite3_extended_errcode(Pointer pDb) {
		return library.sqlite3_extended_errcode(pDb);
	}

	static int sqlite3_initialize() {
		return library.sqlite3_initialize();
	}
	static int sqlite3_shutdown() {
		return library.sqlite3_shutdown();
	}

	static int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs) { // no copy needed
		return library.sqlite3_open_v2(filename, ppDb, flags, vfs);
	}
	static int sqlite3_close(Pointer pDb) {
		return library.sqlite3_close(pDb);
	}
	static int sqlite3_close_v2(Pointer pDb) { // since 3.7.14
		return library.sqlite3_close_v2(pDb);
	}
	static void sqlite3_interrupt(Pointer pDb) {
		library.sqlite3_interrupt(pDb);
	}

	static int sqlite3_busy_handler(Pointer pDb, BusyHandler bh, Pointer pArg) {
		return library.sqlite3_busy_handler(pDb, bh, pArg);
	}
	static int sqlite3_busy_timeout(Pointer pDb, int ms) {
		return library.sqlite3_busy_timeout(pDb, ms);
	}
	static int sqlite3_db_config(Pointer pDb, int op, int v, IntByReference pOk) {
		return library.sqlite3_db_config(pDb, op, v, pOk);
	}
	//#if mvn.project.property.sqlite.omit.load.extension == "true"
	static int sqlite3_enable_load_extension(Object pDb, boolean onoff) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	static int sqlite3_load_extension(Object pDb, String file, String proc, PointerByReference errMsg) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	//#else
	static int sqlite3_enable_load_extension(Pointer pDb, boolean onoff) {
		return library.sqlite3_enable_load_extension(pDb, onoff);
	}
	static int sqlite3_load_extension(Pointer pDb, String file, String proc, PointerByReference errMsg) {
		return library.sqlite3_load_extension(pDb, file, proc, errMsg);
	}
	//#endif
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
			SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
			SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
			SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
	static int sqlite3_limit(Pointer pDb, int id, int newVal) {
		return library.sqlite3_limit(pDb, id, newVal);
	}
	static boolean sqlite3_get_autocommit(Pointer pDb) {
		return library.sqlite3_get_autocommit(pDb);
	}

	static int sqlite3_changes(Pointer pDb) {
		return library.sqlite3_changes(pDb);
	}
	static int sqlite3_total_changes(Pointer pDb) {
		return library.sqlite3_total_changes(pDb);
	}
	static long sqlite3_last_insert_rowid(Pointer pDb) {
		return library.sqlite3_last_insert_rowid(pDb);
	}

	static String sqlite3_db_filename(Pointer pDb, String dbName) { // no copy needed
		return library.sqlite3_db_filename(pDb, dbName);
	}
	static int sqlite3_db_readonly(Pointer pDb, String dbName) { // no copy needed
		return library.sqlite3_db_readonly(pDb, dbName);
	}

	static Pointer sqlite3_next_stmt(Pointer pDb, Pointer pStmt) {
		return library.sqlite3_next_stmt(pDb, pStmt);
	}

	static int sqlite3_table_column_metadata(Pointer pDb, String dbName, String tableName, String columnName,
																					 PointerByReference pzDataType, PointerByReference pzCollSeq,
																					 IntByReference pNotNull, IntByReference pPrimaryKey, IntByReference pAutoinc) { // no copy needed
		return library.sqlite3_table_column_metadata(pDb, dbName, tableName, columnName, pzDataType, pzCollSeq, pNotNull, pPrimaryKey, pAutoinc);
	}

	static int sqlite3_exec(Pointer pDb, String cmd, Pointer c, Pointer udp, PointerByReference errMsg) {
		return library.sqlite3_exec(pDb, cmd, c, udp, errMsg);
	}

	static int sqlite3_prepare_v2(Pointer pDb, Pointer sql, int nByte, PointerByReference ppStmt,
			PointerByReference pTail) {
		return library.sqlite3_prepare_v2(pDb, sql, nByte, ppStmt, pTail);
	}
	static String sqlite3_sql(Pointer pStmt) { // no copy needed
		return library.sqlite3_sql(pStmt);
	}
	static int sqlite3_finalize(Pointer pStmt) {
		return library.sqlite3_finalize(pStmt);
	}
	static int sqlite3_step(Pointer pStmt) {
		return library.sqlite3_step(pStmt);
	}
	static int sqlite3_reset(Pointer pStmt) {
		return library.sqlite3_reset(pStmt);
	}
	static int sqlite3_clear_bindings(Pointer pStmt) {
		return library.sqlite3_clear_bindings(pStmt);
	}
	static boolean sqlite3_stmt_busy(Pointer pStmt) {
		return library.sqlite3_stmt_busy(pStmt);
	}
	static boolean sqlite3_stmt_readonly(Pointer pStmt) {
		return library.sqlite3_stmt_readonly(pStmt);
	}

	static int sqlite3_column_count(Pointer pStmt) {
		return library.sqlite3_column_count(pStmt);
	}
	static int sqlite3_data_count(Pointer pStmt) {
		return library.sqlite3_data_count(pStmt);
	}
	static int sqlite3_column_type(Pointer pStmt, int iCol) {
		return library.sqlite3_column_type(pStmt, iCol);
	}
	static String sqlite3_column_name(Pointer pStmt, int iCol) { // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
		return library.sqlite3_column_name(pStmt, iCol);
	}
	//#if mvn.project.property.sqlite.enable.column.metadata == "true"
	static String sqlite3_column_origin_name(Pointer pStmt, int iCol) { // copy needed
		return library.sqlite3_column_origin_name(pStmt, iCol);
	}
	static String sqlite3_column_table_name(Pointer pStmt, int iCol) { // copy needed
		return library.sqlite3_column_table_name(pStmt, iCol);
	}
	static String sqlite3_column_database_name(Pointer pStmt, int iCol) { // copy needed
		return library.sqlite3_column_database_name(pStmt, iCol);
	}
	static String sqlite3_column_decltype(Pointer pStmt, int iCol) { // copy needed
		return library.sqlite3_column_decltype(pStmt, iCol);
	}
	//#else
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
	//#endif

	static Pointer sqlite3_column_blob(Pointer pStmt, int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		return library.sqlite3_column_blob(pStmt, iCol);
	}
	static int sqlite3_column_bytes(Pointer pStmt, int iCol) {
		return library.sqlite3_column_bytes(pStmt, iCol);
	}
	static double sqlite3_column_double(Pointer pStmt, int iCol) {
		return library.sqlite3_column_double(pStmt, iCol);
	}
	static int sqlite3_column_int(Pointer pStmt, int iCol) {
		return library.sqlite3_column_int(pStmt, iCol);
	}
	static long sqlite3_column_int64(Pointer pStmt, int iCol) {
		return library.sqlite3_column_int64(pStmt, iCol);
	}
	static String sqlite3_column_text(Pointer pStmt, int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		return library.sqlite3_column_text(pStmt, iCol);
	}
	//const void *sqlite3_column_text16(Pointer pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(Pointer pStmt, int iCol);

	static int sqlite3_bind_parameter_count(Pointer pStmt) {
		return library.sqlite3_bind_parameter_count(pStmt);
	}
	static int sqlite3_bind_parameter_index(Pointer pStmt, String name) { // no copy needed
		return library.sqlite3_bind_parameter_index(pStmt, name);
	}
	static String sqlite3_bind_parameter_name(Pointer pStmt, int i) { // copy needed
		return library.sqlite3_bind_parameter_name(pStmt, i);
	}

	static int sqlite3_bind_blob(Pointer pStmt, int i, byte[] value, int n, long xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		return library.sqlite3_bind_blob(pStmt, i, value, n, xDel);
	}
	static int sqlite3_bind_double(Pointer pStmt, int i, double value) {
		return library.sqlite3_bind_double(pStmt, i, value);
	}
	static int sqlite3_bind_int(Pointer pStmt, int i, int value) {
		return library.sqlite3_bind_int(pStmt, i, value);
	}
	static int sqlite3_bind_int64(Pointer pStmt, int i, long value) {
		return library.sqlite3_bind_int64(pStmt, i, value);
	}
	static int sqlite3_bind_null(Pointer pStmt, int i) {
		return library.sqlite3_bind_null(pStmt, i);
	}
	static int sqlite3_bind_text(Pointer pStmt, int i, String value, int n, long xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		return library.sqlite3_bind_text(pStmt, i, value, n, xDel);
	}
	//static int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
	//static int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
	static int sqlite3_bind_zeroblob(Pointer pStmt, int i, int n) {
		return library.sqlite3_bind_zeroblob(pStmt, i, n);
	}
	static int sqlite3_stmt_status(Pointer pStmt, int op, boolean reset) {
		return library.sqlite3_stmt_status(pStmt, op, reset);
	}
	//#if mvn.project.property.sqlite.enable.stmt.scanstatus == "true"
	static int sqlite3_stmt_scanstatus(Pointer pStmt, int idx, int iScanStatusOp, PointerByReference pOut) {
		return library.sqlite3_stmt_scanstatus(pStmt, idx, iScanStatusOp, pOut);
	}
	static void sqlite3_stmt_scanstatus_reset(Pointer pStmt) {
		library.sqlite3_stmt_scanstatus_reset(pStmt);
	}
	//#endif

	static void sqlite3_free(Pointer p) {
		library.sqlite3_free(p);
	}

	static int sqlite3_blob_open(Pointer pDb, String dbName, String tableName, String columnName,
															 long iRow, boolean flags, PointerByReference ppBlob) { // no copy needed
		return library.sqlite3_blob_open(pDb, dbName, tableName, columnName, iRow, flags, ppBlob);
	}
	static int sqlite3_blob_reopen(Pointer pBlob, long iRow) {
		return library.sqlite3_blob_reopen(pBlob, iRow);
	}
	static int sqlite3_blob_bytes(Pointer pBlob) {
		return library.sqlite3_blob_bytes(pBlob);
	}
	static int sqlite3_blob_read(Pointer pBlob, ByteBuffer z, int n, int iOffset) {
		return library.sqlite3_blob_read(pBlob, z, n, iOffset);
	}
	static int sqlite3_blob_write(Pointer pBlob, ByteBuffer z, int n, int iOffset) {
		return library.sqlite3_blob_write(pBlob, z, n, iOffset);
	}
	static int sqlite3_blob_close(Pointer pBlob) {
		return library.sqlite3_blob_close(pBlob);
	}

	static Pointer sqlite3_backup_init(Pointer pDst, String dstName, Pointer pSrc, String srcName) {
		return library.sqlite3_backup_init(pDst, dstName, pSrc, srcName);
	}
	static int sqlite3_backup_step(Pointer pBackup, int nPage) {
		return library.sqlite3_backup_step(pBackup, nPage);
	}
	static int sqlite3_backup_remaining(Pointer pBackup) {
		return library.sqlite3_backup_remaining(pBackup);
	}
	static int sqlite3_backup_pagecount(Pointer pBackup) {
		return library.sqlite3_backup_pagecount(pBackup);
	}
	static int sqlite3_backup_finish(Pointer pBackup) {
		return library.sqlite3_backup_finish(pBackup);
	}

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	static void sqlite3_progress_handler(Pointer pDb, int nOps, ProgressCallback xProgress, Pointer pArg) {
		library.sqlite3_progress_handler(pDb, nOps, xProgress, pArg);
	}
	static void sqlite3_trace(Pointer pDb, TraceCallback xTrace, Pointer pArg) {
		library.sqlite3_trace(pDb, xTrace, pArg);
	}
	static void sqlite3_profile(Pointer pDb, ProfileCallback xProfile, Pointer pArg) {
		library.sqlite3_profile(pDb, xProfile, pArg);
	}

	// TODO sqlite3_commit_hook, sqlite3_rollback_hook
	static Pointer sqlite3_update_hook(Pointer pDb, UpdateHook xUpdate, Pointer pArg) {
		return library.sqlite3_update_hook(pDb, xUpdate, pArg);
	}
	static int sqlite3_set_authorizer(Pointer pDb, Authorizer authorizer, Pointer pUserData) {
		return library.sqlite3_set_authorizer(pDb, authorizer, pUserData);
	}

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	static int sqlite3_create_function_v2(Pointer pDb, String functionName, int nArg, int eTextRep,
																							 Pointer pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateFinalCallback xFinal, Pointer xDestroy) {
		if (xFunc != null) {
			return library.sqlite3_create_function_v2(pDb, functionName, nArg, eTextRep, pApp, xFunc, null, null, xDestroy);
		} else {
			return library.sqlite3_create_function_v2(pDb, functionName, nArg, eTextRep, pApp, null, xStep, xFinal, xDestroy);
		}
	}
	static void sqlite3_result_null(Pointer pCtx) {
		library.sqlite3_result_null(pCtx);
	}
	static void sqlite3_result_int(Pointer pCtx, int i) {
		library.sqlite3_result_int(pCtx, i);
	}
	static void sqlite3_result_double(Pointer pCtx, double d) {
		library.sqlite3_result_double(pCtx, d);
	}
	static void sqlite3_result_text(Pointer pCtx, String text, int n, long xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
		library.sqlite3_result_text(pCtx, text, n, xDel);
	}
	static void sqlite3_result_blob(Pointer pCtx, byte[] blob, int n, long xDel) {
		library.sqlite3_result_blob(pCtx, blob, n, xDel);
	}
	static void sqlite3_result_int64(Pointer pCtx, long l) {
		library.sqlite3_result_int64(pCtx, l);
	}
	static void sqlite3_result_zeroblob(Pointer pCtx, int n) {
		library.sqlite3_result_zeroblob(pCtx, n);
	}

	static void sqlite3_result_error(Pointer pCtx, String err, int length) {
		library.sqlite3_result_error(pCtx, err, length);
	}
	static void sqlite3_result_error_code(Pointer pCtx, int errCode) {
		library.sqlite3_result_error_code(pCtx, errCode);
	}
	static void sqlite3_result_error_nomem(Pointer pCtx) {
		library.sqlite3_result_error_nomem(pCtx);
	}
	static void sqlite3_result_error_toobig(Pointer pCtx) {
		library.sqlite3_result_error_toobig(pCtx);
	}
	//static void sqlite3_result_subtype(Pointer pCtx, int subtype);

	static Pointer sqlite3_value_blob(Pointer pValue) {
		return library.sqlite3_value_blob(pValue);
	}
	static int sqlite3_value_bytes(Pointer pValue) {
		return library.sqlite3_value_bytes(pValue);
	}
	static double sqlite3_value_double(Pointer pValue) {
		return library.sqlite3_value_double(pValue);
	}
	static int sqlite3_value_int(Pointer pValue) {
		return library.sqlite3_value_int(pValue);
	}
	static long sqlite3_value_int64(Pointer pValue) {
		return library.sqlite3_value_int64(pValue);
	}
	static String sqlite3_value_text(Pointer pValue) {
		return library.sqlite3_value_text(pValue);
	}
	static int sqlite3_value_type(Pointer pValue) {
		return library.sqlite3_value_type(pValue);
	}
	static int sqlite3_value_numeric_type(Pointer pValue) {
		return library.sqlite3_value_numeric_type(pValue);
	}

	static Pointer sqlite3_get_auxdata(SQLite3Context pCtx, int n) {
		return library.sqlite3_get_auxdata(pCtx.pCtx, n);
	}
	static void sqlite3_set_auxdata(SQLite3Context pCtx, int n, Pointer p, Destructor free) {
		library.sqlite3_set_auxdata(pCtx.pCtx, n, p, free);
	}
	static Pointer sqlite3_aggregate_context(Pointer pCtx, int nBytes) {
		return library.sqlite3_aggregate_context(pCtx, nBytes);
	}
	static Pointer sqlite3_context_db_handle(Pointer pCtx) {
		return library.sqlite3_context_db_handle(pCtx);
	}

	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	public static final String UTF_8_ECONDING = UTF_8.name();
	static Pointer nativeString(String sql) {
		final byte[] data = sql.getBytes(UTF_8);
		jnr.ffi.Runtime runtime = jnr.ffi.Runtime.getRuntime(library);
		final Pointer pointer = Memory.allocateDirect(runtime, data.length + 1);
		pointer.put(0L, data, 0, data.length);
		pointer.putByte(data.length, (byte) 0);
		return pointer;
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

	public static String doubleQuote(String dbName) {
		if (dbName == null) {
			return "";
		}
		if ("main".equals(dbName) || "temp".equals(dbName)) {
			return dbName;
		}
		return '"' + escapeIdentifier(dbName) + '"'; // surround identifier with quote
	}
	public static String qualify(String dbName) {
		if (dbName == null) {
			return "";
		}
		if ("main".equals(dbName) || "temp".equals(dbName)) {
			return dbName + '.';
		}
		return '"' + escapeIdentifier(dbName) + '"' + '.'; // surround identifier with quote
	}

	public interface LogCallback {
		@SuppressWarnings("unused")
		@Delegate
		default void callback(Pointer udp, int err,@Encoding("UTF-8") String msg) {
			log(err, msg);
		}
		void log(int err, String msg);
	}

	private static final LogCallback LOG_CALLBACK = new LogCallback() {
		@Override
		public void log(int err, String msg) {
			System.out.printf("%d: %s%n", err, msg);
		}
	};

	static {
		if (!System.getProperty("sqlite.config.log", "").isEmpty()) {
			// DriverManager.getLogWriter();
			final int res = sqlite3_config(SQLITE_CONFIG_LOG, LOG_CALLBACK, null);
			if (res != SQLITE_OK) {
				throw new ExceptionInInitializerError("sqlite3_config(SQLITE_CONFIG_LOG, ...) = " + res);
			}
		}
	}

	public static Runtime getRuntime() {
		return Runtime.getRuntime(library);
	}

	/**
	 * Query Progress Callback.
	 * @see <a href="http://sqlite.org/c3ref/progress_handler.html">sqlite3_progress_handler</a>
	 */
	public interface ProgressCallback {
		/**
		 * @param arg
		 * @return <code>true</code> to interrupt
		 */
		@SuppressWarnings("unused")
		@Delegate
		default boolean callback(Pointer arg) {
			return progress();
		}

		boolean progress();
	}

	public interface LibSQLite {
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_libversion(); // no copy needed
		@IgnoreError
		int sqlite3_libversion_number();
		@IgnoreError
		boolean sqlite3_threadsafe();
		@IgnoreError
		boolean sqlite3_compileoption_used(@In @Encoding("UTF-8")String optName);

		@IgnoreError
		int sqlite3_config(int op);
		@IgnoreError
		int sqlite3_config(int op, boolean onoff);
		@IgnoreError
		int sqlite3_config(int op, @In SQLite.LogCallback xLog, @In Pointer udp);
		@IgnoreError
		void sqlite3_log(int iErrCode, @In @Encoding("UTF-8")String msg);

		@IgnoreError
		@Encoding("UTF-8")String sqlite3_errmsg(@In Pointer pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
		@IgnoreError
		int sqlite3_errcode(@In Pointer pDb);

		@IgnoreError
		int sqlite3_extended_result_codes(@In Pointer pDb, boolean onoff);
		@IgnoreError
		int sqlite3_extended_errcode(@In Pointer pDb);

		@IgnoreError
		int sqlite3_initialize();
		@IgnoreError
		int sqlite3_shutdown();

		@IgnoreError
		int sqlite3_open_v2(@In @Encoding("UTF-8")String filename, @Out PointerByReference ppDb, int flags, @In @Encoding("UTF-8")String vfs); // no copy needed
		@IgnoreError
		int sqlite3_close(@In Pointer pDb);
		@IgnoreError
		int sqlite3_close_v2(@In Pointer pDb);
		@IgnoreError
		void sqlite3_interrupt(@In Pointer pDb);

		@IgnoreError
		int sqlite3_busy_handler(@In Pointer pDb, @In BusyHandler bh, @In Pointer pArg);
		@IgnoreError
		int sqlite3_busy_timeout(@In Pointer pDb, int ms);
		@IgnoreError
		int sqlite3_db_config(@In Pointer pDb, int op, int v, @Out IntByReference pOk);
		@IgnoreError
		int sqlite3_enable_load_extension(@In Pointer pDb, boolean onoff);
		@IgnoreError
		int sqlite3_load_extension(@In Pointer pDb, @In @Encoding("UTF-8")String file, @In @Encoding("UTF-8")String proc, @Out PointerByReference errMsg);
		@IgnoreError
		int sqlite3_limit(@In Pointer pDb, int id, int newVal);
		@IgnoreError
		boolean sqlite3_get_autocommit(@In Pointer pDb);

		@IgnoreError
		int sqlite3_changes(@In Pointer pDb);
		@IgnoreError
		int sqlite3_total_changes(@In Pointer pDb);
		@IgnoreError
		long sqlite3_last_insert_rowid(@In Pointer pDb);

		@IgnoreError
		@Encoding("UTF-8")String sqlite3_db_filename(@In Pointer pDb, @In @Encoding("UTF-8")String dbName); // no copy needed
		@IgnoreError
		int sqlite3_db_readonly(@In Pointer pDb, @In @Encoding("UTF-8")String dbName); // no copy needed

		@IgnoreError
		Pointer sqlite3_next_stmt(@In Pointer pDb, @In Pointer pStmt);

		@IgnoreError
		int sqlite3_table_column_metadata(@In Pointer pDb, @In @Encoding("UTF-8")String dbName, @In @Encoding("UTF-8")String tableName, @In @Encoding("UTF-8")String columnName,
																			@Out PointerByReference pzDataType, @Out PointerByReference pzCollSeq,
																			@Out IntByReference pNotNull, @Out IntByReference pPrimaryKey, @Out IntByReference pAutoinc); // no copy needed
		@IgnoreError
		int sqlite3_exec(@In Pointer pDb, @In @Encoding("UTF-8")String cmd, @In Pointer c, @In Pointer udp, @Out PointerByReference errMsg);

		@IgnoreError
		int sqlite3_prepare_v2(@In Pointer pDb, Pointer sql, int nByte, @Out PointerByReference ppStmt,
													 @Out PointerByReference pTail);
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_sql(@In Pointer pStmt); // no copy needed
		@IgnoreError
		int sqlite3_finalize(@In Pointer pStmt);
		@IgnoreError
		int sqlite3_step(@In Pointer pStmt);
		@IgnoreError
		int sqlite3_reset(@In Pointer pStmt);
		@IgnoreError
		int sqlite3_clear_bindings(@In Pointer pStmt);
		@IgnoreError
		boolean sqlite3_stmt_busy(@In Pointer pStmt);
		@IgnoreError
		boolean sqlite3_stmt_readonly(@In Pointer pStmt);

		@IgnoreError
		int sqlite3_column_count(@In Pointer pStmt);
		@IgnoreError
		int sqlite3_data_count(@In Pointer pStmt);
		@IgnoreError
		int sqlite3_column_type(@In Pointer pStmt, int iCol);
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_column_name(@In Pointer pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_column_origin_name(@In Pointer pStmt, int iCol); // copy needed
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_column_table_name(@In Pointer pStmt, int iCol); // copy needed
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_column_database_name(@In Pointer pStmt, int iCol); // copy needed
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_column_decltype(@In Pointer pStmt, int iCol); // copy needed

		@IgnoreError
		Pointer sqlite3_column_blob(@In Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		@IgnoreError
		int sqlite3_column_bytes(@In Pointer pStmt, int iCol);
		@IgnoreError
		double sqlite3_column_double(@In Pointer pStmt, int iCol);
		@IgnoreError
		int sqlite3_column_int(@In Pointer pStmt, int iCol);
		@IgnoreError
		long sqlite3_column_int64(@In Pointer pStmt, int iCol);
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_column_text(@In Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
		//const void *sqlite3_column_text16(Pointer pStmt, int iCol);
		//sqlite3_value *sqlite3_column_value(Pointer pStmt, int iCol);

		@IgnoreError
		int sqlite3_bind_parameter_count(@In Pointer pStmt);
		@IgnoreError
		int sqlite3_bind_parameter_index(@In Pointer pStmt, @Encoding("UTF-8")String name); // no copy needed
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_bind_parameter_name(@In Pointer pStmt, int i); // copy needed

		@IgnoreError
		int sqlite3_bind_blob(@In Pointer pStmt, int i, @In byte[] value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
		@IgnoreError
		int sqlite3_bind_double(@In Pointer pStmt, int i, double value);
		@IgnoreError
		int sqlite3_bind_int(@In Pointer pStmt, int i, int value);
		@IgnoreError
		int sqlite3_bind_int64(@In Pointer pStmt, int i, long value);
		@IgnoreError
		int sqlite3_bind_null(@In Pointer pStmt, int i);
		@IgnoreError
		int sqlite3_bind_text(@In Pointer pStmt, int i, @In @Encoding("UTF-8")String value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
		//int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
		//int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
		@IgnoreError
		int sqlite3_bind_zeroblob(@In Pointer pStmt, int i, int n);
		@IgnoreError
		int sqlite3_stmt_status(@In Pointer pStmt, int op, boolean reset);

		@IgnoreError
		int sqlite3_stmt_scanstatus(@In Pointer pStmt, int idx, int iScanStatusOp, @Out PointerByReference pOut);
		@IgnoreError
		void sqlite3_stmt_scanstatus_reset(@In Pointer pStmt);

		@IgnoreError
		void sqlite3_free(@In Pointer p);

		@IgnoreError
		int sqlite3_blob_open(@In Pointer pDb, @In @Encoding("UTF-8")String dbName, @In @Encoding("UTF-8")String tableName, @In @Encoding("UTF-8")String columnName,
													long iRow, boolean flags, @Out PointerByReference ppBlob); // no copy needed
		@IgnoreError
		int sqlite3_blob_reopen(@In Pointer pBlob, long iRow);
		@IgnoreError
		int sqlite3_blob_bytes(@In Pointer pBlob);
		@IgnoreError
		int sqlite3_blob_read(@In Pointer pBlob, @Out ByteBuffer z, int n, int iOffset);
		@IgnoreError
		int sqlite3_blob_write(@In Pointer pBlob, @In ByteBuffer z, int n, int iOffset);
		@IgnoreError
		int sqlite3_blob_close(@In Pointer pBlob);

		@IgnoreError
		Pointer sqlite3_backup_init(@In Pointer pDst, @In @Encoding("UTF-8")String dstName, @In Pointer pSrc, @In @Encoding("UTF-8")String srcName);
		@IgnoreError
		int sqlite3_backup_step(@In Pointer pBackup, int nPage);
		@IgnoreError
		int sqlite3_backup_remaining(@In Pointer pBackup);
		@IgnoreError
		int sqlite3_backup_pagecount(@In Pointer pBackup);
		@IgnoreError
		int sqlite3_backup_finish(@In Pointer pBackup);

		@IgnoreError
		void sqlite3_progress_handler(@In Pointer pDb, int nOps, @In SQLite.ProgressCallback xProgress, @In Pointer pArg);
		@IgnoreError
		void sqlite3_trace(@In Pointer pDb, @In TraceCallback xTrace, @In Pointer pArg);
		@IgnoreError
		void sqlite3_profile(@In Pointer pDb, @In ProfileCallback xProfile, @In Pointer pArg);

		@IgnoreError
		Pointer sqlite3_update_hook(@In Pointer pDb, @In UpdateHook xUpdate, @In Pointer pArg);
		@IgnoreError
		int sqlite3_set_authorizer(@In Pointer pDb, @In Authorizer authorizer, @In Pointer pUserData);

		// https://github.com/jnr/jnr-ffi/issues/61
		@IgnoreError
		int sqlite3_create_function_v2(@In Pointer pDb, @In @Encoding("UTF-8")String functionName, int nArg, int eTextRep,
																	 @In Pointer pApp, @In ScalarCallback xFunc, @In Pointer xStep, @In Pointer xFinal, @In Pointer xDestroy);
		@IgnoreError
		int sqlite3_create_function_v2(@In Pointer pDb, @In @Encoding("UTF-8")String functionName, int nArg, int eTextRep,
				@In Pointer pApp, @In Pointer xFunc, @In AggregateStepCallback xStep, @In AggregateFinalCallback xFinal, @In Pointer xDestroy);
		@IgnoreError
		void sqlite3_result_null(@In Pointer pCtx);
		@IgnoreError
		void sqlite3_result_int(@In Pointer pCtx, int i);
		@IgnoreError
		void sqlite3_result_double(@In Pointer pCtx, double d);
		@IgnoreError
		void sqlite3_result_text(@In Pointer pCtx, @In @Encoding("UTF-8")String text, int n, long xDel);
		@IgnoreError
		void sqlite3_result_blob(@In Pointer pCtx, @In byte[] blob, int n, long xDel);
		@IgnoreError
		void sqlite3_result_int64(@In Pointer pCtx, long l);
		@IgnoreError
		void sqlite3_result_zeroblob(@In Pointer pCtx, int n);
		@IgnoreError
		void sqlite3_result_error(@In Pointer pCtx, @In @Encoding("UTF-8")String err, int length);
		@IgnoreError
		void sqlite3_result_error_code(@In Pointer pCtx, int errCode);
		@IgnoreError
		void sqlite3_result_error_nomem(@In Pointer pCtx);
		@IgnoreError
		void sqlite3_result_error_toobig(@In Pointer pCtx);

		@IgnoreError
		Pointer sqlite3_value_blob(@In Pointer pValue);
		@IgnoreError
		int sqlite3_value_bytes(@In Pointer pValue);
		@IgnoreError
		double sqlite3_value_double(@In Pointer pValue);
		@IgnoreError
		int sqlite3_value_int(@In Pointer pValue);
		@IgnoreError
		long sqlite3_value_int64(@In Pointer pValue);
		@IgnoreError
		@Encoding("UTF-8")String sqlite3_value_text(@In Pointer pValue);
		@IgnoreError
		int sqlite3_value_type(@In Pointer pValue);
		@IgnoreError
		int sqlite3_value_numeric_type(@In Pointer pValue);

		@IgnoreError
		Pointer sqlite3_get_auxdata(@In Pointer pCtx, int n);
		@IgnoreError
		void sqlite3_set_auxdata(@In Pointer pCtx, int n, @In Pointer p, @In Destructor free);
		@IgnoreError
		Pointer sqlite3_aggregate_context(@In Pointer pCtx, int nBytes);
		@IgnoreError
		Pointer sqlite3_context_db_handle(@In Pointer pCtx);
	}

	/**
	 * SQL function context object
	 * @see <a href="http://sqlite.org/c3ref/context.html">sqlite3_context</a>
	 */
	public static class SQLite3Context {
		private final Pointer pCtx;

		public SQLite3Context(Pointer p) {
			this.pCtx = p;
		}

		/**
		 * @return a copy of the pointer to the database connection (the 1st parameter) of
		 * {@link SQLite#sqlite3_create_function_v2(Pointer, String, int, int, Pointer, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, Pointer)}
		 * @see <a href="http://sqlite.org/c3ref/context_db_handle.html">sqlite3_context_db_handle</a>
		 */
		public Pointer getDbHandle() {
			return sqlite3_context_db_handle(pCtx);
		}

		/**
		 * Sets the return value of the application-defined function to be the BLOB value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_blob</a>
		 */
		public void setResultBlob(byte[] result) {
			sqlite3_result_blob(pCtx, result, result.length, SQLITE_TRANSIENT);
		}
		/**
		 * Sets the return value of the application-defined function to be the floating point value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_double</a>
		 */
		public void setResultDouble(double result) {
			sqlite3_result_double(pCtx, result);
		}
		/**
		 * Sets the return value of the application-defined function to be the 32-bit signed integer value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int</a>
		 */
		public void setResultInt(int result) {
			sqlite3_result_int(pCtx, result);
		}
		/**
		 * Sets the return value of the application-defined function to be the 64-bit signed integer value given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int64</a>
		 */
		public void setResultLong(long result) {
			sqlite3_result_int64(pCtx, result);
		}
		/**
		 * Sets the return value of the application-defined function to be NULL.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_null</a>
		 */
		public void setResultNull() {
			sqlite3_result_null(pCtx);
		}
		/**
		 * Sets the return value of the application-defined function to be the text string given.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_text</a>
		 */
		public void setResultText(String result) {
			sqlite3_result_text(pCtx, result, -1, SQLITE_TRANSIENT);
		}
		/**
		 * Sets the return value of the application-defined function to be a BLOB containing all zero bytes and N bytes in size.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_zeroblob</a>
		 */
		public void setResultZeroBlob(ZeroBlob result) {
			sqlite3_result_zeroblob(pCtx, result.n);
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
			sqlite3_result_error(pCtx, errMsg, -1);
		}
		/**
		 * Causes the implemented SQL function to throw an exception.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_code</a>
		 */
		public void setResultErrorCode(int errCode) {
			sqlite3_result_error_code(pCtx, errCode);
		}
		/**
		 * Causes SQLite to throw an error indicating that a memory allocation failed.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_nomem</a>
		 */
		public void setResultErrorNoMem() {
			sqlite3_result_error_nomem(pCtx);
		}
		/**
		 * Causes SQLite to throw an error indicating that a string or BLOB is too long to represent.
		 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_toobig</a>
		 */
		public void setResultErrorTooBig() {
			sqlite3_result_error_toobig(pCtx);
		}
	}

	/**
	 * Dynamically typed value objects
	 * @see <a href="http://sqlite.org/c3ref/value.html">sqlite3_value</a>
	 */
	public static class SQLite3Values {
		private static final SQLite3Values NO_ARG = new SQLite3Values(new Pointer[0]);
		private final Pointer[] args;

		public static SQLite3Values build(int nArg, Pointer args) {
			if (nArg == 0) {
				return NO_ARG;
			}
			Pointer[] dst = new Pointer[nArg];
			args.get(0, dst, 0, nArg);
			return new SQLite3Values(dst);
		}

		private SQLite3Values(Pointer[] args) {
			this.args = args;
		}

		/**
		 * @return arg count
		 */
		public int getCount() {
			return args.length;
		}

		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_blob</a>
		 */
		public byte[] getBlob(int i) {
			Pointer arg = args[i];
			Pointer blob = sqlite3_value_blob(arg);
			if (blob == null) {
				return null;
			} else {
				final byte[] bytes = new byte[sqlite3_value_bytes(arg)];
				blob.get(0L, bytes, 0, bytes.length); // a copy is made...
				return bytes;
			}
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_double</a>
		 */
		public double getDouble(int i) {
			return sqlite3_value_double(args[i]);
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int</a>
		 */
		public int getInt(int i) {
			return sqlite3_value_int(args[i]);
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int64</a>
		 */
		public long getLong(int i) {
			return sqlite3_value_int64(args[i]);
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_text</a>
		 */
		public String getText(int i) {
			return sqlite3_value_text(args[i]);
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_type</a>
		 */
		public int getType(int i) {
			return sqlite3_value_type(args[i]);
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_numeric_type</a>
		 */
		public int getNumericType(int i) {
			return sqlite3_value_numeric_type(args[i]);
		}
	}
}
