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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// There is no "not prepared" statement in SQLite!
public class Stmt implements Statement {
  //private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
  private Conn c;
  private final boolean prepared;
  private org.sqlite.Stmt stmt;

  // cached columns' index by name
  private Map<String, Integer> colIndexByName;
  private boolean isCloseOnCompletion;
  private int maxRows;
  private int status = -1; // -1: unknown, 0: not a select, 1: select with row, 2: select without row
  private List<String> batch; // sql queries (see addBatch)
  //private int queryTimeout;

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

  void checkOpen() throws SQLException {
    if (prepared) {
      if (stmt == null) {
        throw new SQLException("Statement closed");
      } else {
        stmt.checkOpen();
      }
    } else if (c == null) {
      throw new SQLException("Statement closed");
    }
  }

  int findCol(String col) throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    if (this == c.getGeneratedKeys) { // We don't know the table's name nor the column's name but there is only one possible.
      return 1;
    }
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
      _close();
      checkOpen();
      stmt = c.getConn().prepare(sql);
      final boolean hasRow = stmt.step();
      if (!hasRow && stmt.getColumnCount() == 0) { // FIXME some pragma may return zero...
        throw new StmtException(stmt, "query does not return a ResultSet", ErrCodes.WRAPPER_SPECIFIC);
      }
      return new Rows(this, hasRow);
    }
  }
  @Override
  public int executeUpdate(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      _close();
      checkOpen();
      stmt = c.getConn().prepare(sql);
      if (stmt.step() || stmt.getColumnCount() != 0) {
        throw new StmtException(stmt, "statement returns a ResultSet", ErrCodes.WRAPPER_SPECIFIC);
      }
      return getConn().getChanges();
    }
  }
  @Override
  public void close() throws SQLException {
    //Util.trace("Statement.close");
    _close();
    c = null;
  }
  private void _close() throws SQLException {
    if (stmt != null) {
      stmt.closeAndCheck();
      if (colIndexByName != null) colIndexByName.clear();
      stmt = null;
      status = -1;
    }
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    checkOpen();
    return 0; // TODO
  }
  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    if (max < 0) throw Util.error("max field size must be >= 0");
    checkOpen();
    throw Util.unsupported("*Statement.setMaxFieldSize");
  }
  @Override
  public int getMaxRows() throws SQLException { // Used by Hibernate
    checkOpen();
    return maxRows;
  }
  @Override
  public void setMaxRows(int max) throws SQLException {
    checkOpen();
    if (max < 0) throw Util.error("max row count must be >= 0");
    this.maxRows = max;
  }
  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    checkOpen();
    // TODO
    throw Util.unsupported("Statement.setEscapeProcessing");
  }
  @Override
  public int getQueryTimeout() throws SQLException { // Used by Hibernate
    checkOpen();
    return 0; // TODO ExecutorService#invokeAny(..., queryTimeout, TimeUnit.SECONDS);
  }
  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    if (seconds < 0) throw Util.error("query timeout must be >= 0");
    checkOpen();
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
    //checkOpen();
  }
  @Override
  public void setCursorName(String name) throws SQLException {
    checkOpen();
    throw Util.unsupported("*Statement.setCursorName");
  }
  @Override
  public boolean execute(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      _close();
      checkOpen();
      stmt = c.getConn().prepare(sql);
      return exec();
    }
  }
  protected boolean exec() throws SQLException {
    if (stmt.step()) {
      status = 1;
    } else if (stmt.getColumnCount() != 0) {
      status = 2;
    } else {
      status = 0;
    }
    return status != 0;
  }
  /*protected boolean step(final org.sqlite.Stmt stmt) throws SQLException {
    if (queryTimeout == 0) {
      return stmt.step();
    }
    // TODO Validate usage of stmt in a different thread:
    try {
      return EXECUTOR_SERVICE.invokeAny(Collections.singleton(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return stmt.step();
        }
      }), queryTimeout, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new SQLException(e);
    } catch (TimeoutException e) {
      getConn().interrupt();
      throw new SQLTimeoutException(e);
    }
  }*/
  // Works only with execute (not executeQuery)
  @Override
  public ResultSet getResultSet() throws SQLException {
    checkOpen();
    if (status > 0) {
      final boolean hasRow = status == 1;
      status = -1; // This method should be called only once per result.
      return new Rows(this, hasRow);
    } else {
      return null;
    }
  }
  // Works only with execute (not executeUpdate)
  @Override
  public int getUpdateCount() throws SQLException {
    checkOpen();
    if (status != 0) {
      return -1;
    } else {
      status = -1; // This method should be called only once per result.
      return getConn().getChanges();
    }
  }
  @Override
  public boolean getMoreResults() throws SQLException {
    checkOpen();
    if (prepared) {
      if (stmt.getTail() == null || stmt.getTail().length()== 0) {
        stmt.reset(); // implicitly closes any current ResultSet
        return false; // no more results
      } else {
        throw Util.unsupported("*Statement.getMoreResults"); // TODO
      }
    } else if (stmt != null) {
      String tail = stmt.getTail();
      _close();
      return !(tail == null || tail.length() == 0) && execute(tail);
    }
    return false;
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
    if (rows == 0) {
      return;
    }
    if (rows != 1) {
      //Util.trace(String.format("SQLite does not support setting fetch size to %d", rows));
    }
  }
  @Override
  public int getFetchSize() throws SQLException {
    checkOpen();
    return 1;
  }
  @Override
  public int getResultSetConcurrency() throws SQLException {
    checkOpen();
    return ResultSet.CONCUR_READ_ONLY;
  }
  @Override
  public int getResultSetType() throws SQLException {
    checkOpen();
    return ResultSet.TYPE_FORWARD_ONLY;
  }
  @Override
  public void addBatch(String sql) throws SQLException {
    if (prepared) {
      throw new SQLException("method not supported by PreparedStatement");
    } else {
      checkOpen();
      if (batch == null) {
        batch = new ArrayList<String>();
      }
      batch.add(sql);
    }
  }
  @Override
  public void clearBatch() throws SQLException {
    checkOpen();
    if (batch != null) {
      batch.clear();
    }
  }
  @Override
  public int[] executeBatch() throws SQLException {
    checkOpen();
    if (batch == null) {
      return new int[0];
    }
    final int size = batch.size();
    Exception cause = null;
    final int[] changes = new int[size];
    for (int i = 0; i < size; ++i) {
      try {
        changes[i] = executeUpdate(batch.get(i));
      } catch (SQLException e) {
        if (cause == null) {
          cause = e;
        }
        changes[i] = EXECUTE_FAILED;
      }
    }
    clearBatch();
    if (cause != null) {
      throw new BatchUpdateException("batch failed", changes, cause);
    }
    return changes;
  }
  @Override
  public Connection getConnection() throws SQLException {
    checkOpen();
    return c;
  }
  @Override
  public boolean getMoreResults(int current) throws SQLException {
    checkOpen();
    if (KEEP_CURRENT_RESULT == current || CLOSE_ALL_RESULTS == current) {
      throw Util.caseUnsupported("SQLite supports only CLOSE_CURRENT_RESULT");
    }
    return getMoreResults();
  }
  // Limitations:
  //  - only primary keys defined as rowid's alias work.
  // With the rowid (ok) and the associated table (ko) we can fix these limitations...
  @Override
  public ResultSet getGeneratedKeys() throws SQLException { // Used by hibernate
    checkOpen();
    return c.getGeneratedKeys();
  }
  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    Conn.checkAutoGeneratedKeys(autoGeneratedKeys);
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
    Conn.checkAutoGeneratedKeys(autoGeneratedKeys);
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
    checkOpen();
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }
  @Override
  public boolean isClosed() throws SQLException {
    return c == null;
  }
  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    checkOpen();
    throw Util.unsupported("*Statement.setPoolable"); // TODO
  }
  @Override
  public boolean isPoolable() throws SQLException {
    checkOpen();
    throw Util.unsupported("*Statement.isPoolable"); // TODO
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
