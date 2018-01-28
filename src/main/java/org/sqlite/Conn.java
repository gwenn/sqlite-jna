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
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static org.sqlite.SQLite.*;

/**
 * Database Connection Handle
 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
 */
public final class Conn implements AutoCloseable {
	/** If the filename is ":memory:", then a private, temporary in-memory database is created for the connection. */
	public static final String MEMORY = ":memory:";
	/** If the filename is an empty string, then a private, temporary on-disk database will be created. */
	public static final String TEMP_FILE = "";

	private sqlite3 pDb;
	private final boolean sharedCacheMode;
	private TimeoutProgressCallback timeoutProgressCallback;

	private final LinkedList<Stmt> cache = new LinkedList<>();
	private int maxCacheSize = 100; // TODO parameterize

	// Make sure a stmt is not finalized while current conn is being closed
	final Object lock = new Object();

	/**
	 * Open a new database connection.
	 * <p>
	 * When the `filename` is an URI, extra query parameters can be used ({@link OpenQueryParameter})
	 * @param filename ":memory:" for memory db, "" for temp file db
	 * @param flags {@link org.sqlite.OpenFlags}.* (TODO EnumSet or BitSet, default flags)
	 * @param vfs may be null
	 * @return Opened Connection
	 * @throws SQLiteException
	 */
	public static Conn open(String filename, int flags, String vfs) throws SQLiteException {
		if (!sqlite3_threadsafe()) {
			throw new SQLiteException("sqlite library was not compiled for thread-safe operation", ErrCodes.WRAPPER_SPECIFIC);
		}
		final sqlite3 pDb = new sqlite3();
		final int res = sqlite3_open_v2(nativeString(filename), pDb, flags, nativeString(vfs));
		if (res != SQLITE_OK) {
			if (pDb.isNull()) {
				sqlite3_close(new sqlite3(pDb));
			}
			throw new SQLiteException(String.format("error while opening a database connection to '%s'", filename), res);
		}
		final boolean uri = (flags & OpenFlags.SQLITE_OPEN_URI) != 0;
		final Map<String, String> queryParams = uri ? OpenQueryParameter.getQueryParams(filename) : Collections.emptyMap();
		// TODO not reliable (and may depend on sqlite3_enable_shared_cache global status)
		final boolean sharedCacheMode = "shared".equals(queryParams.get("cache")) || (flags & OpenFlags.SQLITE_OPEN_SHAREDCACHE) != 0;
		final sqlite3 sqlite3 = pDb.isNull() ? null: pDb;
		final Conn conn = new Conn(sqlite3, sharedCacheMode);
		if (uri && !queryParams.isEmpty()) {
			for (OpenQueryParameter parameter : OpenQueryParameter.values()) {
				parameter.config(queryParams, conn);
			}
		}
		return conn;
	}

	@Override
	protected void finalize() throws Throwable {
		if (pDb != null) {
			sqlite3_log(-1, nativeString("dangling SQLite connection."));
			closeNoCheck();
		}
		super.finalize();
	}
	/**
	 * Close a database connection.
	 * @return result code (No exception is thrown).
	 */
	public int closeNoCheck() {
		if (pDb == null) {
			return SQLITE_OK;
		}

		synchronized (lock) {
			flush();

			// Dangling statements
			sqlite3_stmt stmt = sqlite3_next_stmt(pDb, null);
			while (stmt != null && !stmt.isNull()) {
				if (sqlite3_stmt_busy(stmt)) {
					sqlite3_log(ErrCodes.SQLITE_MISUSE, nativeString("Dangling statement (not reset): \"" + sqlite3_sql(stmt) + "\""));
				} else {
					sqlite3_log(ErrCodes.SQLITE_MISUSE, nativeString("Dangling statement (not finalize): \"" + sqlite3_sql(stmt) + "\""));
				}
				stmt = sqlite3_next_stmt(pDb, stmt);
			}
			final int res = sqlite3_close_v2(pDb); // must be called only once...
			pDb = null;
			return res;
		}
	}
	/**
	 * Close a database connection and throw an exception if an error occured.
	 */
	public void close() throws ConnException {
		final int res = closeNoCheck();
		if (res != ErrCodes.SQLITE_OK) {
			throw new ConnException(this, "error while closing connection", res);
		}
	}

	private Conn(sqlite3 pDb, boolean sharedCacheMode) {
		assert pDb != null;
		this.pDb = pDb;
		this.sharedCacheMode = sharedCacheMode;
	}

	/**
	 * Determine if a database is read-only.
	 * @param dbName "main" or "temp" or attached database name
	 * @return <code>true</code> if specified database is in read only mode.
	 * @throws ConnException if current connection is closed or <code>dbName</code> is not valid.
	 * @see <a href="http://sqlite.org/c3ref/db_readonly.html">sqlite3_db_readonly</a>
	 */
	public boolean isReadOnly(String dbName) throws ConnException {
		checkOpen();
		final int res = sqlite3_db_readonly(pDb, nativeString(dbName)); // ko if pDb is null
		if (res < 0) {
			throw new ConnException(this, String.format("'%s' is not the name of a database", dbName), ErrCodes.WRAPPER_SPECIFIC);
		}
		return res == 1;
	}

	/**
	 * Determine if a database is query-only.
	 * @param dbName "main" or "temp" or attached database name
	 * @return <code>true</code> if specified database is in query only mode.
	 * @throws SQLiteException if current connection is closed or <code>dbName</code> is not valid.
	 * @see <a href="http://sqlite.org/pragma.html#pragma_query_only">pragma query_only</a>
	 */
	public boolean isQueryOnly(String dbName) throws SQLiteException { // since 3.8.0
		return pragma(dbName, "query_only");
	}
	/**
	 * Set a database is in query-only mode.
	 * @param dbName "main" or "temp" or attached database name
	 * @param queryOnly <code>true</code> to activate query-only mode
	 * @throws ConnException if current connection is closed or <code>dbName</code> is not valid.
	 */
	public void setQueryOnly(String dbName, boolean queryOnly) throws ConnException { // since 3.8.0
		pragma(dbName, "query_only", queryOnly);
	}

	/**
	 * Determine if a cannection is in shared-cache mode.
	 * <b>not reliable (and may depend on sqlite3_enable_shared_cache global status)</b>
	 * @return <code>true</code> if shared-cache mode is active.
	 * @see <a href="https://www.sqlite.org/sharedcache.html">SQLite Shared-Cache Mode</a>
	 */
	public boolean isSharedCacheMode() {
		return sharedCacheMode;
	}

	/**
	 * Query READ UNCOMMITTED isolation.
	 * @param dbName "main" or "temp" or attached database name
	 * @return <code>true</code> if READ UNCOMMITTED isolation is active.
	 * @throws SQLiteException if current connection is closed or <code>dbName</code> is not valid.
	 */
	public boolean getReadUncommitted(String dbName) throws SQLiteException {
		return pragma(dbName, "read_uncommitted");
	}
	/**
	 * Query READ UNCOMMITTED isolation.
	 * @param dbName "main" or "temp" or attached database name
	 * @param flag <code>true</code> to activate READ UNCOMMITTED isolation
	 * @throws ConnException if current connection is closed or <code>dbName</code> is not valid.
	 */
	public void setReadUncommitted(String dbName, boolean flag) throws ConnException {
		pragma(dbName, "read_uncommitted", flag);
	}

	/**
	 * Test for auto-commit mode.
	 * @return <code>true</code> if auto-commit is active.
	 * @throws ConnException if current connection is closed or <code>dbName</code> is not valid.
	 * @see <a href="https://www.sqlite.org/c3ref/get_autocommit.html">sqlite3_get_autocommit</a>
	 */
	public boolean getAutoCommit() throws ConnException {
		checkOpen();
		return sqlite3_get_autocommit(pDb); // ko if pDb is null
	}

	/**
	 * Compile an SQL statement.
	 * @param sql query
	 * @return Prepared Statement
	 * @throws ConnException if current connection is closed or an error occured during statement compilation.
	 * @see <a href="https://www.sqlite.org/c3ref/prepare.html">sqlite3_prepare_v2</a>
	 */
	public Stmt prepare(String sql, boolean cacheable) throws ConnException {
		checkOpen();
		if (cacheable) {
			final Stmt stmt = find(sql);
			if (stmt != null) {
				return stmt;
			}
		}
		final BytePointer pSql = nativeString(sql);
		sqlite3_stmt stmt = new sqlite3_stmt();
		final BytePointer tail = new BytePointer();
		final int res = sqlite3_prepare_v2(pDb, pSql, -1, stmt, tail);
		check(res, "error while preparing statement '%s'", sql);
		stmt = stmt.isNull() ? null: stmt;
		return new Stmt(this, stmt, tail, cacheable);
	}

	/**
	 * @return Run-time library version number
	 * @see <a href="https://www.sqlite.org/c3ref/libversion.html">sqlite3_libversion</a>
	 */
	public static String libversion() {
		return getString(sqlite3_libversion());
	}

	/**
	 * @return Run-time library version number
	 * @see <a href="https://www.sqlite.org/c3ref/libversion.html">sqlite3_libversion_number</a>
	 */
	public static int libversionNumber() {
		return sqlite3_libversion_number();
	}

	public Stmt prepareAndBind(String sql, boolean cacheable, Object... params) throws SQLiteException {
		Stmt s = null;
		try {
			s = prepare(sql, cacheable);
			s.bind(params);
			return s;
		} catch (SQLiteException e) {
			if (s!= null) {
				s.closeNoCheck();
			}
			throw e;
		}
	}

	/**
	 * Execute an INSERT and return the ROWID.
	 * @param sql INSERT statement
	 * @param params SQL statement parameters
	 * @return the rowid inserted.
	 * @throws SQLiteException if no row is inserted or many rows are inserted.
	 */
	public long insert(String sql, boolean cacheable, Object... params) throws SQLiteException {
		try (Stmt s = prepare(sql, cacheable)) {
			return s.insert(params);
		}
	}

	/**
	 * Execute a DML and return the number of rows impacted.
	 * @param sql DML statement
	 * @param params SQL statement parameters
	 * @return the number of database rows that were changed or inserted or deleted.
	 */
	public int execDml(String sql, boolean cacheable, Object... params) throws SQLiteException {
		try (Stmt s = prepare(sql, cacheable)) {
			return s.execDml(params);
		}
	}

	/**
	 * @param sql SELECT statement
	 * @param params Statement parameters
	 * @return returns <code>true</code> if a query in the SQL statement returns one or more rows and
	 * <code>false</code> if the SQL returns an empty set.
	 */
	public boolean exists(String sql, boolean cacheable, Object... params) throws SQLiteException {
		try (Stmt s = prepare(sql, cacheable)) {
			return s.exists(params);
		}
	}

	/**
	 * Run multiple statements of SQL.
	 * @param sql statements
	 * @throws SQLiteException if current connection is closed or an error occured during SQL execution.
	 */
	public void exec(String sql) throws SQLiteException {
		while (sql != null && !sql.isEmpty()) {
			try (Stmt s = prepare(sql, false)) {
				sql = s.getTail();
				if (!s.isDumb()) { // this happens for a comment or white-space
					s.exec();
				}
			}
		}
	}
	/**
	 * Executes one or many non-parameterized statement(s) (separated by semi-colon) with no control and no stmt cache.
	 * @param sql statements
	 * @throws ConnException if current connection is closed or an error occured during SQL execution.
	 * @see <a href="https://www.sqlite.org/c3ref/exec.html">sqlite3_exec</a>
	 */
	public void fastExec(String sql) throws ConnException {
		checkOpen();
		check(sqlite3_exec(pDb, nativeString(sql), null, null, null), "error while executing '%s'", sql);
	}

	/**
	 * Open A BLOB For Incremental I/O.
	 * @param dbName "main" or "temp" or attached database name
	 * @param tblName table name
	 * @param colName column name
	 * @param iRow row id
	 * @param rw <code>true</code> for read-write mode, <code>false</code> for read-only mode.
	 * @return BLOB
	 * @throws SQLiteException if current connection is closed or an error occured during BLOB open.
	 * @see <a href="https://www.sqlite.org/c3ref/blob_open.html">sqlite3_blob_open</a>
	 */
	public Blob open(String dbName, String tblName, String colName, long iRow, boolean rw) throws SQLiteException {
		checkOpen();
		final sqlite3_blob pBlob = new sqlite3_blob();
		final int res = sqlite3_blob_open(pDb, nativeString(dbName), nativeString(tblName), nativeString(colName), iRow, rw, pBlob); // ko if pDb is null
		final sqlite3_blob blob = pBlob.isNull() ? null : new sqlite3_blob(pBlob);
		if (res != SQLITE_OK) {
			sqlite3_blob_close(blob);
			throw new SQLiteException(this, String.format("error while opening a blob to (db: '%s', table: '%s', col: '%s', row: %d)",
					dbName, tblName, colName, iRow), res);
		}
		return new Blob(this, blob);
	}

	/**
	 * @return the number of database rows that were changed or inserted or deleted by the most recently completed SQL statement
	 * on the current database connection.
	 * @throws ConnException if current connection is closed
	 * @see <a href="https://www.sqlite.org/c3ref/changes.html">sqlite3_changes</a>
	 */
	public int getChanges() throws ConnException {
		checkOpen();
		return sqlite3_changes(pDb);
	}
	/**
	 * @return Total number of rows modified
	 * @throws ConnException if current connection is closed
	 * @see <a href="https://www.sqlite.org/c3ref/total_changes.html">sqlite3_total_changes</a>
	 */
	public int getTotalChanges() throws ConnException {
		checkOpen();
		return sqlite3_total_changes(pDb);
	}

	/**
	 * @return the rowid of the most recent successful INSERT into the database.
	 * @throws ConnException if current connection is closed
	 * @see <a href="https://www.sqlite.org/c3ref/last_insert_rowid.html">sqlite3_last_insert_rowid</a>
	 */
	public long getLastInsertRowid() throws ConnException {
		checkOpen();
		return sqlite3_last_insert_rowid(pDb);
	}

	/**
	 * Interrupt a long-running query
	 * @throws ConnException if current connection is closed
	 * @see <a href="https://www.sqlite.org/c3ref/interrupt.html">sqlite3_interrupt</a>
	 */
	public void interrupt() throws ConnException {
		checkOpen();
		sqlite3_interrupt(pDb);
	}

	/**
	 * Set a busy timeout
	 * @param ms timeout in millis
	 * @throws ConnException if current connection is closed
	 * @see <a href="https://www.sqlite.org/c3ref/busy_timeout.html">sqlite3_busy_timeout</a>
	 */
	public void setBusyTimeout(int ms) throws ConnException {
		checkOpen();
		check(sqlite3_busy_timeout(pDb, ms), "error while setting busy timeout on '%s'", getFilename());
	}
	/**
	 * Register a callback to handle SQLITE_BUSY errors
	 * @param bh Busy handler
	 * @return result code
	 * @see <a href="http://sqlite.org/c3ref/busy_handler.html">sqlite3_busy_handler</a>
	 */
	public int setBusyHandler(BusyHandler bh) throws ConnException {
		checkOpen();
		return sqlite3_busy_handler(pDb, bh, null);
	}

	/**
	 * @return the filename for the current "main" database connection
	 * @see <a href="https://www.sqlite.org/c3ref/db_filename.html">sqlite3_db_filename</a>
	 */
	public String getFilename() {
		if (pDb == null) {
			return null;
		}
		return getString(sqlite3_db_filename(pDb, nativeString("main"))); // ko if pDb is null
	}

	/**
	 * @return english-language text that describes the last error
	 * @see <a href="https://www.sqlite.org/c3ref/errcode.html">sqlite3_errmsg</a>
	 */
	public String getErrMsg() {
		return getString(sqlite3_errmsg(pDb)); // ok if pDb is null => SQLITE_NOMEM
	}

	/**
	 * @return the numeric result code or extended result code of the last API call
	 * @see ErrCodes
	 * @see <a href="https://www.sqlite.org/c3ref/errcode.html">sqlite3_errmsg</a>
	 */
	public int getErrCode() {
		return sqlite3_errcode(pDb); // ok if pDb is null => SQLITE_NOMEM
	}

	/**
	 * @param onoff enable or disable extended result codes
	 */
	public void setExtendedResultCodes(boolean onoff) throws ConnException {
		checkOpen();
		check(sqlite3_extended_result_codes(pDb, onoff), "error while enabling extended result codes on '%s'", getFilename()); // ko if pDb is null
	}

	/**
	 * @return the extended result code even when extended result codes are disabled.
	 * @see ExtErrCodes
	 * @see <a href="https://www.sqlite.org/c3ref/errcode.html">sqlite3_errmsg</a>
	 */
	public int getExtendedErrcode() {
		return sqlite3_extended_errcode(pDb); // ok if pDb is null => SQLITE_NOMEM
	}

	/**
	 * @param onoff enable or disable foreign keys constraints
	 */
	public boolean enableForeignKeys(boolean onoff) throws ConnException {
		checkOpen();
		final IntPointer pOk = new IntPointer(1);
		check(sqlite3_db_config(pDb, 1002, onoff ? 1 : 0, pOk), "error while setting db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @return whether or not foreign keys constraints enforcement is enabled
	 */
	public boolean areForeignKeysEnabled() throws ConnException {
		checkOpen();
		final IntPointer pOk = new IntPointer(1);
		check(sqlite3_db_config(pDb, 1002, -1, pOk), "error while querying db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @param onoff enable or disable triggers
	 */
	public boolean enableTriggers(boolean onoff) throws ConnException {
		checkOpen();
		final IntPointer pOk = new IntPointer(1);
		check(sqlite3_db_config(pDb, 1003, onoff ? 1 : 0, pOk), "error while setting db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @return whether or not triggers are enabled
	 */
	public boolean areTriggersEnabled() throws ConnException {
		checkOpen();
		final IntPointer pOk = new IntPointer(1);
		check(sqlite3_db_config(pDb, 1003, -1, pOk), "error while querying db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @param onoff enable or disable loading extension
	 * @see <a href="https://www.sqlite.org/c3ref/enable_load_extension.html">sqlite3_enable_load_extension</a>
	 */
	public void enableLoadExtension(boolean onoff) throws ConnException {
		checkOpen();
		check(sqlite3_enable_load_extension(pDb, onoff), "error while enabling load extension on '%s'", getFilename());
	}

	/**
	 * Load an extension.
	 * @param file path to the extension
	 * @param proc entry point (may be null)
	 * @return error message or null
	 * @see <a href="https://www.sqlite.org/c3ref/load_extension.html">sqlite3_load_extension</a>
	 */
	public String loadExtension(String file, String proc) throws ConnException {
		checkOpen();
		final BytePointer pErrMsg = new BytePointer();
		final int res = sqlite3_load_extension(pDb, nativeString(file), nativeString(proc), pErrMsg);
		if (res != SQLITE_OK) {
			final String errMsg = getString(pErrMsg);
			sqlite3_free(pErrMsg);
			return errMsg;
		}
		return null;
	}

	/**
	 * Find the current value of a limit.
	 * @param id one of the limit categories
	 * @return current limit value
	 * @throws ConnException
	 * @see <a href="https://www.sqlite.org/c3ref/limit.html">sqlite3_limit</a>
	 */
	public int getLimit(int id) throws ConnException {
		checkOpen();
		return sqlite3_limit(pDb, id, -1);
	}
	/**
	 * Change the value of a limit.
	 * @param id one of the limit categories
	 * @param newVal new limit value
	 * @return previous limit value
	 * @throws ConnException
	 * @see <a href="https://www.sqlite.org/c3ref/limit.html">sqlite3_limit</a>
	 */
	public int setLimit(int id, int newVal) throws ConnException {
		checkOpen();
		return sqlite3_limit(pDb, id, newVal);
	}

	boolean[] getTableColumnMetadata(String dbName, String tblName, String colName) throws ConnException {
		checkOpen();
		final IntPointer pNotNull = new IntPointer(1);
		final IntPointer pPrimaryKey = new IntPointer(1);
		final IntPointer pAutoinc = new IntPointer(1);

		check(sqlite3_table_column_metadata(pDb,
				nativeString(dbName),
				nativeString(tblName),
				nativeString(colName),
				null, null,
				pNotNull, pPrimaryKey, pAutoinc), "error while accessing table column metatada of '%s'", tblName);

		return new boolean[]{toBool(pNotNull), toBool(pPrimaryKey), toBool(pAutoinc)};
	}

	private static boolean toBool(IntPointer p) {
		return p.get() != 0;
	}

	/**
	 * Initialize the backup.
	 * @param dst destination database
	 * @param dstName destination database name
	 * @param src source database
	 * @param srcName source database name
	 * @return Backup
	 * @throws ConnException if backup init failed.
	 * @see <a href="https://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupinit">sqlite3_backup_init</a>
	 */
	public static Backup open(Conn dst, String dstName, Conn src, String srcName) throws ConnException {
		dst.checkOpen();
		src.checkOpen();
		final sqlite3_backup pBackup = sqlite3_backup_init(dst.pDb, nativeString(dstName), src.pDb, nativeString(srcName));
		if (pBackup == null || pBackup.isNull()) {
			throw new ConnException(dst, "backup init failed", dst.getErrCode());
		}
		return new Backup(pBackup, dst, src);
	}

	/**
	 * Sets the number of seconds the driver will wait for a statement to execute to the given number of seconds.
	 * @param timeout in seconds
	 */
	public void setQueryTimeout(int timeout) throws ConnException {
		if (timeout == 0) {
			if (timeoutProgressCallback == null) {
				return; // nothing to do
			}
		}
		if (timeoutProgressCallback == null) {
			checkOpen();
			timeoutProgressCallback = new TimeoutProgressCallback();
			sqlite3_progress_handler(pDb, 100, timeoutProgressCallback, null);
		}
		timeoutProgressCallback.setTimeout(timeout * 1000);
	}

	/**
	 * @param tc Tracing callback
	 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
	 */
	public void trace(TraceCallback tc) throws ConnException {
		checkOpen();
		sqlite3_trace(pDb, tc, null);
	}

	/**
	 * @param pc Profiling callback
	 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_profile</a>
	 */
	public void profile(ProfileCallback pc) throws ConnException {
		checkOpen();
		sqlite3_profile(pDb, pc, null);
	}

	/**
	 * @param uh Data change notification callback.
	 * @see <a href="http://sqlite.org/c3ref/update_hook.html">sqlite3_update_hook</a>
	 */
	public Pointer updateHook(UpdateHook uh) throws ConnException {
		checkOpen();
		return sqlite3_update_hook(pDb, uh, null);
	}
	/**
	 * Register an authorizer callback.
	 * @param auth Compile-time authorization callback (may be null)
	 * @return result code
	 * @see <a href="http://sqlite.org/c3ref/set_authorizer.html">sqlite3_set_authorizer</a>
	 */
	public int setAuhtorizer(Authorizer auth) throws ConnException {
		checkOpen();
		return sqlite3_set_authorizer(pDb, auth, null);
	}

	/**
	 * Create a user defined SQL scalar function.
	 * @param name function name
	 * @param nArg number of arguments expected
	 * @param flags {@link org.sqlite.FunctionFlags}.*
	 * @param xFunc function implementation
	 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
	 */
	public void createScalarFunction(String name, int nArg, int flags, ScalarCallback xFunc) throws ConnException {
		checkOpen();
		check(sqlite3_create_function_v2(pDb, nativeString(name), nArg, flags, null, xFunc, null, null, null),
				"error while registering function %s", name);
	}
	/**
	 * Create a user defined SQL aggregate function.
	 * @param name function name
	 * @param nArg number of arguments expected
	 * @param flags {@link org.sqlite.FunctionFlags}.*
	 * @param xStep function implementation
	 * @param xFinal function implementation
	 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
	 */
	public void createAggregateFunction(String name, int nArg, int flags, AggregateStepCallback xStep,
			AggregateFinalCallback xFinal) throws ConnException {
		checkOpen();
		check(sqlite3_create_function_v2(pDb, nativeString(name), nArg, flags, null, null, xStep, xFinal, null),
				"error while registering function %s", name);
	}

	/**
	 * @param dbName "main" or "temp" or attached database name
	 * @return the text encoding used by the <code>dbName</code> database
	 * @see <a href="http://sqlite.org/pragma.html#pragma_encoding">pragma encoding</a>
	 */
	public String encoding(String dbName) throws SQLiteException {
		try (Stmt s = prepare("PRAGMA " + qualify(dbName) + "encoding", false)) {
			if (!s.step(0)) {
				throw new StmtException(s, "No result", ErrCodes.WRAPPER_SPECIFIC);
			}
			return s.getColumnText(0);
		}
	}

	/**
	 * @return whether or not this database connection is closed
	 */
	public boolean isClosed() {
		return pDb == null;
	}

	/**
	 * @throws ConnException when this database connection is closed
	 */
	public void checkOpen() throws ConnException {
		if (isClosed()) {
			throw new ConnException(this, "connection closed", ErrCodes.WRAPPER_SPECIFIC);
		}
	}

	private void check(int res, String format, String param) throws ConnException {
		if (res != SQLITE_OK) {
			throw new ConnException(this, String.format(format, param), res);
		}
	}

	boolean pragma(String dbName, String name) throws SQLiteException {
		try (Stmt s = prepare("PRAGMA " + qualify(dbName) + name, false)) {
			if (!s.step(0)) {
				throw new StmtException(s, "No result", ErrCodes.WRAPPER_SPECIFIC);
			}
			return s.getColumnInt(0) == 1;
		}
	}
	void pragma(String dbName, String name, boolean value) throws ConnException {
		fastExec("PRAGMA " + qualify(dbName) + name + '=' + (value ? 1 : 0));
	}

	// To be called in Conn.prepare
	Stmt find(String sql) {
		if (maxCacheSize <= 0) {
			return null;
		}
		synchronized (cache) {
			final Iterator<Stmt> it = cache.iterator();
			while (it.hasNext()) {
				final Stmt stmt = it.next();
				if (stmt.getSql().equals(sql)) { // TODO s.SQL() may have been trimmed by SQLite
					it.remove();
					return stmt;
				}
			}
		}
		return null;
	}

	// To be called in Stmt.close
	boolean release(Stmt stmt) {
		if (maxCacheSize <= 0) {
			return false;
		}
		synchronized (cache) {
			cache.push(stmt);
			while (cache.size() > maxCacheSize) {
				cache.removeLast().close(true);
			}
		}
		return true;
	}

	/**
	 * Prepared statements cache is turned off when max size is 0
	 */
	public int getMaxCacheSize() {
		return maxCacheSize;
	}
	/**
	 * Prepared statements cache size
	 */
	public int getCacheSize() {
		return cache.size();
	}

	/**
	 * sets the size of prepared statements cache.
	 * Cache is turned off (and flushed) when size &lt;= 0
	 */
	public void setMaxCacheSize(int maxCacheSize) {
		if (maxCacheSize <= 0) {
			flush();
		}
		this.maxCacheSize = maxCacheSize;
	}
	/**
	 * Finalize and free the cached prepared statements
	 * To be called in Conn.close
	 */
	private void flush() {
		if (maxCacheSize <= 0) {
			return;
		}
		synchronized (cache) {
			final Iterator<Stmt> it = cache.iterator();
			while (it.hasNext()) {
				final Stmt stmt = it.next();
				stmt.close(true);
				it.remove();
			}
		}
	}
}
