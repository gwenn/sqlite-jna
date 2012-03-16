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

public abstract class AbstractPreparedStatement extends AbstractStmt implements PreparedStatement {
  @Override
  public ResultSet executeQuery() throws SQLException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public int executeUpdate() throws SQLException {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNull(int parameterIndex, int sqlType) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBoolean(int parameterIndex, boolean x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setByte(int parameterIndex, byte x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setShort(int parameterIndex, short x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setInt(int parameterIndex, int x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setLong(int parameterIndex, long x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setFloat(int parameterIndex, float x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setDouble(int parameterIndex, double x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setString(int parameterIndex, String x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBytes(int parameterIndex, byte[] x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setDate(int parameterIndex, Date x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setTime(int parameterIndex, Time x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  @SuppressWarnings("deprecation")
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void clearParameters() throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setObject(int parameterIndex, Object x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public boolean execute() throws SQLException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void addBatch() throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setRef(int parameterIndex, Ref x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBlob(int parameterIndex, Blob x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setClob(int parameterIndex, Clob x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setArray(int parameterIndex, Array x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setURL(int parameterIndex, URL x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public ParameterMetaData getParameterMetaData() throws SQLException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
