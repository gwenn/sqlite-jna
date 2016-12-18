/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.sqlite.ColTypes.SQLITE_NULL;
import static org.sqlite.SQLite.*;

public class Stmt implements AutoCloseable, Row {
	final Conn c;
	private long pStmt;
	private final String tail;
	// cached parameter count
	private int paramCount = -1;
	// cached parameters index by name
	private Map<String, Integer> params;
	// cached column count
	private int columnCount = -1;
	private String[] columnNames;
	private int[] columnAffinities;
	private boolean cacheable;

	Stmt(Conn c, long pStmt, String[] tail, boolean cacheable) {
		assert c != null;
		this.c = c;
		this.pStmt = pStmt;
		this.tail = blankToNull(tail[0]);
		this.cacheable = cacheable;
	}

	boolean isDumb() {
		return pStmt == 0;
	}

	public String getSql() {
		return sqlite3_sql(pStmt); // ok if pStmt is null
	}
	public String getTail() {
		return tail;
	}

	public String getErrMsg() {
		return c.getErrMsg();
	}

	@Override
	protected void finalize() throws Throwable {
		if (pStmt != 0) {
			sqlite3_log(-1, "dangling SQLite statement.");
			close(true);
		}
		super.finalize();
	}
	/**
	 * @return result code (No exception is thrown).
	 */
	public int closeNoCheck() {
		return close(false);
	}
	public int close(boolean force) {
		if (pStmt == 0) return SQLITE_OK;
		if (!force && cacheable && (tail == null || tail.isEmpty()) && !isBusy()) {
			if (sqlite3_reset(pStmt) == SQLITE_OK &&
					sqlite3_clear_bindings(pStmt) == SQLITE_OK &&
					c.release(this)) {
				return SQLITE_OK;
			}
		}
		synchronized (c.lock) {
			final int res = sqlite3_finalize(pStmt); // must be called only once
			pStmt = 0;
			return res;
		}
	}
	@Override
	public void close() throws StmtException {
		final int res = close(false);
		if (res != ErrCodes.SQLITE_OK) {
			throw new StmtException(this, "error while closing statement '%s'", res);
		}
	}

	/**
	 * Execute an INSERT and return the ROWID.
	 * @param params SQL statement parameters
	 * @return the rowid of the most recent successful INSERT into the database.
	 * @throws SQLiteException if no row is inserted or many rows are inserted.
	 */
	public long insert(Object... params) throws SQLiteException {
		final int changes = execDml(params);
		if (changes == 1) {
			return c.getLastInsertRowid();
		} else if (changes == 0) {
			throw new StmtException(this, String.format("No row inserted by '%s'", getSql()), ErrCodes.WRAPPER_SPECIFIC);
		} else {
			throw new StmtException(this, String.format("%d rows inserted by '%s'", changes, getSql()), ErrCodes.WRAPPER_SPECIFIC);
		}
	}

	/**
	 * Execute a DML and return the number of rows impacted.
	 * @param params SQL statement parameters
	 * @return the number of database rows that were changed or inserted or deleted.
	 */
	public int execDml(Object... params) throws SQLiteException {
		bind(params);
		exec();
		return c.getChanges();
	}

	/**
	 * Executes the prepared statement and maps a function over the resulting rows.
	 * @param mapper Row mapper
	 * @param params Statement parameters
	 * @return an iterator of mapped rows
	 */
	public <T> Iterator<T> queryMap(RowMapper<T> mapper, Object... params) throws SQLiteException {
		bind(params);
		return new Iterator<T>() {
			private State state = State.NOT_READY;
			@Override
			public boolean hasNext() {
				if (State.FAILED == state) {
					throw new IllegalStateException();
				}
				if (State.DONE == state) {
					return false;
				} else if (State.READY == state) {
					return true;
				}
				state = State.FAILED;
				try {
					if (Stmt.this.step(0)) {
						state = State.READY;
						return true;
					} else {
						state = State.DONE;
						return false;
					}
				} catch (SQLiteException e) {
					throw new IllegalStateException(e);
				}
			}
			@Override
			public T next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				state = State.NOT_READY;
				try {
					return mapper.map(Stmt.this);
				} catch (StmtException e) {
					throw new IllegalStateException(e);
				}
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	private enum State {
		READY, NOT_READY, DONE, FAILED,
	}

	/**
	 * @param params Statement parameters
	 * @return returns <code>true</code> if a query in the SQL statement it executes returns one or more rows and
	 * <code>false</code> if the SQL returns an empty set.
	 */
	public boolean exists(Object... params) throws SQLiteException {
		bind(params);
		return step(0);
	}

	/**
	 * @param timeout in seconds
	 * @return true until finished.
	 * @throws StmtException
	 */
	public boolean step(int timeout) throws SQLiteException {
		c.setQueryTimeout(timeout);
		final int res = sqlite3_step(pStmt); // ok if pStmt is null => SQLITE_MISUSE
		if (res == SQLITE_ROW) {
			return true;
		}
		// Release implicit lock as soon as possible
		sqlite3_reset(pStmt); // ok if pStmt is null
		if (res == SQLITE_DONE) {
			return false;
		}
		throw new StmtException(this, String.format("error while stepping '%s'", getSql()), res);
	}

	/**
	 * @param timeout in seconds
	 */
	public int stepNoCheck(int timeout) throws SQLiteException {
		c.setQueryTimeout(timeout);
		final int res = sqlite3_step(pStmt); // ok if pStmt is null => SQLITE_MISUSE
		if (res == SQLITE_ROW) {
			return res;
		}
		// Release implicit lock as soon as possible
		sqlite3_reset(pStmt); // ok if pStmt is null
		return res;
	}
	public void exec() throws SQLiteException {
		c.setQueryTimeout(0);
		final int res = sqlite3_step(pStmt); // ok if pStmt is null => SQLITE_MISUSE
		// Release implicit lock as soon as possible
		sqlite3_reset(pStmt); // ok if pStmt is null
		if (res == SQLITE_ROW) {
			throw new StmtException(this, String.format("only non SELECT expected but got '%s'", getSql()), res);
		}
		if (res != SQLITE_DONE) {
			throw new StmtException(this, String.format("error while executing '%s'", getSql()), res);
		}
	}

	public void reset() throws StmtException {
		check(sqlite3_reset(pStmt), "Error while resetting '%s'"); // ok if pStmt is null
	}

	public boolean isBusy() {
		return sqlite3_stmt_busy(pStmt); // ok if pStmt is null
	}

	public boolean isReadOnly() {
		return sqlite3_stmt_readonly(pStmt); // ok if pStmt is null
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public void clearBindings() throws StmtException {
		checkOpen();
		check(sqlite3_clear_bindings(pStmt), "Error while clearing bindings '%s'");
	}

	@Override
	public int getColumnCount() {
		if (columnCount == -1) {
			columnCount = sqlite3_column_count(pStmt); // ok if pStmt is null
		}
		return columnCount;
	}

	/**
	 * @return data count
	 */
	public int getDataCount() {
		return sqlite3_data_count(pStmt); // ok if pStmt is null
	}

	@Override
	public int getColumnType(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		return sqlite3_column_type(pStmt, iCol); // ok if pStmt is null
	}

	@Override
	public String getColumnDeclType(int iCol) throws StmtException {
		checkOpen();
		checkColumnIndex(iCol);
		return sqlite3_column_decltype(pStmt, iCol); // ko if pStmt is null
	}

	@Override
	public int getColumnAffinity(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		if (null == columnAffinities) {
			columnAffinities = new int[getColumnCount()];
			Arrays.fill(columnAffinities, -1);
		} else if (columnAffinities[iCol] != -1) {
			return columnAffinities[iCol];
		}
		columnAffinities[iCol] = getAffinity(getColumnDeclType(iCol));
		return columnAffinities[iCol];
	}

	@Override
	public String getColumnName(int iCol) throws StmtException {
		checkOpen();
		checkColumnIndex(iCol);
		if (null == columnNames) {
			columnNames = new String[getColumnCount()];
		} else if (columnNames[iCol] != null) {
			return columnNames[iCol];
		}
		columnNames[iCol] = sqlite3_column_name(pStmt, iCol); // ko if pStmt is null
		return columnNames[iCol];
	}
	@Override
	public String getColumnOriginName(int iCol) throws StmtException {
		checkOpen();
		checkColumnIndex(iCol);
		return sqlite3_column_origin_name(pStmt, iCol); // ko if pStmt is null
	}
	@Override
	public String getColumnTableName(int iCol) throws StmtException {
		checkOpen();
		checkColumnIndex(iCol);
		return sqlite3_column_table_name(pStmt, iCol); // ko if pStmt is null
	}
	@Override
	public String getColumnDatabaseName(int iCol) throws StmtException {
		checkOpen();
		checkColumnIndex(iCol);
		return sqlite3_column_database_name(pStmt, iCol); // ko if pStmt is null
	}

	@Override
	public byte[] getColumnBlob(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		final int type = getColumnType(iCol);
		if (type == SQLITE_NULL) {
			return null;
		}
		final byte[] blob = sqlite3_column_blob(pStmt, iCol); // ok if pStmt is null
		if (blob == null) {
			final int bytes = getColumnBytes(iCol);
			// The return value from sqlite3_column_blob() for a zero-length BLOB is a NULL pointer.
			if (bytes == 0) {
				return new byte[0];
			}
			throw new StmtException(this, String.format("sqlite3_column_blob returns a NULL pointer for a %d-length BLOB", bytes),
					ErrCodes.WRAPPER_SPECIFIC);
		} else {
			return blob;
		}
	}

	@Override
	public int getColumnBytes(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		return sqlite3_column_bytes(pStmt, iCol); // ok if pStmt is null
	}

	@Override
	public double getColumnDouble(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		return sqlite3_column_double(pStmt, iCol); // ok if pStmt is null
	}
	@Override
	public int getColumnInt(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		return sqlite3_column_int(pStmt, iCol); // ok if pStmt is null
	}
	@Override
	public long getColumnLong(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		return sqlite3_column_int64(pStmt, iCol); // ok if pStmt is null
	}
	@Override
	public String getColumnText(int iCol) throws StmtException {
		checkColumnIndex(iCol);
		return sqlite3_column_text(pStmt, iCol); // ok if pStmt is null
	}

	public void bind(Object... params) throws StmtException {
		reset();
		if (params.length != getBindParameterCount()) {
			throw new StmtException(this,
					String.format("incorrect argument count for Stmt.bind: have %d want %d", params.length, getBindParameterCount()),
					ErrCodes.WRAPPER_SPECIFIC);
		}
		for (int i = 0; i < params.length; i++) {
			bindByIndex(i + 1, params[i]);
		}
	}

	public void namedBind(Object... params) throws StmtException {
		reset();
		if (params.length % 2 != 0) {
			throw new StmtException(this, "expected an even number of arguments", ErrCodes.WRAPPER_SPECIFIC);
		}
		for (int i = 0; i < params.length; i += 2) {
			if (!(params[i] instanceof String)) {
				throw new StmtException(this, "non-string param name", ErrCodes.WRAPPER_SPECIFIC);
			}
			bindByIndex(getBindParameterIndex((String) params[i]), params[i + 1]);
		}
	}

	public void bindByIndex(int i, Object value) throws StmtException {
		if (value == null) {
			bindNull(i);
		} else if (value instanceof String) {
			bindText(i, (String) value);
		} else if (value instanceof Integer) {
			bindInt(i, (Integer) value);
		} else if (value instanceof Byte) {
			bindInt(i, (Byte) value);
		} else if (value instanceof Short) {
			bindInt(i, (Short) value);
		} else if (value instanceof Boolean) {
			bindInt(i, (Boolean) value ? 1 : 0);
		} else if (value instanceof Long) {
			bindLong(i, (Long) value);
		} else if (value instanceof Double) {
			bindDouble(i, (Double) value);
		} else if (value instanceof Float) {
			bindDouble(i, (Float) value);
		} else if (value instanceof byte[]) {
			bindBlob(i, (byte[]) value);
		} else if (value instanceof ZeroBlob) {
			bindZeroblob(i, ((ZeroBlob) value).n);
		} else {
			throw new StmtException(this, String.format("unsupported type in bind: %s", value.getClass().getSimpleName()), ErrCodes.WRAPPER_SPECIFIC);
		}
	}

	/**
	 * @return the number of SQL parameters
	 */
	public int getBindParameterCount() {
		if (paramCount == -1) {
			paramCount = sqlite3_bind_parameter_count(pStmt); // ok if pStmt is null
		}
		return paramCount;
	}

	/**
	 * @param name SQL parameter name
	 * @return SQL parameter index or 0 if no match (cached)
	 */
	public int getBindParameterIndex(String name) {
		if (params == null) {
			params = new HashMap<>(getBindParameterCount());
		}
		final Integer index = params.get(name);
		if (index != null) {
			return index;
		}
		final int i = sqlite3_bind_parameter_index(pStmt, name); // ok if pStmt is null
		if (i == 0) { // Invalid name
			return i;
		}
		params.put(name, i);
		return i;
	}
	/**
	 * @param i The leftmost SQL parameter has an index of 1
	 * @return SQL parameter name or null.
	 */
	public String getBindParameterName(int i) { // TODO Cache?
		return sqlite3_bind_parameter_name(pStmt, i); // ok if pStmt is null
	}

	/**
	 * @param i     The leftmost SQL parameter has an index of 1
	 * @param value SQL parameter value
	 * @throws StmtException
	 */
	public void bindBlob(int i, byte[] value) throws StmtException {
		if (value == null) {
			bindNull(i);
			return;
		}
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_blob(pStmt, i, value, value.length), "sqlite3_bind_blob", i);
	}
	/**
	 * @param i     The leftmost SQL parameter has an index of 1
	 * @param value SQL parameter value
	 * @throws StmtException
	 */
	public void bindDouble(int i, double value) throws StmtException {
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_double(pStmt, i, value), "sqlite3_bind_double", i);
	}
	/**
	 * @param i     The leftmost SQL parameter has an index of 1
	 * @param value SQL parameter value
	 * @throws StmtException
	 */
	public void bindInt(int i, int value) throws StmtException {
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_int(pStmt, i, value), "sqlite3_bind_int", i);
	}
	/**
	 * @param i     The leftmost SQL parameter has an index of 1
	 * @param value SQL parameter value
	 * @throws StmtException
	 */
	public void bindLong(int i, long value) throws StmtException {
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_int64(pStmt, i, value), "sqlite3_bind_int64", i);
	}
	/**
	 * @param i The leftmost SQL parameter has an index of 1
	 * @throws StmtException
	 */
	public void bindNull(int i) throws StmtException {
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_null(pStmt, i), "sqlite3_bind_null", i);
	}
	/**
	 * @param i     The leftmost SQL parameter has an index of 1
	 * @param value SQL parameter value
	 * @throws StmtException
	 */
	public void bindText(int i, String value) throws StmtException {
		if (value == null) {
			bindNull(i);
			return;
		}
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_text(pStmt, i, value, -1), "sqlite3_bind_text", i);
	}
	/**
	 * @param i The leftmost SQL parameter has an index of 1
	 * @param n length of BLOB
	 * @throws StmtException
	 */
	public void bindZeroblob(int i, int n) throws StmtException {
		// ok if pStmt is null => SQLITE_MISUSE
		checkBind(sqlite3_bind_zeroblob(pStmt, i, n), "sqlite3_bind_zeroblob", i);
	}

	private static final boolean[] UNKNOWN = new boolean[3];
	public boolean[] getMetadata(int iCol) throws StmtException, ConnException {
		final String colName = getColumnOriginName(iCol);
		if (colName != null) {
			return c.getTableColumnMetadata(
					getColumnDatabaseName(iCol), getColumnTableName(iCol), colName);
		}
		return UNKNOWN;
	}

	void check(int res, String format) throws StmtException {
		if (res != SQLITE_OK) {
			throw new StmtException(this, String.format(format, getSql()), res);
		}
	}
	private void checkBind(int res, String method, int i) throws StmtException {
		if (res != SQLITE_OK) {
			throw new StmtException(this, String.format("error while calling %s for param %d of '%s'", method, i, getSql()), res);
		}
	}
	public void checkOpen() throws StmtException {
		if (pStmt == 0) {
			throw new StmtException(this, "stmt finalized", ErrCodes.WRAPPER_SPECIFIC);
		}
		if (c.isClosed()) {
			throw new StmtException(this, "connection closed", ErrCodes.WRAPPER_SPECIFIC);
		}
	}

	public boolean isClosed() {
		return pStmt == 0;
	}

	// Only lossy conversion is reported as error.
	public void checkTypeMismatch(int iCol, int sourceType, int targetType) throws StmtException {
		switch (targetType) {
			case ColTypes.SQLITE_INTEGER:
				switch (sourceType) {
					case ColTypes.SQLITE_FLOAT:
					case ColTypes.SQLITE_TEXT:
					case ColTypes.SQLITE_BLOB:
						throw new StmtException(this, String.format("Type mismatch for %s, source %d vs target %d", getColumnName(iCol), sourceType, targetType), ErrCodes.WRAPPER_SPECIFIC);
				}
				break;
			case ColTypes.SQLITE_FLOAT:
				switch (sourceType) {
					case ColTypes.SQLITE_TEXT:
					case ColTypes.SQLITE_BLOB:
						throw new StmtException(this, String.format("Type mismatch for %s, source %d vs target %d", getColumnName(iCol), sourceType, targetType), ErrCodes.WRAPPER_SPECIFIC);
				}
		}
	}

	public Blob open(int iCol, long iRow, boolean rw) throws SQLiteException {
		/*final int type = getColumnType(iCol);
    if (ColTypes.SQLITE_NULL == type) { // cannot open value of type null
      return null;
    }*/
		final String colName = getColumnOriginName(iCol);
		if (colName != null) {
			return c.open(getColumnDatabaseName(iCol), getColumnTableName(iCol), colName, iRow, rw);
		}
		return null;
	}
	public String encoding(int iCol) throws SQLiteException {
		return c.encoding(getColumnDatabaseName(iCol));
	}

	public int status(StmtStatus op, boolean reset) throws StmtException {
		checkOpen();
		return sqlite3_stmt_status(pStmt, op.value, reset);
	}

	private void checkColumnIndex(int iCol) throws StmtException {
		if (iCol < 0 || iCol >= getColumnCount()) {
			throw new StmtException(this, String.format("column index %d out of range [0,%d[.", iCol, getColumnCount()), ErrCodes.SQLITE_RANGE);
		}
	}

	private static String blankToNull(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isSpaceChar(s.charAt(i))) {
				return s;
			}
		}
		return null;
	}
}
