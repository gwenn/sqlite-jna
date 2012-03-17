/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public abstract class AbstractStmt implements Statement {
  abstract Rows execQuery(String sql) throws ConnException, StmtException;
  abstract int execUpdate(String sql) throws ConnException, StmtException;
  abstract void check(int res, String reason) throws StmtException;
  abstract void checkOpen() throws StmtException;
  abstract int _close() throws StmtException;
  abstract void interrupt() throws ConnException;
  abstract boolean prepared();
  abstract void autoClose();
  abstract boolean isAutoClosed();

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    if (prepared()) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      return execQuery(sql);
    }
  }
  @Override
  public int executeUpdate(String sql) throws SQLException {
    if (prepared()) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      Util.trace("Statement.executeUpdate");
      return execUpdate(sql);
    }
  }
  @Override
  public void close() throws StmtException {
    Util.trace("Statement.close");
    check(_close(), "error while closing statement '%s'");
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
  public int getMaxRows() throws SQLException {
    Util.trace("Statement.getMaxRows");
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
  public int getQueryTimeout() throws SQLException {
    checkOpen();
    Util.trace("Statement.getQueryTimeout");
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
    checkOpen();
    interrupt();
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
    if (prepared()) {
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
    if (prepared()) {
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
    autoClose();
  }
  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    checkOpen();
    return isAutoClosed();
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
