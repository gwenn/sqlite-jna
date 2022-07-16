/*
This program follows the Apache License version 2.0 (<http://www.apache.org/licenses/> ) That means:

It allows you to:

*   freely download and use this software, in whole or in part, for personal, company internal, or commercial purposes;
*   use this software in packages or distributions that you create.

It forbids you to:

*   redistribute any piece of our originated software without proper attribution;
*   use any marks owned by us in any way that might state or imply that we xerial.org endorse your distribution;
*   use any marks owned by us in any way that might state or imply that you created this software in question.

It requires you to:

*   include a copy of the license in any redistribution you may make that includes this software;
*   provide clear attribution to us, xerial.org for any distributions that include this software

It does not require you to:

*   include the source of this software itself, or of any modifications you may have
    made to it, in any redistribution you may assemble that includes it;
*   submit changes that you make to the software back to this software (though such feedback is encouraged).

See License FAQ <http://www.apache.org/foundation/licence-FAQ.html> for more details.
 */

package org.sqlite.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InsertQueryTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private String dbName;

	@Before
	public void setUp() throws Exception {
		File tmpFile = folder.newFile("tmp-sqlite.db");
		dbName = tmpFile.getAbsolutePath();
	}

	@After
	public void tearDown() throws Exception {

	}

	interface ConnectionFactory {
		Connection getConnection() throws SQLException;

		void dispose() throws SQLException;
	}

	class IndependentConnectionFactory implements ConnectionFactory {
		public Connection getConnection() throws SQLException {
			return DriverManager.getConnection(JDBC.PREFIX + dbName);
		}

		public void dispose() throws SQLException {

		}

	}

	class SharedConnectionFactory implements ConnectionFactory {
		private Connection conn = null;

		public Connection getConnection() throws SQLException {
			if (conn == null)
				conn = DriverManager.getConnection(JDBC.PREFIX + dbName);
			return conn;
		}

		public void dispose() throws SQLException {
			if (conn != null)
				conn.close();
		}
	}

	static class BD {
		String fullId;
		String type;

		BD(String fullId, String type) {
			this.fullId = fullId;
			this.type = type;
		}

		public String getFullId() {
			return fullId;
		}

		public void setFullId(String fullId) {
			this.fullId = fullId;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public static byte[] serializeBD(BD item) {
			return new byte[0];
		}

	}

	@Test
	public void insertLockTestUsingSharedConnection() throws Exception {
		insertAndQuery(new SharedConnectionFactory());
	}

	@Test
	public void insertLockTestUsingIndependentConnection() throws Exception {
		insertAndQuery(new IndependentConnectionFactory());
	}

	void insertAndQuery(ConnectionFactory factory) throws SQLException {
		try {
			Statement st = factory.getConnection().createStatement();
			st
					.executeUpdate("CREATE TABLE IF NOT EXISTS data (fid VARCHAR(255) PRIMARY KEY, type VARCHAR(64), data BLOB);");
			st
					.executeUpdate("CREATE TABLE IF NOT EXISTS ResourcesTags (bd_fid VARCHAR(255), name VARCHAR(64), version INTEGER);");
			st.close();

			factory.getConnection().setAutoCommit(false);

			// Object Serialization
			PreparedStatement statAddBD = factory.getConnection().prepareStatement(
					"INSERT OR REPLACE INTO data values (?, ?, ?)");
			PreparedStatement statDelRT = factory.getConnection().prepareStatement(
					"DELETE FROM ResourcesTags WHERE bd_fid = ?");
			PreparedStatement statAddRT = factory.getConnection().prepareStatement(
					"INSERT INTO ResourcesTags values (?, ?, ?)");

			for (int i = 0; i < 10; i++) {
				BD item = new BD(Integer.toHexString(i), Integer.toString(i));

				// SQLite database insertion
				statAddBD.setString(1, item.getFullId());
				statAddBD.setString(2, item.getType());
				statAddBD.setBytes(3, BD.serializeBD(item));
				statAddBD.execute();

				// Then, its resources tags
				statDelRT.setString(1, item.getFullId());
				statDelRT.execute();

				statAddRT.setString(1, item.getFullId());

				for (int j = 0; j < 2; j++) {
					statAddRT.setString(2, "1");
					statAddRT.setLong(3, 1L);
					statAddRT.execute();
				}
			}

			factory.getConnection().setAutoCommit(true);

			statAddBD.close();
			statDelRT.close();
			statAddRT.close();

			//
			PreparedStatement stat;
			Long result = 0L;
			String query = "SELECT COUNT(fid) FROM data";

			stat = factory.getConnection().prepareStatement(query);
			ResultSet rs = stat.executeQuery();

			rs.next();
			result = rs.getLong(1);
			//System.out.println("count = " + result);

			rs.close();
			stat.close();
		} finally {
			factory.dispose();
		}

	}

	@Test(expected = SQLException.class)
	public void reproduceDatabaseLocked() throws SQLException {
		try (Connection conn = DriverManager.getConnection(JDBC.PREFIX + dbName);
			Connection conn2 = DriverManager.getConnection(JDBC.PREFIX + dbName);
			Statement stat = conn.createStatement();
			Statement stat2 = conn2.createStatement()) {

			stat.executeUpdate("drop table if exists sample");
			stat.executeUpdate("create table sample(id, name)");
			conn.setAutoCommit(false);
			stat.executeUpdate("insert into sample values(1, 'leo')");

			try (ResultSet rs = stat2.executeQuery("select count(*) from sample")) {
				rs.next();
				conn.commit(); // causes "database is locked" (SQLITE_BUSY)
			}
		}
	}
}
