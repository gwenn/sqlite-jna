/**
 * Copy from https://github.com/xerial/sqlite-jdbc/blob/master/src/test/java/org/sqlite/SavepointTest.java
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
import java.sql.Savepoint;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * These tests assume that Statements and PreparedStatements are working as per
 * normal and test the interactions of commit(), setSavepoint(), setSavepoint(String),
 * rollback(Savepoint), and release(Savepoint).
 */
public class SavepointTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Connection conn1, conn2;
	private Statement stat1, stat2;

	@Before
	public void connect() throws Exception {
		final File file = folder.newFile("test-trans.db");

		conn1 = DriverManager.getConnection(JDBC.PREFIX + "file:" + file + "?cache=private");
		conn2 = DriverManager.getConnection(JDBC.PREFIX + "file:" + file + "?cache=private");

		stat1 = conn1.createStatement();
		stat2 = conn2.createStatement();
	}

	@After
	public void close() throws Exception {
		Guard.closeAll(stat1, stat2, conn1, conn2);
	}

	@Test
	public void insert() throws SQLException {
		String countSql = "select count(*) from trans;";

		stat1.executeUpdate("create table trans (c1);");
		conn1.setSavepoint();

		assertEquals(1, stat1.executeUpdate("insert into trans values (4);"));

		// transaction not yet committed, conn1 can see, conn2 can not
		try (ResultSet rs = stat1.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
		try (ResultSet rs = stat2.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(0, rs.getInt(1));
		}

		conn1.commit();

		// all connects can see data
		try (ResultSet rs = stat2.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
	}

	@Test
	public void rollback() throws SQLException {
		String select = "select * from trans;";

		stat1.executeUpdate("create table trans (c1);");
		Savepoint sp = conn1.setSavepoint();
		stat1.executeUpdate("insert into trans values (3);");

		try (ResultSet rs = stat1.executeQuery(select)) {
			assertTrue(rs.next());
		}

		conn1.rollback(sp);

		try (ResultSet rs = stat1.executeQuery(select)) {
			assertFalse(rs.next());
		}
	}

	@Test
	public void multiRollback() throws SQLException {

		stat1.executeUpdate("create table t (c1);");
		conn1.setSavepoint();
		stat1.executeUpdate("insert into t values (1);");
		conn1.commit();

		Savepoint sp = conn1.setSavepoint();
		stat1.executeUpdate("insert into t values (1);");
		conn1.rollback(sp);

		stat1.addBatch("insert into t values (2);");
		stat1.addBatch("insert into t values (3);");
		stat1.executeBatch();
		conn1.commit();

		Savepoint sp7 = conn1.setSavepoint("num7");
		stat1.addBatch("insert into t values (7);");
		stat1.executeBatch();

		// nested savepoint
		Savepoint sp8 = conn1.setSavepoint("num8");
		stat1.addBatch("insert into t values (8);");
		stat1.executeBatch();
		conn1.rollback(sp8);

		conn1.rollback(sp7);

		stat1.executeUpdate("insert into t values (4);");

		conn1.setAutoCommit(true);
		stat1.executeUpdate("insert into t values (5);");
		conn1.setAutoCommit(false);
		try (PreparedStatement p = conn1.prepareStatement("insert into t values (?);")) {
			p.setInt(1, 6);
			p.executeUpdate();
			p.setInt(1, 7);
			p.executeUpdate();
		}

		// conn1 can see (1+...+7), conn2 can see (1+...+5)
		try (ResultSet rs = stat1.executeQuery("select sum(c1) from t;")) {
			assertTrue(rs.next());
			assertEquals(1 + 2 + 3 + 4 + 5 + 6 + 7, rs.getInt(1));
		}

		try (ResultSet rs = stat2.executeQuery("select sum(c1) from t;")) {
			assertTrue(rs.next());
			assertEquals(1 + 2 + 3 + 4 + 5, rs.getInt(1));
		}
	}

	@Test
	public void release() throws SQLException {
		String countSql = "select count(*) from trans;";

		stat1.executeUpdate("create table trans (c1);");

		Savepoint outerSP = conn1.setSavepoint("outer_sp");
		assertEquals(1, stat1.executeUpdate("insert into trans values (4);"));

		// transaction not yet committed, conn1 can see, conn2 can not
		try (ResultSet rs = stat1.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
		try (ResultSet rs = stat2.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(0, rs.getInt(1));
		}

		Savepoint innerSP = conn1.setSavepoint("inner_sp");
		assertEquals(1, stat1.executeUpdate("insert into trans values (5);"));

		// transaction not yet committed, conn1 can see, conn2 can not
		try (ResultSet rs = stat1.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(2, rs.getInt(1));
		}
		try (ResultSet rs = stat2.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(0, rs.getInt(1));
		}

		// releasing an inner savepoint, statements are still wrapped by the outer savepoint
		conn1.releaseSavepoint(innerSP);

		try (ResultSet rs = stat2.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(0, rs.getInt(1));
		}

		// releasing the outer savepoint is like a commit
		conn1.releaseSavepoint(outerSP);

		// all connects can see SP1 data
		try (ResultSet rs = stat2.executeQuery(countSql)) {
			assertTrue(rs.next());
			assertEquals(2, rs.getInt(1));
		}
	}
}
