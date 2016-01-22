/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// TODO JNA/Bridj/JNR/JNI and native libs embedded in JAR.
public final class SQLite implements Library {
	private static final String JNA_LIBRARY_NAME = "sqlite3";

	// public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(SQLite.JNA_LIBRARY_NAME);
	static {
		Native.register(JNA_LIBRARY_NAME);
	}

	public static final int SQLITE_OK = 0;

	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;

	static final int SQLITE_TRANSIENT = -1;

	static native String sqlite3_libversion(); // no copy needed
	static native int sqlite3_libversion_number();
	static native boolean sqlite3_threadsafe();
	static native boolean sqlite3_compileoption_used(String optName);
	static native String sqlite3_compileoption_get(int n);

	public static final int SQLITE_CONFIG_SINGLETHREAD = 1,
			SQLITE_CONFIG_MULTITHREAD = 2, SQLITE_CONFIG_SERIALIZED = 3,
			SQLITE_CONFIG_MEMSTATUS = 9,
			SQLITE_CONFIG_LOG = 16,
			SQLITE_CONFIG_URI = 17;
	//sqlite3_config(SQLITE_CONFIG_SINGLETHREAD|SQLITE_CONFIG_MULTITHREAD|SQLITE_CONFIG_SERIALIZED)
	static native int sqlite3_config(int op);
	//sqlite3_config(SQLITE_CONFIG_URI, int onoff)
	//sqlite3_config(SQLITE_CONFIG_MEMSTATUS, int onoff)
	static native int sqlite3_config(int op, boolean onoff);
	//sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
	public static native int sqlite3_config(int op, LogCallback xLog, Pointer udp);
	// Applications can use the sqlite3_log(E,F,..) API to send new messages to the log, if desired, but this is discouraged.
	public static native void sqlite3_log(int iErrCode, String msg);

	static native String sqlite3_errmsg(SQLite3 pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
	static native int sqlite3_errcode(SQLite3 pDb);

	static native int sqlite3_extended_result_codes(SQLite3 pDb, boolean onoff);
	static native int sqlite3_extended_errcode(SQLite3 pDb);

	static native int sqlite3_initialize();
	static native int sqlite3_shutdown();

	static native int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs); // no copy needed
	static native int sqlite3_close(SQLite3 pDb);
	static native int sqlite3_close_v2(SQLite3 pDb); // since 3.7.14
	static native void sqlite3_interrupt(SQLite3 pDb);

	static native int sqlite3_busy_handler(SQLite3 pDb, BusyHandler bh, Pointer pArg);
	static native int sqlite3_busy_timeout(SQLite3 pDb, int ms);
	static native int sqlite3_db_status(SQLite3 pDb, int op, IntByReference pCur, IntByReference pHiwtr, boolean resetFlg);
	static native int sqlite3_db_config(SQLite3 pDb, int op, int v, IntByReference pOk);
	// TODO sqlite3_db_status
	static native int sqlite3_db_config(Pointer pDb, int op, int v, IntByReference pOk);
	//#if mvn.project.property.sqlite.omit.load.extension == "true"
	static int sqlite3_enable_load_extension(Object pDb, boolean onoff) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	static int sqlite3_load_extension(Object pDb, String file, String proc, PointerByReference errMsg) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	//#else
	static native int sqlite3_enable_load_extension(SQLite3 pDb, boolean onoff);
	static native int sqlite3_load_extension(SQLite3 pDb, String file, String proc, PointerByReference errMsg);
	//#endif
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
			SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
			SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
			SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
	static native int sqlite3_limit(SQLite3 pDb, int id, int newVal);
	static native boolean sqlite3_get_autocommit(SQLite3 pDb);

	static native int sqlite3_changes(SQLite3 pDb);
	static native int sqlite3_total_changes(SQLite3 pDb);
	static native long sqlite3_last_insert_rowid(SQLite3 pDb);

	static native String sqlite3_db_filename(SQLite3 pDb, String dbName); // no copy needed
	static native int sqlite3_db_readonly(SQLite3 pDb, String dbName); // no copy needed

	static native SQLite3Stmt sqlite3_next_stmt(SQLite3 pDb, SQLite3Stmt pStmt);

	static native int sqlite3_table_column_metadata(SQLite3 pDb, String dbName, String tableName, String columnName,
																									PointerByReference pzDataType, PointerByReference pzCollSeq,
																									IntByReference pNotNull, IntByReference pPrimaryKey, IntByReference pAutoinc); // no copy needed

	static native int sqlite3_exec(SQLite3 pDb, String cmd, Callback c, Pointer udp, PointerByReference errMsg);

	static native int sqlite3_prepare_v2(SQLite3 pDb, Pointer sql, int nByte, PointerByReference ppStmt,
																			 PointerByReference pTail);
	static native String sqlite3_sql(SQLite3Stmt pStmt); // no copy needed
	static native int sqlite3_finalize(SQLite3Stmt pStmt);
	static native int sqlite3_step(SQLite3Stmt pStmt);
	static native int sqlite3_reset(SQLite3Stmt pStmt);
	static native int sqlite3_clear_bindings(SQLite3Stmt pStmt);
	static native boolean sqlite3_stmt_busy(SQLite3Stmt pStmt);
	static native boolean sqlite3_stmt_readonly(SQLite3Stmt pStmt);

	static native int sqlite3_column_count(SQLite3Stmt pStmt);
	static native int sqlite3_data_count(SQLite3Stmt pStmt);
	static native int sqlite3_column_type(SQLite3Stmt pStmt, int iCol);
	static native String sqlite3_column_name(SQLite3Stmt pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
	//#if mvn.project.property.sqlite.enable.column.metadata == "true"
	static native String sqlite3_column_origin_name(SQLite3Stmt pStmt, int iCol); // copy needed
	static native String sqlite3_column_table_name(SQLite3Stmt pStmt, int iCol); // copy needed
	static native String sqlite3_column_database_name(SQLite3Stmt pStmt, int iCol); // copy needed
	static native String sqlite3_column_decltype(SQLite3Stmt pStmt, int iCol); // copy needed
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

	static native Pointer sqlite3_column_blob(SQLite3Stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	static native int sqlite3_column_bytes(SQLite3Stmt pStmt, int iCol);
	static native double sqlite3_column_double(SQLite3Stmt pStmt, int iCol);
	static native int sqlite3_column_int(SQLite3Stmt pStmt, int iCol);
	static native long sqlite3_column_int64(SQLite3Stmt pStmt, int iCol);
	static native String sqlite3_column_text(SQLite3Stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	static native int sqlite3_bind_parameter_count(SQLite3Stmt pStmt);
	static native int sqlite3_bind_parameter_index(SQLite3Stmt pStmt, String name); // no copy needed
	static native String sqlite3_bind_parameter_name(SQLite3Stmt pStmt, int i); // copy needed

	static native int sqlite3_bind_blob(SQLite3Stmt pStmt, int i, byte[] value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native int sqlite3_bind_double(SQLite3Stmt pStmt, int i, double value);
	static native int sqlite3_bind_int(SQLite3Stmt pStmt, int i, int value);
	static native int sqlite3_bind_int64(SQLite3Stmt pStmt, int i, long value);
	static native int sqlite3_bind_null(SQLite3Stmt pStmt, int i);
	static native int sqlite3_bind_text(SQLite3Stmt pStmt, int i, String value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	//static native int sqlite3_bind_text16(SQLite3Stmt pStmt, int i, const void*, int, void(*)(void*));
	//static native int sqlite3_bind_value(SQLite3Stmt pStmt, int i, const sqlite3_value*);
	static native int sqlite3_bind_zeroblob(SQLite3Stmt pStmt, int i, int n);
	static native int sqlite3_stmt_status(SQLite3Stmt pStmt, int op, boolean reset);
	//#if mvn.project.property.sqlite.enable.stmt.scanstatus == "true"
	static native int sqlite3_stmt_scanstatus(SQLite3Stmt pStmt, int idx, int iScanStatusOp, PointerByReference pOut);
	static native void sqlite3_stmt_scanstatus_reset(SQLite3Stmt pStmt);
	//#endif

	static native void sqlite3_free(Pointer p);

	static native int sqlite3_blob_open(SQLite3 pDb, String dbName, String tableName, String columnName,
																			long iRow, boolean flags, PointerByReference ppBlob); // no copy needed
	static native int sqlite3_blob_reopen(SQLite3Blob pBlob, long iRow);
	static native int sqlite3_blob_bytes(SQLite3Blob pBlob);
	static native int sqlite3_blob_read(SQLite3Blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_write(SQLite3Blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_close(SQLite3Blob pBlob);

	static native SQLite3Backup sqlite3_backup_init(SQLite3 pDst, String dstName, SQLite3 pSrc, String srcName);
	static native int sqlite3_backup_step(SQLite3Backup pBackup, int nPage);
	static native int sqlite3_backup_remaining(SQLite3Backup pBackup);
	static native int sqlite3_backup_pagecount(SQLite3Backup pBackup);
	static native int sqlite3_backup_finish(SQLite3Backup pBackup);

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	static native void sqlite3_progress_handler(SQLite3 pDb, int nOps, ProgressCallback xProgress, Pointer pArg);
	static native void sqlite3_trace(SQLite3 pDb, TraceCallback xTrace, Pointer pArg);
	static native void sqlite3_profile(SQLite3 pDb, ProfileCallback xProfile, Pointer pArg);

	// TODO sqlite3_commit_hook, sqlite3_rollback_hook
	static native Pointer sqlite3_update_hook(SQLite3 pDb, UpdateHook xUpdate, Pointer pArg);
	static native int sqlite3_set_authorizer(SQLite3 pDb, Authorizer authorizer, Pointer pUserData);

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	static native int sqlite3_create_function_v2(SQLite3 pDb, String functionName, int nArg, int eTextRep,
																							 Pointer pApp, ScalarCallback xFunc, Callback xStep, Callback xFinal, Callback xDestroy);

	public static native void sqlite3_result_null(SQLite3Context pCtx);
	public static native void sqlite3_result_int(SQLite3Context pCtx, int i);
	public static native void sqlite3_result_double(SQLite3Context pCtx, double d);
	// TODO To be tested
	public static native void sqlite3_result_text(SQLite3Context pCtx, String text, Callback free);
	public static native void sqlite3_result_blob(SQLite3Context pCtx, byte[] blob, int n, Callback free);
	public static native void sqlite3_result_int64(SQLite3Context pCtx, long l);
	public static native void sqlite3_result_zeroblob(SQLite3Context pCtx, int n);

	public static native void sqlite3_result_error(SQLite3Context pCtx, String err, int length);
	public static native void sqlite3_result_error_code(SQLite3Context pCtx, int errCode);
	public static native void sqlite3_result_error_nomem(SQLite3Context pCtx);
	public static native void sqlite3_result_error_toobig(SQLite3Context pCtx);

	public static native Pointer sqlite3_value_blob(SQLite3Value pValue);
	public static native int sqlite3_value_bytes(SQLite3Value pValue);
	public static native double sqlite3_value_double(SQLite3Value pValue);
	public static native int sqlite3_value_int(SQLite3Value pValue);
	public static native long sqlite3_value_int64(SQLite3Value pValue);
	public static native String sqlite3_value_text(SQLite3Value pValue);
	public static native int sqlite3_value_type(SQLite3Value pValue);
	public static native int sqlite3_value_numeric_type(SQLite3Value pValue);
	// TODO To be tested
	public static native Pointer sqlite3_get_auxdata(SQLite3Context pCtx, int n);
	public static native void sqlite3_set_auxdata(SQLite3Context pCtx, int n, Pointer p, Callback free);
	// TODO To be tested
	public static native Pointer sqlite3_user_data(SQLite3Context pCtx);

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String UTF_8_ECONDING = UTF_8.name();
	static Pointer nativeString(String sql) {
		final byte[] data = sql.getBytes(UTF_8);
		final Pointer pointer = new Memory(data.length + 1);
		pointer.write(0L, data, 0, data.length);
		pointer.setByte(data.length, (byte) 0);
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

	public interface LogCallback extends Callback {
		@SuppressWarnings("unused")
		void invoke(Pointer udp, int err, String msg);
	}

	private static final LogCallback LOG_CALLBACK = new LogCallback() {
		@Override
		public void invoke(Pointer udp, int err, String msg) {
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

	/**
	 * Query Progress Callback.
	 * @see <a href="http://sqlite.org/c3ref/progress_handler.html">sqlite3_progress_handler</a>
	 */
	public interface ProgressCallback extends Callback {
		/**
		 * @param arg
		 * @return <code>true</code> to interrupt
		 */
		@SuppressWarnings("unused")
		boolean invoke(Pointer arg);
	}

	/**
	 * Database connection handle
	 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
	 */
	public static class SQLite3 extends PointerType {
		public SQLite3() {
		}
		public SQLite3(Pointer p) {
			super(p);
		}
	}

	/**
	 * Prepared statement object
	 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt</a>
	 */
	public static class SQLite3Stmt extends PointerType {
		public SQLite3Stmt() {
		}
		public SQLite3Stmt(Pointer p) {
			super(p);
		}
	}

	/**
	 * A handle to an open BLOB
	 * @see <a href="http://sqlite.org/c3ref/blob.html">sqlite3_blob</a>
	 */
	public static class SQLite3Blob extends PointerType {
		public SQLite3Blob() {
		}
		public SQLite3Blob(Pointer p) {
			super(p);
		}
	}

	/**
	 * Online backup object
	 * @see <a href="http://sqlite.org/c3ref/backup.html">sqlite3_backup</a>
	 */
	public static class SQLite3Backup extends PointerType {
		public SQLite3Backup() {
		}
		public SQLite3Backup(Pointer p) {
			super(p);
		}
	}

	/**
	 * SQL function context object
	 * @see <a href="http://sqlite.org/c3ref/context.html">sqlite3_context</a>
	 */
	public static class SQLite3Context extends PointerType {
		public SQLite3Context() {
		}
		public SQLite3Context(Pointer p) {
			super(p);
		}
	}

	/**
	 * Dynamically typed value object
	 * @see <a href="http://sqlite.org/c3ref/value.html">sqlite3_value</a>
	 */
	public static class SQLite3Value extends PointerType {
		public SQLite3Value() {
		}
		public SQLite3Value(Pointer p) {
			super(p);
		}
	}
}
