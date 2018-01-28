/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.sqlite.ColTypes;
import org.sqlite.ErrCodes;
import org.sqlite.StmtException;

class Rows implements ResultSet {
	private Stmt s;
	private org.sqlite.Stmt stmt;
	private RowsMeta meta;
	private int row; // -1: no data, 0: before first, 1: first, ..., -2: after last
	private Boolean wasNull;
	private RowIdImpl rowId;
	private Map<Integer, org.sqlite.Blob> blobByColIndex = Collections.emptyMap();

	Rows(Stmt s, boolean hasRow) throws SQLException {
		this.s = s;
		stmt = s.getStmt();
		row = hasRow ? 0 : -1; // Initialized at -1 when there is no result otherwise 0
	}

	private org.sqlite.Stmt getStmt() throws SQLException {
		checkOpen();
		// TODO Check Statement is opened?
		return stmt;
	}

	private void checkOpen() throws SQLException {
		if (stmt == null) {
			throw new SQLException("resultSet closed");
		}
	}

	private boolean step() throws SQLException {
		checkOpen();
		if (row < 0) { // no result or after last
			return false;
		}
		if (row == 0) {
			row++;
			return true;
		}
		final int maxRows = s.getMaxRows();
		if (maxRows != 0 && row >= maxRows) {
			row = -2;
			stmt.reset();
			return false;
		}

		final boolean hasRow = s.step(false);
		if (hasRow) {
			row++;
		} else {
			row = -2;
		}
		return hasRow;
	}

	private int fixCol(int columnIndex) throws StmtException {
		if (row < 0) {
			throw new StmtException(stmt, "No data available", ErrCodes.WRAPPER_SPECIFIC);
		} else if (row == 0) {
			throw new StmtException(stmt, "No data available until next() is called.", ErrCodes.WRAPPER_SPECIFIC);
		}
		return columnIndex - 1;
	}

	@Override
	public boolean next() throws SQLException {
		wasNull = null;
		rowId = null;
		return step();
	}

	@Override
	public void close() throws SQLException {
		//Util.trace("ResultSet.close");
		if (stmt != null) {
			if (!stmt.isClosed()) {
				if (s.isCloseOnCompletion()) {
					s.close();
				} else {
					stmt.reset();
				}
			}
			s = null;
			stmt = null;
			meta = null;
			for (org.sqlite.Blob blob : blobByColIndex.values()) {
				blob.closeNoCheck();
			}
			blobByColIndex.clear();
		}
	}

	@Override
	public boolean wasNull() throws SQLException {
		if (wasNull == null) {
			throw new SQLException("no column has been read");
		}
		return wasNull;
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		final String str = getStmt().getColumnText(fixCol(columnIndex));
		wasNull = str == null;
		return str;
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return getInt(columnIndex) != 0;
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return (byte) getInt(columnIndex);
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return (short) getInt(columnIndex);
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		stmt.checkTypeMismatch(fixCol(columnIndex), sourceType, ColTypes.SQLITE_INTEGER);
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		if (wasNull) {
			return 0;
		} else {
			return stmt.getColumnInt(fixCol(columnIndex));
		}
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		stmt.checkTypeMismatch(fixCol(columnIndex), sourceType, ColTypes.SQLITE_INTEGER);
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		if (wasNull) {
			return 0L;
		} else {
			return stmt.getColumnLong(fixCol(columnIndex));
		}
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return (float) getDouble(columnIndex);
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		stmt.checkTypeMismatch(fixCol(columnIndex), sourceType, ColTypes.SQLITE_FLOAT);
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		if (wasNull) {
			return 0.0;
		} else {
			return stmt.getColumnDouble(fixCol(columnIndex));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw Util.unsupported("Resultset.getBigDecimal(int,int)");
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		final byte[] blob = getStmt().getColumnBlob(fixCol(columnIndex));
		wasNull = blob == null;
		return blob;
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return getDate(columnIndex, null);
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return getTime(columnIndex, null);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return getTimestamp(columnIndex, null);
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getAsciiStream");
	}

	@Override
	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getUnicodeStream");
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		if (rowId != null) {
			final Blob blob = getBlob(columnIndex);
			return blob == null ? null : blob.getBinaryStream();
		} else { // no streaming...
			final byte[] bytes = getBytes(columnIndex);
			if (bytes == null) {
				return null;
			}
			return new ByteArrayInputStream(bytes);
		}
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return getString(findColumn(columnLabel));
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return getBoolean(findColumn(columnLabel));
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return getByte(findColumn(columnLabel));
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return getShort(findColumn(columnLabel));
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return getInt(findColumn(columnLabel));
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return getLong(findColumn(columnLabel));
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return getFloat(findColumn(columnLabel));
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return getDouble(findColumn(columnLabel));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return getBigDecimal(findColumn(columnLabel), scale);
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return getBytes(findColumn(columnLabel));
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return getDate(findColumn(columnLabel));
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		return getTime(findColumn(columnLabel));
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return getTimestamp(findColumn(columnLabel));
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return getAsciiStream(findColumn(columnLabel));
	}

	@Override
	@SuppressWarnings("deprecation")
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return getUnicodeStream(findColumn(columnLabel));
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return getBinaryStream(findColumn(columnLabel));
	}

	@Override
	public SQLWarning getWarnings() {
		// checkOpen();
		return null;
	}

	@Override
	public void clearWarnings() {
		// checkOpen();
	}

	@Override
	public String getCursorName() throws SQLException {
		Util.trace("ResultSet.getCursorName");
		checkOpen();
		return null;
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException { // Used by Hibernate
		checkOpen();
		if (meta == null) {
			meta = new RowsMeta(stmt);
		}
		return meta;
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		switch (sourceType) {
			case ColTypes.SQLITE_TEXT:
				return getString(columnIndex);
			case ColTypes.SQLITE_INTEGER:
				final long l = getLong(columnIndex);
				if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
					return (int) l;
				}
				return l;
			case ColTypes.SQLITE_FLOAT:
				return getDouble(columnIndex);
			case ColTypes.SQLITE_BLOB:
				return getBytes(columnIndex);
			case ColTypes.SQLITE_NULL:
				wasNull = true;
				return null;
			default:
				throw new AssertionError(String.format("Unknown column type %d", sourceType));
		}
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return getObject(findColumn(columnLabel));
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		checkOpen();
		return s.findCol(columnLabel);
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		if (rowId != null) {
			try {
				final InputStream in = getBinaryStream(columnIndex);
				if (in == null) {
					return null;
				}
				return new InputStreamReader(in, getStmt().encoding(columnIndex));
			} catch (UnsupportedEncodingException e) {
				throw new StmtException(getStmt(), e.getMessage(), ErrCodes.WRAPPER_SPECIFIC);
			}
		} else { // no streaming...
			final String s = getString(columnIndex);
			if (s == null) {
				return null;
			}
			return new StringReader(s);
		}
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return getCharacterStream(findColumn(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		final String stringValue = getString(columnIndex);
		if (stringValue == null) {
			return null;
		} else {
			try {
				return new BigDecimal(stringValue);
			} catch (NumberFormatException e) {
				throw new StmtException(getStmt(), "Bad value for type BigDecimal : " + stringValue, ErrCodes.WRAPPER_SPECIFIC);
			}
		}
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return getBigDecimal(findColumn(columnLabel));
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		checkOpen();
		return row == 0;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		checkOpen();
		return row == -2;
	}

	@Override
	public boolean isFirst() throws SQLException {
		checkOpen();
		return row == 1;
	}

	@Override
	public boolean isLast() throws SQLException {
		Util.trace("ResultSet.isLast");
		checkOpen();
		final int maxRows = s.getMaxRows();
		if (maxRows != 0 && row == maxRows) {
			return true;
		}
		return false; // TODO
	}

	@Override
	public void beforeFirst() throws SQLException {
		throw typeForwardOnly();
	}

	@Override
	public void afterLast() throws SQLException {
		throw typeForwardOnly();
	}

	@Override
	public boolean first() throws SQLException {
		throw typeForwardOnly();
	}

	@Override
	public boolean last() throws SQLException {
		throw typeForwardOnly();
	}

	@Override
	public int getRow() throws SQLException {
		checkOpen();
		return Math.max(row, 0);
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		throw typeForwardOnly();
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		throw typeForwardOnly();
	}

	@Override
	public boolean previous() throws SQLException {
		throw typeForwardOnly();
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
		return FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		if (rows < 0) throw Util.error("fetch size must be >= 0");
		checkOpen();
		if (rows == 0) {
			return;
		}
		if (rows != 1) {
			throw Util.caseUnsupported("SQLite does not support setting fetch size");
		}
	}

	@Override
	public int getFetchSize() throws SQLException {
		checkOpen();
		return 1;
	}

	@Override
	public int getType() throws SQLException {
		checkOpen();
		return TYPE_FORWARD_ONLY;
	}

	@Override
	public int getConcurrency() throws SQLException {
		checkOpen();
		return CONCUR_READ_ONLY;
	}

	@Override
	public boolean rowUpdated() throws SQLException {
		checkOpen();
		return false;
	}

	@Override
	public boolean rowInserted() throws SQLException {
		checkOpen();
		return false;
	}

	@Override
	public boolean rowDeleted() throws SQLException {
		checkOpen();
		return false;
	}

	@Override
	public void updateNull(int columnIndex) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateShort(int columnIndex, short x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateInt(int columnIndex, int x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateLong(int columnIndex, long x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateString(int columnIndex, String x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNull(String columnLabel) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateShort(String columnLabel, short x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateInt(String columnLabel, int x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateLong(String columnLabel, long x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateString(String columnLabel, String x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void insertRow() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateRow() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void deleteRow() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void refreshRow() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void cancelRowUpdates() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void moveToInsertRow() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public Statement getStatement() throws SQLException {
		checkOpen();
		return s;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		if (map == null || map.isEmpty()) {
			return getObject(columnIndex);
		}
		throw Util.unsupported("ResultSet.getObject(int,Map)");
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getRef");
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		checkOpen();
		if (rowId == null) { // FIXME check PrepStmt.rowId as well...
			throw new SQLException("You must read the associated RowId before opening a Blob");
		}
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		if (wasNull) {
			return null;
		}
		org.sqlite.Blob blob = blobByColIndex.get(columnIndex);
		if (blob == null || blob.isClosed()) {
			blob = getStmt().open(fixCol(columnIndex), rowId.value, false);
			if (blob != null) {
				if (blobByColIndex.isEmpty() && !(blobByColIndex instanceof TreeMap)) {
					blobByColIndex = new TreeMap<>();
				}
				blobByColIndex.put(columnIndex, blob);
			}
		} else {
			blob.reopen(rowId.value);
		}
		return blob == null ? null : new BlobImpl(blob);
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getClob");
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getArray");
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return getObject(findColumn(columnLabel), map);
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return getRef(findColumn(columnLabel));
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		return getBlob(findColumn(columnLabel));
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		return getClob(findColumn(columnLabel));
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return getArray(findColumn(columnLabel));
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		switch (sourceType) {
			case ColTypes.SQLITE_NULL:
				return null;
			case ColTypes.SQLITE_TEXT: // does not work as expected if column type affinity is TEXT but inserted value was a numeric
				final String txt = stmt.getColumnText(fixCol(columnIndex));
				return DateUtil.toDate(txt, cal);
			case ColTypes.SQLITE_INTEGER:
				final long unixepoch = stmt.getColumnLong(fixCol(columnIndex));
				return DateUtil.toDate(unixepoch, cal);
			case ColTypes.SQLITE_FLOAT: // does not work as expected if column affinity is REAL but inserted value was an integer
				final double jd = stmt.getColumnDouble(fixCol(columnIndex));
				return DateUtil.toDate(jd, cal);
			default:
				throw new SQLException("The column type is not one of SQLITE_INTEGER, SQLITE_FLOAT, SQLITE_TEXT, or SQLITE_NULL");
		}
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return getDate(findColumn(columnLabel), cal);
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		switch (sourceType) {
			case ColTypes.SQLITE_NULL:
				return null;
			case ColTypes.SQLITE_TEXT: // does not work as expected if column type affinity is TEXT but inserted value was a numeric
				final String txt = stmt.getColumnText(fixCol(columnIndex));
				return DateUtil.toTime(txt, cal);
			case ColTypes.SQLITE_INTEGER:
				final long unixepoch = stmt.getColumnLong(fixCol(columnIndex));
				return DateUtil.toTime(unixepoch);
			case ColTypes.SQLITE_FLOAT: // does not work as expected if column affinity is REAL but inserted value was an integer
				final double jd = stmt.getColumnDouble(fixCol(columnIndex));
				return DateUtil.toTime(jd);
			default:
				throw new SQLException("The column type is not one of SQLITE_INTEGER, SQLITE_FLOAT, SQLITE_TEXT, or SQLITE_NULL");
		}
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return getTime(findColumn(columnLabel), cal);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		final org.sqlite.Stmt stmt = getStmt();
		// After a type conversion, the value returned by sqlite3_column_type() is undefined.
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		switch (sourceType) {
			case ColTypes.SQLITE_NULL:
				return null;
			case ColTypes.SQLITE_TEXT: // does not work as expected if column type affinity is TEXT but inserted value was a numeric
				final String txt = stmt.getColumnText(fixCol(columnIndex));
				return DateUtil.toTimestamp(txt, cal);
			case ColTypes.SQLITE_INTEGER:
				final long unixepoch = stmt.getColumnLong(fixCol(columnIndex));
				return DateUtil.toTimestamp(unixepoch);
			case ColTypes.SQLITE_FLOAT: // does not work as expected if column affinity is REAL but inserted value was an integer
				final double jd = stmt.getColumnDouble(fixCol(columnIndex));
				return DateUtil.toTimestamp(jd);
			default:
				throw new SQLException("The column type is not one of SQLITE_INTEGER, SQLITE_FLOAT, SQLITE_TEXT, or SQLITE_NULL");
		}
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return getTimestamp(findColumn(columnLabel), cal);
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getURL");
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return getURL(findColumn(columnLabel));
	}

	@Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public RowId getRowId(int columnIndex) throws SQLException {
		rowId = new RowIdImpl(getLong(columnIndex));
		return rowId;
	}

	@Override
	public RowId getRowId(String columnLabel) throws SQLException {
		return getRowId(findColumn(columnLabel));
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public int getHoldability() {
		return CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() {
		return stmt == null;
	}

	@Override
	public void updateNString(int columnIndex, String nString) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNString(String columnLabel, String nString) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		throw Util.unsupported("ResultSet.getNClob");
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return getNClob(findColumn(columnLabel));
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		final int sourceType = stmt.getColumnType(fixCol(columnIndex));
		wasNull = sourceType == ColTypes.SQLITE_NULL;
		if (wasNull) {
			return null;
		}
		return new SQLXMLFromRows(this, columnIndex);
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return getSQLXML(findColumn(columnLabel));
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		return getString(columnIndex);
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		return getNString(findColumn(columnLabel));
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return getCharacterStream(columnIndex);
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return getNCharacterStream(findColumn(columnLabel));
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		throw concurReadOnly();
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) throws SQLException {
		throw concurReadOnly();
	}

	//#if mvn.project.property.jdbc.specification.version >= "4.1"
	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		throw Util.unsupported("ResultSet.getObject(int, Class)");
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		return getObject(findColumn(columnLabel), type);
	}
	//#endif

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		throw new SQLException("Cannot unwrap to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass());
	}

	private static SQLException typeForwardOnly() {
		return Util.error("ResultSet is TYPE_FORWARD_ONLY");
	}

	private static SQLException concurReadOnly() {
		return Util.error("ResultSet is CONCUR_READ_ONLY");
	}
}
