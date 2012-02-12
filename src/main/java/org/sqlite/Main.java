package org.sqlite;

public class Main {
  public static void main(String[] args) {
    final Conn c = Conn.open("test.db",
        OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null);
    final Stmt s = c.prepare("SELECT 1, 'test'");
    int columnCount = s.getColumnCount();
    System.out.println("columnCount = " + columnCount);
/*
    for (int iCol = 0; iCol < columnCount; iCol++) {
      int colType = sqlite3_column_type(pStmt, iCol);
      System.out.println("colType[" + iCol + "] = " + colType);
    }
*/
    final boolean b = s.step();
    System.out.println("b = " + b);
    for (int iCol = 0; iCol < columnCount; iCol++) {
      int colType = s.getColumnType(iCol);
      System.out.println("colType[" + iCol + "] = " + colType);
    }
    check(s.close(), "sqlite3_finalize");
    check(c.close(), "sqlite3_close");
  }

  private static void check(int res, String name) {
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(null, String.format("Method: %s, error code: %d", name, res), res);
    }
  }
}
