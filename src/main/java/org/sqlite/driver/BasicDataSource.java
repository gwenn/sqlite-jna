package org.sqlite.driver;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Simple DataSource which does not perform connection pooling. In order to use the DataSource, you
 * must set the property filename.
 */
public class BasicDataSource extends JDBC implements DataSource, Referenceable {
	private String filename = org.sqlite.Conn.TEMP_FILE;
	private int loginTimeout;
	private PrintWriter logWriter;

	/**
	 * Gets the path of the SQLite database.
	 * @return path of the SQLite database.
	 */
	public String getFilename() {
		return filename;
	}
	/**
	 * Sets the path of the SQLite database. If this is changed, it will only affect future calls to getConnection.
	 * @param filename path of the SQLite database.
	 */
	public void setFilename(String filename) {
		if (filename == null) {
			filename = org.sqlite.Conn.TEMP_FILE;
		}
		this.filename = filename;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connect(filename.startsWith(PREFIX) ? filename : PREFIX + filename, null);
	}
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		final Properties info = new Properties();
		if (username != null) {
			info.put("user", username);
		}
		if (password != null) {
			info.put("password", password);
		}
		return connect(filename.startsWith(PREFIX) ? filename : PREFIX + filename, info);
	}
	@Override
	public PrintWriter getLogWriter() {
		return logWriter;
	}
	@Override
	public void setLogWriter(PrintWriter out) {
		logWriter = out;
	}
	@Override
	public void setLoginTimeout(int seconds) {
		loginTimeout = seconds;
	}
	@Override
	public int getLoginTimeout() {
		return loginTimeout;
	}
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

	@Override
	public Reference getReference() {
		final Reference ref = new Reference(getClass().getName());
		ref.add(new StringRefAddr("filename", filename));
		return ref;
	}
}
