package org.sqlite;

import com.sun.jna.Pointer;

import java.util.HashMap;
import java.util.Map;

public class Stmt {
  final Conn c;
  private Pointer pStmt;
  private final String tail;
  // cached columns index by name
  private Map<String, Integer> cols;
  // cached parameters index by name
  private Map<String, Integer> params;

  Stmt(Conn c, Pointer pStmt, Pointer tail) {
    this.c = c;
    this.pStmt = pStmt;
    this.tail = tail.getString(0);
  }

  public String getSql() {
    return SQLite.sqlite3_sql(pStmt);
  }

  public String getTail() {
    return tail;
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
    throw new StmtException(this, String.format("Error while stepping '%s'", getSql()), res);
  }
  public void exec() {
    final int res = SQLite.sqlite3_step(pStmt);
    if (res != SQLite.SQLITE_DONE) {
      throw new StmtException(this, String.format("Error while executing '%s'", getSql()), res);
    }
  }

  public void reset() {
    checkOpen();
    check(SQLite.sqlite3_reset(pStmt), "Error while resetting '%s'");
  }

  public boolean isBusy() {
    checkOpen();
    return SQLite.sqlite3_stmt_busy(pStmt);
  }

  public void clearBindings() {
    checkOpen();
    check(SQLite.sqlite3_clear_bindings(pStmt), "Error while clearing bindings '%s'");
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
   * @return data count
   * @throws StmtException
   */
  public int getDataCount() {
    checkOpen();
    return SQLite.sqlite3_data_count(pStmt);
  }

  /**
   * @param iCol The leftmost column is number 0.
   * @return org.sqlite.ColTypes.*
   * @throws StmtException
   */
  public int getColumnType(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_type(pStmt, iCol);
  }

  /**
   * @param iCol The leftmost column is number 0.
   * @return Column name
   * @throws StmtException
   */
  public String getColumnName(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_name(pStmt, iCol);
  }

  public byte[] getColumnBlob(int iCol) {
    checkOpen();
    final Pointer p = SQLite.sqlite3_column_blob(pStmt, iCol);
    return p.getByteArray(0, getColumnBytes(iCol));
  }

  /**
   * @param iCol The leftmost column is number 0.
   * @return the number of bytes in that BLOB or string.
   * @throws StmtException
   */
  public int getColumnBytes(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_bytes(pStmt, iCol);
  }

  /**
   * @param iCol The leftmost column is number 0.
   * @return double value
   * @throws StmtException
   */
  public double getColumnDouble(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_double(pStmt, iCol);
  }
  /**
   * @param iCol The leftmost column is number 0.
   * @return int value
   * @throws StmtException
   */
  public int getColumnInt(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_int(pStmt, iCol);
  }
  /**
   * @param iCol The leftmost column is number 0.
   * @return long value
   * @throws StmtException
   */
  public long getColumnLong(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_int64(pStmt, iCol);
  }
  /**
   * @param iCol The leftmost column is number 0.
   * @return text value
   * @throws StmtException
   */
  public String getColumnText(int iCol) {
    checkOpen();
    return SQLite.sqlite3_column_text(pStmt, iCol);
  }

  public void bind(Object ...params) {
    reset();
    if (params.length != getBindParameterCount()) {
      throw new StmtException(this,
          String.format("Incorrect argument count for Stmt.bind: have %d want %d", params.length, getBindParameterCount()),
          ErrCodes.WRAPPER_SPECIFIC);
    }
    for (int i = 0; i < params.length; i++) {
      bindByIndex(i + 1, params[i]);
    }
  }

  public void namedBind(Object ...params) {
    reset();
    if (params.length % 2 != 0) {
      throw new StmtException(this, "Expected an even number of arguments", ErrCodes.WRAPPER_SPECIFIC);
    }
    for (int i = 0; i < params.length; i += 2) {
      if (!(params[i] instanceof String)) {
        throw new StmtException(this, "Expected an even number of arguments", ErrCodes.WRAPPER_SPECIFIC);
      }
      bindByIndex(getBindParameterIndex((String) params[i]), params[i + 1]);
    }
  }

  private void bindByIndex(int i, Object value) {
    if (value == null) {
      bindNull(i);
    } else if (value instanceof String) {
      bindText(i, (String) value);
    } else if (value instanceof Integer) {
      bindInt(i, (Integer) value);
    } else if (value instanceof Byte) {
      bindInt(i, (Byte) value);
    } else if (value instanceof Short) {
      bindInt(i, (Short) value);
    } else if (value instanceof Boolean) {
      bindInt(i, (Boolean) value ? 1 : 0);
    } else if (value instanceof Long) {
      bindLong(i, (Long) value);
    } else if (value instanceof Double) {
      bindDouble(i, (Double) value);
    } else if (value instanceof Float) {
      bindDouble(i, (Float) value);
    } else if (value instanceof byte[]) {
      bindBlob(i, (byte[]) value);
    } else { // TODO ZeroBlob
      throw new StmtException(this, String.format("Unsupported type in bind: %s", value.getClass().getSimpleName()), ErrCodes.WRAPPER_SPECIFIC);
    }
  }

  /**
   * @return the number of SQL parameters
   * @throws StmtException
   */
  public int getBindParameterCount() {
    checkOpen();
    return SQLite.sqlite3_bind_parameter_count(pStmt);
  }

  /**
   * @param name SQL parameter name
   * @return SQL parameter index or 0 if no match (cached)
   * @throws StmtException
   */
  public int getBindParameterIndex(String name) {
    checkOpen();
    if (params == null) {
      params = new HashMap<String, Integer>(getBindParameterCount());
    }
    final Integer index = params.get(name);
    if (index != null) {
      return index;
    }
    final int i = SQLite.sqlite3_bind_parameter_index(pStmt, name);
    if (i == 0) { // Invalid name
      return i;
    }
    params.put(name, i);
    return i;
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @return SQL parameter name or null.
   * @throws StmtException
   */
  public String getBindParameterName(int i) { // TODO Cache?
    checkOpen();
    return SQLite.sqlite3_bind_parameter_name(pStmt, i);
  }

  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @param value SQL parameter value
   * @throws StmtException
   */
  public void bindBlob(int i, byte[] value) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_blob(pStmt, i, value, value.length, SQLite.SQLITE_TRANSIENT), "sqlite3_bind_blob", i);
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @param value SQL parameter value
   * @throws StmtException
   */
  public void bindDouble(int i, double value) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_double(pStmt, i, value), "sqlite3_bind_double", i);
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @param value SQL parameter value
   * @throws StmtException
   */
  public void bindInt(int i, int value) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_int(pStmt, i, value), "sqlite3_bind_int", i);
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @param value SQL parameter value
   * @throws StmtException
   */
  public void bindLong(int i, long value) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_int64(pStmt, i, value), "sqlite3_bind_int64", i);
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @throws StmtException
   */
  public void bindNull(int i) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_null(pStmt, i), "sqlite3_bind_null", i);
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @param value SQL parameter value
   * @throws StmtException
   */
  public void bindText(int i, String value) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_text(pStmt, i, value, -1, SQLite.SQLITE_TRANSIENT), "sqlite3_bind_text", i);
  }
  /**
   * @param i The leftmost SQL parameter has an index of 1
   * @param n length of BLOB
   * @throws StmtException
   */
  public void bindZeroblob(int i, int n) {
    checkOpen();
    checkBind(SQLite.sqlite3_bind_zeroblob(pStmt, i, n), "sqlite3_bind_zeroblob", i);
  }

  boolean[][] getMetadata() {
    final int colCount = getColumnCount();
    final boolean[][] metadata = new boolean[colCount][3];
    for (int col = 0; col < colCount; col++) {
      final String colName = getColumnName(col); // FIXME origin_name?
      final String tblName = SQLite.sqlite3_column_table_name(pStmt, col);
      if (colName != null && tblName != null) {
        final boolean[] colMetaData = c.getTableColumnMetadata(null /*FIXME*/, tblName, colName);
        metadata[col] = colMetaData;
      }
    }
    return metadata;
  }

  private void check(int res, String format) {
    if (res != SQLite.SQLITE_OK) {
      throw new StmtException(this, String.format(format, getSql()), res);
    }
  }
  private void checkBind(int res, String method, int i) {
    if (res != SQLite.SQLITE_OK) {
      throw new StmtException(this, String.format("Error while calling %s for param %d of '%s'", method, i, getSql()), res);
    }
  }
  private void checkOpen() {
    if (pStmt == null) {
      throw new StmtException(this, "Stmt finalized", ErrCodes.WRAPPER_SPECIFIC);
    }
  }
}
