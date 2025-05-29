/*
Copyright (c) 2006, David Crawshaw.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * These tests assume that Statements and PreparedStatements are working
 * as per normal and test the interactions of commit(), rollback() and
 * setAutoCommit(boolean) with multiple connections to the same db.
 */
public class TransactionTest {
	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();
	private Connection conn1, conn2, conn3;
	private Statement stat1, stat2, stat3;
	private boolean done;

	@Before
	public void connect() throws Exception {
		final File file = folder.newFile("test-trans.db");
		conn1 = DriverManager.getConnection(JDBC.PREFIX + file);
		conn2 = DriverManager.getConnection(JDBC.PREFIX + file);
		conn3 = DriverManager.getConnection(JDBC.PREFIX + file);
		stat1 = conn1.createStatement();
		stat2 = conn2.createStatement();
		stat3 = conn3.createStatement();
	}

	@After
	public void close() throws Exception {
		Guard.closeAll(stat1, stat2, stat3, conn1, conn2, conn3);
	}

	private void failedUpdatedPreventedFutureRollback(boolean prepared) throws SQLException {
		stat1.execute("create table test (c1);");
		stat1.execute("insert into test values (1);");

		// First transaction starts
		conn1.setAutoCommit(false);
		stat1.execute("insert into test values (2);");

		final PreparedStatement pstat2 = prepared ? conn2.prepareStatement("insert into test values (3);") : null;

		// Second transaction starts and tries to complete but fails because first is still running
		boolean gotException = false;
		try {
			conn2.setAutoCommit(false);
			if (pstat2 != null) {
				// The prepared case would fail regardless of whether this was "execute" or "executeUpdate"
				pstat2.execute();
			} else {
				// If you changed this to "executeUpdate" instead of "execute", the test would pass
				stat2.execute("insert into test values (3);");
			}
		} catch (SQLException e) {
			if (e.getMessage().contains("is locked")) {
				gotException = true;
			} else {
				throw e;
			}
		}
		assertTrue(gotException);
		conn2.rollback();
		// The test would fail here: the trivial "transaction" created in between the rollback we just
		// did and this point would fail to commit because "SQL statements in progress"
		conn2.setAutoCommit(true);

		// First transaction completes
		conn1.setAutoCommit(true);

		// Second transaction retries
		conn2.setAutoCommit(false);
		if (pstat2 != null) {
			pstat2.execute();
			pstat2.close();
		} else {
			stat2.execute("insert into test values (3);");
		}
		conn2.setAutoCommit(true);

		Set<Integer> seen;
		try (ResultSet rs = stat1.executeQuery("select c1 from test")) {
			seen = new HashSet<>();
			while (rs.next()) {
				assertTrue(seen.add(rs.getInt(1)));
			}
		}

		assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), seen);
	}

	@Test
	public void failedUpdatePreventedFutureRollbackUnprepared() throws SQLException {
		failedUpdatedPreventedFutureRollback(false);
	}

	@Test
	public void failedUpdatePreventedFutureRollbackPrepared() throws SQLException {
		failedUpdatedPreventedFutureRollback(true);
	}

	@Test
	public void multiConn() throws SQLException {
		stat1.executeUpdate("create table test (c1);");
		stat1.executeUpdate("insert into test values (1);");
		stat2.executeUpdate("insert into test values (2);");
		stat3.executeUpdate("insert into test values (3);");

		try (ResultSet rs = stat1.executeQuery("select sum(c1) from test;")) {
			assertTrue(rs.next());
			assertEquals(6, rs.getInt(1));
		}

		try (ResultSet rs = stat3.executeQuery("select sum(c1) from test;")) {
			assertTrue(rs.next());
			assertEquals(6, rs.getInt(1));
		}
	}

	@Test
	public void locking() throws SQLException {
		stat1.executeUpdate("create table test (c1);");
		stat1.executeUpdate("begin immediate;");
		stat2.executeQuery("select * from test;");
	}

	@Test
	public void insert() throws SQLException {
		String countSql = "select count(*) from trans;";

		stat1.executeUpdate("create table trans (c1);");
		conn1.setAutoCommit(false);

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
		conn1.setAutoCommit(false);
		stat1.executeUpdate("insert into trans values (3);");

		try (ResultSet rs = stat1.executeQuery(select)) {
			assertTrue(rs.next());
		}

		conn1.rollback();

		try (ResultSet rs = stat1.executeQuery(select)) {
			assertFalse(rs.next());
		}
	}

	@Test
	public void multiRollback() throws SQLException {

		stat1.executeUpdate("create table t (c1);");
		conn1.setAutoCommit(false);
		stat1.executeUpdate("insert into t values (1);");
		conn1.commit();
		stat1.executeUpdate("insert into t values (1);");
		conn1.rollback();
		stat1.addBatch("insert into t values (2);");
		stat1.addBatch("insert into t values (3);");
		stat1.executeBatch();
		conn1.commit();
		stat1.addBatch("insert into t values (7);");
		stat1.executeBatch();
		conn1.rollback();
		stat1.executeUpdate("insert into t values (4);");
		conn1.setAutoCommit(true);
		stat1.executeUpdate("insert into t values (5);");
		conn1.setAutoCommit(false);
		try (PreparedStatement p = conn1.prepareStatement(
			"insert into t values (?);")) {
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
	public void transactionsDontMindReads() throws SQLException {
		stat1.executeUpdate("create table t (c1);");
		stat1.executeUpdate("insert into t values (1);");
		stat1.executeUpdate("insert into t values (2);");
		try (ResultSet rs = stat1.executeQuery("select * from t;")) {
			assertTrue(rs.next()); // select is open

			conn2.setAutoCommit(false);
			stat1.executeUpdate("insert into t values (2);");
		}
		conn2.commit();
	}

	@Test
	public void secondConnWillWait() throws Exception {
		stat1.executeUpdate("create table t (c1);");
		stat1.executeUpdate("insert into t values (1);");
		stat1.executeUpdate("insert into t values (2);");
		ResultSet rs = stat1.executeQuery("select * from t;");
		assertTrue(rs.next());

		final TransactionTest lock = this;
		lock.done = false;
		new Thread(() -> {
			try {
				stat2.executeUpdate("insert into t values (3);");
			} catch (SQLException e) {
				e.printStackTrace();
				return;
			}

			synchronized (lock) {
				lock.done = true;
				lock.notifyAll();
			}
		}).start();

		Thread.sleep(100);
		rs.close();

		synchronized (lock) {
			lock.wait(5000);
			if (!lock.done)
				throw new Exception("should be done");
		}
	}

	@Test(expected = SQLException.class)
	public void secondConnMustTimeout() throws SQLException {
		stat1.setQueryTimeout(1);
		stat1.executeUpdate("create table t (c1);");
		stat1.executeUpdate("insert into t values (1);");
		stat1.executeUpdate("insert into t values (2);");
		try (ResultSet rs = stat1.executeQuery("select * from t;")) {
			assertTrue(rs.next());

			stat2.executeUpdate("insert into t values (3);"); // can't be done
		}
	}

	@Test
	public void cantUpdateWhileReading() throws SQLException {
		stat1.executeUpdate("create table t (c1);");
		stat1.executeUpdate("insert into t values (1);");
		stat1.executeUpdate("insert into t values (2);");
		try (Statement stmt = conn1.createStatement();
				 ResultSet rs = stmt.executeQuery("select * from t;")) {
			assertTrue(rs.next());
			// commit now succeeds since sqlite 3.6.5
			stat1.executeUpdate("insert into t values (3);"); // can't be done
		}
	}

	@Test
	public void unnamedSavepoint() throws SQLException {
		Savepoint spt = conn1.setSavepoint();
		conn1.rollback(spt);
		conn1.releaseSavepoint(spt);
	}

	@Test
	public void namedSavepoint() throws SQLException {
		Savepoint spt = conn1.setSavepoint("test");
		conn1.rollback(spt);
		conn1.releaseSavepoint(spt);
	}

	@Test(expected = SQLException.class)
	public void cantCommit() throws SQLException {
		conn1.commit();
	}

	@Test(expected = SQLException.class)
	public void cantRollback() throws SQLException {
		conn1.rollback();
	}

  /*@Test
	public void transactionModes() throws Exception {
    File tmpFile = folder.newFile("test-trans.db");

    Field transactionMode = SQLiteConnection.class.getDeclaredField("transactionMode");
    transactionMode.setAccessible(true);
    Field beginCommandMap = SQLiteConnection.class.getDeclaredField("beginCommandMap");
    beginCommandMap.setAccessible(true);

    SQLiteDataSource ds = new SQLiteDataSource();
    ds.setUrl(JDBC.PREFIX + tmpFile.getAbsolutePath());

    // deferred
    SQLiteConnection con = (SQLiteConnection) ds.getConnection();
    assertEquals(TransactionMode.DEFERRED, transactionMode.get(con));
    assertEquals("begin;",
        ((Map<?, ?>) beginCommandMap.get(con)).get(TransactionMode.DEFERRED));
    runUpdates(con, "tbl1");

    ds.setTransactionMode(TransactionMode.DEFERRED.name());
    con = (SQLiteConnection) ds.getConnection();
    assertEquals(TransactionMode.DEFERRED, transactionMode.get(con));
    assertEquals("begin;",
        ((Map<?, ?>) beginCommandMap.get(con)).get(TransactionMode.DEFERRED));

    // immediate
    ds.setTransactionMode(TransactionMode.IMMEDIATE.name());
    con = (SQLiteConnection) ds.getConnection();
    assertEquals(TransactionMode.IMMEDIATE, transactionMode.get(con));
    assertEquals("begin immediate;",
        ((Map<?, ?>) beginCommandMap.get(con)).get(TransactionMode.IMMEDIATE));
    runUpdates(con, "tbl2");

    // exclusive
    ds.setTransactionMode(TransactionMode.EXCLUSIVE.name());
    con = (SQLiteConnection) ds.getConnection();
    assertEquals(TransactionMode.EXCLUSIVE, transactionMode.get(con));
    assertEquals("begin exclusive;",
        ((Map<?, ?>) beginCommandMap.get(con)).get(TransactionMode.EXCLUSIVE));
    runUpdates(con, "tbl3");

  }

  public void runUpdates(Connection con, String table) throws SQLException {
    Statement stat = con.createStatement();

    con.setAutoCommit(false);
    stat.execute("create table " + table + "(id)");
    stat.executeUpdate("insert into " + table + " values(1)");
    stat.executeUpdate("insert into " + table + " values(2)");
    con.commit();

    ResultSet rs = stat.executeQuery("select * from " + table);
    rs.next();
    assertEquals(1, rs.getInt(1));
    rs.next();
    assertEquals(2, rs.getInt(1));
    rs.close();
    con.close();
  }*/
}
