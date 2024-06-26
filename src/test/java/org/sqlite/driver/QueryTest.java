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
import org.junit.Test;
import org.sqlite.ConnException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class QueryTest {
	private Connection conn;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.MEMORY);
	}

	@After
	public void close() throws SQLException {
		conn.close();
	}

	@Test
	public void nullQuery() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.execute(null);
		} catch (ConnException e) {
		}
	}

	@Test
	public void createTable() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE IF NOT EXISTS sample " + "(id INTEGER PRIMARY KEY, descr VARCHAR(40))");
		}
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM sample")) {
				assertFalse(rs.next());
			}
		}
	}

	@Test
	public void setFloatTest() throws Exception {
		float f = 3.141597f;
		try (Statement s = conn.createStatement()) {
			s.execute("create table sample (data NOAFFINITY)");
		}
		try (PreparedStatement prep = conn.prepareStatement("insert into sample values(?)")) {
			prep.setFloat(1, f);
			prep.executeUpdate();
		}

		try (PreparedStatement stmt = conn.prepareStatement("select * from sample where data > ?")) {
			stmt.setObject(1, 3.0f);
			try (ResultSet rs = stmt.executeQuery()) {
				assertTrue(rs.next());
				float f2 = rs.getFloat(1);
				assertEquals(f, f2, 0.0000001);
			}
		}
	}

	@Test
	public void dateTimeTest() throws Exception {
		Date now;
		try (Statement s = conn.createStatement()) {
			s.execute("create table sample (start_time datetime)");

			now = new Date();
			String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX").format(now);

			s.execute("insert into sample values(" + now.getTime() + ")");
			s.execute("insert into sample values('" + date + "')");

			try (ResultSet rs = s.executeQuery("select * from sample")) {
				assertTrue(rs.next());
				assertEquals(now, rs.getTimestamp(1));
				assertTrue(rs.next());
				assertEquals(now, rs.getTimestamp(1));
			}
		}

		try (PreparedStatement stmt = conn.prepareStatement("insert into sample values(?)")) {
			stmt.setDate(1, new java.sql.Date(now.getTime()));
		}
	}

	@Test
	public void viewTest() throws Exception {
		String tableName = "sample";
		try (Statement st1 = conn.createStatement()) {
			// drop table if it already exists
			st1.execute("DROP TABLE IF EXISTS " + tableName);
		}
		try (Statement st2 = conn.createStatement()) {
			st2.execute("DROP VIEW IF EXISTS " + tableName);
		}
	}

	@Test
	public void timeoutTest() throws Exception {
		try (Statement st1 = conn.createStatement()) {
			st1.setQueryTimeout(1);
		}
	}

	@Test
	public void concatTest() throws SQLException {
		try (Statement statement = conn.createStatement()) {
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			statement.executeUpdate("drop table if exists person");
			statement.executeUpdate("create table person (id integer, name string, shortname string)");
			statement.executeUpdate("insert into person values(1, 'leo','L')");
			statement.executeUpdate("insert into person values(2, 'yui','Y')");
			statement.executeUpdate("insert into person values(3, 'abc', null)");

			statement.executeUpdate("drop table if exists message");
			statement.executeUpdate("create table message (id integer, subject string)");
			statement.executeUpdate("insert into message values(1, 'Hello')");
			statement.executeUpdate("insert into message values(2, 'World')");

			statement.executeUpdate("drop table if exists mxp");
			statement.executeUpdate("create table mxp (pid integer, mid integer, type string)");
			statement.executeUpdate("insert into mxp values(1,1, 'F')");
			statement.executeUpdate("insert into mxp values(2,1,'T')");
			statement.executeUpdate("insert into mxp values(1,2, 'F')");
			statement.executeUpdate("insert into mxp values(2,2,'T')");
			statement.executeUpdate("insert into mxp values(3,2,'T')");

			try (ResultSet rs = statement
				.executeQuery("select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=2 and mxp.pid=person.id and mxp.type='T'")) {
				while (rs.next()) {
					// read the result set
					assertEquals("Y,abc", rs.getString(1));
				}
			}
			try (ResultSet rs = statement
				.executeQuery("select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=1 and mxp.pid=person.id and mxp.type='T'")) {
				while (rs.next()) {
					// read the result set
					assertEquals("Y", rs.getString(1));
				}
			}
		}

		try (PreparedStatement ps = conn
			.prepareStatement("select group_concat(ifnull(shortname, name)) from mxp, person where mxp.mid=? and mxp.pid=person.id and mxp.type='T'")) {
			ps.clearParameters();
			ps.setInt(1, 2);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					// read the result set
					assertEquals("Y,abc", rs.getString(1));
				}
			}
			ps.clearParameters();
			ps.setInt(1, 2);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					// read the result set
					assertEquals("Y,abc", rs.getString(1));
				}
			}
		}
	}

	@Test
	public void emptyBlobTest() throws SQLException {
		try (Statement st = conn.createStatement()) {
			st.executeUpdate("CREATE TABLE test (data BLOB)");
			st.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(0))");
			try (ResultSet rs = st.executeQuery("SELECT data from test")) {
				assertTrue(rs.next());
				assertArrayEquals(new byte[0], rs.getBytes(1));
			}
		}
	}

	@Test
	public void notEmptyBlobTest() throws SQLException {
		try (Statement st = conn.createStatement()) {
			st.executeUpdate("CREATE TABLE test (data BLOB)");
			st.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(5))");
			try (ResultSet rs = st.executeQuery("SELECT data from test")) {
				assertTrue(rs.next());
				assertArrayEquals(new byte[5], rs.getBytes(1));
			}
		}
	}

	@Test
	public void nullBlobTest() throws SQLException {
		try (Statement st = conn.createStatement()) {
			st.executeUpdate("CREATE TABLE test (data BLOB)");
			st.executeUpdate("INSERT INTO test (data) VALUES (NULL)");
			try (ResultSet rs = st.executeQuery("SELECT data from test")) {
				assertTrue(rs.next());
				assertNull(rs.getBytes(1));
			}
		}
	}
}
