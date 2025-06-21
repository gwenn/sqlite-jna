package org.sqlite.driver;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Prepared statement and named parameter holders
 */
public interface SQLitePreparedStatement extends PreparedStatement/*, CallableStatement*/ {
	void setNull(@NonNull String parameterName, int sqlType) throws SQLException;
	void setBoolean(@NonNull String parameterName, boolean x) throws SQLException;
	void setByte(@NonNull String parameterName, byte x) throws SQLException;
	void setShort(@NonNull String parameterName, short x) throws SQLException;
	void setInt(@NonNull String parameterName, int x) throws SQLException;
	void setLong(@NonNull String parameterName, long x) throws SQLException;
	void setFloat(@NonNull String parameterName, float x) throws SQLException;
	void setDouble(@NonNull String parameterName, double x) throws SQLException;
	void setBigDecimal(@NonNull String parameterName, @Nullable BigDecimal x) throws SQLException;
	void setString(@NonNull String parameterName, @Nullable String x) throws SQLException;
	void setBytes(@NonNull String parameterName, byte @Nullable[] x) throws SQLException;
	void setDate(@NonNull String parameterName, @Nullable Date x) throws SQLException;
	void setTime(@NonNull String parameterName, @Nullable Time x) throws SQLException;
	void setTimestamp(@NonNull String parameterName, @Nullable Timestamp x) throws SQLException;
	void setAsciiStream(@NonNull String parameterName, @Nullable InputStream x, int length) throws SQLException;
	void setBinaryStream(@NonNull String parameterName, @Nullable InputStream x, int length) throws SQLException;
	void setObject(@NonNull String parameterName, @Nullable Object x, int targetSqlType) throws SQLException;
	void setObject(@NonNull String parameterName, @Nullable Object x, @NonNull SQLType targetSqlType) throws SQLException;
	void setObject(@NonNull String parameterName, @Nullable Object x) throws SQLException;
	void setCharacterStream(@NonNull String parameterName, Reader reader, int length) throws SQLException;
	void setBlob(@NonNull String parameterName, @Nullable Blob x) throws SQLException;
	void setClob(@NonNull String parameterName, Clob x) throws SQLException;
	void setDate(@NonNull String parameterName, @Nullable Date x, @Nullable Calendar cal) throws SQLException;
	void setTime(@NonNull String parameterName, @Nullable Time x, @Nullable Calendar cal) throws SQLException;
	void setTimestamp(@NonNull String parameterName, @Nullable Timestamp x, @Nullable Calendar cal) throws SQLException;
	void setNull(@NonNull String parameterName, int sqlType, String typeName) throws SQLException;
	void setURL(@NonNull String parameterName, URL val) throws SQLException;
	void setRowId(@NonNull String parameterName, @Nullable RowId x) throws SQLException;
	void setNString(@NonNull String parameterName, @Nullable String value) throws SQLException;
	void setNCharacterStream(@NonNull String parameterName, Reader value, long length) throws SQLException;
	void setNClob(@NonNull String parameterName, @Nullable NClob value) throws SQLException;
	void setClob(@NonNull String parameterName, Reader reader, long length) throws SQLException;
	void setBlob(@NonNull String parameterName, @Nullable InputStream inputStream, long length) throws SQLException;
	void setNClob(@NonNull String parameterName, Reader reader, long length) throws SQLException;
	void setSQLXML(@NonNull String parameterName, @Nullable SQLXML xmlObject) throws SQLException;
	void setObject(@NonNull String parameterName, @Nullable Object x, int targetSqlType, int scaleOrLength) throws SQLException;
	void setObject(@NonNull String parameterName, @Nullable Object x, @NonNull SQLType targetSqlType, int scaleOrLength) throws SQLException;
	void setAsciiStream(@NonNull String parameterName, @Nullable InputStream x, long length) throws SQLException;
	void setBinaryStream(@NonNull String parameterName, @Nullable InputStream x, long length) throws SQLException;
	void setCharacterStream(@NonNull String parameterName, Reader reader, long length) throws SQLException;
	void setAsciiStream(@NonNull String parameterName, @Nullable InputStream x) throws SQLException;
	void setBinaryStream(@NonNull String parameterName, @Nullable InputStream x) throws SQLException;
	void setCharacterStream(@NonNull String parameterName, Reader reader) throws SQLException;
	void setNCharacterStream(@NonNull String parameterName, Reader value) throws SQLException;
	void setClob(@NonNull String parameterName, @Nullable Reader reader) throws SQLException;
	void setBlob(@NonNull String parameterName, @Nullable InputStream inputStream) throws SQLException;
	void setNClob(@NonNull String parameterName, @Nullable Reader reader) throws SQLException;
}
