/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

public abstract class AbstractPrepStmt extends AbstractStmt implements PreparedStatement {
  abstract Rows execQuery() throws StmtException;
  @Override
  public ResultSet executeQuery() throws SQLException {
    Util.trace("*PreparedStatement.executeQuery");
    return execQuery();
  }
  @Override
  public int executeUpdate() throws SQLException {
    Util.trace("*PreparedStatement.executeUpdate");
    return 0; // FIXME
  }
  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    Util.trace("*PreparedStatement.setNull");
    // FIXME
  }
  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    Util.trace("*PreparedStatement.setBoolean");
    // FIXME
  }
  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    Util.trace("*PreparedStatement.setByte");
    // FIXME
  }
  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    Util.trace("*PreparedStatement.setShort");
    // FIXME
  }
  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    Util.trace("*PreparedStatement.setInt");
    // FIXME
  }
  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    Util.trace("*PreparedStatement.setLong");
    // FIXME
  }
  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    Util.trace("*PreparedStatement.setFloat");
    // FIXME
  }
  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    Util.trace("*PreparedStatement.setDouble");
    // FIXME
  }
  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    Util.trace("*PreparedStatement.setBigDecimal");
    // FIXME
  }
  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    Util.trace("*PreparedStatement.setString");
    // FIXME
  }
  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    Util.trace("*PreparedStatement.setBytes");
    // FIXME
  }
  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    Util.trace("*PreparedStatement.setDate");
    // FIXME
  }
  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    Util.trace("*PreparedStatement.setTime");
    // FIXME
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    Util.trace("*PreparedStatement.setTimestamp");
    // FIXME
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
    Util.trace("*PreparedStatement.clearParameters");
    // FIXME
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    Util.trace("*PreparedStatement.setObject");
    // FIXME
  }
  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    Util.trace("*PreparedStatement.setObject");
    // FIXME
  }
  @Override
  public boolean execute() throws SQLException {
    Util.trace("*PreparedStatement.execute");
    return false; // FIXME
  }
  @Override
  public void addBatch() throws SQLException {
    Util.trace("*PreparedStatement.addBatch");
    // FIXME
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
    Util.trace("*PreparedStatement.getMetaData");
    return null; // FIXME
  }
  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    Util.trace("*PreparedStatement.setDate");
    // FIXME
  }
  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    Util.trace("*PreparedStatement.setTime");
    // FIXME
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    Util.trace("*PreparedStatement.setTimestamp");
    // FIXME
  }
  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    Util.trace("*PreparedStatement.setNull");
    // FIXME
  }
  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    throw Util.unsupported("PreparedStatement.setURL");
  }
  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    Util.trace("*PreparedStatement.getParameterMetaData");
    return null; // FIXME
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
