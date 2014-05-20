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
import com.sun.jna.ptr.PointerByReference;

import java.util.Iterator;
import java.util.LinkedList;

import static org.sqlite.SQLite.qualify;

public class Conn {
  public static final String MEMORY = ":memory:";
  public static final String TEMP_FILE = "";

  private Pointer pDb;
  private final boolean sharedCacheMode;
  private TimeoutProgressCallback timeoutProgressCallback;

  private final LinkedList<Stmt> cache = new LinkedList<>();
  private int maxCacheSize = 100; // TODO parameterize

  /**
   * @param filename ":memory:" for memory db, "" for temp file db
   * @param flags    org.sqlite.OpenFlags.* (TODO EnumSet or BitSet, default flags)
   * @param vfs      may be null
   * @return Opened Connection
   * @throws SQLiteException
   */
  public static Conn open(String filename, int flags, String vfs) throws SQLiteException {
    if (!SQLite.sqlite3_threadsafe()) {
      throw new SQLiteException("sqlite library was not compiled for thread-safe operation", ErrCodes.WRAPPER_SPECIFIC);
    }
    final PointerByReference ppDb = new PointerByReference();
    final int res = SQLite.sqlite3_open_v2(filename, ppDb, flags, vfs);
    if (res != SQLite.SQLITE_OK) {
      if (ppDb.getValue() != null) {
        SQLite.sqlite3_close(ppDb.getValue());
      }
      throw new SQLiteException(String.format("error while opening a database connection to '%s'", filename), res);
    }
    // TODO not reliable (and may depend on sqlite3_enable_shared_cache global status)
    boolean sharedCacheMode = filename.contains("cache=shared") || (flags & OpenFlags.SQLITE_OPEN_SHAREDCACHE) != 0;
    return new Conn(ppDb.getValue(), sharedCacheMode);
  }

  @Override
  protected void finalize() throws Throwable {
    if (pDb != null) {
      SQLite.sqlite3_log(-1, "dangling SQLite connection.");
      close();
    }
    super.finalize();
  }
  /**
   * @return result code (No exception is thrown).
   */
  public int close() {
    if (pDb == null) {
      return SQLite.SQLITE_OK;
    }

    flush();
    final int res = SQLite.sqlite3_close_v2(pDb); // must be called only once...
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
    final int res = SQLite.sqlite3_db_readonly(pDb, dbName); // ko if pDb is null
    if (res < 0) {
      throw new ConnException(this, String.format("'%s' is not the name of a database", dbName), ErrCodes.WRAPPER_SPECIFIC);
    }
    return res == 1;
  }

  public boolean isQueryOnly(String dbName) throws SQLiteException {
    return pragma(dbName, "query_only");
  }

  public void setQueryOnly(String dbName, boolean queryOnly) throws ConnException {
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
    return SQLite.sqlite3_get_autocommit(pDb); // ko if pDb is null
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
    final Pointer pSql = SQLite.nativeString(sql);
    final PointerByReference ppStmt = new PointerByReference();
    final PointerByReference ppTail = new PointerByReference();
    final int res = SQLite.sqlite3_prepare_v2(pDb, pSql, -1, ppStmt, ppTail); // FIXME nbytes + 1
    check(res, "error while preparing statement '%s'", sql);
    return new Stmt(this, ppStmt.getValue(), ppTail.getValue(), cacheable);
  }

  /**
   * @return Run-time library version number
   */
  public static String libversion() {
    return SQLite.sqlite3_libversion();
  }

  public static String mprintf(String format, String arg) {
    final Pointer p = SQLite.sqlite3_mprintf(format, arg);
    final String s = p.getString(0);
    SQLite.sqlite3_free(p);
    return s;
  }

  public void exec(String sql) throws SQLiteException {
    while (sql != null && sql.length() > 0) {
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
    check(SQLite.sqlite3_exec(pDb, sql, null, null, null), "error while executing '%s'", sql);
  }

  public Blob open(String dbName, String tblName, String colName, long iRow, boolean rw) throws SQLiteException {
    checkOpen();
    final PointerByReference ppBlob = new PointerByReference();
    final int res = SQLite.sqlite3_blob_open(pDb, dbName, tblName, colName, iRow, rw, ppBlob); // ko if pDb is null
    if (res != SQLite.SQLITE_OK) {
      SQLite.sqlite3_blob_close(ppBlob.getValue());
      throw new SQLiteException(String.format("error while opening a blob to (db: '%s', table: '%s', col: '%s', row: %d)",
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
    return SQLite.sqlite3_changes(pDb);
  }
  /**
   * @return Total number of rows modified
   * @throws ConnException
   */
  public int getTotalChanges() throws ConnException {
    checkOpen();
    return SQLite.sqlite3_total_changes(pDb);
  }

  /**
   * @return the rowid of the most recent successful INSERT into the database.
   */
  public long getLastInsertRowid() throws ConnException {
    checkOpen();
    return SQLite.sqlite3_last_insert_rowid(pDb);
  }

  /**
   * Interrupt a long-running query
   */
  public void interrupt() throws ConnException {
    checkOpen();
    SQLite.sqlite3_interrupt(pDb);
  }

  public void setBusyTimeout(int ms) throws ConnException {
    checkOpen();
    check(SQLite.sqlite3_busy_timeout(pDb, ms), "error while setting busy timeout on '%s'", getFilename());
  }

  public String getFilename() {
    if (pDb == null) {
      return null;
    }
    return SQLite.sqlite3_db_filename(pDb, "main"); // ko if pDb is null
  }

  public String getErrMsg() {
    return SQLite.sqlite3_errmsg(pDb); // ok if pDb is null => SQLITE_NOMEM
  }

  /**
   * @return org.sqlite.ErrCodes.*
   */
  public int getErrCode() {
    return SQLite.sqlite3_errcode(pDb); // ok if pDb is null => SQLITE_NOMEM
  }

  /**
   * @param onoff enable or disable extended result codes
   */
  public void setExtendedResultCodes(boolean onoff) throws ConnException {
    checkOpen();
    check(SQLite.sqlite3_extended_result_codes(pDb, onoff), "error while enabling extended result codes on '%s'", getFilename()); // ko if pDb is null
  }

  /**
   * @return org.sqlite.ExtErrCodes.*
   */
  public int getExtendedErrcode() {
    return SQLite.sqlite3_extended_errcode(pDb); // ok if pDb is null => SQLITE_NOMEM
  }

  /**
   * @param onoff enable or disable foreign keys constraints
   */
  public boolean enableForeignKeys(boolean onoff) throws ConnException {
    checkOpen();
    final PointerByReference pOk = new PointerByReference();
    check(SQLite.sqlite3_db_config(pDb, 1002, onoff ? 1 : 0, pOk), "error while setting db config on '%s'", getFilename());
    return toBool(pOk);
  }
  /**
   * @return whether or not foreign keys constraints enforcement is enabled
   */
  public boolean areForeignKeysEnabled() throws ConnException {
    checkOpen();
    final PointerByReference pOk = new PointerByReference();
    check(SQLite.sqlite3_db_config(pDb, 1002, -1, pOk), "error while querying db config on '%s'", getFilename());
    return toBool(pOk);
  }
  /**
   * @param onoff enable or disable triggers
   */
  public boolean enableTriggers(boolean onoff) throws ConnException {
    checkOpen();
    final PointerByReference pOk = new PointerByReference();
    check(SQLite.sqlite3_db_config(pDb, 1003, onoff ? 1 : 0, pOk), "error while setting db config on '%s'", getFilename());
    return toBool(pOk);
  }
  /**
   * @return whether or not triggers are enabled
   */
  public boolean areTriggersEnabled() throws ConnException {
    checkOpen();
    final PointerByReference pOk = new PointerByReference();
    check(SQLite.sqlite3_db_config(pDb, 1003, -1, pOk), "error while querying db config on '%s'", getFilename());
    return toBool(pOk);
  }
  /**
   * @param onoff enable or disable loading extension
   */
  public void enableLoadExtension(boolean onoff) throws ConnException {
    checkOpen();
    check(SQLite.sqlite3_enable_load_extension(pDb, onoff), "error while enabling load extension on '%s'", getFilename());
  }

  /**
   * @param file path to the extension
   * @param proc entry point (may be null)
   * @return error message  or null
   */
  public String loadExtension(String file, String proc) throws ConnException {
    checkOpen();
    final PointerByReference pErrMsg = new PointerByReference();
    int res = SQLite.sqlite3_load_extension(pDb, file, proc, pErrMsg);
    if (res != SQLite.SQLITE_OK) {
      return pErrMsg.getValue().getString(0);
    }
    return null;
  }

  public int getLimit(int id) throws ConnException {
    checkOpen();
    return SQLite.sqlite3_limit(pDb, id, -1);
  }
  public int setLimit(int id, int newVal) throws ConnException {
    checkOpen();
    return SQLite.sqlite3_limit(pDb, id, newVal);
  }

  boolean[] getTableColumnMetadata(String dbName, String tblName, String colName) throws ConnException {
    checkOpen();
    final PointerByReference pNotNull = new PointerByReference();
    final PointerByReference pPrimaryKey = new PointerByReference();
    final PointerByReference pAutoinc = new PointerByReference();

    check(SQLite.sqlite3_table_column_metadata(pDb,
        dbName,
        tblName,
        colName,
        null, null,
        pNotNull, pPrimaryKey, pAutoinc), "error while accessing table column metatada of '%s'", tblName);

    return new boolean[]{toBool(pNotNull), toBool(pPrimaryKey), toBool(pAutoinc)};
  }

  private static boolean toBool(PointerByReference p) {
    return p.getPointer().getInt(0) > 0;
  }

  public static Backup open(Conn dst, String dstName, Conn src, String srcName) throws ConnException {
    dst.checkOpen();
    src.checkOpen();
    final Pointer pBackup = SQLite.sqlite3_backup_init(dst.pDb, dstName, src.pDb, srcName);
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
      SQLite.sqlite3_progress_handler(pDb, 100, timeoutProgressCallback, null);
    }
    timeoutProgressCallback.setTimeout(timeout * 1000);
  }

  public void trace(TraceCallback tc, Pointer arg) throws ConnException {
    checkOpen();
    SQLite.sqlite3_trace(pDb, tc, arg);
  }

  public void createScalarFunction(String name, int nArg, ScalarCallback xFunc) throws ConnException {
    checkOpen();
    // TODO SQLITE_DETERMINISTIC
    // TODO SQLITE_UTF8 versus SQLITE_UTF16LE
    SQLite.sqlite3_create_function_v2(pDb, name, nArg, 1, null, xFunc, null, null, null);
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
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(this, String.format(format, param), res);
    }
  }

  private boolean pragma(String dbName, String name) throws SQLiteException {
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
  private void pragma(String dbName, String name, boolean value) throws ConnException {
    fastExec("PRAGMA " + qualify(dbName) + name + "=" + (value ? 1 : 0));
  }

  // To be called in Conn.prepare
  Stmt find(String sql) {
    if (maxCacheSize <= 0) {
      return null;
    }
    synchronized (this) {
      final Iterator<Stmt> it = cache.iterator();
      while (it.hasNext()) {
        Stmt stmt = it.next();
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
    synchronized (this) {
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

  /** sets the size of prepared statements cache.
   * Cache is turned off (and flushed) when size <= 0
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
    synchronized (this) {
      final Iterator<Stmt> it = cache.iterator();
      while (it.hasNext()) {
        Stmt stmt = it.next();
        stmt.close(true);
        it.remove();
      }
    }
  }
}
