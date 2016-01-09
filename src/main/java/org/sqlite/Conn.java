/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import static org.sqlite.SQLite.*;

public final class Conn {
	public static final String MEMORY = ":memory:";
	public static final String TEMP_FILE = "";

	private Pointer pDb;
	private final boolean sharedCacheMode;
	private TimeoutProgressCallback timeoutProgressCallback;

	private final LinkedList<Stmt> cache = new LinkedList<Stmt>();
	private int maxCacheSize = 100; // TODO parameterize

	/**
	 * When the `filename` is an URI, extra query parameters can be used ({@link OpenQueryParameter})
	 * @param filename ":memory:" for memory db, "" for temp file db
	 * @param flags    org.sqlite.OpenFlags.* (TODO EnumSet or BitSet, default flags)
	 * @param vfs      may be null
	 * @return Opened Connection
	 * @throws SQLiteException
	 */
	public static Conn open(String filename, int flags, String vfs) throws SQLiteException {
		if (!sqlite3_threadsafe()) {
			throw new SQLiteException("sqlite library was not compiled for thread-safe operation", ErrCodes.WRAPPER_SPECIFIC);
		}
		final PointerByReference ppDb = new PointerByReference();
		final int res = sqlite3_open_v2(filename, ppDb, flags, vfs);
		if (res != SQLITE_OK) {
			if (ppDb.getValue() != null) {
				sqlite3_close(ppDb.getValue());
			}
			throw new SQLiteException(String.format("error while opening a database connection to '%s'", filename), res);
		}
		final boolean uri = (flags & OpenFlags.SQLITE_OPEN_URI) != 0;
		final Map<String, String> queryParams = uri ? OpenQueryParameter.getQueryParams(filename) : Collections.<String, String>emptyMap();
		// TODO not reliable (and may depend on sqlite3_enable_shared_cache global status)
		final boolean sharedCacheMode = "shared".equals(queryParams.get("cache")) || (flags & OpenFlags.SQLITE_OPEN_SHAREDCACHE) != 0;
		final Conn conn = new Conn(ppDb.getValue(), sharedCacheMode);
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
			sqlite3_log(-1, "dangling SQLite connection.");
			close();
		}
		super.finalize();
	}
	/**
	 * @return result code (No exception is thrown).
	 */
	public int close() {
		if (pDb == null) {
			return SQLITE_OK;
		}

		flush();

		// Dangling statements
		Pointer stmt = sqlite3_next_stmt(pDb, null);
		while (stmt != null) {
			if (sqlite3_stmt_busy(stmt)) {
				sqlite3_log(ErrCodes.SQLITE_MISUSE, "Dangling statement (not reset): \"" + sqlite3_sql(stmt) + "\"");
			} else {
				sqlite3_log(ErrCodes.SQLITE_MISUSE, "Dangling statement (not finalize): \"" + sqlite3_sql(stmt) + "\"");
			}
			stmt = sqlite3_next_stmt(pDb, stmt);
		}
		final int res = sqlite3_close_v2(pDb); // must be called only once...
		pDb = null;
		return res;
	}
	public void closeAndCheck() throws ConnException {
		final int res = close();
		if (res != ErrCodes.SQLITE_OK) {
			throw new ConnException(this, "error while closing connection", res);
		}
	}

	private Conn(Pointer pDb, boolean sharedCacheMode) {
		assert pDb != null;
		this.pDb = pDb;
		this.sharedCacheMode = sharedCacheMode;
	}

	public boolean isReadOnly(String dbName) throws ConnException {
		checkOpen();
		final int res = sqlite3_db_readonly(pDb, dbName); // ko if pDb is null
		if (res < 0) {
			throw new ConnException(this, String.format("'%s' is not the name of a database", dbName), ErrCodes.WRAPPER_SPECIFIC);
		}
		return res == 1;
	}

	public boolean isQueryOnly(String dbName) throws SQLiteException { // since 3.8.0
		return pragma(dbName, "query_only");
	}

	public void setQueryOnly(String dbName, boolean queryOnly) throws ConnException { // since 3.8.0
		pragma(dbName, "query_only", queryOnly);
	}

	public boolean isSharedCacheMode() {
		return sharedCacheMode;
	}

	public boolean getReadUncommitted(String dbName) throws SQLiteException {
		return pragma(dbName, "read_uncommitted");
	}
	public void setReadUncommitted(String dbName, boolean flag) throws SQLiteException {
		pragma(dbName, "read_uncommitted", flag);
	}

	public boolean getAutoCommit() throws ConnException {
		checkOpen();
		return sqlite3_get_autocommit(pDb); // ko if pDb is null
	}

	/**
	 * @param sql query
	 * @return Prepared Statement
	 * @throws ConnException
	 */
	public Stmt prepare(String sql, boolean cacheable) throws ConnException {
		checkOpen();
		if (cacheable) {
			final Stmt stmt = find(sql);
			if (stmt != null) {
				return stmt;
			}
		}
		final Pointer pSql = nativeString(sql);
		final PointerByReference ppStmt = new PointerByReference();
		final PointerByReference ppTail = new PointerByReference();
		final int res = sqlite3_prepare_v2(pDb, pSql, -1, ppStmt, ppTail);
		check(res, "error while preparing statement '%s'", sql);
		return new Stmt(this, ppStmt.getValue(), ppTail.getValue(), cacheable);
	}

	/**
	 * @return Run-time library version number
	 */
	public static String libversion() {
		return sqlite3_libversion();
	}

	/**
	 * @return Run-time library version number
	 */
	public static int libversionNumber() {
		return sqlite3_libversion_number();
	}

	public void exec(String sql) throws SQLiteException {
		while (sql != null && !sql.isEmpty()) {
			Stmt s = null;
			try {
				s = prepare(sql, false);
				sql = s.getTail();
				if (!s.isDumb()) { // this happens for a comment or white-space
					s.exec();
				}
			} finally {
				if (s != null) {
					s.close();
				}
			}
		}
	}
	// FastExec executes one or many non-parameterized statement(s) (separated by semi-colon) with no control and no stmt cache.
	public void fastExec(String sql) throws ConnException {
		checkOpen();
		check(sqlite3_exec(pDb, sql, null, null, null), "error while executing '%s'", sql);
	}

	public Blob open(String dbName, String tblName, String colName, long iRow, boolean rw) throws SQLiteException {
		checkOpen();
		final PointerByReference ppBlob = new PointerByReference();
		final int res = sqlite3_blob_open(pDb, dbName, tblName, colName, iRow, rw, ppBlob); // ko if pDb is null
		if (res != SQLITE_OK) {
			sqlite3_blob_close(ppBlob.getValue());
			throw new SQLiteException(this, String.format("error while opening a blob to (db: '%s', table: '%s', col: '%s', row: %d)",
					dbName, tblName, colName, iRow), res);
		}
		return new Blob(this, ppBlob.getValue());
	}

	/**
	 * @return the number of database rows that were changed or inserted or deleted by the most recently completed SQL statement
	 * on the database connection specified by the first parameter.
	 * @throws ConnException
	 */
	public int getChanges() throws ConnException {
		checkOpen();
		return sqlite3_changes(pDb);
	}
	/**
	 * @return Total number of rows modified
	 * @throws ConnException
	 */
	public int getTotalChanges() throws ConnException {
		checkOpen();
		return sqlite3_total_changes(pDb);
	}

	/**
	 * @return the rowid of the most recent successful INSERT into the database.
	 */
	public long getLastInsertRowid() throws ConnException {
		checkOpen();
		return sqlite3_last_insert_rowid(pDb);
	}

	/**
	 * Interrupt a long-running query
	 */
	public void interrupt() throws ConnException {
		checkOpen();
		sqlite3_interrupt(pDb);
	}

	public void setBusyTimeout(int ms) throws ConnException {
		checkOpen();
		check(sqlite3_busy_timeout(pDb, ms), "error while setting busy timeout on '%s'", getFilename());
	}

	public String getFilename() {
		if (pDb == null) {
			return null;
		}
		return sqlite3_db_filename(pDb, "main"); // ko if pDb is null
	}

	public String getErrMsg() {
		return sqlite3_errmsg(pDb); // ok if pDb is null => SQLITE_NOMEM
	}

	/**
	 * @return org.sqlite.ErrCodes.*
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
	 * @return org.sqlite.ExtErrCodes.*
	 */
	public int getExtendedErrcode() {
		return sqlite3_extended_errcode(pDb); // ok if pDb is null => SQLITE_NOMEM
	}

	/**
	 * @param onoff enable or disable foreign keys constraints
	 */
	public boolean enableForeignKeys(boolean onoff) throws ConnException {
		checkOpen();
		final IntByReference pOk = new IntByReference();
		check(sqlite3_db_config(pDb, 1002, onoff ? 1 : 0, pOk), "error while setting db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @return whether or not foreign keys constraints enforcement is enabled
	 */
	public boolean areForeignKeysEnabled() throws ConnException {
		checkOpen();
		final IntByReference pOk = new IntByReference();
		check(sqlite3_db_config(pDb, 1002, -1, pOk), "error while querying db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @param onoff enable or disable triggers
	 */
	public boolean enableTriggers(boolean onoff) throws ConnException {
		checkOpen();
		final IntByReference pOk = new IntByReference();
		check(sqlite3_db_config(pDb, 1003, onoff ? 1 : 0, pOk), "error while setting db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @return whether or not triggers are enabled
	 */
	public boolean areTriggersEnabled() throws ConnException {
		checkOpen();
		final IntByReference pOk = new IntByReference();
		check(sqlite3_db_config(pDb, 1003, -1, pOk), "error while querying db config on '%s'", getFilename());
		return toBool(pOk);
	}
	/**
	 * @param onoff enable or disable loading extension
	 */
	public void enableLoadExtension(boolean onoff) throws ConnException {
		checkOpen();
		check(sqlite3_enable_load_extension(pDb, onoff), "error while enabling load extension on '%s'", getFilename());
	}

	/**
	 * @param file path to the extension
	 * @param proc entry point (may be null)
	 * @return error message  or null
	 */
	public String loadExtension(String file, String proc) throws ConnException {
		checkOpen();
		final PointerByReference pErrMsg = new PointerByReference();
		final int res = sqlite3_load_extension(pDb, file, proc, pErrMsg);
		if (res != SQLITE_OK) {
			final String errMsg = pErrMsg.getValue().getString(0L, UTF_8_ECONDING);
			sqlite3_free(pErrMsg.getValue());
			return errMsg;
		}
		return null;
	}

	public int getLimit(int id) throws ConnException {
		checkOpen();
		return sqlite3_limit(pDb, id, -1);
	}
	public int setLimit(int id, int newVal) throws ConnException {
		checkOpen();
		return sqlite3_limit(pDb, id, newVal);
	}

	boolean[] getTableColumnMetadata(String dbName, String tblName, String colName) throws ConnException {
		checkOpen();
		final IntByReference pNotNull = new IntByReference();
		final IntByReference pPrimaryKey = new IntByReference();
		final IntByReference pAutoinc = new IntByReference();

		check(sqlite3_table_column_metadata(pDb,
				dbName,
				tblName,
				colName,
				null, null,
				pNotNull, pPrimaryKey, pAutoinc), "error while accessing table column metatada of '%s'", tblName);

		return new boolean[]{toBool(pNotNull), toBool(pPrimaryKey), toBool(pAutoinc)};
	}

	private static boolean toBool(IntByReference p) {
		return p.getValue() != 0;
	}

	public static Backup open(Conn dst, String dstName, Conn src, String srcName) throws ConnException {
		dst.checkOpen();
		src.checkOpen();
		final Pointer pBackup = sqlite3_backup_init(dst.pDb, dstName, src.pDb, srcName);
		if (pBackup == null) {
			throw new ConnException(dst, "backup init failed", dst.getErrCode());
		}
		return new Backup(pBackup, dst, src);
	}

	/**
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

	public void trace(TraceCallback tc, Pointer arg) throws ConnException {
		checkOpen();
		sqlite3_trace(pDb, tc, arg);
	}

	public void profile(ProfileCallback pc, Pointer arg) throws ConnException {
		checkOpen();
		sqlite3_profile(pDb, pc, arg);
	}

	public Pointer updateHook(UpdateHook uh, Pointer arg) throws ConnException {
		checkOpen();
		return sqlite3_update_hook(pDb, uh, arg);
	}

	/**
	 * @param flags org.sqlite.FunctionFlags.*
	 */
	public void createScalarFunction(String name, int nArg, int flags, ScalarCallback xFunc) throws ConnException {
		checkOpen();
		check(sqlite3_create_function_v2(pDb, name, nArg, flags, null, xFunc, null, null, null),
				"error while registering function %s", name);
	}

	public String encoding(String dbName) throws SQLiteException {
		Stmt s = null;
		try {
			s = prepare("PRAGMA " + qualify(dbName) + "encoding", false);
			if (!s.step(0)) {
				throw new StmtException(s, "No result", ErrCodes.WRAPPER_SPECIFIC);
			}
			return s.getColumnText(0);
		} finally {
			if (s != null) {
				s.close();
			}
		}
	}

	public boolean isClosed() {
		return pDb == null;
	}

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
		Stmt s = null;
		try {
			s = prepare("PRAGMA " + qualify(dbName) + name, false);
			if (!s.step(0)) {
				throw new StmtException(s, "No result", ErrCodes.WRAPPER_SPECIFIC);
			}
			return s.getColumnInt(0) == 1;
		} finally {
			if (s != null) {
				s.close();
			}
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
