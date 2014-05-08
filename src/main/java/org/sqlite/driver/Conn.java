/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.ConnException;
import org.sqlite.ErrCodes;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.sqlite.SQLite.escapeIdentifier;

public class Conn implements Connection {
  private org.sqlite.Conn c;

  private DbMeta meta = null;
  PreparedStatement getGeneratedKeys;

  private Properties clientInfo;
  private int savepointId = 0;
  private SQLWarning warnings;

  public Conn(org.sqlite.Conn c) {
    this.c = c;
  }

  org.sqlite.Conn getConn() throws SQLException {
    checkOpen();
    return c;
  }

  private void checkOpen() throws SQLException {
    if (c == null) {
      throw new SQLException("Connection closed");
    } else {
      c.checkOpen();
    }
  }

  ResultSet getGeneratedKeys() throws SQLException {
    if (getGeneratedKeys == null) {
      getGeneratedKeys = prepareStatement("select last_insert_rowid()");
    }
    return getGeneratedKeys.executeQuery();
  }

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
    Util.trace("Connection.nativeSQL");
    return sql;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    if (getAutoCommit() == autoCommit) return;
    getConn().exec(autoCommit ? "COMMIT" : "BEGIN");
  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return getConn().getAutoCommit();
  }

  @Override
  public void commit() throws SQLException {
    if (getAutoCommit()) throw new ConnException(c, "database in auto-commit mode", ErrCodes.WRAPPER_SPECIFIC);
    getConn().exec("COMMIT; BEGIN");
  }

  @Override
  public void rollback() throws SQLException {
    if (getAutoCommit()) throw new ConnException(c, "database in auto-commit mode", ErrCodes.WRAPPER_SPECIFIC);
    getConn().exec("ROLLBACK; BEGIN");
  }

  @Override
  public void close() throws SQLException {
    if (c != null) {
      if (getGeneratedKeys != null) getGeneratedKeys.close();
      c.closeAndCheck();
      if (clientInfo != null) clientInfo.clear();
      c = null;
    }
  }

  @Override
  public boolean isClosed() throws SQLException {
    return c == null;
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    if (meta == null) meta = new DbMeta(this);
    return meta;
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    Util.trace("Connection.setReadOnly");
    checkOpen();
    final boolean ro = getConn().isReadOnly(null);
    if (ro == readOnly) {
      return;
    }
    if (ro) {
      addWarning(new SQLWarning("readOnly mode cannot be reset"));
    } else {
      getConn().setQueryOnly(null, readOnly);
    }
  }

  @Override
  public boolean isReadOnly() throws SQLException {
    checkOpen();
    return getConn().isReadOnly(null) || getConn().isQueryOnly(null);
  }

  @Override
  public void setCatalog(String catalog) throws SQLException {
    checkOpen();
  }

  @Override
  public String getCatalog() throws SQLException {
    checkOpen();
    return null; // "main" is not the default catalog ("temp" catalog is searched first)
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    // TODO http://sqlite.org/pragma.html#pragma_read_uncommitted
    if (TRANSACTION_SERIALIZABLE != level) {
      throw Util.error("SQLite supports only TRANSACTION_SERIALIZABLE.");
    }
  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    return TRANSACTION_SERIALIZABLE;
  }

  public void addWarning(SQLWarning warn) {
    if (warnings != null) {
      warnings.setNextWarning(warn);
    } else {
      warnings = warn;
    }
  }

  @Override
  public SQLWarning getWarnings() throws SQLException { // SQLITE_CONFIG_LOG
    checkOpen();
    return warnings;
  }

  @Override
  public void clearWarnings() throws SQLException {
    checkOpen();
    warnings = null;
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
    throw Util.unsupported("Connection.getTypeMap");
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.setTypeMap");
  }

  @Override
  public void setHoldability(int holdability) throws SQLException {
    checkOpen();
    if (holdability != ResultSet.CLOSE_CURSORS_AT_COMMIT)
      throw Util.caseUnsupported("SQLite only supports CLOSE_CURSORS_AT_COMMIT");
  }

  @Override
  public int getHoldability() throws SQLException {
    checkOpen();
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    final int id = savepointId++;
    final Savepoint savepoint = new Savepoint() {
      @Override
      public int getSavepointId() throws SQLException {
        return id;
      }

      @Override
      public String getSavepointName() throws SQLException {
        throw new ConnException(c, "un-named savepoint", ErrCodes.WRAPPER_SPECIFIC);
      }

      @Override
      public String toString() {
        return String.valueOf(id);
      }
    };
    getConn().exec("SAVEPOINT \"" + id + "\""); // SAVEPOINT 1; fails
    return savepoint;
  }

  @Override
  public Savepoint setSavepoint(final String name) throws SQLException {
    final Savepoint savepoint = new Savepoint() {
      @Override
      public int getSavepointId() throws SQLException {
        throw new ConnException(c, "named savepoint", ErrCodes.WRAPPER_SPECIFIC);
      }

      @Override
      public String getSavepointName() throws SQLException {
        return name;
      }

      @Override
      public String toString() {
        return name;
      }
    };
    getConn().exec("SAVEPOINT \"" + escapeIdentifier(name) + "\"");
    return savepoint;
  }

  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    getConn().exec("ROLLBACK TO SAVEPOINT \"" + escapeIdentifier(savepoint.toString()) + "\"");
  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    getConn().exec("RELEASE SAVEPOINT \"" + escapeIdentifier(savepoint.toString()) + "\"");
  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkOpen();
    checkCursor(resultSetType, resultSetConcurrency, resultSetHoldability);
    return new Stmt(this);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkOpen();
    checkCursor(resultSetType, resultSetConcurrency, resultSetHoldability);
    return new PrepStmt(this, getConn().prepare(sql));
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.prepareCall");
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    checkAutoGeneratedKeys(autoGeneratedKeys);
    return prepareStatement(sql);
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.prepareStatement(String,int[])");
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.prepareStatement(String,String[])");
  }

  @Override
  public Clob createClob() throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.createClob");
  }

  @Override
  public Blob createBlob() throws SQLException { // FIXME ...
    checkOpen();
    throw Util.unsupported("Connection.createBlob");
  }

  @Override
  public NClob createNClob() throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.createNClob");
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.createSQLXML");
  }

  @Override
  public boolean isValid(int seconds) throws SQLException {
    Util.trace("Connection.isValid");
    if (seconds < 0) throw Util.error("timeout must be >= 0");
    return !isClosed();
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    Util.trace("Connection.setClientInfo(String,String)");
    //checkOpen();
    if (clientInfo == null) return;
    clientInfo.setProperty(name, value);
  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    Util.trace("Connection.setClientInfo(Properties)");
    //checkOpen();
    clientInfo = new Properties(properties);
  }

  @Override
  public String getClientInfo(String name) throws SQLException {
    Util.trace("Connection.getClientInfo(String)");
    checkOpen();
    if (clientInfo == null) return null;
    return clientInfo.getProperty(name);
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    Util.trace("Connection.getClientInfo()");
    checkOpen();
    if (clientInfo == null) {
      clientInfo = new Properties();
    }
    return clientInfo;
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.createArrayOf");
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    checkOpen();
    throw Util.unsupported("Connection.createStruct");
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
    Util.trace("Connection.abort");
    // TODO
  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    throw Util.unsupported("Connection.setNetworkTimeout");
  }

  @Override
  public int getNetworkTimeout() throws SQLException {
    throw Util.unsupported("Connection.getNetworkTimeout");
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw Util.error("not a wrapper");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  private static void checkCursor(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLFeatureNotSupportedException {
    if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) throw Util.caseUnsupported(
        "SQLite only supports TYPE_FORWARD_ONLY cursors");
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) throw Util.caseUnsupported(
        "SQLite only supports CONCUR_READ_ONLY cursors");
    if (resultSetHoldability != ResultSet.CLOSE_CURSORS_AT_COMMIT) throw Util.caseUnsupported(
        "SQLite only supports closing cursors at commit");
  }

  static void checkAutoGeneratedKeys(int autoGeneratedKeys) throws SQLException {
    if (Statement.NO_GENERATED_KEYS != autoGeneratedKeys && Statement.RETURN_GENERATED_KEYS != autoGeneratedKeys) {
      throw new SQLException(String.format("unsupported autoGeneratedKeys value: %d", autoGeneratedKeys));
    }
  }
}
