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
import org.sqlite.parser.ast.Release;
import org.sqlite.parser.ast.Rollback;

import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

class Conn implements Connection {
	private org.sqlite.Conn c;
	final String[] dateTimeConfig;

	private DbMeta meta;
	PreparedStatement getGeneratedKeys;

	private Properties clientInfo;
	private int savepointId;
	private SQLWarning warnings;
	private int transactionIsolation = TRANSACTION_SERIALIZABLE;

	Conn(org.sqlite.Conn c, String[] dateTimeConfig, SQLWarning warnings) {
		this.c = c;
		this.dateTimeConfig = dateTimeConfig;
		this.warnings = warnings;
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
	public String nativeSQL(String sql) {
		Util.trace("Connection.nativeSQL");
		return sql;
	}

	// FIXME By default, new connections should be in auto-commit mode.
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		final org.sqlite.Conn c = getConn();
		if (c.getAutoCommit() == autoCommit) return;
		c.fastExec(autoCommit ? "COMMIT" : "BEGIN");
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return getConn().getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		final org.sqlite.Conn c = getConn();
		if (c.getAutoCommit()) throw new ConnException(c, "database in auto-commit mode", ErrCodes.WRAPPER_SPECIFIC);
		c.fastExec("COMMIT; BEGIN");
	}

	@Override
	public void rollback() throws SQLException {
		final org.sqlite.Conn c = getConn();
		if (getAutoCommit()) throw new ConnException(c, "database in auto-commit mode", ErrCodes.WRAPPER_SPECIFIC);
		c.fastExec("ROLLBACK; BEGIN");
	}

	@Override
	public void close() throws SQLException {
		if (c != null) {
			Guard.closeAll(getGeneratedKeys, c);
			if (clientInfo != null) clientInfo.clear();
			c = null;
		}
	}

	@Override
	public boolean isClosed() {
		return c == null;
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		checkOpen();
		if (meta == null) meta = new DbMeta(this);
		return meta;
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		checkOpen();
		final boolean ro = c.isReadOnly(null);
		final boolean qo = c.isQueryOnly(null);
		if (ro == readOnly && qo == readOnly) {
			return;
		}
		if (ro && !readOnly) {
			addWarning(new SQLWarning("readOnly mode cannot be reset"));
		} else {
			if (!c.getAutoCommit()) {
				throw new ConnException(c, "setReadOnly is called during a transaction", ErrCodes.WRAPPER_SPECIFIC);
			}
			c.setQueryOnly(null, readOnly);
		}
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		final org.sqlite.Conn c = getConn();
		return c.isReadOnly(null) || c.isQueryOnly(null);
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		checkOpen();
		addWarning(new SQLWarning("catalog cannot be set"));
	}

	@Override
	public String getCatalog() throws SQLException {
		checkOpen();
		return null; // "main" is not the default catalog ("temp" catalog is searched first)
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		checkOpen();
		if (level == transactionIsolation) {
			return;
		}
		if (level == TRANSACTION_SERIALIZABLE) {
			c.setReadUncommitted(null, false);
		} else if (level == TRANSACTION_READ_UNCOMMITTED) {
			if (!c.isSharedCacheMode()) {
				addWarning(new SQLWarning("read_uncommitted is effective only in shared-cache mode."));
			}
			c.setReadUncommitted(null, true);
		} else {
			throw Util.error("SQLite supports only TRANSACTION_SERIALIZABLE or TRANSACTION_READ_UNCOMMITTED.");
		}
		transactionIsolation = level;
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		checkOpen();
		return transactionIsolation;
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

	// A SAVEPOINT can be started either within or outside of a BEGIN...COMMIT.
	// When a SAVEPOINT is the outermost savepoint and it is not within a BEGIN...COMMIT
	// then the behavior is the same as BEGIN DEFERRED TRANSACTION.
	@Override
	public Savepoint setSavepoint() throws SQLException {
		final int id = savepointId++;
		final Savepoint savepoint = new Savepoint() {
			@Override
			public int getSavepointId() {
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
		getConn().fastExec("SAVEPOINT \"" + id + '"'); // SAVEPOINT 1; fails
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
			public String getSavepointName() {
				return name;
			}

			@Override
			public String toString() {
				return name;
			}
		};
		org.sqlite.parser.ast.Savepoint sp = new org.sqlite.parser.ast.Savepoint(name);
		getConn().fastExec(sp.toSql());
		return savepoint;
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		Rollback rollback = new Rollback(null, savepoint.toString());
		getConn().fastExec(rollback.toSql());
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		Release release = new Release(savepoint.toString());
		getConn().fastExec(release.toSql());
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		checkOpen();
		checkCursor(resultSetType, resultSetConcurrency, resultSetHoldability);
		return new Stmt(this);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		final org.sqlite.Conn c = getConn();
		checkCursor(resultSetType, resultSetConcurrency, resultSetHoldability);
		return new PrepStmt(this, c.prepare(sql, true), Generated.NO_GENERATED_KEYS);
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		checkOpen();
		throw Util.unsupported("Connection.prepareCall");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		final Generated generated = checkAutoGeneratedKeys(autoGeneratedKeys);
		final org.sqlite.Conn c = getConn();
		return new PrepStmt(this, c.prepare(sql, true), generated);
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		checkOpen();
		if (empty(columnIndexes)) {
			return prepareStatement(sql);
		}
		throw Util.unsupported("Connection.prepareStatement(String,int[])");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		checkOpen();
		if (empty(columnNames)) {
			return prepareStatement(sql);
		}
		// https://sqlite.org/lang_returning.html
		// TODO Check columnNames match RETURNING clause
		return prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
		return new SQLXMLImpl(Charset.forName(getConn().encoding(null)));
	}

	@Override
	public boolean isValid(int seconds) throws SQLException {
		Util.trace("Connection.isValid");
		if (seconds < 0) throw Util.error("timeout must be >= 0");
		return !isClosed();
	}

	@Override
	public void setClientInfo(String name, String value) {
		Util.trace("Connection.setClientInfo(String,String)");
		//checkOpen();
		if (clientInfo == null) return;
		clientInfo.setProperty(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) {
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

	//#if mvn.project.property.jdbc.specification.version >= "4.1"
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
	//#endif

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		} else if (org.sqlite.Conn.class.equals(iface)) {
			return iface.cast(c);
		}
		throw new SQLException("Cannot unwrap to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass()) || org.sqlite.Conn.class.equals(iface);
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

	static Generated checkAutoGeneratedKeys(int autoGeneratedKeys) throws SQLException {
		if (Statement.NO_GENERATED_KEYS != autoGeneratedKeys && Statement.RETURN_GENERATED_KEYS != autoGeneratedKeys) {
			throw new SQLException(String.format("unsupported autoGeneratedKeys value: %d", autoGeneratedKeys));
		}
		return Statement.RETURN_GENERATED_KEYS == autoGeneratedKeys ? Generated.RETURN_GENERATED_KEYS : Generated.NO_GENERATED_KEYS;
	}
	static boolean empty(int[] columnIndexes) {
		return columnIndexes == null || columnIndexes.length == 0;
	}
	static boolean empty(String[] columnNames) {
		return columnNames == null || columnNames.length == 0;
	}
}
