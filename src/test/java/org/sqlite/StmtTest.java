package org.sqlite;

import org.junit.Test;

import static org.junit.Assert.*;

public class StmtTest {
	@Test
	public void checkPrepare() throws SQLiteException {
		final Conn c = ConnTest.open();
		for (int i = 0; i < 100; i++) {
			final Stmt s = c.prepare("SELECT 1", false);
			assertNotNull(s);
			checkResult(s.close());
		}
		checkResult(c.close());
	}

	@Test
	public void checkBind() throws SQLiteException {
		final Conn c = ConnTest.open();
		final Stmt s = c.prepare("SELECT ?", false);
		assertNotNull(s);
		for (int i = 0; i < 100; i++) {
			s.bind("TEST");
			if (s.step(0)) {
				assertEquals("TEST", s.getColumnText(0));
			} else {
				fail("No result");
			}
			s.reset();
		}
		checkResult(s.close());
		checkResult(c.close());
	}

	@Test
	public void checkMissingBind() throws SQLiteException {
		final Conn c = ConnTest.open();
		final Stmt s = c.prepare("SELECT ?", false);
		assertNotNull(s);
		if (s.step(0)) {
			assertNull(s.getColumnText(0));
		} else {
			fail("No result");
		}
		checkResult(s.close());
		checkResult(c.close());
	}

	@Test
	public void readOnly() throws SQLiteException {
		final Conn c = ConnTest.open();
		final String[] sqls = {"SELECT 1", "PRAGMA encoding", "PRAGMA database_list",
				"PRAGMA table_info('sqlite_master')", "PRAGMA foreign_key_list('sqlite_master')",
				"PRAGMA index_list('sqlite_master')", "BEGIN", "ROLLBACK"};
		for (String sql : sqls) {
			final Stmt s = c.prepare(sql, false);
			assertTrue("readOnly expected", s.isReadOnly());
			checkResult(s.close());
		}
		checkResult(c.close());
	}

	@Test
	public void closedStmt() throws SQLiteException {
		final Conn c = ConnTest.open();
		final Stmt stmt = c.prepare("SELECT 1", false);
		stmt.getColumnCount();
		stmt.closeAndCheck();
		assertEquals(ColTypes.SQLITE_NULL, stmt.getColumnType(0));
	}

	static void checkResult(int res) {
		assertEquals(0, res);
	}
}
