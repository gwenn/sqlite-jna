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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

public class PrepStmt extends Stmt implements PreparedStatement {
  PrepStmt(Conn c, org.sqlite.Stmt stmt) {
    super(c, stmt);
  }

  @Override
  public ResultSet executeQuery() throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    final boolean hasRow = stmt.step();
    if (!hasRow && stmt.getColumnCount() == 0) {
      throw new StmtException(stmt, "query does not return a ResultSet", ErrCodes.WRAPPER_SPECIFIC);
    }
    return new Rows(this, hasRow);
  }
  @Override
  public int executeUpdate() throws SQLException {
    final org.sqlite.Stmt stmt = getStmt();
    if (stmt.step() || stmt.getColumnCount() != 0) {
      throw new StmtException(stmt, "statement returns a ResultSet", ErrCodes.WRAPPER_SPECIFIC);
    }
    return getConn().getChanges();
  }
  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    getStmt().bindNull(parameterIndex);
  }
  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    getStmt().bindInt(parameterIndex, x ? 1 : 0);
  }
  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    getStmt().bindInt(parameterIndex, x);
  }
  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    getStmt().bindInt(parameterIndex, x);
  }
  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    getStmt().bindInt(parameterIndex, x);
  }
  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    getStmt().bindLong(parameterIndex, x);
  }
  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    getStmt().bindDouble(parameterIndex, x);
  }
  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    getStmt().bindDouble(parameterIndex, x);
  }
  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    throw Util.unsupported("*PreparedStatement.setBigDecimal"); // TODO
  }
  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    getStmt().bindText(parameterIndex, x);
  }
  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    if (x == null) {
      getStmt().bindNull(parameterIndex);
    } else {
      getStmt().bindBlob(parameterIndex, x);
    }
  }
  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    bindDate(parameterIndex, x);
  }
  private void bindDate(int parameterIndex, java.util.Date x) throws SQLException {
    if (null == x) {
      getStmt().bindNull(parameterIndex);
    } else {
      getStmt().bindLong(parameterIndex, x.getTime());
    }
  }
  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    bindDate(parameterIndex, x);
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    bindDate(parameterIndex, x);
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setAsciiStream");
  }
  @Override
  @SuppressWarnings("deprecation")
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setUnicodeStream");
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setBinaryStream");
  }
  @Override
  public void clearParameters() throws SQLException {
    getStmt().clearBindings();
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    throw Util.unsupported("*PreparedStatement.setObject"); // TODO
  }
  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    throw Util.unsupported("*PreparedStatement.setObject"); // TODO
  }
  @Override
  public boolean execute() throws SQLException {
    return exec();
  }

  @Override
  public void addBatch() throws SQLException {
    throw Util.unsupported("*PreparedStatement.addBatch"); // FIXME
  }
  @Override
  public void clearBatch() throws SQLException {
    throw Util.unsupported("*PreparedStatement.clearBatch"); // FIXME
  }
  @Override
  public int[] executeBatch() throws SQLException {
    throw Util.unsupported("*PreparedStatement.executeBatch"); // FIXME
  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setCharacterStream");
  }
  @Override
  public void setRef(int parameterIndex, Ref x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setRef");
  }
  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setBlob");
  }
  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setClob");
  }
  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setArray");
  }
  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    throw Util.unsupported("*PreparedStatement.getMetaData"); // FIXME
  }
  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    throw Util.unsupported("*PreparedStatement.setDate"); // TODO
  }
  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    throw Util.unsupported("*PreparedStatement.setTime"); // TODO
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    throw Util.unsupported("*PreparedStatement.setTimestamp"); // TODO
  }
  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    setNull(parameterIndex, sqlType);
  }
  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setURL");
  }
  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    throw Util.unsupported("*PreparedStatement.getParameterMetaData"); // FIXME
  }
  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setRowId");
  }
  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    setString(parameterIndex, value);
  }
  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setNCharacterStream");
  }
  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    throw Util.unsupported("PreparedStatement.setNClob");
  }
  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setClob");
  }
  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setBlob");
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setNClob");
  }
  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    throw Util.unsupported("PreparedStatement.setSQLXML");
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
    throw Util.unsupported("PreparedStatement.setObject");
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setAsciiStream");
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setBinaryStream");
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    throw Util.unsupported("PreparedStatement.setCharacterStream");
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setAsciiStream");
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setBinaryStream");
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    throw Util.unsupported("PreparedStatement.setCharacterStream");
  }
  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    throw Util.unsupported("PreparedStatement.setNCharacterStream");
  }
  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    throw Util.unsupported("PreparedStatement.setClob");
  }
  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    throw Util.unsupported("PreparedStatement.setBlob");
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw Util.unsupported("PreparedStatement.setNClob");
  }
}
