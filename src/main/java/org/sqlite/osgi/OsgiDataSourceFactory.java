package org.sqlite.osgi;

import org.osgi.service.jdbc.DataSourceFactory;
import org.sqlite.ErrCodes;
import org.sqlite.driver.BasicDataSource;
import org.sqlite.driver.JDBC;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Properties;

public class OsgiDataSourceFactory implements DataSourceFactory {
	@Override
	public DataSource createDataSource(Properties props) throws SQLException {
		// Make copy of properties
		Properties copy = new Properties();
		if (props != null) {
			copy.putAll(props);
		}
		// Verify that no unsupported standard options are used
		rejectUnsupportedOptions(copy);
		// Standard pool properties in OSGi not applicable here
		rejectPoolingOptions(copy);

		BasicDataSource ds = new BasicDataSource();
		setupDataSource(ds, copy);
		return ds;
	}

	private static void setupDataSource(BasicDataSource ds, Properties p) {
		// Setting user and password
		p.remove(DataSourceFactory.JDBC_USER);
		p.remove(DataSourceFactory.JDBC_PASSWORD);
		// Setting description
		p.remove(DataSourceFactory.JDBC_DESCRIPTION);
		if (p.containsKey(DataSourceFactory.JDBC_URL)) {
			ds.setFilename((String)p.remove(DataSourceFactory.JDBC_URL));
		} else {
			ds.setFilename((String)p.remove(DataSourceFactory.JDBC_DATABASE_NAME));
		}
	}

	@Override
	public ConnectionPoolDataSource createConnectionPoolDataSource(Properties props) throws SQLException {
		throw new SQLFeatureNotSupportedException("ConnectionPoolDataSource not implemented");
	}

	@Override
	public XADataSource createXADataSource(Properties props) throws SQLException {
		throw new SQLFeatureNotSupportedException("XADataSource not implemented");
	}

	@Override
	public Driver createDriver(Properties props) throws SQLException {
		if (props != null && !props.isEmpty()) {
			throw new SQLException(String.format("Unsupported properties: %s", props.stringPropertyNames()), null,
				ErrCodes.WRAPPER_SPECIFIC);
		}
		return new JDBC();
	}

	private static void rejectUnsupportedOptions(Properties p) throws SQLFeatureNotSupportedException {
		// Unsupported standard properties in OSGi
		for (String key : Arrays.asList(DataSourceFactory.JDBC_ROLE_NAME,
			DataSourceFactory.JDBC_DATASOURCE_NAME,
			DataSourceFactory.JDBC_NETWORK_PROTOCOL,
			DataSourceFactory.JDBC_SERVER_NAME,
			DataSourceFactory.JDBC_PORT_NUMBER)) {
			throw new SQLFeatureNotSupportedException(String.format("The %s property is not supported by SQLite", key));
		}
	}
	private static void rejectPoolingOptions(Properties p)
		throws SQLFeatureNotSupportedException {
		if (p.containsKey(DataSourceFactory.JDBC_INITIAL_POOL_SIZE) ||
			p.containsKey(DataSourceFactory.JDBC_MAX_IDLE_TIME) ||
			p.containsKey(DataSourceFactory.JDBC_MAX_POOL_SIZE) ||
			p.containsKey(DataSourceFactory.JDBC_MAX_STATEMENTS) ||
			p.containsKey(DataSourceFactory.JDBC_MIN_POOL_SIZE) ||
			p.containsKey(DataSourceFactory.JDBC_PROPERTY_CYCLE)) {
			throw new SQLFeatureNotSupportedException(
				"Pooling properties are not supported by SQLite");
		}
	}
}
