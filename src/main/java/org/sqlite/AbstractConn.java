/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public abstract class AbstractConn implements Connection {
  private Properties clientInfo = null;
  private int savepointId = 0;
  protected boolean autoCommit = true;

  abstract void checkOpen() throws ConnException;
  abstract void check(int res, String reason) throws ConnException;
  abstract int _close();
  abstract boolean readOnly();
  abstract void exec(String sql) throws SQLiteException;
  abstract String mprintf(String format, String arg);
  abstract Stmt prepare(String sql) throws ConnException;

  @Override
  public Statement createStatement() throws SQLException {
    return createStatement(ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY,
        ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }
  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY);
  }
  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    return prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY,
        ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }
  @Override
  public String nativeSQL(String sql) throws SQLException {
    return sql;
  }
  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    checkOpen();
    if (this.autoCommit == autoCommit) return;
    this.autoCommit = autoCommit;
    exec(autoCommit ? "COMMIT" : "BEGIN");
  }
  @Override
  public boolean getAutoCommit() throws SQLException {
    checkOpen();
    return autoCommit;
  }
  @Override
  public void commit() throws SQLException {
    checkOpen();
    if (autoCommit) throw new SQLException("database in auto-commit mode");
    exec("COMMIT; BEGIN");
  }
  @Override
  public void rollback() throws SQLException {
    checkOpen();
    if (autoCommit) throw new SQLException("database in auto-commit mode");
    exec("ROLLBACK; BEGIN");
  }
  @Override
  public void close() throws SQLException {
    if (clientInfo != null) clientInfo.clear();
    check(_close(), "error while closing connection");
  }
  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return null;  // FIXME
  }
  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    checkOpen();
    // TODO
  }
  @Override
  public boolean isReadOnly() throws SQLException {
    checkOpen();
    return readOnly();
  }
  @Override
  public void setCatalog(String catalog) throws SQLException {
    checkOpen();
  }
  @Override
  public String getCatalog() throws SQLException {
    checkOpen();
    return null;
  }
  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    // TODO http://sqlite.org/pragma.html#pragma_read_uncommitted
    if (TRANSACTION_SERIALIZABLE != level) {
      throw new SQLException("SQLite supports only TRANSACTION_SERIALIZABLE.");
    }
  }
  @Override
  public int getTransactionIsolation() throws SQLException {
    return TRANSACTION_SERIALIZABLE;
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
  public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    return createStatement(resultSetType, resultSetConcurrency,
        ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }
  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    return prepareStatement(sql, resultSetType, resultSetConcurrency,
        ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }
  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    return prepareCall(sql, resultSetType, resultSetConcurrency,
        ResultSet.CLOSE_CURSORS_AT_COMMIT);
  }
  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public void setHoldability(int holdability) throws SQLException {
    checkOpen();
    if (holdability != ResultSet.CLOSE_CURSORS_AT_COMMIT)
      throw new SQLFeatureNotSupportedException("SQLite only supports CLOSE_CURSORS_AT_COMMIT");
  }
  @Override
  public int getHoldability() throws SQLException {
    checkOpen();
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }
  @Override
  public Savepoint setSavepoint() throws SQLException {
    return setSavepoint(String.valueOf(savepointId++));
  }
  @Override
  public Savepoint setSavepoint(final String name) throws SQLException {
    final Savepoint savepoint = new Savepoint() {
      @Override
      public int getSavepointId() throws SQLException {
        throw new SQLException("named savepoint");
      }
      @Override
      public String getSavepointName() throws SQLException {
        return name;
      }
    };
    exec(mprintf("SAVEPOINT %Q", name));
    return savepoint;
  }
  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    exec(mprintf("ROLLBACK TO SAVEPOINT %Q", savepoint.getSavepointName()));
  }
  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    exec(mprintf("RELEASE SAVEPOINT %Q", savepoint.getSavepointName()));
  }
  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkOpen();
    checkCursor(resultSetType, resultSetConcurrency, resultSetHoldability);
    return null;  // FIXME
  }
  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkOpen();
    checkCursor(resultSetType, resultSetConcurrency, resultSetHoldability);
    return prepare(sql);
  }
  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    if (Statement.NO_GENERATED_KEYS != autoGeneratedKeys) {
      throw Util.unsupported();
    }
    return prepareStatement(sql);
  }
  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public Clob createClob() throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public Blob createBlob() throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public NClob createNClob() throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public SQLXML createSQLXML() throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public boolean isValid(int timeout) throws SQLException {
    return false;  // TODO
  }
  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    //checkOpen();
    if (clientInfo == null) return;
    clientInfo.setProperty(name, value);
  }
  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    //checkOpen();
    clientInfo = new Properties(properties);
  }
  @Override
  public String getClientInfo(String name) throws SQLException {
    checkOpen();
    if (clientInfo == null) return null;
    return clientInfo.getProperty(name);
  }
  @Override
  public Properties getClientInfo() throws SQLException {
    checkOpen();
    if (clientInfo == null) {
      clientInfo = new Properties();
    }
    return clientInfo;
  }
  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    checkOpen();
    throw Util.unsupported();
  }
  @Override
  public void setSchema(String schema) throws SQLException {
    checkOpen();
  }
  @Override
  public String getSchema() throws SQLException {
    checkOpen();
    return null;
  }
  @Override
  public void abort(Executor executor) throws SQLException {
    // TODO
  }
  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    // TODO
  }
  @Override
  public int getNetworkTimeout() throws SQLException {
    return 0;  // TODO
  }
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException("not a wrapper");
  }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  private static void checkCursor(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLFeatureNotSupportedException {
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) throw new SQLFeatureNotSupportedException(
        "SQLite only supports TYPE_FORWARD_ONLY cursors");
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) throw new SQLFeatureNotSupportedException(
        "SQLite only supports CONCUR_READ_ONLY cursors");
    if (resultSetHoldability != ResultSet.CLOSE_CURSORS_AT_COMMIT) throw new SQLFeatureNotSupportedException(
        "SQLite only supports closing cursors at commit");
  }
}
