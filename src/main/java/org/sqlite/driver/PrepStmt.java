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
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/*
Blob Incremental I/O:
--------------------

* Initialization:

CREATE TABLE test (
-- pk   INTEGER PRIMARY KEY -- optional ROWID alias
-- ...
   data BLOB
-- ...
);
INSERT INTO test (data) VALUES (zeroblob(:blob_size));
SELECT last_insert_rowid();
-- or
UPDATE test SET data = zeroblob(:blob_size) WHERE rowid = :rowid;

* Read:

SELECT [rowid,] data FROM test [WHERE rowid = :rowid ...];
  stmt.setRowId(1, rowId); -- (1): optional if (2)
  ...
  ResultSet rs = stmt.getResultSet();

  Blob blob;
  while (rs.next()) {
    rs.getRowId(1); -- (2): mandatory if not (1)
    blob = rs.getBlob(2);
    // ...
  }
  blob.free();
  rs.close();

* Write:

UPDATE test SET data = :blob WHERE rowid = :rowid;
  smt.setRowId(2, rowId); -- mandatory, first
  smt.setBlob|setBinaryStream(1, ...);

 */
public class PrepStmt extends Stmt implements PreparedStatement, ParameterMetaData {
  private RowId rowId; // FIXME clear/reset to null when ?
  private Map<Integer, org.sqlite.Blob> blobByParamIndex = Collections.emptyMap();

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
    if (x == null) {
      getStmt().bindNull(parameterIndex);
    }
    throw Util.unsupported("PreparedStatement.setBinaryStream"); // FIXME
    // The data will be read from the stream as needed until end-of-file/length is reached.
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
    throw Util.unsupported("PreparedStatement.addBatch"); // Too cumbersome to implements
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
    if (x == null) {
      getStmt().bindNull(parameterIndex);
    }
    setBinaryStream(parameterIndex, x.getBinaryStream(), x.length());
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
    return new RowsMeta(getStmt());
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
    return this;
  }
  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    if (x == null) {
      getStmt().bindNull(parameterIndex);
    } else {
      rowId = x;
      getStmt().bindLong(parameterIndex, RowIdImpl.getValue(x));
    }
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
    setBinaryStream(parameterIndex, inputStream, length);
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
    if (x == null) {
      getStmt().bindNull(parameterIndex);
    }
    setBinaryStream(parameterIndex, x, BlobImpl.checkLength(length));
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
    if (x == null) {
      getStmt().bindNull(parameterIndex);
    }
    if (rowId == null) {
      throw new SQLException("You must set the associated RowId before opening a Blob");
    }
    org.sqlite.Blob blob = blobByParamIndex.get(parameterIndex);
    if (blob == null || blob.isClosed()) {
      blob = getStmt().open(parameterIndex, RowIdImpl.getValue(rowId), true); // FIXME getColumnOriginName can be called only with SELECT...
      if (blob != null) {
        if (blobByParamIndex.isEmpty() && !(blobByParamIndex instanceof TreeMap)) {
          blobByParamIndex = new TreeMap<Integer, org.sqlite.Blob>();
        }
        blobByParamIndex.put(parameterIndex, blob);
      } else {
        throw new SQLException("No Blob!"); // TODO improve message
      }
    } else {
      blob.reopen(RowIdImpl.getValue(rowId));
    }

    throw Util.unsupported("PreparedStatement.setBinaryStream"); // FIXME
    // The data will be read from the stream as needed until end-of-file is reached.
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
    setBinaryStream(parameterIndex, inputStream);
  }
  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    throw Util.unsupported("PreparedStatement.setNClob");
  }

  @Override
  public int getParameterCount() throws SQLException {
    return getStmt().getBindParameterCount();
  }
  @Override
  public int isNullable(int param) throws SQLException {
    return parameterNullableUnknown;
  }
  @Override
  public boolean isSigned(int param) throws SQLException {
    throw Util.unsupported("ParameterMetaData.isSigned");
  }
  @Override
  public int getPrecision(int param) throws SQLException {
    throw Util.unsupported("ParameterMetaData.getPrecision");
  }
  @Override
  public int getScale(int param) throws SQLException {
    throw Util.unsupported("ParameterMetaData.getScale");
  }
  @Override
  public int getParameterType(int param) throws SQLException {
    throw Util.unsupported("ParameterMetaData.getParameterType");
  }
  @Override
  public String getParameterTypeName(int param) throws SQLException {
    throw Util.unsupported("ParameterMetaData.getParameterTypeName");
  }
  @Override
  public String getParameterClassName(int param) throws SQLException {
    throw Util.unsupported("ParameterMetaData.getParameterClassName");
  }
  @Override
  public int getParameterMode(int param) throws SQLException {
    return parameterModeIn;
  }
}
