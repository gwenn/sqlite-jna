package org.sqlite.driver;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Prepared statement and named parameter holders
 */
public interface SQLitePreparedStatement extends PreparedStatement/*, CallableStatement*/ {
  void setNull(String parameterName, int sqlType) throws SQLException;
  void setBoolean(String parameterName, boolean x) throws SQLException;
  void setByte(String parameterName, byte x) throws SQLException;
  void setShort(String parameterName, short x) throws SQLException;
  void setInt(String parameterName, int x) throws SQLException;
  void setLong(String parameterName, long x) throws SQLException;
  void setFloat(String parameterName, float x) throws SQLException;
  void setDouble(String parameterName, double x) throws SQLException;
  void setBigDecimal(String parameterName, BigDecimal x) throws SQLException;
  void setString(String parameterName, String x) throws SQLException;
  void setBytes(String parameterName, byte[] x) throws SQLException;
  void setDate(String parameterName, Date x) throws SQLException;
  void setTime(String parameterName, Time x) throws SQLException;
  void setTimestamp(String parameterName, Timestamp x) throws SQLException;
  void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException;
  void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException;
  void setObject(String parameterName, Object x, int targetSqlType) throws SQLException;
  void setObject(String parameterName, Object x) throws SQLException;
  void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException;
  void setBlob(String parameterName, Blob x) throws SQLException;
  void setClob(String parameterName, Clob x) throws SQLException;
  void setDate(String parameterName, Date x, Calendar cal) throws SQLException;
  void setTime(String parameterName, Time x, Calendar cal) throws SQLException;
  void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException;
  void setNull(String parameterName, int sqlType, String typeName) throws SQLException;
  void setURL(String parameterName, URL val) throws SQLException;
  void setRowId(String parameterName, RowId x) throws SQLException;
  void setNString(String parameterName, String value) throws SQLException;
  void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException;
  void setNClob(String parameterName, NClob value) throws SQLException;
  void setClob(String parameterName, Reader reader, long length) throws SQLException;
  void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException;
  void setNClob(String parameterName, Reader reader, long length) throws SQLException;
  void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException;
  void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException;
  void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException;
  void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException;
  void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException;
  void setAsciiStream(String parameterName, InputStream x) throws SQLException;
  void setBinaryStream(String parameterName, InputStream x) throws SQLException;
  void setCharacterStream(String parameterName, Reader reader) throws SQLException;
  void setNCharacterStream(String parameterName, Reader value) throws SQLException;
  void setClob(String parameterName, Reader reader) throws SQLException;
  void setBlob(String parameterName, InputStream inputStream) throws SQLException;
  void setNClob(String parameterName, Reader reader) throws SQLException;
}
