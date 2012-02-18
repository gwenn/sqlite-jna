package org.sqlite;

public class Main {
  public static void main(String[] args) {
    final Conn c = Conn.open(":memory:",
        OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null);
    final Stmt s = c.prepare("SELECT 1 as num, 3.14, 'test où çà' WHERE :i = 1 OR :d > 0.0 OR :s = 't'");

    final Tuple params = new Tuple(1, 3, "t");
    s.bind(params.i, params.d, params.s);
    s.namedBind(":i", params.i, ":d", params.d, ":s", params.s);

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
      final int colType = s.getColumnType(iCol);
      final String colName = s.getColumnName(iCol);
      final Object colValue;
      if (colType == ColTypes.SQLITE_INTEGER) {
        //colValue = s.getColumnInt(iCol);
        colValue = s.getColumnLong(iCol);
      } else if (colType == ColTypes.SQLITE_TEXT) {
        colValue = s.getColumnText(iCol);
        //colValue = s.getColumnBlob(iCol);
      } else if (colType == ColTypes.SQLITE_FLOAT) {
        colValue = s.getColumnDouble(iCol);
      } else {
        colValue = null;
      }
      System.out.println("colType[" + iCol + "] = " + colType);
      System.out.println("colName[" + iCol + "] = " + colName);
      System.out.println("colValue[" + iCol + "] = " + colValue);
    }
    check(s.close(), "sqlite3_finalize");
    check(c.close(), "sqlite3_close");
  }

  private static void check(int res, String name) {
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(null, String.format("Method: %s, error code: %d", name, res), res);
    }
  }
  private static class Tuple {
    private final int i;
    private final double d;
    private final String s;

    public Tuple(int i, double d, String s) {
      this.i = i;
      this.d = d;
      this.s = s;
    }
  }
}
