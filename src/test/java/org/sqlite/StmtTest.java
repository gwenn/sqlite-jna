package org.sqlite;

import org.junit.Assume;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class StmtTest {
	@Test
	public void checkPrepare() throws SQLiteException {
		final Conn c = ConnTest.open();
		for (int i = 0; i < 100; i++) {
			final Stmt s = c.prepare("SELECT 1", false);
			assertNotNull(s);
			checkResult(s.closeNoCheck());
		}
		checkResult(c.closeNoCheck());
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
		checkResult(s.closeNoCheck());
		checkResult(c.closeNoCheck());
	}

	@Test
	public void expandedSql() throws SQLiteException {
		try (Conn c = ConnTest.open(); Stmt s = c.prepare("SELECT ?", false)) {
			assertNotNull(s);
			s.bind("TEST");
			assertEquals("SELECT 'TEST'", s.getExpandedSql());
		}
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
		checkResult(s.closeNoCheck());
		checkResult(c.closeNoCheck());
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
			checkResult(s.closeNoCheck());
		}
		checkResult(c.closeNoCheck());
	}

	@Test
	public void closedStmt() throws SQLiteException {
		final Conn c = ConnTest.open();
		final Stmt stmt = c.prepare("SELECT 1", false);
		stmt.getColumnCount();
		stmt.close();
		//assertEquals(ColTypes.SQLITE_NULL, stmt.getColumnType(0));
		checkResult(c.closeNoCheck());
	}

	@Test
	public void status() throws SQLiteException {
		final Conn c = ConnTest.open();
		final Stmt s = c.prepare("SELECT 1", false);
		assertEquals(0, s.status(StmtStatus.SQLITE_STMTSTATUS_FULLSCAN_STEP, false));
		assertEquals(0, s.status(StmtStatus.SQLITE_STMTSTATUS_SORT, false));
		assertEquals(0, s.status(StmtStatus.SQLITE_STMTSTATUS_AUTOINDEX, false));
		assertEquals(0, s.status(StmtStatus.SQLITE_STMTSTATUS_VM_STEP, false));
		checkResult(s.closeNoCheck());
		checkResult(c.closeNoCheck());
	}

	//@Rule
	//public TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public void reset_asap() throws Exception {
		//File dbFile = testFolder.newFile("test.db");
		try (Conn c = ConnTest.open()) {
			c.fastExec("CREATE TABLE foo (x INT)");
			c.fastExec("BEGIN EXCLUSIVE");
			try (Stmt ins = c.prepare("ROLLBACK", false)) {
				ins.exec();
				assertFalse(ins.isBusy());
			}
		}
	}

	@Test
	public void utf8() throws Exception {
		try (Conn c = ConnTest.open()) {
			c.fastExec("PRAGMA encoding=\"UTF-8\"");
			c.fastExec("CREATE TABLE foo (data TEXT)");
			String text;
			try (Stmt ins = c.prepare("INSERT INTO foo VALUES (?)", false)) {
				text = new String(Character.toChars(0x1F604));
				ins.bindText(1, text);
				ins.exec();
			}
			try (Stmt sel = c.prepare("SELECT data FROM foo", false)) {
				assertTrue(sel.step(0));
				final byte[] bytes = sel.getColumnBlob(0);
				assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), bytes);
			}
		}
	}

	@Test
	public void pragma_func() throws Exception {
		Assume.assumeTrue(SQLite.versionAtLeast(3020000));
		try (Conn c = ConnTest.open()) {
			try (Stmt sel = c.prepare("SELECT * FROM pragma_table_info(?)", false)) {
				sel.bindText(1, "sqlite_master");
				assertTrue(sel.step(0));
				assertEquals("type", sel.getColumnText(1));
			}
		}
	}

	@Test
	public void params() throws Exception {
		try (Conn c = ConnTest.open()) {
			c.fastExec("CREATE TABLE IF NOT EXISTS my_context (\n" +
							 "    elt_name TEXT NOT NULL COLLATE NOCASE,\n" +
							 "    attr_prefix TEXT NOT NULL COLLATE NOCASE, -- empty if there is no '/' in complete attr name\n" +
							 "    entity_kind TEXT NULL, -- COLLATE NOCASE\n" +
							 "    pdl TEXT, -- NOT NULL (CASE WHEN attr_prefix IS NULL THEN 'all PDL element attributes without prefix' ELSE 'all PDL attributes with prefix equals to attr_prefix' END)\n" +
							 "    PRIMARY KEY (elt_name, attr_prefix)\n" +
							 ") WITHOUT ROWID;");

			try (Stmt stmt = c.prepare("SELECT json_type(pdl, '$.\"' || :attr_radix || '\"') AS attr_type, json_extract(pdl, '$.\"' || :attr_radix || '\"') AS attr_value\n" +
											 " FROM my_context WHERE elt_name = :elt_name AND attr_prefix = :attr_prefix;", false)) {
				assertEquals(3, stmt.getBindParameterCount());
				assertEquals(":attr_radix", stmt.getBindParameterName(1));
				assertEquals(":elt_name", stmt.getBindParameterName(2));
				assertEquals(":attr_prefix", stmt.getBindParameterName(3));
			}
		}
	}

	static void checkResult(int res) {
		assertEquals(0, res);
	}
}
