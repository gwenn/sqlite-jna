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
  abstract void checkOpen();

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
    // TODO
  }
  @Override
  public boolean getAutoCommit() throws SQLException {
    return false;  // TODO
  }
  @Override
  public void commit() throws SQLException {
    // TODO
  }
  @Override
  public void rollback() throws SQLException {
    // TODO
  }
  @Override
  public void close() throws SQLException {
    // TODO
  }
  @Override
  public boolean isClosed() throws SQLException {
    return false;  // TODO
  }
  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return null;  // TODO
  }
  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    // TODO
  }
  @Override
  public boolean isReadOnly() throws SQLException {
    return false;  // TODO
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
    // TODO
  }
  @Override
  public int getTransactionIsolation() throws SQLException {
    return 0;  // TODO
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
    return null;  // TODO
  }
  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    return null;  // TODO
  }
  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    // TODO
  }
  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    // TODO
  }
  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return null;  // TODO
  }
  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return null;  // TODO
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
    // TODO
  }
  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    // TODO
  }
  @Override
  public String getClientInfo(String name) throws SQLException {
    return null;  // TODO
  }
  @Override
  public Properties getClientInfo() throws SQLException {
    return null;  // TODO
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
    return null;  // TODO
  }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }
}
