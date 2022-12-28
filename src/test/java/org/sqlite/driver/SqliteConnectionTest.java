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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ClientInfoStatus;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class SqliteConnectionTest extends SqliteTestHelper {
	@Ignore
	@Test
	public void testFileType() throws Exception {
		Path path = Paths.get(dbFile.getAbsolutePath());

		assertEquals("application/x-sqlite3", Files.probeContentType(path));
	}

	@Test
	public void testIsValid() throws Exception {
		assertTrue(conn.isValid(0));
		conn.close();
		assertFalse(conn.isValid(0));
		assertTrue(conn.isClosed());
	}

	@Test
	public void testTransactionIsolation() throws Exception {
		assertEquals(Connection.TRANSACTION_SERIALIZABLE, conn.getTransactionIsolation());
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, conn.getTransactionIsolation());
		conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		assertEquals(Connection.TRANSACTION_SERIALIZABLE, conn.getTransactionIsolation());
	}

	@Test(expected = SQLException.class)
	public void testTransactionIsolationOnClosedDB() throws Exception {
		conn.close();
		conn.getTransactionIsolation();
	}

	@Test(expected = SQLException.class)
	public void testBadTransactionIsolation1() throws Exception {
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
	}

	@Test(expected = SQLException.class)
	public void testBadTransactionIsolation2() throws Exception {
		conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
	}

	@Test(expected = SQLException.class)
	public void testBadTransactionIsolation3() throws Exception {
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
	}

	@Test
	public void testAutoCommit() throws Exception {
		assertTrue(conn.getAutoCommit());

		try {
			conn.commit();
			fail("commit should throw an exception since we're in auto-commit mode");
		} catch (SQLException e) {
		}

		try {
			conn.rollback();
			fail("rollback should throw an exception since we're in auto-commit mode");
		} catch (SQLException e) {
		}

		conn.setAutoCommit(false);
		assertFalse(conn.getAutoCommit());

		conn.setAutoCommit(false);
		assertFalse(conn.getAutoCommit());

		conn.commit();
		assertFalse(conn.getAutoCommit());

		conn.rollback();
		assertFalse(conn.getAutoCommit());

		try (Connection conn2 = DriverManager.getConnection(JDBC.PREFIX + dbFile.getAbsolutePath(), null)) {
			conn2.setAutoCommit(false);

			boolean reachedCommit = false;

			try (Statement stmt = conn.createStatement()) {
				try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
					while (rs.next()) {
						rs.getString(2);
					}
				}

				try (Statement stmt2 = conn2.createStatement()) {
					stmt2.executeUpdate("INSERT INTO test_table VALUES (2, 'test2')");
					reachedCommit = true;
					conn2.commit();
				}
				fail("Insert should fail with a collision error");
			} catch (SQLException e) {
				assertTrue(reachedCommit);
				assertTrue(e.getMessage(), e.getMessage().contains("locked"));
				assertFalse(conn.getAutoCommit());
			}
		}

		conn.setAutoCommit(true);
		assertTrue(conn.getAutoCommit());
	}

	@Test(expected = SQLIntegrityConstraintViolationException.class)
	public void testRollbackException() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("INSERT INTO test_table VALUES (1, 'test')");
		}
	}

	@Test
	public void testCreateStatement() throws Exception {
		Statement stmt = conn.createStatement();

		assertFalse(stmt.isClosed());

		ResultSet rs = stmt.executeQuery("SELECT * FROM test_table");
		assertFalse(rs.isClosed());

		conn.close();
		assertFalse(rs.isClosed());
		assertFalse(stmt.isClosed());
		conn.close();
		assertTrue(conn.isClosed());
	}

	@Test
	public void testReadOnly() throws Exception {
		if (org.sqlite.Conn.libversionNumber() < 3008000) {
			return;
		}
		assertFalse(conn.isReadOnly());

		conn.setAutoCommit(false);

		try {
			conn.setReadOnly(true);
			fail("Able to set read-only mode when in a transaction?");
		} catch (SQLException e) {
		}

		conn.setAutoCommit(true);
		conn.setReadOnly(true);

		try (Statement stmt = conn.createStatement()) {
			final String[] sqlStatements = {
					"INSERT INTO test_table VALUES (3, 'test')",
					"CREATE TABLE foo (id INTEGER)",
					"DROP TABLE test_table",
					//"PRAGMA synchronous = 1",
			};

			for (String sql : sqlStatements) {
				try {
					stmt.executeUpdate(sql);
					fail("Database modification should fail when in read-only mode");
				} catch (SQLException e) {
					assertTrue(e.getMessage().contains("attempt to write a readonly database"));
				}
			}

			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				while (rs.next()) {
					rs.getString(2);
				}
			}

			stmt.execute("PRAGMA collation_list");
		}

		conn.setReadOnly(true);
		assertTrue(conn.isReadOnly());

		conn.setReadOnly(false);
		assertFalse(conn.isReadOnly());

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("INSERT INTO test_table VALUES (3, 'test')");
		}
	}

	@Test
	public void testSetCatalog() throws Exception {
		assertNull(conn.getWarnings());
		conn.setCatalog("foo");
		assertNotNull(conn.getWarnings());
	}

	private static final String[] EXPECTED_WARNING_MSGS = {
			"SQLite only supports TYPE_FORWARD_ONLY result sets",
			"SQLite only supports CONCUR_READ_ONLY result sets",
			"SQLite only supports CLOSE_CURSORS_AT_COMMIT result sets",
	};

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testWarnings() throws Exception {
		assertNull(conn.getWarnings());
		try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM test_table",
				ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_UPDATABLE,
				ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
			SQLWarning warnings = conn.getWarnings();
			Set<String> msgs = new HashSet<>();

			assertEquals(ResultSet.TYPE_FORWARD_ONLY, stmt.getResultSetType());
			assertEquals(ResultSet.CONCUR_READ_ONLY, stmt.getResultSetConcurrency());
			assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, stmt.getResultSetHoldability());

			assertNotNull(warnings);
			while (warnings != null) {
				msgs.add(warnings.getMessage());
				warnings = warnings.getNextWarning();
			}

			assertEquals(new HashSet<>(Arrays.asList(EXPECTED_WARNING_MSGS)), msgs);
		}
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testPrepareCall() throws Exception {
		conn.prepareCall("foo");
	}

	@Test
	public void testRollback() throws Exception {
		conn.setAutoCommit(false);

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'test')");
			conn.rollback();

			assertFalse(conn.getAutoCommit());
			conn.setAutoCommit(true);
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				int count = 0;

				while (rs.next()) {
					count += 1;
				}
				assertEquals(1, count);
			}
		}
	}

    /*@Test
		public void testAbort() throws Exception {
        Sqlite3.ProgressCallbackBase delayCallback = new Sqlite3.ProgressCallbackBase() {
            @Override
            public int apply(Pointer<Void> context) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }
                return 0;
            }
        };

        Sqlite3.sqlite3_progress_handler(((SqliteConnection)conn).getHandle(), 1, Pointer.pointerTo(delayCallback), null);

        final List<Runnable> commandList = new ArrayList<>();
        final Executor monitor = new Executor() {
            @Override
            public void execute(Runnable command) {
                commandList.add(command);
            }
        };

        Thread aborter = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    conn.abort(monitor);
                } catch (SQLException e) {

                } catch (InterruptedException e) {

                }
            }
        });
        aborter.setDaemon(true);
        aborter.start();

        boolean reachedExecute = false;

        try (Statement stmt = conn.createStatement()) {
            reachedExecute = true;
            stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'test')");
            fail("Statement was not aborted?");
        }
        catch (SQLException e) {
            assertTrue(reachedExecute);
        }

        assertTrue(conn.isClosed());

        assertEquals(1, commandList.size());

        Thread waiter = new Thread(commandList.get(0));

        waiter.setDaemon(true);
        waiter.start();

        waiter.join(100);
        assertTrue(waiter.isAlive());
        conn.close();
        waiter.join(100);
        assertFalse(waiter.isAlive());
    }*/

	@Test
	public void testSavepoint() throws Exception {
/*
        try {
            conn.setSavepoint();
            fail("Setting a savepoint should fail outside of autocommit");
        }
        catch (SQLException e) {

        }
*/

		conn.setAutoCommit(false);

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("INSERT INTO test_table VALUES (2, 'test')");
			Savepoint sp = conn.setSavepoint();
			stmt.executeUpdate("INSERT INTO test_table VALUES (3, 'test')");
			try {
				sp.getSavepointName();
				fail("Able to get savepoint name?");
			} catch (SQLException e) {
			}
			sp.getSavepointId();
			conn.rollback(sp);
			conn.commit();

			try {
				conn.rollback(sp);
				fail("Rollback should fail on an invalid savepoint");
			} catch (SQLException e) {
			}

			try {
				conn.releaseSavepoint(sp);
				fail("Release should fail on an invalid savepoint");
			} catch (SQLException e) {
			}

			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertTrue(rs.next());
				assertEquals(1, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(2, rs.getInt(1));
				assertFalse(rs.next());
			}

			stmt.executeUpdate("INSERT INTO test_table VALUES (3, 'test')");
			sp = conn.setSavepoint("test");
			stmt.executeUpdate("INSERT INTO test_table VALUES (4, 'test')");
			try {
				sp.getSavepointId();
				fail("Able to get savepoint id?");
			} catch (SQLException e) {
			}
			assertEquals("test", sp.getSavepointName());
			conn.rollback(sp);
			conn.commit();

			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertTrue(rs.next());
				assertEquals(1, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(2, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(3, rs.getInt(1));
				assertFalse(rs.next());
			}

			stmt.executeUpdate("INSERT INTO test_table VALUES (4, 'test')");
			sp = conn.setSavepoint("test");
			stmt.executeUpdate("INSERT INTO test_table VALUES (5, 'test')");
			conn.releaseSavepoint(sp);
			conn.commit();

			try (ResultSet rs = stmt.executeQuery("SELECT * FROM test_table")) {
				assertTrue(rs.next());
				assertEquals(1, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(2, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(3, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(4, rs.getInt(1));
				assertTrue(rs.next());
				assertEquals(5, rs.getInt(1));
				assertFalse(rs.next());
			}
		}
	}

	@Test
	public void testGetClientInfo() throws Exception {
		assertNull(conn.getClientInfo("AppName"));
		assertNotNull(conn.getClientInfo());
	}

	@Ignore
	@Test
	public void testSetClientInfo() throws Exception {
		Properties props = new Properties();

		props.put("AppName", "Test");
		props.put("Key", "Value");
		try {
			conn.setClientInfo(props);
			fail("setClientInfo should not be supported");
		} catch (SQLClientInfoException e) {
			assertEquals(ClientInfoStatus.REASON_UNKNOWN_PROPERTY, e.getFailedProperties().get("AppName"));
			assertEquals(ClientInfoStatus.REASON_UNKNOWN_PROPERTY, e.getFailedProperties().get("Key"));
			assertEquals(2, e.getFailedProperties().size());
		}

		try {
			conn.setClientInfo("AppName", "Test");
			fail("setClientInfo should not be supported");
		} catch (SQLClientInfoException e) {
			assertEquals(ClientInfoStatus.REASON_UNKNOWN_PROPERTY, e.getFailedProperties().get("AppName"));
			assertEquals(1, e.getFailedProperties().size());
		}
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testGetTypeMap() throws Exception {
		conn.getTypeMap();
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testSetTypeMap() throws Exception {
		conn.setTypeMap(new HashMap<String, Class<?>>());
	}

	@Test
	public void testCreateSQLXML() throws Exception {
		assertNotNull(conn.createSQLXML());
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testSetNetworkTimeout() throws Exception {
		conn.setNetworkTimeout(null, 0);
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testGetNetworkTimeout() throws Exception {
		conn.getNetworkTimeout();
	}

	@Test
	public void testGetMetaData() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		assertNotNull(dmd);
		assertEquals(dmd, conn.getMetaData());

		conn.close();
		try {
			conn.getMetaData();
			fail("getMetaData should fail after the DB is closed");
		} catch (SQLException e) {

		}
	}
}
