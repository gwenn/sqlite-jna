/**
 * https://github.com/dirk-olmes/sqlite-jdbc/blob/2b38f87a9f9182540a81b52e4bd39ccdcfdd6b37/src/test/java/org/sqlite/BatchTest.java
 */

package org.sqlite.driver;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class BatchTest {
	private Connection conn;

	@Before
	public void setUp() throws Exception {
		conn = DriverManager.getConnection(JDBC.MEMORY);

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("create table test (id integer primary key, stuff text);");
		}
	}

	@After
	public void tearDown() throws Exception {
		conn.close();
	}

	@Test
	public void clearParametersShouldNotDiscardBatch() throws Exception {
		try (PreparedStatement stmt = conn.prepareStatement("insert into test(id, stuff) values (?, ?)")) {

			for (int i = 0; i < 2; i++) {
				stmt.clearParameters();

				stmt.setInt(1, i);
				stmt.setString(2, "test" + i);
				stmt.addBatch();
			}
			stmt.executeBatch();

			assertRowCount();
		}
	}

	private void assertRowCount() throws Exception {
		try (Statement select = conn.createStatement()) {
			select.execute("select count(*) from test");
			try (ResultSet results = select.getResultSet()) {
				results.next();
				int rowCount = results.getInt(1);
				Assert.assertEquals(2, rowCount);
			}
		}
	}
}
