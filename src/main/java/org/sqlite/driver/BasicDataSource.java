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

public class BasicDataSource extends JDBC implements DataSource, Referenceable {
	private String filename = org.sqlite.Conn.TEMP_FILE;
	private int loginTimeout;
	private PrintWriter logWriter;

	public String getFilename() {
		return filename;
	}
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
	public PrintWriter getLogWriter() throws SQLException {
		return logWriter;
	}
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		this.logWriter = out;
	}
	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		this.loginTimeout = seconds;
	}
	@Override
	public int getLoginTimeout() throws SQLException {
		return loginTimeout;
	}
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw Util.error("not a wrapper");
	}
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public Reference getReference() throws NamingException {
		final Reference ref = new Reference(getClass().getName());
		ref.add(new StringRefAddr("filename", filename));
		return ref;
	}
}
