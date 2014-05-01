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

public class Conn {
  public static final String MEMORY = ":memory:";
  public static final String TEMP_FILE = "";

  private Pointer pDb;

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
    return new Conn(ppDb.getValue());
  }

  @Override
  protected void finalize() throws Throwable {
    if (pDb != null) {
      //System.err.println("Dangling SQLite connection");
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

    // Dangling statements
    /*Pointer pStmt = SQLite.sqlite3_next_stmt(pDb, null);
    while (pStmt != null) {
      // "Dangling statement: " + SQLite.sqlite3_sql(pStmt); TODO log
      // SQLite.sqlite3_finalize(pStmt); // must be called only once...
      pStmt = SQLite.sqlite3_next_stmt(pDb, null);
    }*/

    final int res = SQLite.sqlite3_close_v2(pDb); // must be called only once...
    //if (res == SQLite.SQLITE_OK) {
    pDb = null;
    //}
    return res;
  }
  public void closeAndCheck() throws ConnException {
    final int res = close();
    if (res != ErrCodes.SQLITE_OK) {
      throw new ConnException(this, "error while closing connection", res);
    }
  }

  private Conn(Pointer pDb) {
    this.pDb = pDb;
  }

  public boolean isReadOnly() {
    return SQLite.sqlite3_db_readonly(pDb, "main") == 1;
  }

  public boolean getAutoCommit() {
    return SQLite.sqlite3_get_autocommit(pDb);
  }

  /**
   * @param sql query
   * @return Prepared Statement
   * @throws ConnException
   */
  public Stmt prepare(String sql) throws ConnException {
    checkOpen();
    final Pointer pSql = SQLite.nativeString(sql);
    final PointerByReference ppStmt = new PointerByReference();
    final PointerByReference ppTail = new PointerByReference();
    final int res = SQLite.sqlite3_prepare_v2(pDb, pSql, -1, ppStmt, ppTail); // FIXME nbytes + 1
    check(res, "error while preparing statement '%s'", sql);
    return new Stmt(this, ppStmt.getValue(), ppTail.getValue());
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

  public void exec(String sql) throws ConnException, StmtException {
    while (sql != null && sql.length() > 0) {
      Stmt s = null;
      try {
        s = prepare(sql);
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

  public Blob open(String dbName, String tblName, String colName, long iRow, boolean rw) throws SQLiteException {
    final PointerByReference ppBlob = new PointerByReference();
    final int res = SQLite.sqlite3_blob_open(pDb, dbName, tblName, colName, iRow, rw, ppBlob);
    if (res != SQLite.SQLITE_OK) {
      SQLite.sqlite3_close(ppBlob.getValue());
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
    return SQLite.sqlite3_db_filename(pDb, "main");
  }

  public String getErrMsg() {
    return SQLite.sqlite3_errmsg(pDb);
  }

  /**
   * @return org.sqlite.ErrCodes.*
   */
  public int getErrCode() {
    return SQLite.sqlite3_errcode(pDb);
  }

  /**
   * @param onoff enable or disable extended result codes
   */
  public void setExtendedResultCodes(boolean onoff) throws ConnException {
    checkOpen();
    check(SQLite.sqlite3_extended_result_codes(pDb, onoff), "error while enabling extended result codes on '%s'", getFilename());
  }

  /**
   * @return org.sqlite.ExtErrCodes.*
   */
  public int getExtendedErrcode() {
    return SQLite.sqlite3_extended_errcode(pDb);
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
   * @param onoff enable or disable loading extension
   */
  public void enableLoadExtension(boolean onoff) throws ConnException {
    checkOpen();
    check(SQLite.sqlite3_enable_load_extension(pDb, onoff), "error while enabling load extension on '%s'", getFilename());
  }

  boolean[] getTableColumnMetadata(String dbName, String tblName, String colName) throws ConnException {
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
    final Pointer pBackup = SQLite.sqlite3_backup_init(dst.pDb, dstName, src.pDb, srcName);
    if (pBackup == null) {
      throw new ConnException(dst, "backup init failed", dst.getErrCode());
    }
    return new Backup(pBackup, dst, src);
  }

  public boolean isClosed() {
    return pDb == null;
  }

  public void checkOpen() throws ConnException {
    if (isClosed()) {
      throw new ConnException(this, String.format("connection to '%s' closed", getFilename()), ErrCodes.WRAPPER_SPECIFIC);
    }
  }

  private void check(int res, String format, String param) throws ConnException {
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(this, String.format(format, param), res);
    }
  }
}
