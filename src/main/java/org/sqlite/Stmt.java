package org.sqlite;

import com.sun.jna.Pointer;

import java.util.Map;

public class Stmt {
  final Conn c;
  private Pointer pStmt;
  private final String tail;
  // cached columns index by name
  private Map<String, Integer> cols;
  // cached parameters index by name
  private Map<String, Integer> params;

  Stmt(Conn c, Pointer pStmt, String tail) {
    this.c = c;
    this.pStmt = pStmt;
    this.tail = tail;
  }

  /**
   * @return result code (No exception is thrown).
   */
  public int close() {
    final int res = SQLite.sqlite3_finalize(pStmt);
    if (res == SQLite.SQLITE_OK) {
      pStmt = null;
    }
    return res;
  }

  /**
   * @return true until finished.
   * @throws StmtException
   */
  public boolean step() {
    final int res = SQLite.sqlite3_step(pStmt);
    if (res == SQLite.SQLITE_ROW) {
      return true;
    } else if (res == SQLite.SQLITE_DONE) {
      return false;
    }
    throw new StmtException(this, "", res);
  }

  /**
   * @return column count
   * @throws StmtException
   */
  public int getColumnCount() {
    checkOpen();
    return SQLite.sqlite3_column_count(pStmt);
  }

  /**
   * @param iCol The leftmost column is number 0.
   * @return org.sqlite.ColumnTypes.* (TODO Enum?)
   * @throws StmtException
   */
  public int getColumnType(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_type(pStmt, iCol);
  }

  private void checkOpen() {
    if (pStmt == null) {
      throw new StmtException(this, "Stmt finalized", ErrCodes.SQLITE_MISUSE);
    }
  }
}
