/**
 * Copy from https://github.com/xerial/sqlite-jdbc/blob/master/src/test/java/org/sqlite/ResultSetTest.java
 */
package org.sqlite.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResultSetTest {

	private Connection conn;
	private Statement stat;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.MEMORY);
		stat = conn.createStatement();
		stat.executeUpdate("create table test (id int primary key, DESCRIPTION varchar(40), fOo varchar(3), some_date Date);");
		stat.executeUpdate("insert into test values (1, 'description', 'bar', '2016-01-01 00:00:00')");
	}

	@After
	public void close() throws SQLException {
		stat.close();
		conn.close();
	}

	@Test
	public void testTableColumnLowerNowFindLowerCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(1, resultSet.findColumn("id"));
		}
	}

	@Test
	public void testTableColumnLowerNowFindUpperCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(1, resultSet.findColumn("ID"));
		}
	}

	@Test
	public void testTableColumnLowerNowFindMixedCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(1, resultSet.findColumn("Id"));
		}
	}

	@Test
	public void testTableColumnUpperNowFindLowerCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(2, resultSet.findColumn("description"));
		}
	}

	@Test
	public void testTableColumnUpperNowFindUpperCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(2, resultSet.findColumn("DESCRIPTION"));
		}
	}

	@Test
	public void testTableColumnUpperNowFindMixedCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(2, resultSet.findColumn("Description"));
		}
	}

	@Test
	public void testTableColumnMixedNowFindLowerCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(3, resultSet.findColumn("foo"));
		}
	}

	@Test
	public void testTableColumnMixedNowFindUpperCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(3, resultSet.findColumn("FOO"));
		}
	}

	@Test
	public void testTableColumnMixedNowFindMixedCaseColumn()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select * from test")) {
			assertEquals(3, resultSet.findColumn("fOo"));
		}
	}

	@Test
	public void testSelectWithTableNameAliasNowFindWithoutTableNameAlias()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select t.id from test as t")) {
			assertEquals(1, resultSet.findColumn("id"));
		}
	}

	/**
	 * Can't produce a case where column name contains table name
	 * https://www.sqlite.org/c3ref/column_name.html :
	 * "If there is no AS clause then the name of the column is unspecified"
	 */
	@Test(expected = SQLException.class)
	public void testSelectWithTableNameAliasNowNotFindWithTableNameAlias()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select t.id from test as t")) {
			resultSet.findColumn("t.id");
		}
	}

	@Test
	public void testSelectWithTableNameNowFindWithoutTableName()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select test.id from test")) {
			assertEquals(1, resultSet.findColumn("id"));
		}
	}

	@Test(expected = SQLException.class)
	public void testSelectWithTableNameNowNotFindWithTableName()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select test.id from test")) {
			resultSet.findColumn("test.id");
		}
	}

	@Test
	public void testSelectIso8601Timestamp()
			throws SQLException {
		try (ResultSet resultSet = stat.executeQuery("select test.some_date from test")) {
			assertTrue(resultSet.next());
			Timestamp expected = Timestamp.valueOf("2016-01-01 00:00:00");
			assertEquals(expected, resultSet.getTimestamp(1));
		}
	}
}
