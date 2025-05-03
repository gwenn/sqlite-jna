package org.sqlite.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.ConnException;

import java.lang.reflect.Method;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * These tests are designed to stress Statements on memory databases.
 */
public class StatementTest {
	private Connection conn;
	private Statement stat;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.MEMORY);
		stat = conn.createStatement();
	}

	@After
	public void close() throws SQLException {
		Guard.closeAll(stat, conn);
	}

	@Test
	public void executeUpdate() throws SQLException {
		assertEquals(0, stat.executeUpdate("create table s1 (c1);"));
		assertEquals(1, stat.executeUpdate("insert into s1 values (0);"));
		assertEquals(1, stat.executeUpdate("insert into s1 values (1);"));
		assertEquals(1, stat.executeUpdate("insert into s1 values (2);"));
		assertEquals(3, stat.executeUpdate("update s1 set c1 = 5;"));
		// count_changes_pgrama. truncate_optimization
		assertEquals(3, stat.executeUpdate("delete from s1;"));

		// multiple SQL statements
		int tuc = stat.executeUpdate("insert into s1 values (11);" +
				"insert into s1 values (12)");
		while (!stat.getMoreResults()) {
			final int uc = stat.getUpdateCount();
			if (uc == -1) {
				break;
			}
			tuc += uc;
		}
		assertEquals(2, tuc);
		tuc = stat.executeUpdate("update s1 set c1 = 21 where c1 = 11;" +
				"update s1 set c1 = 22 where c1 = 12;" +
				"update s1 set c1 = 23 where c1 = 13");
		while (!stat.getMoreResults()) {
			final int uc = stat.getUpdateCount();
			if (uc == -1) {
				break;
			}
			tuc += uc;
		}
		assertEquals(2, tuc); // c1 = 13 does not exist
		tuc = stat.executeUpdate("delete from s1 where c1 = 21;" +
				"delete from s1 where c1 = 22;" +
				"delete from s1 where c1 = 23");
		while (!stat.getMoreResults()) {
			final int uc = stat.getUpdateCount();
			if (uc == -1) {
				break;
			}
			tuc += uc;
		}
		assertEquals(2, tuc); // c1 = 23 does not exist

		assertEquals(0, stat.executeUpdate("drop table s1;"));

		assertEquals(0, stat.executeUpdate("CREATE TABLE t (t TEXT);"));
		assertEquals(0, stat.executeUpdate("ALTER TABLE t RENAME TO t2;"));
	}

	@Test
	public void stmtUpdate() throws SQLException {
		assertEquals(0, stat.executeUpdate("create table s1 (c1);"));
		assertEquals(1, stat.executeUpdate("insert into s1 values (0);"));
		assertEquals(1, stat.executeUpdate("insert into s1 values (1);"));
		assertEquals(1, stat.executeUpdate("insert into s1 values (2);"));
		try (ResultSet rs = stat.executeQuery("select count(c1) from s1;")) {
			assertTrue(rs.next());
			assertEquals(3, rs.getInt(1));
		}
		assertEquals(3, stat.executeUpdate("update s1 set c1 = 5;"));
		assertEquals(3, stat.executeUpdate("delete from s1;"));
		assertEquals(0, stat.executeUpdate("drop table s1;"));
	}

	@Test
	public void emptyRS() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select null limit 0;")) {
			assertFalse(rs.next());
		}
	}

	@Test
	public void singleRowRS() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select " + Integer.MAX_VALUE + ";")) {
			assertTrue(rs.next());
			assertEquals(Integer.MAX_VALUE, rs.getInt(1));
			assertEquals(Integer.toString(Integer.MAX_VALUE), rs.getString(1));
			assertEquals(Integer.valueOf(Integer.MAX_VALUE).doubleValue(),
					rs.getDouble(1), 1e-3);
			assertFalse(rs.next());
			assertTrue(rs.isAfterLast());
			assertFalse(rs.next());
		}
	}

	@Test
	public void twoRowRS() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select 9 union all select 7;")) {
			assertTrue(rs.next());
			assertEquals(9, rs.getInt(1));
			assertTrue(rs.next());
			assertEquals(7, rs.getInt(1));
			assertFalse(rs.next());
		}
	}

	@Test
	public void autoClose() throws SQLException {
		conn.createStatement().executeQuery("select 1;");
	}

	@Test
	public void stringRS() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select \"Russell\";")) {
			assertTrue(rs.next());
			assertEquals("Russell", rs.getString(1));
			assertFalse(rs.next());
		}
	}

	@Test
	public void execute() throws SQLException {
		assertTrue(stat.execute("select null;"));
		ResultSet rs = stat.getResultSet();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertNull(rs.getString(1));
		assertTrue(rs.wasNull());
		assertFalse(stat.getMoreResults());
		assertEquals(-1, stat.getUpdateCount());

		assertTrue(stat.execute("select null;"));
		assertFalse(stat.getMoreResults());
		assertEquals(-1, stat.getUpdateCount());

		assertFalse(stat.execute("create table test (c1);"));
		assertEquals(0, stat.getUpdateCount());
		assertFalse(stat.getMoreResults());
		assertEquals(-1, stat.getUpdateCount());

		assertFalse(stat.execute("insert into test values ('1'); insert into test values ('2');"));
		assertEquals(1, stat.getUpdateCount());
		assertFalse(stat.getMoreResults());
		assertEquals(1, stat.getUpdateCount());
		assertFalse(stat.getMoreResults());
		assertEquals(-1, stat.getUpdateCount());
	}

	@Test
	public void colNameAccess() throws SQLException {
		assertEquals(0, stat.executeUpdate(
				"create table tab (id, firstname, surname);"));
		assertEquals(1, stat.executeUpdate(
				"insert into tab values (0, 'Bob', 'Builder');"));
		assertEquals(1, stat.executeUpdate(
				"insert into tab values (1, 'Fred', 'Blogs');"));
		assertEquals(1, stat.executeUpdate(
				"insert into tab values (2, 'John', 'Smith');"));
		try (ResultSet rs = stat.executeQuery("select * from tab;")) {
			assertTrue(rs.next());
			assertEquals(0, rs.getInt("id"));
			assertEquals("Bob", rs.getString("firstname"));
			assertEquals("Builder", rs.getString("surname"));
			assertTrue(rs.next());
			assertEquals(1, rs.getInt("id"));
			assertEquals("Fred", rs.getString("firstname"));
			assertEquals("Blogs", rs.getString("surname"));
			assertTrue(rs.next());
			assertEquals(2, rs.getInt("id"));
			assertEquals("2", rs.getString("id"));
			assertEquals("John", rs.getString("firstname"));
			assertEquals("Smith", rs.getString("surname"));
			assertFalse(rs.next());
		}
		assertEquals(0, stat.executeUpdate("drop table tab;"));
	}

	@Test
	public void nulls() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select null union all select null;")) {
			assertTrue(rs.next());
			assertNull(rs.getString(1));
			assertTrue(rs.wasNull());
			assertTrue(rs.next());
			assertNull(rs.getString(1));
			assertTrue(rs.wasNull());
			assertFalse(rs.next());
		}
	}

	@Test
	public void nullsForGetObject() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select 1, null union all select null, null;")) {
			assertTrue(rs.next());
			assertNotNull(rs.getString(1));
			assertFalse(rs.wasNull());
			assertNull(rs.getObject(2));
			assertTrue(rs.wasNull());
			assertTrue(rs.next());
			assertNull(rs.getObject(2));
			assertTrue(rs.wasNull());
			assertNull(rs.getObject(1));
			assertTrue(rs.wasNull());
			assertFalse(rs.next());
		}
	}

	@Test
	public void tempTable() throws SQLException {
		assertEquals(0, stat.executeUpdate("create temp table myTemp (a);"));
		assertEquals(1, stat.executeUpdate("insert into myTemp values (2);"));
	}

	@Test
	public void insert1000() throws SQLException {
		assertEquals(0, stat.executeUpdate("create table in1000 (a);"));
		conn.setAutoCommit(false);
		for (int i = 0; i < 1000; i++)
			assertEquals(1, stat.executeUpdate(
					"insert into in1000 values (" + i + ");"));
		conn.commit();

		try (ResultSet rs = stat.executeQuery("select count(a) from in1000;")) {
			assertTrue(rs.next());
			assertEquals(1000, rs.getInt(1));
		}

		assertEquals(0, stat.executeUpdate("drop table in1000;"));
	}

	private void assertArrayEq(int[] a, int[] b) {
		assertNotNull(a);
		assertNotNull(b);
		assertEquals(a.length, b.length);
		for (int i = 0; i < a.length; i++)
			assertEquals(a[i], b[i]);
	}

	@Test
	public void batch() throws SQLException {
		stat.addBatch("create table batch (c1);");
		stat.addBatch("insert into batch values (1);");
		stat.addBatch("insert into batch values (1);");
		stat.addBatch("insert into batch values (2);");
		stat.addBatch("insert into batch values (3);");
		stat.addBatch("insert into batch values (4);");
		stat.addBatch("insert into batch values (5);");
		stat.addBatch("insert into batch values (6);");
		stat.addBatch("insert into batch values (7);");
		stat.addBatch("insert into batch values (8);");
		stat.addBatch("insert into batch values (9);");
		stat.addBatch("insert into batch values (10);");
		stat.addBatch("insert into batch values (11);");
		stat.addBatch("insert into batch values (12);");
		assertArrayEq(new int[]{0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
				stat.executeBatch());
		assertArrayEq(new int[]{}, stat.executeBatch());
		stat.clearBatch();
		stat.addBatch("insert into batch values (9);");
		assertArrayEq(new int[]{1}, stat.executeBatch());
		assertArrayEq(new int[]{}, stat.executeBatch());
		stat.clearBatch();
		stat.addBatch("insert into batch values (7);");
		stat.addBatch("insert into batch values (7);");
		assertArrayEq(new int[]{1, 1}, stat.executeBatch());
		stat.clearBatch();

		try (ResultSet rs = stat.executeQuery("select count(*) from batch;")) {
			assertTrue(rs.next());
			assertEquals(16, rs.getInt(1));
		}
	}

	@Test
	public void closeOnFalseNext() throws SQLException {
		stat.executeUpdate("create table t1 (c1);");
		try (Statement stmt = conn.createStatement()) {
			stmt.executeQuery("select * from t1;").next();
			stat.executeUpdate("drop table t1;");
		}
	}

	@Test
	public void getGeneratedKeys() throws SQLException {
		stat.executeUpdate("create table t1 (c1 integer primary key, v);");
		stat.executeUpdate("insert into t1 (v) values ('red');", RETURN_GENERATED_KEYS);
		try (ResultSet rs = stat.getGeneratedKeys()) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
		stat.executeUpdate("insert into t1 (v) values ('blue');", RETURN_GENERATED_KEYS);
		try (ResultSet rs = stat.getGeneratedKeys()) {
			assertTrue(rs.next());
			assertEquals(2, rs.getInt(1));
		}

		// closing one statement shouldn't close shared db metadata object.
		stat.close();
		try (Statement stat2 = conn.createStatement();
			ResultSet rs = stat2.getGeneratedKeys()) {
			assertNotNull(rs);
		}
	}

	@Test
	public void isBeforeFirst() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select 1 union all select 2;")) {
			assertTrue(rs.isBeforeFirst());
			assertTrue(rs.next());
			assertTrue(rs.isFirst());
			assertEquals(1, rs.getInt(1));
			assertTrue(rs.next());
			assertFalse(rs.isBeforeFirst());
			assertFalse(rs.isFirst());
			assertEquals(2, rs.getInt(1));
			assertFalse(rs.next());
			assertFalse(rs.isBeforeFirst());
		}
	}

	@Test
	public void columnNaming() throws SQLException {
		stat.executeUpdate("create table t1 (c1 integer);");
		stat.executeUpdate("create table t2 (c1 integer);");
		stat.executeUpdate("insert into t1 values (1);");
		stat.executeUpdate("insert into t2 values (1);");
		try (ResultSet rs = stat.executeQuery(
				"select a.c1 AS c1 from t1 a, t2 where a.c1=t2.c1;")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt("c1"));
		}
	}

	@Test
	public void nullDate() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select null;")) {
			assertTrue(rs.next());
			assertNull(rs.getDate(1));
			assertNull(rs.getTime(1));
			assertNull(rs.getTimestamp(1));
		}
	}

	@Test
	public void ambiguousColumnNaming() throws SQLException {
		stat.executeUpdate("create table t1 (c1 int);");
		stat.executeUpdate("create table t2 (c1 int, c2 int);");
		stat.executeUpdate("insert into t1 values (1);");
		stat.executeUpdate("insert into t2 values (2, 1);");
		try (ResultSet rs = stat.executeQuery(
				"select a.c1, b.c1 from t1 a, t2 b where a.c1=b.c2;")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt("c1"));
		}
	}

	@Test(expected = SQLException.class)
	public void failToDropWhenRSOpen() throws SQLException {
		stat.executeUpdate("create table t1 (c1);");
		stat.executeUpdate("insert into t1 values (4);");
		stat.executeUpdate("insert into t1 values (4);");
		try (Statement statement = conn.createStatement()) {
			statement.executeQuery("select * from t1;").next();
			stat.executeUpdate("drop table t1;");
		}
	}

	@Test(expected = SQLException.class)
	public void executeNoRS() throws SQLException {
		assertFalse(stat.execute("insert into test values (8);"));
		stat.getResultSet();
	}

	@Test
	public void executeClearRS() throws SQLException {
		assertTrue(stat.execute("select null;"));
		assertNotNull(stat.getResultSet());
		assertFalse(stat.getMoreResults());
		assertNull(stat.getResultSet());
	}

	@Test(expected = BatchUpdateException.class)
	public void batchReturnsResults() throws SQLException {
		stat.addBatch("select null;");
		stat.executeBatch();
	}

	@Test(expected = SQLException.class)
	public void noSuchTable() throws SQLException {
		stat.executeQuery("select * from doesnotexist;");
	}

	@Test(expected = SQLException.class)
	public void noSuchCol() throws SQLException {
		stat.executeQuery("select notacol from (select 1);");
	}

	@Test(expected = SQLException.class)
	public void noSuchColName() throws SQLException {
		try (ResultSet rs = stat.executeQuery("select 1;")) {
			assertTrue(rs.next());
			rs.getInt("noSuchColName");
		}
	}

	@Test
	public void multipleStatements() throws SQLException {
		stat.executeUpdate("create table person (id integer, name string); " +
				"insert into person values(1, 'leo'); insert into person values(2, 'yui');");
		while (stat.getMoreResults() || stat.getUpdateCount() != -1) {
		}

		try (ResultSet rs = stat.executeQuery("select * from person")) {
			assertTrue(rs.next());
			assertTrue(rs.next());
		}
	}

	@Test
	public void dateTimeTest() throws SQLException {
		Date day = new Date(new java.util.Date().getTime());

		stat.executeUpdate("create table day (time datetime)");
		try (PreparedStatement prep = conn.prepareStatement("insert into day values(?)")) {
			prep.setDate(1, day);
			prep.executeUpdate();
		}
		try (ResultSet rs = stat.executeQuery("select * from day")) {
			assertTrue(rs.next());
			Date d = rs.getDate(1);
			assertEquals(day.toString(), d.toString());
		}
	}

	@Test
	public void maxRows() throws SQLException {
		stat.setMaxRows(1);
		assertEquals(1, stat.getMaxRows());
		try (ResultSet rs = stat.executeQuery("select 1 union select 2 union select 3")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
			assertFalse(rs.next());
		}
		stat.setMaxRows(2);
		try (ResultSet rs = stat.executeQuery("select 1 union select 2 union select 3")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
			assertTrue(rs.next());
			assertEquals(2, rs.getInt(1));
			assertFalse(rs.next());
		}
	}

	@Test
	public void queryTimeout() throws SQLException {
		final int timeout = 30;
		stat.setQueryTimeout(timeout);
		assertEquals(timeout, stat.getQueryTimeout());
	}

	@Test
	public void blobTest() throws SQLException {
		stat.executeUpdate("CREATE TABLE Foo (KeyId INTEGER, Stuff BLOB)");
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void setEscapeProcessingToFalse() throws SQLException {
		stat.setEscapeProcessing(false);
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void setEscapeProcessingToTrue() throws SQLException {
		stat.setEscapeProcessing(true);
	}

	@Test
	public void unwrapTest() throws SQLException {
		assertTrue(conn.isWrapperFor(Connection.class));
		assertFalse(conn.isWrapperFor(Statement.class));
		assertEquals(conn, conn.unwrap(Connection.class));
		assertEquals(conn, conn.unwrap(Conn.class));

		assertTrue(stat.isWrapperFor(Statement.class));
		assertEquals(stat, stat.unwrap(Statement.class));
		assertEquals(stat, stat.unwrap(Stmt.class));

		try (ResultSet rs = stat.executeQuery("select 1")) {
			assertTrue(rs.isWrapperFor(ResultSet.class));
			assertEquals(rs, rs.unwrap(ResultSet.class));
		}
	}

	@Test
	public void closeOnCompletionTest() throws Exception {
		Method mIsCloseOnCompletion = Stmt.class.getDeclaredMethod("isCloseOnCompletion");
		Method mCloseOnCompletion = Stmt.class.getDeclaredMethod("closeOnCompletion");
		assertFalse((Boolean) mIsCloseOnCompletion.invoke(stat));

		mCloseOnCompletion.invoke(stat);
		assertTrue((Boolean) mIsCloseOnCompletion.invoke(stat));

		try (ResultSet rs = stat.executeQuery("select 1")) {
		}

		assertTrue(stat.isClosed());
	}

	@Test(expected = ConnException.class)
	public void nullQuery() throws Exception {
		stat.executeQuery(null);
	}
	@Test
	public void emptyQuery() throws Exception {
		assertEquals(0, stat.executeUpdate(";"));
		assertFalse(stat.execute(";"));
		try (ResultSet rs = stat.executeQuery(";")) {
			assertFalse(rs.next());
		}
	}
}
