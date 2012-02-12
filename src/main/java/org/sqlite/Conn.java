package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public class Conn {
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
      throw new SQLiteException("sqlite library was not compiled for thread-safe operation", ErrCodes.SQLITE_ERROR);
    }
    final PointerByReference ppDb = new PointerByReference();
    final int res = SQLite.sqlite3_open_v2(filename, ppDb, flags, vfs);
    if (res != SQLite.SQLITE_OK) {
      throw new SQLiteException("sqlite3_open_v2", res);
    }
    return new Conn(filename, ppDb.getValue());
  }

  /**
   * @return result code (No exception is thrown).
   */
  public int close() {
    // TODO Close dangling statements.
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
    final int res = SQLite.sqlite3_prepare_v2(pDb, sql, -1, ppStmt, null); // TODO Tail handling
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(this, "sqlite3_prepare_v2", res);
    }
    return new Stmt(this, ppStmt.getValue(), null); // TODO Tail handling
  }

  private void checkOpen() {
    if (pDb == null) {
      throw new ConnException(this, "Connection closed", ErrCodes.SQLITE_MISUSE);
    }
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
}
