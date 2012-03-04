package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class Conn {
  public static final String MEMORY = ":memory:";
  public static final String TEMP_FILE = "";

  private final String filename;
  private Pointer pDb;

  /**
   * @return Run-time library version number
   */
  public static String libversion() {
    return SQLite.sqlite3_libversion();
  }

  /**
   * @param filename ":memory:" for memory db, "" for temp file db
   * @param flags org.sqlite.OpenFlags.* (TODO EnumSet or BitSet, default flags)
   * @param vfs may be null
   * @return Opened Connection
   * @throws SQLiteException
   */
  public static Conn open(String filename, int flags, String vfs) {
    if (!SQLite.sqlite3_threadsafe()) {
      throw new SQLiteException("sqlite library was not compiled for thread-safe operation", ErrCodes.WRAPPER_SPECIFIC);
    }
    final PointerByReference ppDb = new PointerByReference();
    final int res = SQLite.sqlite3_open_v2(filename, ppDb, flags, vfs);
    if (res != SQLite.SQLITE_OK) {
      if (ppDb.getValue() != null) {
        SQLite.sqlite3_close(ppDb.getValue());
      }
      throw new SQLiteException(String.format("error while opening a database connexion to '%s'", filename), res);
    }
    return new Conn(filename, ppDb.getValue());
  }

  /**
   * @return result code (No exception is thrown).
   */
  public int close() {
    if (pDb == null) {
      return SQLite.SQLITE_OK;
    }

    // Dangling statements
    Pointer pStmt = SQLite.sqlite3_next_stmt(pDb, null);
    while (pStmt != null) {
      // SQLITE_MISUSE, "Dangling statement" TODO log
      SQLite.sqlite3_finalize(pStmt);
      pStmt = SQLite.sqlite3_next_stmt(pDb, pStmt);
    }

    final int res = SQLite.sqlite3_close(pDb);
    if (res == SQLite.SQLITE_OK) {
      pDb = null;
    }
    return res;
  }

  private Conn(String filename, Pointer pDb) {
    this.filename = filename;
    this.pDb = pDb;
  }

  /**
   * @param sql query
   * @return Prepared Statement
   * @throws ConnException
   */
  public Stmt prepare(String sql) {
    checkOpen();
    final PointerByReference ppStmt = new PointerByReference();
    final PointerByReference ppTail = new PointerByReference();
    final int res = SQLite.sqlite3_prepare_v2(pDb, sql, -1, ppStmt, ppTail);
    check(res, "Error while preparing statement '%s'", sql);
    return new Stmt(this, ppStmt.getValue(), ppTail.getValue());
  }

  public void exec(String sql) {
    while (sql != null && sql.length() > 0) {
      Stmt s = null;
      try {
        s = prepare(sql);
        sql = s.getTail();
        s.exec();
      } finally {
        if (s != null) {
          check(s.close(), "Error while closing statement");
        }
      }
    }
  }

  /**
   * @return the number of database rows that were changed or inserted or deleted by the most recently completed SQL statement
   * on the database connection specified by the first parameter.
   * @throws ConnException
   */
  public int getChanges() {
    checkOpen();
    return SQLite.sqlite3_changes(pDb);
  }
  /**
   * @return Total number of rows modified
   * @throws ConnException
   */
  public int getTotalChanges() {
    checkOpen();
    return SQLite.sqlite3_total_changes(pDb);
  }

  /**
   * @return the rowid of the most recent successful INSERT into the database.
   */
  public long getLastInsertRowid() {
    checkOpen();
    return SQLite.sqlite3_last_insert_rowid(pDb);
  }

  /**
   * Interrupt a long-running query
   */
  public void interrupt() {
    checkOpen();
    SQLite.sqlite3_interrupt(pDb);
  }

  public void setBusyTimeout(int ms) {
    checkOpen();
    check(SQLite.sqlite3_busy_timeout(pDb, ms), "Error while setting busy timeout on '%s'", filename);
  }

  public String getFilename() {
    return filename;
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
   * @param onoff Enable Or Disable Extended Result Codes
   */
  public void setExtendedResultCodes(boolean onoff) {
    checkOpen();
    check(SQLite.sqlite3_extended_result_codes(pDb, onoff), "Error while enabling extended result codes on '%s'", filename);
  }

  /**
   * @return org.sqlite.ExtErrCodes.*
   */
  public int getExtendedErrcode() {
    return SQLite.sqlite3_extended_errcode(pDb);
  }

  boolean[] getTableColumnMetadata(String dbName, String tblName, String colName) {
    final PointerByReference pNotNull = new PointerByReference();
    final PointerByReference pPrimaryKey = new PointerByReference();
    final PointerByReference pAutoinc = new PointerByReference();

    check(SQLite.sqlite3_table_column_metadata(pDb,
        dbName,
        tblName,
        colName,
        null, null,
        pNotNull, pPrimaryKey, pAutoinc), "Error while accessing table column metatada of '%s'", tblName);

    return new boolean[] {toBool(pNotNull), toBool(pPrimaryKey), toBool(pAutoinc)};
  }
  private static boolean toBool(PointerByReference p) {
    return p.getPointer().getInt(0) > 0;
  }

  private void checkOpen() {
    if (pDb == null) {
      throw new ConnException(this, String.format("Connection to '%s' closed", filename), ErrCodes.WRAPPER_SPECIFIC);
    }
  }
  private void check(int res, String format, String param) {
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(this, String.format(format, param), res);
    }
  }
  private void check(int res, String msg) {
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(this, msg, res);
    }
  }
}
