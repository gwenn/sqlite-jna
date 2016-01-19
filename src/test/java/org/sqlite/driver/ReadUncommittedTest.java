/**
 * Copy from https://github.com/xerial/sqlite-jdbc/blob/master/src/test/java/org/sqlite/ReadUncommittedTest.java
 */
package org.sqlite.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ReadUncommittedTest {
	private Connection conn;
	private Statement stat;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.PREFIX + "file:memdb1?mode=memory&cache=shared");
		stat = conn.createStatement();
		stat.executeUpdate("create table test (id integer primary key, fn, sn);");
		stat.executeUpdate("create view testView as select * from test;");
	}

	@After
	public void close() throws SQLException {
		stat.close();
		conn.close();
	}

	@Test
	public void setReadUncommitted() throws SQLException {
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
	}

	@Test
	public void setSerializable() throws SQLException {
		conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test(expected = SQLException.class)
	public void setUnsupportedIsolationLevel() throws SQLException {
		conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
	}
}
