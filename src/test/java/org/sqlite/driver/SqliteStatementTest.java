/*
 * Copyright (c) 2013, Timothy Stack
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sqlite.driver;

import org.junit.Ignore;
import org.junit.Test;
import org.sqlite.FunctionFlags;
import org.sqlite.SQLite.sqlite3_context;
import org.sqlite.SQLite.SQLite3Values;
import org.sqlite.SQLiteException;
import org.sqlite.ScalarCallback;

import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class SqliteStatementTest extends SqliteTestHelper {
	private static final String[] BATCH_ATTACH_RESULT = {
			"|db2|",
			"|main|",
	};

	@Test
	public void testExecuteBatch() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.addBatch("INSERT INTO test_table VALUES (2, 'testing')");
			stmt.addBatch("ATTACH ':memory:' as db2");
			//stmt.addBatch("SELECT * FROM test_table");
			stmt.addBatch("INSERT INTO test_table VALUES (3, 'testing again')");

			assertArrayEquals(new int[]{1, 0/*, Statement.SUCCESS_NO_INFO*/, 1},
					stmt.executeBatch());

			final ResultSet catalogs = conn.getMetaData().getCatalogs();
			assertArrayEquals(BATCH_ATTACH_RESULT,
					formatResultSet(catalogs));
			catalogs.close();

			assertArrayEquals(new int[0], stmt.executeBatch());

			stmt.addBatch("INSERT INTO test_table VALUES (4, 'testing again too')");
			stmt.addBatch("INSERT INTO test_table VALUES (4, 'testing again too')");
			try {
				stmt.executeBatch();
				fail("executeBatch should not have succeeded");
			} catch (BatchUpdateException e) {
			}

			assertArrayEquals(new int[0], stmt.executeBatch());

			final String[] tableDump = {
					"|1|test|",
					"|2|testing|",
					"|3|testing again|",
					"|4|testing again too|",
			};

			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertArrayEquals(tableDump, formatResultSet(rs));
			}

			stmt.addBatch("INSERT INTO test_table VALUES (2, 'testing')");
			stmt.clearBatch();
			assertArrayEquals(new int[0], stmt.executeBatch());
		}
	}

	@Test
	public void testCloseOnCompletion() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			assertFalse(stmt.isCloseOnCompletion());

			stmt.closeOnCompletion();
			assertTrue(stmt.isCloseOnCompletion());
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				formatResultSet(rs);
			}
			assertTrue(stmt.isClosed());
		}

		try (Statement stmt = conn.createStatement()) {
			assertFalse(stmt.isCloseOnCompletion());

			stmt.closeOnCompletion();
			assertTrue(stmt.isCloseOnCompletion());
			assertEquals(1, stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'testing')"));
			assertFalse(stmt.isClosed());
		}
	}

	@Test(expected = SQLException.class)
	public void testBadExecuteUpdate() throws Exception {

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("SELECT * FROM test_table");
		}
	}

	@Ignore
	@Test
	public void testQueryTimeout() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			try {
				stmt.setQueryTimeout(-1);
				fail("negative timeout value allowed?");
			} catch (SQLException e) {

			}

			((Conn) conn).getConn().createScalarFunction("delay", 0, FunctionFlags.SQLITE_UTF8, new ScalarCallback() {
				@Override
				public void func(sqlite3_context pCtx, SQLite3Values args) {
					try {
						Thread.currentThread().join(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					pCtx.setResultInt(0);
				}
			});
			stmt.setQueryTimeout(1);
			assertEquals(1, stmt.getQueryTimeout());

			long startTime = System.currentTimeMillis();
			try (ResultSet rs = stmt.executeQuery("SELECT *, delay() from test_table")) {
				rs.next();
				fail("Expected a timeout exception");
			} catch (SQLTimeoutException e) {
				long endTime = System.currentTimeMillis();

				if (endTime - startTime < 1000) {
					fail("Timeout expired early -- " + (endTime - startTime));
				}
			}

			try {
				stmt.execute("INSERT INTO test_table VALUES (2, delay())");
			} catch (SQLiteException e) {
				long endTime = System.currentTimeMillis();

				if (endTime - startTime < 1000) {
					fail("Timeout expired early -- " + (endTime - startTime));
				}
			}
		}
	}

	@Test
	public void testMaxRows() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'testing')");

			assertEquals(0, stmt.getMaxRows());

			try {
				stmt.setMaxRows(-1);
				fail("able to set max rows to a negative number?");
			} catch (SQLException e) {
				assertEquals(0, stmt.getMaxRows());
			}

			stmt.setMaxRows(1);
			assertEquals(1, stmt.getMaxRows());
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertTrue(rs.next());
				assertFalse(rs.next());
			}

			stmt.setMaxRows(4);
			assertEquals(4, stmt.getMaxRows());
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertTrue(rs.next());
				assertTrue(rs.next());
				assertFalse(rs.next());
			}
		}
	}

	@Ignore
	@Test
	public void testCancel() throws Exception {
		try (final Statement stmt = conn.createStatement()) {
			final Object barrier = new Object();

			stmt.cancel();

			Thread canceller = new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized (barrier) {
						barrier.notifyAll();
					}
					try {
						Thread.sleep(10);
						stmt.cancel();
					} catch (InterruptedException e) {

					} catch (SQLException e) {

					}
				}
			});

			canceller.start();
			synchronized (barrier) {
				barrier.wait();
			}

			try {
				stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'testing cancel')");
				fail("Statement was not cancelled?");
			} catch (SQLException e) {
			}
		}
	}

	@Test(expected = SQLIntegrityConstraintViolationException.class)
	public void testIntegrityException() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.execute("INSERT INTO test_table VALUES (1, 'test')");
		}
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testFetchDirection() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			assertEquals(ResultSet.FETCH_FORWARD, stmt.getFetchDirection());
			stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
			assertEquals(ResultSet.FETCH_FORWARD, stmt.getFetchDirection());
			stmt.setFetchDirection(ResultSet.FETCH_REVERSE);
		}
	}

	@Test
	public void testFetchSize() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			assertEquals(1, stmt.getFetchSize());
			stmt.setFetchSize(10);
			assertEquals(1, stmt.getFetchSize());
		}
	}

	@Test(expected = SQLException.class)
	public void testExecuteNonQuery() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeQuery("INSERT INTO test_table VALUES (2, 'testing')");
		}
	}

	@Test(expected = SQLException.class)
	public void testClosedStatement() throws Exception {
		Statement stmt = conn.createStatement();

		assertFalse(stmt.isClosed());
		stmt.close();
		assertTrue(stmt.isClosed());
		stmt.execute("SELECT * FROM test_table");
	}

	@Test
	public void testUpdateCount() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			assertEquals(1, stmt.executeUpdate("REPLACE INTO test_table VALUES (1, 'test')"));
			assertEquals(-1, stmt.getUpdateCount());
			assertEquals(1, stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'testing')"));
			assertEquals(0, stmt.executeUpdate("CREATE TABLE change_tab (id INTEGER, name VARCHAR)"));
			assertEquals(-1, stmt.getUpdateCount());
			assertEquals(0, stmt.executeUpdate("UPDATE test_table set name='more testing' where id > 2"));
			assertEquals(1, stmt.executeUpdate("UPDATE test_table set name='more testing' where id > 1"));

			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertEquals(-1, stmt.getUpdateCount());
				assertNull(stmt.getResultSet());
			}

			assertEquals(2, stmt.executeUpdate("DELETE FROM test_table WHERE 1"));
		}
	}

	private static final String[] ESCAPE_RESULTS = {
			"||",
			"|1|",
			"|4|",
			"|2011-10-06|",
			"|15:00:00|",
			"|2011-10-06 15:00:00|",
			"|fooBAR,BAZ|",
			"|0|",
	};

	private static final String[] ESCAPE_LIMIT_RESULTS = {
			"|1|",
			"|2|",
	};

	@Ignore
	@Test
	public void testEscapedQueries() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery(
					"SELECT {fn user()} AS RESULT UNION ALL " +
							"SELECT {fn abs(-1)} AS RESULT UNION ALL " +
							"SELECT {fn char_length('test')} AS RESULT UNION ALL " +
							"SELECT {d '2011-10-06'} AS RESULT UNION ALL " +
							"SELECT {t '15:00:00'} AS RESULT UNION ALL " +
							"SELECT {ts '2011-10-06 15:00:00'} AS RESULT UNION ALL " +
							"SELECT {fn concat('foo', (select 'BAR,BAZ'))} AS RESULT UNION ALL " +
							"SELECT 'FOO' LIKE '\\%' {escape '\\'} AS RESULT")) {
				assertArrayEquals(ESCAPE_RESULTS, formatResultSet(rs));
			}

			try (ResultSet rs = stmt.executeQuery(
					"SELECT 1 AS RESULT UNION ALL " +
							"SELECT 2 AS RESULT UNION ALL " +
							"SELECT 3 AS RESULT {limit 2}")) {
				assertArrayEquals(ESCAPE_LIMIT_RESULTS, formatResultSet(rs));
			}

			try (ResultSet rs = stmt.executeQuery(
					"SELECT * FROM test_table {limit 1 offset 1}")) {
				assertArrayEquals(new String[0], formatResultSet(rs));
			}

			stmt.setEscapeProcessing(false);
			try (ResultSet rs = stmt.executeQuery(
					"SELECT * FROM test_table {limit 1 offset 1}")) {
				fail("escaped statement worked?");
			} catch (SQLSyntaxErrorException e) {

			}
		}
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testCursorName() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.setCursorName("foo");
		}
	}
}
