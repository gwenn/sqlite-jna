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
	static native boolean sqlite3_compileoption_used(@Cast("const char*") BytePointer optName);
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
	public static native void sqlite3_log(int iErrCode,@Cast("const char*")  BytePointer msg);

	static native @Cast("const char*") BytePointer sqlite3_errmsg(sqlite3 pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
	static native int sqlite3_errcode(sqlite3 pDb);

	static native int sqlite3_extended_result_codes(sqlite3 pDb, boolean onoff);
	static native int sqlite3_extended_errcode(sqlite3 pDb);

	static native int sqlite3_initialize();
	static native int sqlite3_shutdown();

	static native int sqlite3_open_v2(@Cast("const char*") BytePointer filename, @ByPtrPtr sqlite3 ppDb, int flags,@Cast("const char*") BytePointer vfs); // no copy needed
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
	static int sqlite3_load_extension(Object pDb, BytePointer file, BytePointer proc, @Cast("char**") @ByPtrPtr BytePointer errMsg) {
		throw new UnsupportedOperationException("SQLITE_OMIT_LOAD_EXTENSION activated");
	}
	//#else
	static native int sqlite3_enable_load_extension(sqlite3 pDb, boolean onoff);
	static native int sqlite3_load_extension(sqlite3 pDb,@Cast("const char*") BytePointer file,@Cast("const char*") BytePointer proc, @Cast("char**") @ByPtrPtr BytePointer errMsg);
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

	static native @Cast("const char*") BytePointer sqlite3_db_filename(sqlite3 pDb,@Cast("const char*") BytePointer dbName); // no copy needed
	static native int sqlite3_db_readonly(sqlite3 pDb,@Cast("const char*") BytePointer dbName); // no copy needed

	static native sqlite3_stmt sqlite3_next_stmt(sqlite3 pDb, sqlite3_stmt pStmt);

	static native int sqlite3_table_column_metadata(sqlite3 pDb,@Cast("const char*") BytePointer dbName,@Cast("const char*") BytePointer tableName,@Cast("const char*") BytePointer columnName,
																									@Cast("char const**") @ByPtrPtr BytePointer pzDataType, @Cast("char const**") @ByPtrPtr BytePointer pzCollSeq,
																									IntPointer pNotNull, IntPointer pPrimaryKey, IntPointer pAutoinc); // no copy needed

	static native int sqlite3_exec(sqlite3 pDb,@Cast("const char*") BytePointer cmd, @Cast("int (*)(void*,int,char**,char**)") Pointer c, Pointer udp, @Cast("char**") @ByPtrPtr BytePointer errMsg);

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

	static native @Const BytePointer sqlite3_column_blob(sqlite3_stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	static native int sqlite3_column_bytes(sqlite3_stmt pStmt, int iCol);
	static native double sqlite3_column_double(sqlite3_stmt pStmt, int iCol);
	static native int sqlite3_column_int(sqlite3_stmt pStmt, int iCol);
	static native @Cast("sqlite3_int64") long sqlite3_column_int64(sqlite3_stmt pStmt, int iCol);
	static native @Cast("const unsigned char*") BytePointer sqlite3_column_text(sqlite3_stmt pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
	//const void *sqlite3_column_text16(SQLite3Stmt pStmt, int iCol);
	//sqlite3_value *sqlite3_column_value(SQLite3Stmt pStmt, int iCol);

	static native int sqlite3_bind_parameter_count(sqlite3_stmt pStmt);
	static native int sqlite3_bind_parameter_index(sqlite3_stmt pStmt,@Cast("const char*") BytePointer name); // no copy needed
	static native @Cast("const char*") BytePointer sqlite3_bind_parameter_name(sqlite3_stmt pStmt, int i); // copy needed

	static native int sqlite3_bind_blob(sqlite3_stmt pStmt, int i, byte[] value, int n,@Cast("sqlite3_destructor_type") long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native int sqlite3_bind_double(sqlite3_stmt pStmt, int i, double value);
	static native int sqlite3_bind_int(sqlite3_stmt pStmt, int i, int value);
	static native int sqlite3_bind_int64(sqlite3_stmt pStmt, int i, @Cast("sqlite3_int64") long value);
	static native int sqlite3_bind_null(sqlite3_stmt pStmt, int i);
	static native int sqlite3_bind_text(sqlite3_stmt pStmt, int i,@Cast("const char*") BytePointer value, int n,@Cast("sqlite3_destructor_type") long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	//static native int sqlite3_bind_text16(SQLite3Stmt pStmt, int i, const void*, int, void(*)(void*));
	//static native int sqlite3_bind_value(SQLite3Stmt pStmt, int i, const sqlite3_value*);
	static native int sqlite3_bind_zeroblob(sqlite3_stmt pStmt, int i, int n);
	static native int sqlite3_stmt_status(sqlite3_stmt pStmt, int op, boolean reset);
	//#if mvn.project.property.sqlite.enable.stmt.scanstatus == "true"
	static native int sqlite3_stmt_scanstatus(sqlite3_stmt pStmt, int idx, int iScanStatusOp, Pointer pOut);
	static native void sqlite3_stmt_scanstatus_reset(sqlite3_stmt pStmt);
	//#endif

	static native void sqlite3_free(Pointer p);

	static native int sqlite3_blob_open(sqlite3 pDb,@Cast("const char*") BytePointer dbName,@Cast("const char*") BytePointer tableName,@Cast("const char*") BytePointer columnName,
																			@Cast("sqlite3_int64") long iRow, boolean flags, @ByPtrPtr sqlite3_blob ppBlob); // no copy needed
	static native int sqlite3_blob_reopen(sqlite3_blob pBlob, @Cast("sqlite3_int64") long iRow);
	static native int sqlite3_blob_bytes(sqlite3_blob pBlob);
	static native int sqlite3_blob_read(sqlite3_blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_write(sqlite3_blob pBlob, ByteBuffer z, int n, int iOffset);
	static native int sqlite3_blob_close(sqlite3_blob pBlob);

	static native sqlite3_backup sqlite3_backup_init(sqlite3 pDst,@Cast("const char*") BytePointer dstName, sqlite3 pSrc,@Cast("const char*") BytePointer srcName);
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
	static native int sqlite3_create_function_v2(sqlite3 pDb,@Cast("const char*") BytePointer functionName, int nArg, int eTextRep,
		 Pointer pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateFinalCallback xFinal, Destructor xDestroy);

	static native void sqlite3_result_null(sqlite3_context pCtx);
	static native void sqlite3_result_int(sqlite3_context pCtx, int i);
	static native void sqlite3_result_double(sqlite3_context pCtx, double d);
	static native void sqlite3_result_text(sqlite3_context pCtx,@Cast("const char*") BytePointer text, int n,@Cast("sqlite3_destructor_type") long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
	static native void sqlite3_result_blob(sqlite3_context pCtx, byte[] blob, int n,@Cast("sqlite3_destructor_type") long xDel);
	static native void sqlite3_result_int64(sqlite3_context pCtx, @Cast("sqlite3_int64") long l);
	static native void sqlite3_result_zeroblob(sqlite3_context pCtx, int n);

	static native void sqlite3_result_error(sqlite3_context pCtx,@Cast("const char*") BytePointer err, int length);
	static native void sqlite3_result_error_code(sqlite3_context pCtx, int errCode);
	static native void sqlite3_result_error_nomem(sqlite3_context pCtx);
	static native void sqlite3_result_error_toobig(sqlite3_context pCtx);
	//static native void sqlite3_result_subtype(sqlite3_context pCtx, /*unsigned*/ int subtype);

	static native @Const BytePointer sqlite3_value_blob(sqlite3_value pValue);
	static native int sqlite3_value_bytes(sqlite3_value pValue);
	static native double sqlite3_value_double(sqlite3_value pValue);
	static native int sqlite3_value_int(sqlite3_value pValue);
	static native @Cast("sqlite3_int64") long sqlite3_value_int64(sqlite3_value pValue);
	static native @Cast("const unsigned char*") BytePointer sqlite3_value_text(sqlite3_value pValue);
	static native int sqlite3_value_type(sqlite3_value pValue);
	static native int sqlite3_value_numeric_type(sqlite3_value pValue);

	static native Pointer sqlite3_get_auxdata(sqlite3_context pCtx, int n);
	static native void sqlite3_set_auxdata(sqlite3_context pCtx, int n, Pointer p, Destructor free);
	static native Pointer sqlite3_aggregate_context(sqlite3_context pCtx, int nBytes);
	static native sqlite3 sqlite3_context_db_handle(sqlite3_context pCtx);

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final String UTF_8_ECONDING = UTF_8.name();
	public static BytePointer nativeString(String s) {
		if (s == null) {
			return null;
		}
		byte[] bytes = s.getBytes(UTF_8);
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
		protected LogCallback() {
			allocate();
		}
		private native void allocate();
		@SuppressWarnings("unused")
		public void call(Pointer udp, int err,@Cast("const char*") BytePointer msg) {
			log(err, getString(msg));
		}
		protected abstract void log(int err, String msg);
	}

	private static final LogCallback LOG_CALLBACK = new LogCallback() {
		protected void log(int err, String msg) {
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
	public static abstract class ProgressCallback extends FunctionPointer {
		protected ProgressCallback() {
			allocate();
		}
		private native void allocate();
		/**
		 * @param arg
		 * @return <code>true</code> to interrupt
		 */
		@SuppressWarnings("unused")
		public @Cast("int") boolean call(Pointer arg) {
			return progress();
		}
		protected abstract boolean progress();
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
		 * @return a copy of the pointer to the database connection (the 1st parameter) of
		 * {@link SQLite#sqlite3_create_function_v2(sqlite3, BytePointer, int, int, Pointer, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, Destructor)}
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
		public void setResultText(BytePointer result) {
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
			sqlite3_result_error(this, nativeString(errMsg), -1);
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
		public long getCount() {
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
			BytePointer blob = sqlite3_value_blob(arg);
			if (blob == null || blob.isNull()) {
				return null;
			} else {
				final byte[] bytes = new byte[sqlite3_value_bytes(arg)];
				blob.get(bytes);
				return bytes; // a copy is made...
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

	public abstract static class Destructor extends FunctionPointer {
        protected Destructor() {
            allocate();
        }
        private native void allocate();
        @SuppressWarnings("unused")
        public abstract void call(Pointer p);
    }

	/**
     * User defined SQL scalar function.
     * <pre>{@code
     * new ScalarCallback() {
     *   \@Override
     *   protected void func(SQLite3Context pCtx, SQLite3Values args) {
     * 	   pCtx.setResultInt(0);
     *   }
     * }
     * }</pre>
     * @see Conn#createScalarFunction(String, int, int, ScalarCallback)
     * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
     */
    public abstract static class ScalarCallback extends FunctionPointer {
        protected ScalarCallback() {
            allocate();
        }
        private native void allocate();
        //void (*)(sqlite3_context*,int,sqlite3_value**)
        /**
         * @param pCtx <code>sqlite3_context*</code>
         * @param nArg number of arguments
         * @param args function arguments
         */
        @SuppressWarnings("unused")
        public void call(sqlite3_context pCtx, int nArg,@Cast("sqlite3_value**") PointerPointer args) {
            func(pCtx, SQLite3Values.build(nArg, args));
        }

        /**
         * @param pCtx <code>sqlite3_context*</code>
         * @param args function arguments
         */
        protected abstract void func(sqlite3_context pCtx, SQLite3Values args);

        /**
         * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_set_auxdata</a>
         */
        public void setAuxData(sqlite3_context pCtx, int n, Pointer auxData, Destructor free) {
            sqlite3_set_auxdata(pCtx, n, auxData, free);
        }
        /**
         * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_get_auxdata</a>
         */
        public Pointer getAuxData(sqlite3_context pCtx, int n) {
            return sqlite3_get_auxdata(pCtx, n);
        }
    }

	/**
     * User defined SQL aggregate function.
     * <pre>{@code
     * new AggregateStepCallback() {
     *   \@Override
     *   public void step(SQLite3Context pCtx, Pointer aggrCtx, SQLite3Values args) {
     *     args.getX(...);
     *     ...
     *     aggrCtx.setX(...);
     *   }
     * }
     * }</pre>
     *
     * @see Conn#createAggregateFunction(String, int, int, AggregateStepCallback, AggregateFinalCallback)
     * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
     */
    public abstract static class AggregateStepCallback extends FunctionPointer {
        protected AggregateStepCallback() {
            allocate();
        }
        private native void allocate();
        //void (*)(sqlite3_context*,int,sqlite3_value**),
        /**
         * @param pCtx <code>sqlite3_context*</code>
         * @param nArg number of arguments
         * @param args function arguments
         */
        @SuppressWarnings("unused")
        public void call(sqlite3_context pCtx, int nArg,@Cast("sqlite3_value**") PointerPointer args) {
            final int nBytes = numberOfBytes();
            final Pointer p = sqlite3_aggregate_context(pCtx, nBytes);
            if ((p == null || p.isNull()) && nBytes > 0) {
                sqlite3_result_error_nomem(pCtx);
                return;
            }
            step(pCtx, p, SQLite3Values.build(nArg, args));
        }

        /**
         * @return number of bytes to allocate.
         * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_aggregate_context</a>
         */
        protected abstract int numberOfBytes();

        /**
         * @param pCtx <code>sqlite3_context*</code>
         * @param aggrCtx aggregate context
         * @param args function arguments
         */
        protected abstract void step(sqlite3_context pCtx, Pointer aggrCtx, SQLite3Values args);
    }

	/**
	 * User defined SQL aggregate function.
	 * <pre>{@code
	 * new AggregateFinalCallback() {
	 *   \@Override
	 *   public void finalStep(SQLite3Context pCtx, Pointer aggrCtx) {
	 *     if (aggrCtx == null) {
	 *       pCtx.setResultNull();
	 *       return;
	 *     }
	 *     ...
	 *     pCtx.setResultX(...);
	 *   }
	 * }
	 * }</pre>
	 *
	 * @see Conn#createAggregateFunction(String, int, int, AggregateStepCallback, AggregateFinalCallback)
	 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
	 */
	public abstract static class AggregateFinalCallback extends FunctionPointer {
		protected AggregateFinalCallback() {
			allocate();
		}
		private native void allocate();

		/**
		 * @param pCtx <code>sqlite3_context*</code>
		 */
		@SuppressWarnings("unused")
		public void call(sqlite3_context pCtx) {
			finalStep(pCtx, getAggregateContext(pCtx));
		}

		protected abstract void finalStep(sqlite3_context pCtx, Pointer aggrCtx);

		/**
		 * Obtain aggregate function context.
		 *
		 * @return <code>null</code> when no rows match an aggregate query.
		 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_aggregate_context</a>
		 */
		protected Pointer getAggregateContext(sqlite3_context pCtx) {
			// Within the xFinal callback, it is customary to set N=0 in calls to sqlite3_aggregate_context(C,N)
			// so that no pointless memory allocations occur.
			return sqlite3_aggregate_context(pCtx, 0);
		}
	}

	/**
     * Callback to handle SQLITE_BUSY errors
     *
     * @see Conn#setBusyHandler(BusyHandler)
     * @see <a href="http://sqlite.org/c3ref/busy_handler.html">sqlite3_busy_handler</a>
     */
    public abstract static class BusyHandler extends FunctionPointer {
        protected BusyHandler() {
            allocate();
        }
        private native void allocate();
        /**
         * @param pArg  User data (<code>null</code>)
         * @param count the number of times that the busy handler has been invoked previously for the same locking event.
         * @return <code>true</code> to try again, <code>false</code> to abort.
         */
        public @Cast("int") boolean call(Pointer pArg, int count) {
					return busy(count);
				}

        /**
         * @param count the number of times that the busy handler has been invoked previously for the same locking event.
         * @return <code>true</code> to try again, <code>false</code> to abort.
         */
				protected abstract boolean busy(int count);
    }

	/**
     * Data change notification callback.
     * <ul>
     * <li>The update hook is not invoked when internal system tables are modified (i.e. sqlite_master and sqlite_sequence).</li>
     * <li>The update hook is not invoked when WITHOUT ROWID tables are modified.</li>
     * <li>In the current implementation, the update hook is not invoked when duplication rows are deleted because of an ON CONFLICT REPLACE clause.</li>
     * <li>Nor is the update hook invoked when rows are deleted using the truncate optimization.</li>
     * </ul>
     * @see Conn#updateHook(UpdateHook)
     * @see <a href="http://sqlite.org/c3ref/update_hook.html">sqlite3_update_hook</a>
     */
    public abstract static class UpdateHook extends FunctionPointer {
        protected UpdateHook() {
            allocate();
        }
        private native void allocate();
        /**
         * Data Change Notification Callback
         * @param pArg <code>null</code>.
         * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
         * @param dbName database name containing the affected row.
         * @param tblName table name containing the affected row.
         * @param rowId id of the affected row.
         */
        public void call(Pointer pArg, int actionCode,@Cast("const char*") BytePointer dbName,
                @Cast("const char*") BytePointer tblName, @Cast("sqlite3_int64") long rowId) {
					update(actionCode, getString(dbName), getString(tblName), rowId);
				}
				/**
				 * Data Change Notification Callback
				 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
				 * @param dbName database name containing the affected row.
				 * @param tblName table name containing the affected row.
				 * @param rowId id of the affected row.
				 */
				protected abstract void update(int actionCode, String dbName, String tblName, long rowId);
    }

	/**
     * Tracing callback.
     * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
     */
    public abstract static class TraceCallback extends FunctionPointer {
        protected TraceCallback() {
            allocate();
        }
        private native void allocate();
        /**
         * @param sql SQL statement text.
         */
        @SuppressWarnings("unused")
        public void call(Pointer arg,@Cast("const char*") BytePointer sql) {
            trace(getString(sql));
        }

        /**
         * @param sql SQL statement text.
         */
        protected abstract void trace(String sql);
    }

	/**
     * Profiling callback.
     * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_profile</a>
     */
    public abstract static class ProfileCallback extends FunctionPointer {
        protected ProfileCallback() {
            allocate();
        }
        private native void allocate();
        /**
         * @param sql SQL statement text.
         * @param ns time in nanoseconds
         */
        @SuppressWarnings("unused")
        public void call(Pointer arg,@Cast("const char*") BytePointer sql,@Cast("sqlite3_uint64") long ns) {
            profile(getString(sql), ns);
        }

        /**
         * @param sql SQL statement text.
         * @param ns time in nanoseconds
         */
        protected abstract void profile(String sql, long ns);
    }

	/**
     * Compile-time authorization callback
     *
     * @see Conn#setAuhtorizer(Authorizer)
     * @see <a href="http://sqlite.org/c3ref/set_authorizer.html">sqlite3_set_authorizer</a>
     */
    public abstract static class Authorizer extends FunctionPointer {
        protected Authorizer() {
            allocate();
        }
        private native void allocate();
        /**
         * @param pArg       User data (<code>null</code>)
         * @param actionCode {@link ActionCodes}.*
         * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
         */
        public int call(Pointer pArg, int actionCode,
                @Cast("const char*") BytePointer arg1,@Cast("const char*") BytePointer arg2,
                @Cast("const char*") BytePointer dbName,@Cast("const char*") BytePointer triggerName) {
            return authorize(actionCode, getString(arg1), getString(arg2), getString(dbName), getString(triggerName));
        }
        protected abstract int authorize(int actionCode, String arg1, String arg2, String dbName, String triggerName);

        public static final int SQLITE_OK = ErrCodes.SQLITE_OK;
        /**
         * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
         */
        public static final int SQLITE_DENY = 1;
        /**
         * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
         */
        public static final int SQLITE_IGNORE = 2;
    }
}
