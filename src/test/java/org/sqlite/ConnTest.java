package org.sqlite;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sqlite.SQLite.SQLite3Context;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.sqlite.SQLite.*;

public class ConnTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void checkLibversion() throws SQLiteException {
		assertTrue(Conn.libversion().startsWith("3"));
	}

	@Test
	public void checkOpenTempFile() throws SQLiteException {
		final Conn c = Conn.open(Conn.TEMP_FILE, OpenFlags.SQLITE_OPEN_READWRITE, null);
		assertNotNull(c);
		assertEquals(Conn.TEMP_FILE, c.getFilename());
		checkResult(c.closeNoCheck());
	}

	@Test
	public void checkOpenDir() throws SQLiteException, IOException {
		File folder = this.folder.newFolder();
		try(Conn c = Conn.open(folder.getPath(), OpenFlags.SQLITE_OPEN_READWRITE, null)) {
			fail("SQLiteException expected");
		} catch (SQLException e) {
			assertEquals(ErrCodes.SQLITE_CANTOPEN, e.getErrorCode() & 0xFF);
		}
	}

	@Test
	public void checkOpenInMemoryDb() throws SQLiteException {
		final Conn c = open();
		assertNotNull(c);
		assertEquals("", c.getFilename());
		checkResult(c.closeNoCheck());
	}

	@Test
	public void checkInitialState() throws SQLiteException {
		final Conn c = open();
		assertEquals(0, c.getChanges());
		assertEquals(0, c.getTotalChanges());
		assertEquals(0, c.getLastInsertRowid());

		assertEquals(0, c.getErrCode());
		assertEquals(0, c.getExtendedErrcode());
		assertEquals("not an error", c.getErrMsg());
		checkResult(c.closeNoCheck());
	}

	@Test
	public void readOnly() throws SQLiteException {
		final Conn c = open();
		assertFalse("not read only", c.isReadOnly(null));
		assertFalse("not read only", c.isReadOnly("main"));
		checkResult(c.closeNoCheck());
	}

	@Test
	public void queryOnly() throws SQLiteException {
		if (Conn.libversionNumber() < 3008000) {
			return;
		}
		final Conn c = open();
		assertFalse("not query only", c.isQueryOnly(null));
		c.setQueryOnly(null, true);
		assertTrue("query only", c.isQueryOnly(null));
		checkResult(c.closeNoCheck());
	}

	@Test
	public void checkPrepare() throws SQLiteException {
		final Conn c = open();
		final Stmt s = c.prepare("SELECT 1", false);
		assertNotNull(s);
		s.closeNoCheck();
		checkResult(c.closeNoCheck());
	}

	@Test
	public void checkExec() throws SQLiteException {
		final Conn c = open();
		c.exec("DROP TABLE IF EXISTS test;\n" +
				"CREATE TABLE test (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
				" d REAL, i INTEGER, s TEXT); -- bim");

		final boolean[] metadata = c.getTableColumnMetadata("main", "test", "id");
		assertTrue(metadata[0]);
		assertTrue(metadata[1]);
		assertTrue(metadata[2]);
		checkResult(c.closeNoCheck());
	}

	@Test
	public void fastExec() throws SQLiteException {
		final Conn c = open();
		c.fastExec("PRAGMA encoding=\"UTF-8\"");
		checkResult(c.closeNoCheck());
	}

	@Test
	public void checkGetTableColumnMetadata() {
		// TODO
	}

	@Test
	public void enableFKey() throws SQLiteException {
		final Conn c = open();
		assertFalse(c.areForeignKeysEnabled());
		assertTrue(c.enableForeignKeys(true));
		assertTrue(c.areForeignKeysEnabled());
		checkResult(c.closeNoCheck());
	}

	@Test
	public void enableTriggers() throws SQLiteException {
		final Conn c = open();
		assertTrue(c.areTriggersEnabled());
		assertFalse(c.enableForeignKeys(false));
		assertFalse(c.areForeignKeysEnabled());
		checkResult(c.closeNoCheck());
	}

	@Test
	public void enableLoadExtension() throws SQLiteException {
		final Conn c = open();
		if (!sqlite3_compileoption_used("OMIT_LOAD_EXTENSION")) {
			c.enableLoadExtension(true);
		}
		checkResult(c.closeNoCheck());
	}

	@Ignore
	@Test
	public void loadExtension() throws SQLiteException {
		final Conn c = open();
		c.enableLoadExtension(true);
		final String errMsg = c.loadExtension("/home/gwen/C/sqlite-csv-ext/csv", null);
		assertNull(errMsg);
		checkResult(c.closeNoCheck());
	}

	@Test
	public void limit() throws SQLiteException {
		final Conn c = open();
		final int max = c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER);
		assertEquals(max, c.setLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER, max+1));
		assertEquals(max, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER)); // SQLITE_MAX_VARIABLE_NUMBER
		assertEquals(max, c.setLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER, max-1));
		assertEquals(max-1, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER));
	}

	@Test
	public void trace() throws SQLiteException {
		final Conn c = open();
		final String[] traces = new String[1];
		c.trace(new TraceCallback() {
			private int i;

			@Override
			public void trace(String sql) {
				traces[i++] = sql;
			}
		});
		final String sql = "SELECT 1";
		c.fastExec(sql);
		assertArrayEquals("traces", new String[]{sql}, traces);
	}

	@Test
	public void profile() throws SQLiteException {
		final Conn c = open();
		final String[] profiles = new String[1];
		c.profile(new ProfileCallback() {
			private int i;

			@Override
			public void profile(String sql, long ns) {
				profiles[i++] = sql;
			}
		});
		final String sql = "SELECT 1";
		c.fastExec(sql);
		assertArrayEquals("profiles", new String[]{sql}, profiles);
	}

	@Test
	public void setBusyHandler() throws SQLiteException {
		final Conn c = open();
		c.setBusyHandler(new BusyHandler() {
			@Override
			public boolean busy(int count) {
				return false;
			}
		});
		final String sql = "SELECT 1";
		c.fastExec(sql);
	}

	@Test
	public void createScalarFunction() throws SQLiteException {
		final Conn c = open();
		c.createScalarFunction("test", 0, FunctionFlags.SQLITE_UTF8 | FunctionFlags.SQLITE_DETERMINISTIC, new ScalarCallback() {
			@Override
			protected void func(SQLite3Context pCtx, SQLite3Values args) {
				assertNotNull(pCtx);
				assertEquals(0, args.getCount());
				pCtx.setResultNull();
			}
		});
		c.fastExec("SELECT test()");
		c.createScalarFunction("test", 0, 0, null);
		c.closeNoCheck();
	}

	@Test
	public void createScalarFunctionWithArg() throws SQLiteException {
		final Conn c = open();
		c.createScalarFunction("test", 2, FunctionFlags.SQLITE_UTF8 | FunctionFlags.SQLITE_DETERMINISTIC, new ScalarCallback() {
			@Override
			protected void func(SQLite3Context pCtx, SQLite3Values args) {
				assertNotNull(pCtx);
				assertEquals(2, args.getCount());
				assertEquals(ColTypes.SQLITE_INTEGER, args.getNumericType(0));
				final int value = args.getInt(0);
				assertEquals(123456, value);
				assertEquals(2, args.getInt(1));
				pCtx.setResultInt(value);
			}
		});
		final Stmt stmt = c.prepare("SELECT test(123456, 2)", false);
		assertTrue(stmt.step(0));
		assertEquals(123456, stmt.getColumnInt(0));
		stmt.close();
		c.close();
	}

	@Test
	public void createAggregateFunction() throws SQLiteException {
		final Conn c = open();
		c.createAggregateFunction("my_sum", 1, FunctionFlags.SQLITE_UTF8 | FunctionFlags.SQLITE_DETERMINISTIC, new AggregateStepCallback() {
			@Override
			protected int numberOfBytes() {
				return Native.POINTER_SIZE;
			}
			@Override
			protected void step(SQLite3Context pCtx, Pointer aggrCtx, SQLite3Values args) {
				assertNotNull(pCtx);
				assertNotNull(aggrCtx);
				assertEquals(1, args.getCount());
				assertEquals(ColTypes.SQLITE_INTEGER, args.getNumericType(0));
				aggrCtx.setLong(0, aggrCtx.getLong(0) + args.getLong(0));
			}
		}, new AggregateFinalCallback() {
			@Override
			protected void finalStep(SQLite3Context pCtx, Pointer aggrCtx) {
				assertNotNull(pCtx);
				if (aggrCtx == null) {
					pCtx.setResultNull();
					return;
				}
				pCtx.setResultLong(aggrCtx.getLong(0));
			}
		});

		Stmt stmt = c.prepare("SELECT my_sum(i) FROM (SELECT 2 AS i WHERE 1 <> 1)", false);
		assertTrue(stmt.step(0));
		assertEquals(ColTypes.SQLITE_NULL, stmt.getColumnType(0));
		stmt.close();

		stmt = c.prepare("SELECT my_sum(i) FROM (SELECT 2 AS i UNION ALL SELECT 2)", false);
		assertTrue(stmt.step(0));
		assertEquals(ColTypes.SQLITE_INTEGER, stmt.getColumnType(0));
		assertEquals(4L, stmt.getColumnLong(0));
		stmt.close();

		stmt = c.prepare("SELECT my_sum(i), my_sum(j) FROM (SELECT 2 AS i, 1 AS j UNION ALL SELECT 2, 1)", false);
		assertTrue(stmt.step(0));
		assertEquals(4L, stmt.getColumnLong(0));
		assertEquals(2L, stmt.getColumnLong(1));
		stmt.close();

		c.close();
	}

	@Test(expected = ConnException.class)
	public void closedConn() throws SQLiteException {
		final Conn c = open();
		c.closeNoCheck();
		c.getAutoCommit();
	}

	@Test
	public void virtualTable() throws SQLiteException {
		// sqlite3 lib provided by vcpkg is not compiled with fst4 extension
		Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
		final Conn c = open();
		c.fastExec("CREATE VIRTUAL TABLE names USING fts4(name, desc, tokenize=porter)");
		c.close();
	}

	@Test
	public void updateHook() throws SQLiteException {
		final Conn c = open();
		AtomicInteger count = new AtomicInteger();
		c.updateHook(new UpdateHook() {
			@Override
			public void update(int actionCode, String dbName, String tblName, long rowId) {
				count.incrementAndGet();
			}
		});
		c.fastExec("CREATE TABLE test AS SELECT 0 as x;");
		assertEquals(1, c.execDml("UPDATE test SET x = 1;", false));
		assertEquals(1, count.get());
	}

	private static class ConnState {
		private boolean triggersEnabled = true;
		private final String encoding = UTF_8_ECONDING;
		private boolean foreignKeys = false;
		private String journalMode = "memory";
		private final String lockingMode = "normal";
		private boolean queryOnly = false;
		private boolean recursiveTriggers = false;
		private final String synchronous = "2";
	}
	private static abstract class ConnStateTest {
		private final String uri;
		private final ConnState state;

		private ConnStateTest(String uri) {
			this.uri = uri;
			state = new ConnState();
			expected(state);
		}
		protected abstract void expected(ConnState s);
	}

	private static final ConnStateTest[] CONN_STATE_TESTS = new ConnStateTest[]{
			new ConnStateTest("file:memdb?mode=memory") {
				@Override
				protected void expected(ConnState s) {
				}
			},
			new ConnStateTest("file:memdb?mode=memory&enable_triggers=off") {
				@Override
				protected void expected(ConnState s) {
					s.triggersEnabled = false;
				}
			},
			/*new ConnStateTest("file:memdb?mode=memory&encoding=UTF-16") {
				@Override
				protected void expected(ConnState s) {
					//s.encoding = "UTF-16";
				}
			},*/
			new ConnStateTest("file:memdb?mode=memory&foreign_keys=on") {
				@Override
				protected void expected(ConnState s) {
					s.foreignKeys = true;
				}
			},
			new ConnStateTest("file:?journal_mode=off") {
				@Override
				protected void expected(ConnState s) {
					s.journalMode = "off";
				}
			},
			/*new ConnStateTest("file:memdb?mode=memory&lockingMode=EXCLUSIVE") {
				@Override
				protected void expected(ConnState s) {
					s.lockingMode = "off";
				}
			},*/
			new ConnStateTest("file:memdb?mode=memory&query_only=on") {
				@Override
				protected void expected(ConnState s) {
					s.queryOnly = true;
				}
			},
			new ConnStateTest("file:memdb?mode=memory&recursive_triggers=on") {
				@Override
				protected void expected(ConnState s) {
					s.recursiveTriggers = true;
				}
			},
	};
	@Test
	public void openUriQueryParameters() throws SQLiteException {
		for (ConnStateTest t: CONN_STATE_TESTS) {
			final Conn c = Conn.open(t.uri, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_URI, null);
			check(t.state, c);
			c.close();
		}
	}

	private static void check(ConnState state, Conn c) throws SQLiteException {
		assertEquals("triggersEnabled", state.triggersEnabled, c.areTriggersEnabled());
		assertEquals("encoding", state.encoding, c.encoding(null));
		assertEquals("foreignKeys", state.foreignKeys, c.areForeignKeysEnabled());
		assertEquals("journalMode", state.journalMode, pragma(c, OpenQueryParameter.JOURNAL_MODE.name));
		assertEquals("lockingMode", state.lockingMode, pragma(c, OpenQueryParameter.LOCKING_MODE.name));
		assertEquals("queryOnly", state.queryOnly, c.isQueryOnly(null));
		assertEquals("recursiveTriggers", state.recursiveTriggers, c.pragma(null, OpenQueryParameter.RECURSIVE_TRIGGERS.name));
		assertEquals("synchronous", state.synchronous, pragma(c, OpenQueryParameter.SYNCHRONOUS.name));
	}

	static void checkResult(int res) {
		assertEquals(0, res);
	}

	static Conn open() throws SQLiteException {
		final Conn conn = Conn.open(Conn.MEMORY, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null);
		conn.setAuhtorizer(new Authorizer() {
			@Override
			public int authorize(int actionCode, String arg1, String arg2, String dbName, String triggerName) {
				//System.out.println("actionCode = [" + actionCode + "], arg1 = [" + arg1 + "], arg2 = [" + arg2 + "], dbName = [" + dbName + "], triggerName = [" + triggerName + "]");
				return Authorizer.SQLITE_OK;
			}
		});
		return conn;
	}

	static String pragma(Conn c, String name) throws SQLiteException {
		Stmt s = null;
		try {
			s = c.prepare("PRAGMA " + name, false);
			if (!s.step(0)) {
				throw new StmtException(s, "No result", ErrCodes.WRAPPER_SPECIFIC);
			}
			return s.getColumnText(0);
		} finally {
			if (s != null) {
				s.closeNoCheck();
			}
		}
	}
}
