/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.ErrCodes;
import org.sqlite.StmtException;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Stmt implements Statement {
  private Conn c;
  private final boolean prepared;
  private org.sqlite.Stmt stmt;

  // cached columns index by name
  // FIXME to invalidate when Stmt
  //if (colIndexByName != null) colIndexByName.clear();
  private Map<String, Integer> colIndexByName;
  private boolean isCloseOnCompletion;

  Stmt(Conn c) {
    this.c = c;
    this.prepared = false;
  }
  Stmt(Conn c, org.sqlite.Stmt stmt) {
    this.c = c;
    this.stmt = stmt;
    this.prepared = true;
  }

  org.sqlite.Stmt getStmt() throws SQLException {
    checkOpen();
    return stmt;
  }
  org.sqlite.Conn getConn() throws SQLException {
    checkOpen();
    return c.getConn();
  }

  private void checkOpen() throws SQLException {
    if (stmt == null) {
      throw new SQLException("Statement closed");
    } else {
      stmt.checkOpen();
    }
  }

  int findCol(String col) throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    Integer index = findColIndexInCache(col);
    if (null != index) {
      return index;
    }
    final int columnCount = stmt.getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      if (col.equalsIgnoreCase(stmt.getColumnName(i))) {
        addColIndexInCache(col, i + 1, columnCount);
        return i + 1;
      }
    }
    throw new StmtException(stmt, "no such column: '" + col + "'", ErrCodes.WRAPPER_SPECIFIC);
  }

  private Integer findColIndexInCache(String col) {
    if (null == colIndexByName) {
      return null;
    } else {
      return colIndexByName.get(col);
    }
  }
  private void addColIndexInCache(String col, int index, int columnCount) throws StmtException {
    if (null == colIndexByName) {
      colIndexByName = new HashMap<String, Integer>(columnCount);
    }
    colIndexByName.put(col, index);
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      throw Util.unsupported("Statement.executeQuery");
    }
  }
  @Override
  public int executeUpdate(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      throw Util.unsupported("Statement.executeUpdate");
    }
  }
  @Override
  public void close() throws StmtException {
    Util.trace("Statement.close");
    if (stmt != null) {
      stmt.closeAndCheck();
      if (colIndexByName != null) colIndexByName.clear();
      stmt = null;
      c = null;
    }
  }
  @Override
  public int getMaxFieldSize() throws SQLException {
    Util.trace("*Statement.getMaxFieldSize");
    checkOpen();
    return 0; // TODO
  }
  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    if (max < 0) throw Util.error("max field size must be >= 0");
    checkOpen();
    // TODO
    Util.trace("*Statement.setMaxFieldSize");
  }
  @Override
  public int getMaxRows() throws SQLException { // Used by Hibernate
    return 0;
  }
  @Override
  public void setMaxRows(int max) throws SQLException {
    if (max < 0) throw Util.error("max row count must be >= 0");
    throw Util.unsupported("*Statement.setMaxRows"); // TODO
  }
  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    checkOpen();
    // TODO
    Util.trace("Statement.setEscapeProcessing");
  }
  @Override
  public int getQueryTimeout() throws SQLException { // Used by Hibernate
    checkOpen();
    return 0; // TODO
  }
  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    if (seconds < 0) throw Util.error("query timeout must be >= 0");
    checkOpen();
    // TODO
    Util.trace("Statement.setQueryTimeout");
  }
  @Override
  public void cancel() throws SQLException {
    getConn().interrupt();
  }
  @Override
  public SQLWarning getWarnings() throws SQLException {
    checkOpen();
    return null;
  }
  @Override
  public void clearWarnings() throws SQLException {
    checkOpen();
  }
  @Override
  public void setCursorName(String name) throws SQLException {
    checkOpen();
    // TODO
    Util.trace("*Statement.setCursorName");
  }
  @Override
  public boolean execute(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      throw Util.unsupported("*Statement.execute(String)"); // TODO
    }
  }
  @Override
  public ResultSet getResultSet() throws SQLException {
    throw Util.unsupported("*Statement.getResultSet"); // TODO
  }
  @Override
  public int getUpdateCount() throws SQLException {
    throw Util.unsupported("*Statement.getUpdateCount"); // TODO
  }
  @Override
  public boolean getMoreResults() throws SQLException {
    throw Util.unsupported("*Statement.getMoreResults"); // TODO
  }
  @Override
  public void setFetchDirection(int direction) throws SQLException {
    checkOpen();
    if (ResultSet.FETCH_FORWARD != direction) {
      throw Util.caseUnsupported("SQLite supports only FETCH_FORWARD direction");
    }
  }
  @Override
  public int getFetchDirection() throws SQLException {
    checkOpen();
    return ResultSet.FETCH_FORWARD;
  }
  @Override
  public void setFetchSize(int rows) throws SQLException {
    if (rows < 0) throw Util.error("fetch size must be >= 0");
    checkOpen();
    if (rows != 1) {
      throw Util.caseUnsupported("SQLite does not support setting fetch size");
    }
  }
  @Override
  public int getFetchSize() throws SQLException {
    checkOpen();
    return 1;
  }
  @Override
  public int getResultSetConcurrency() throws SQLException {
    return ResultSet.CONCUR_READ_ONLY;
  }
  @Override
  public int getResultSetType() throws SQLException {
    return ResultSet.TYPE_FORWARD_ONLY;
  }
  @Override
  public void addBatch(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      throw Util.unsupported("*Statement.addBatch"); // TODO
    }
  }
  @Override
  public void clearBatch() throws SQLException {
    throw Util.unsupported("*Statement.clearBatch"); // TODO
  }
  @Override
  public int[] executeBatch() throws SQLException {
    throw Util.unsupported("*Statement.executeBatch"); // TODO
  }
  @Override
  public Connection getConnection() throws SQLException {
    return c;
  }
  @Override
  public boolean getMoreResults(int current) throws SQLException {
    throw Util.unsupported("*Statement.getMoreResults"); // TODO
  }
  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    throw Util.unsupported("*Statement.getGeneratedKeys"); // TODO
  }
  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    if (Statement.NO_GENERATED_KEYS != autoGeneratedKeys) {
      throw Util.unsupported("Statement.executeUpdate(String, int)");
    }
    return executeUpdate(sql);
  }
  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw Util.unsupported("Statement.executeUpdate(String, int[])");
  }
  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw Util.unsupported("Statement.executeUpdate(String, String[])");
  }
  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    if (Statement.NO_GENERATED_KEYS != autoGeneratedKeys) {
      throw Util.unsupported("Statement.execute(String, int)");
    }
    return execute(sql);
  }
  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw Util.unsupported("Statement.execute(String, int[])");
  }
  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw Util.unsupported("Statement.execute(String, String[])");
  }
  @Override
  public int getResultSetHoldability() throws SQLException {
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }
  @Override
  public boolean isClosed() throws SQLException {
    return stmt == null;
  }
  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    Util.trace("*Statement.setPoolable");
    checkOpen();
    // TODO
  }
  @Override
  public boolean isPoolable() throws SQLException {
    Util.trace("*Statement.isPoolable");
    checkOpen();
    return false; // TODO
  }
  @Override
  public void closeOnCompletion() throws SQLException {
    checkOpen();
    isCloseOnCompletion = true;
  }
  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    checkOpen();
    return isCloseOnCompletion;
  }
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw Util.error("not a wrapper");
  }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }
}
