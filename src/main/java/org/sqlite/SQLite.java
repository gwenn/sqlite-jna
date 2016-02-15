/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.annotation.ByPtrPtr;
import org.bytedeco.javacpp.annotation.Cast;
import org.bytedeco.javacpp.annotation.Const;
import org.bytedeco.javacpp.annotation.Opaque;
import org.bytedeco.javacpp.annotation.Platform;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

// TODO JNA/Bridj/JNR/JNI and native libs embedded in JAR.
@Platform(cinclude = "sqlite3.h", link = "sqlite3")
public final class SQLite {
	static { Loader.load(); }

	public static final int SQLITE_OK = 0;

	public static final int SQLITE_ROW = 100;
	public static final int SQLITE_DONE = 101;

	static final long SQLITE_TRANSIENT = -1;

	static native @Cast("const char*") BytePointer sqlite3_libversion(); // no copy needed
	static native int sqlite3_libversion_number();
	static native boolean sqlite3_threadsafe();
	static native boolean sqlite3_compileoption_used(String optName);
	static native @Cast("const char*") BytePointer sqlite3_compileoption_get(int n);

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

	static native @Cast("const char*") BytePointer sqlite3_errmsg(sqlite3 pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
	static native int sqlite3_errcode(sqlite3 pDb);

	static native int sqlite3_extended_result_codes(sqlite3 pDb, boolean onoff);
	static native int sqlite3_extended_errcode(sqlite3 pDb);

	static native int sqlite3_initialize();
	static native int sqlite3_shutdown();

	static native int sqlite3_open_v2(String filename, @ByPtrPtr sqlite3 ppDb, int flags, String vfs); // no copy needed
	static native int sqlite3_close(sqlite3 pDb);
	static native int sqlite3_close_v2(sqlite3 pDb); // since 3.7.14
	static native void sqlite3_interrupt(sqlite3 pDb);

	static native int sqlite3_busy_handler(sqlite3 pDb, BusyHandler bh, Pointer pArg);
	static native int sqlite3_busy_timeout(sqlite3 pDb, int ms);
	static native int sqlite3_db_status(sqlite3 pDb, int op, IntPointer pCur, IntPointer pHiwtr, boolean resetFlg);
	static native int sqlite3_db_config(sqlite3 pDb, int op, int v, IntPointer pOk);
	//#if mvn.project.property.sqlite.omit.load.extension == "true"
	static int sqlite3_enable_load_extension(Object pDb, boolean onoff) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	static int sqlite3_load_extension(Object pDb, String file, String proc, @Cast("char**") @ByPtrPtr BytePointer errMsg) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	//#else
	static native int sqlite3_enable_load_extension(sqlite3 pDb, boolean onoff);
	static native int sqlite3_load_extension(sqlite3 pDb, String file, String proc, @Cast("char**") @ByPtrPtr BytePointer errMsg);
	//#endif
	public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
			SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
			SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
			SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
	static native int sqlite3_limit(sqlite3 pDb, int id, int newVal);
	static native boolean sqlite3_get_autocommit(sqlite3 pDb);

	static native int sqlite3_changes(sqlite3 pDb);
	static native int sqlite3_total_changes(sqlite3 pDb);
	static native @Cast("sqlite3_int64") long sqlite3_last_insert_rowid(sqlite3 pDb);

	static native @Cast("const char*") BytePointer sqlite3_db_filename(sqlite3 pDb, String dbName); // no copy needed
	static native int sqlite3_db_readonly(sqlite3 pDb, String dbName); // no copy needed

	static native sqlite3_stmt sqlite3_next_stmt(sqlite3 pDb, sqlite3_stmt pStmt);

	static native int sqlite3_table_column_metadata(sqlite3 pDb, String dbName, String tableName, String columnName,
																									@Cast("char const**") @ByPtrPtr BytePointer pzDataType, @Cast("char const**") @ByPtrPtr BytePointer pzCollSeq,
																									IntPointer pNotNull, IntPointer pPrimaryKey, IntPointer pAutoinc); // no copy needed

	static native int sqlite3_exec(sqlite3 pDb, String cmd, ExecCallback c, Pointer udp, @Cast("char**") @ByPtrPtr BytePointer errMsg);

	static native int sqlite3_prepare_v2(sqlite3 pDb, @Cast("const char*") BytePointer sql, int nByte, @ByPtrPtr sqlite3_stmt ppStmt,
																			 @Cast("const char**") @ByPtrPtr BytePointer pTail);
	static native @Cast("const char*") BytePointer sqlite3_sql(sqlite3_stmt pStmt); // no copy needed
	static native int sqlite3_finalize(sqlite3_stmt pStmt);
	static native int sqlite3_step(sqlite3_stmt pStmt);
	static native int sqlite3_reset(sqlite3_stmt pStmt);
	static native int sqlite3_clear_bindings(sqlite3_stmt pStmt);
	static native boolean sqlite3_stmt_busy(sqlite3_stmt pStmt);
	static native boolean sqlite3_stmt_readonly(sqlite3_stmt pStmt);

	static native int sqlite3_column_count(sqlite3_stmt pStmt);
	static native int sqlite3_data_count(sqlite3_stmt pStmt);
	static native int sqlite3_column_type(sqlite3_stmt pStmt, int iCol);
	static native @Cast("const char*") BytePointer sqlite3_column_name(sqlite3_stmt pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
	//#if mvn.project.property.sqlite.enable.column.metadata == "true"
	static native @Cast("const char*") BytePointer sqlite3_column_origin_name(sqlite3_stmt pStmt, int iCol); // copy needed
	static native @Cast("const char*") BytePointer sqlite3_column_table_name(sqlite3_stmt pStmt, int iCol); // copy needed
	static native @Cast("const char*") BytePointer sqlite3_column_database_name(sqlite3_stmt pStmt, int iCol); // copy needed
	static native @Cast("const char*") BytePointer sqlite3_column_decltype(sqlite3_stmt pStmt, int iCol); // copy needed
	//#else
	static BytePointer sqlite3_column_origin_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static BytePointer sqlite3_column_table_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static BytePointer sqlite3_column_database_name(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	static BytePointer sqlite3_column_decltype(Object pStmt, int iCol) {
		throw new UnsupportedOperationException("SQLITE_ENABLE_COLUMN_METADATA not activated");
	}
	//#endif

	static native @Const Pointer sqlite3_column_blob(sqlite3_stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	static native int sqlite3_column_bytes(sqlite3_stmt pStmt, int iCol);
	static native double sqlite3_column_double(sqlite3_stmt pStmt, int iCol);
	static native int sqlite3_column_int(sqlite3_stmt pStmt, int iCol);
	static native @Cast("sqlite3_int64") long sqlite3_column_int64(sqlite3_stmt pStmt, int iCol);
	static native @Cast("const unsigned char*") BytePointer sqlite3_column_text(sqlite3_stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	static native int sqlite3_bind_parameter_count(sqlite3_stmt pStmt);
	static native int sqlite3_bind_parameter_index(sqlite3_stmt pStmt, String name); // no copy needed
	static native @Cast("const char*") BytePointer sqlite3_bind_parameter_name(sqlite3_stmt pStmt, int i); // copy needed

	static native int sqlite3_bind_blob(sqlite3_stmt pStmt, int i, byte[] value, int n,@Cast("void(*)(void*)") long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native int sqlite3_bind_double(sqlite3_stmt pStmt, int i, double value);
	static native int sqlite3_bind_int(sqlite3_stmt pStmt, int i, int value);
	static native int sqlite3_bind_int64(sqlite3_stmt pStmt, int i, @Cast("sqlite3_int64") long value);
	static native int sqlite3_bind_null(sqlite3_stmt pStmt, int i);
	static native int sqlite3_bind_text(sqlite3_stmt pStmt, int i, String value, int n,@Cast("void(*)(void*)") long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	//static native int sqlite3_bind_text16(SQLite3Stmt pStmt, int i, const void*, int, void(*)(void*));
	//static native int sqlite3_bind_value(SQLite3Stmt pStmt, int i, const sqlite3_value*);
	static native int sqlite3_bind_zeroblob(sqlite3_stmt pStmt, int i, int n);
	static native int sqlite3_stmt_status(sqlite3_stmt pStmt, int op, boolean reset);
	//#if mvn.project.property.sqlite.enable.stmt.scanstatus == "true"
	static native int sqlite3_stmt_scanstatus(sqlite3_stmt pStmt, int idx, int iScanStatusOp, Pointer pOut);
	static native void sqlite3_stmt_scanstatus_reset(sqlite3_stmt pStmt);
	//#endif

	static native void sqlite3_free(Pointer p);

	static native int sqlite3_blob_open(sqlite3 pDb, String dbName, String tableName, String columnName,
																			@Cast("sqlite3_int64") long iRow, boolean flags, @ByPtrPtr sqlite3_blob ppBlob); // no copy needed
	static native int sqlite3_blob_reopen(sqlite3_blob pBlob, @Cast("sqlite3_int64") long iRow);
	static native int sqlite3_blob_bytes(sqlite3_blob pBlob);
	static native int sqlite3_blob_read(sqlite3_blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_write(sqlite3_blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_close(sqlite3_blob pBlob);

	static native sqlite3_backup sqlite3_backup_init(sqlite3 pDst, String dstName, sqlite3 pSrc, String srcName);
	static native int sqlite3_backup_step(sqlite3_backup pBackup, int nPage);
	static native int sqlite3_backup_remaining(sqlite3_backup pBackup);
	static native int sqlite3_backup_pagecount(sqlite3_backup pBackup);
	static native int sqlite3_backup_finish(sqlite3_backup pBackup);

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	static native void sqlite3_progress_handler(sqlite3 pDb, int nOps, ProgressCallback xProgress, Pointer pArg);
	static native void sqlite3_trace(sqlite3 pDb, TraceCallback xTrace, Pointer pArg);
	static native void sqlite3_profile(sqlite3 pDb, ProfileCallback xProfile, Pointer pArg);

	// TODO sqlite3_commit_hook, sqlite3_rollback_hook
	static native Pointer sqlite3_update_hook(sqlite3 pDb, UpdateHook xUpdate, Pointer pArg);
	static native int sqlite3_set_authorizer(sqlite3 pDb, Authorizer authorizer, Pointer pUserData);

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	static native int sqlite3_create_function_v2(sqlite3 pDb, String functionName, int nArg, int eTextRep,
																							 Pointer pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateFinalCallback xFinal, Destructor xDestroy);

	static native void sqlite3_result_null(sqlite3_context pCtx);
	static native void sqlite3_result_int(sqlite3_context pCtx, int i);
	static native void sqlite3_result_double(sqlite3_context pCtx, double d);
	static native void sqlite3_result_text(sqlite3_context pCtx, String text, int n,@Cast("void(*)(void*)") long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native void sqlite3_result_blob(sqlite3_context pCtx, byte[] blob, int n,@Cast("void(*)(void*)") long xDel);
	static native void sqlite3_result_int64(sqlite3_context pCtx, @Cast("sqlite3_int64") long l);
	static native void sqlite3_result_zeroblob(sqlite3_context pCtx, int n);

	static native void sqlite3_result_error(sqlite3_context pCtx, String err, int length);
	static native void sqlite3_result_error_code(sqlite3_context pCtx, int errCode);
	static native void sqlite3_result_error_nomem(sqlite3_context pCtx);
	static native void sqlite3_result_error_toobig(sqlite3_context pCtx);
	//static native void sqlite3_result_subtype(sqlite3_context pCtx, /*unsigned*/ int subtype);

	static native @Const Pointer sqlite3_value_blob(sqlite3_value pValue);
	static native int sqlite3_value_bytes(sqlite3_value pValue);
	static native double sqlite3_value_double(sqlite3_value pValue);
	static native int sqlite3_value_int(sqlite3_value pValue);
	static native @Cast("sqlite3_int64") long sqlite3_value_int64(sqlite3_value pValue);
	static native @Cast("const unsigned char*") BytePointer sqlite3_value_text(sqlite3_value pValue);
	static native int sqlite3_value_type(sqlite3_value pValue);
	static native int sqlite3_value_numeric_type(sqlite3_value pValue);

	static native Pointer sqlite3_get_auxdata(sqlite3_context pCtx, int n);
	static native void sqlite3_set_auxdata(sqlite3_context pCtx, int n, Pointer p, Destructor free);
	static native Pointer sqlite3_user_data(sqlite3_context pCtx);
	static native Pointer sqlite3_aggregate_context(sqlite3_context pCtx, int nBytes);
	static native sqlite3 sqlite3_context_db_handle(sqlite3_context pCtx);

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String UTF_8_ECONDING = UTF_8.name();
	static BytePointer nativeString(String sql) {
		byte[] bytes = sql.getBytes(UTF_8);
		final BytePointer ptr = new BytePointer(bytes.length + 1);
		ptr.put(bytes).put(bytes.length, (byte)0).limit(bytes.length);
		return ptr;
	}
	static String getString(BytePointer ptr) {
		if (ptr == null || ptr.isNull()) {
			return null;
		} else {
			final byte[] bytes = ptr.getStringBytes();
			return new String(bytes, UTF_8);
		}
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

	public static abstract class LogCallback extends FunctionPointer {
		@SuppressWarnings("unused")
		public abstract void call(Pointer udp, int err, String msg);
	}

	private static final LogCallback LOG_CALLBACK = new LogCallback() {
		@Override
		public void call(Pointer udp, int err, String msg) {
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
	 * sqlite3_exec() callback function.
	 * @see <a href="http://sqlite.org/c3ref/exec.html">sqlite3_exec</a>
	 */
	public static abstract class ExecCallback extends FunctionPointer {
		@SuppressWarnings("unused")
		public abstract int call(Pointer arg, int n,@Cast("char**") BytePointer values,@Cast("char**") BytePointer names);
	}

	/**
	 * Query Progress Callback.
	 * @see <a href="http://sqlite.org/c3ref/progress_handler.html">sqlite3_progress_handler</a>
	 */
	public static abstract class ProgressCallback extends FunctionPointer {
		/**
		 * @param arg
		 * @return <code>true</code> to interrupt
		 */
		@SuppressWarnings("unused")
		public int call(Pointer arg) {
			return progress(arg) ? 1 : 0;
		}
		/**
		 * @param arg
		 * @return <code>true</code> to interrupt
		 */
		protected abstract boolean progress(Pointer arg);
	}

	/**
	 * Database connection handle
	 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
	 */
	@Opaque
	public static class sqlite3 extends Pointer {
		public sqlite3() {
		}
		public sqlite3(Pointer p) {
			super(p);
		}
	}

	/**
	 * Prepared statement object
	 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt</a>
	 */
	@Opaque
	public static class sqlite3_stmt extends Pointer {
		public sqlite3_stmt() {
		}
		public sqlite3_stmt(Pointer p) {
			super(p);
		}
	}

	/**
	 * A handle to an open BLOB
	 * @see <a href="http://sqlite.org/c3ref/blob.html">sqlite3_blob</a>
	 */
	@Opaque
	public static class sqlite3_blob extends Pointer {
		public sqlite3_blob() {
		}
		public sqlite3_blob(Pointer p) {
			super(p);
		}
	}

	/**
	 * Online backup object
	 * @see <a href="http://sqlite.org/c3ref/backup.html">sqlite3_backup</a>
	 */
	@Opaque
	public static class sqlite3_backup extends Pointer {
		public sqlite3_backup() {
		}
		public sqlite3_backup(Pointer p) {
			super(p);
		}
	}

	/**
	 * Dynamically Typed Value Object
	 * @see <a href="http://sqlite.org/c3ref/value.html">sqlite3_value</a>
	 */
	@Opaque
	public static class sqlite3_value extends Pointer {
		public sqlite3_value() {
		}
		public sqlite3_value(Pointer p) {
			super(p);
		}
	}

	/**
	 * SQL function context object
	 * @see <a href="http://sqlite.org/c3ref/context.html">sqlite3_context</a>
	 */
	@Opaque
	public static class sqlite3_context extends Pointer {
		public sqlite3_context() {
		}
		public sqlite3_context(Pointer p) {
			super(p);
		}

		/**
		 * @return a copy of the pointer that was the <code>pUserData</code> parameter (the 5th parameter) of
		 * {@link SQLite#sqlite3_create_function_v2(sqlite3, String, int, int, Pointer, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, Destructor)}
		 * @see <a href="http://sqlite.org/c3ref/user_data.html">sqlite3_user_data</a>
         */
		public Pointer getUserData() {
			return sqlite3_user_data(this);
		}

		/**
		 * @return a copy of the pointer to the database connection (the 1st parameter) of
		 * {@link SQLite#sqlite3_create_function_v2(sqlite3, String, int, int, Pointer, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, Destructor)}
		 * @see <a href="http://sqlite.org/c3ref/context_db_handle.html">sqlite3_context_db_handle</a>
         */
		public sqlite3 getDbHandle() {
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
		private static SQLite3Values NO_ARG = new SQLite3Values(new PointerPointer());
		private PointerPointer args;

		public static SQLite3Values build(int nArg, PointerPointer args) {
			if (nArg == 0) {
				return NO_ARG;
			}
			args.limit(nArg);
			return new SQLite3Values(args);
		}

		private SQLite3Values(PointerPointer args) {
			this.args = args;
		}

		/**
		 * @return arg count
		 */
		public int getCount() {
			return args.limit();
		}

		public sqlite3_value getValue(int i) {
			return new sqlite3_value(args.get(i));
		}

		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_blob</a>
		 */
		public byte[] getBlob(int i) {
			sqlite3_value arg = getValue(i);
			Pointer blob = sqlite3_value_blob(arg);
			if (blob == null) {
				return null;
			} else {
				final ByteBuffer byteBuffer = blob.asByteBuffer();
				byteBuffer.limit(sqlite3_value_bytes(arg));
				return byteBuffer.array(); // a copy is made...
			}
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_double</a>
		 */
		public double getDouble(int i) {
			return sqlite3_value_double(getValue(i));
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int</a>
		 */
		public int getInt(int i) {
			return sqlite3_value_int(getValue(i));
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int64</a>
		 */
		public long getLong(int i) {
			return sqlite3_value_int64(getValue(i));
		}
		/**
		 * @param i 0...
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_text</a>
		 */
		public String getText(int i) {
			return getString(sqlite3_value_text(getValue(i)));
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_type</a>
		 */
		public int getType(int i) {
			return sqlite3_value_type(getValue(i));
		}
		/**
		 * @param i 0...
		 * @return {@link ColTypes}.*
		 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_numeric_type</a>
		 */
		public int getNumericType(int i) {
			return sqlite3_value_numeric_type(getValue(i));
		}
	}
}
