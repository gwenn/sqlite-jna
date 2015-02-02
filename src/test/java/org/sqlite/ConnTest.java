package org.sqlite;

import com.sun.jna.Pointer;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConnTest {
  @Test
  public void checkLibversion() throws SQLiteException {
    assertTrue(Conn.libversion().startsWith("3"));
  }

  @Test
  public void checkOpenTempFile() throws SQLiteException {
    final Conn c = Conn.open(Conn.TEMP_FILE, OpenFlags.SQLITE_OPEN_READWRITE, null);
    assertNotNull(c);
    assertEquals(Conn.TEMP_FILE, c.getFilename());
    checkResult(c.close());
  }

  @Test
  public void checkOpenInMemoryDb() throws SQLiteException {
    final Conn c = open();
    assertNotNull(c);
    assertEquals("", c.getFilename());
    checkResult(c.close());
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
    checkResult(c.close());
  }

  @Test
  public void readOnly() throws SQLiteException {
    final Conn c = open();
    assertFalse("not read only", c.isReadOnly(null));
    assertFalse("not read only", c.isReadOnly("main"));
    checkResult(c.close());
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
    checkResult(c.close());
  }

  @Test
  public void checkPrepare() throws SQLiteException {
    final Conn c = open();
    final Stmt s = c.prepare("SELECT 1", false);
    assertNotNull(s);
    s.close();
    checkResult(c.close());
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
    checkResult(c.close());
  }

  @Test
  public void fastExec() throws SQLiteException {
    final Conn c = open();
    c.fastExec("PRAGMA encoding=\"UTF-8\"");
    checkResult(c.close());
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
    checkResult(c.close());
  }

  @Test
  public void enableTriggers() throws SQLiteException {
    final Conn c = open();
    assertTrue(c.areTriggersEnabled());
    assertFalse(c.enableForeignKeys(false));
    assertFalse(c.areForeignKeysEnabled());
    checkResult(c.close());
  }

  @Test
  public void enableLoadExtension() throws SQLiteException {
    final Conn c = open();
    c.enableLoadExtension(true);
    checkResult(c.close());
  }

  @Ignore
  @Test
  public void loadExtension() throws SQLiteException {
    final Conn c = open();
    c.enableLoadExtension(true);
    final String errMsg = c.loadExtension("/home/gwen/C/sqlite-csv-ext/csv", null);
    assertNull(errMsg);
    checkResult(c.close());
  }

  @Test
  public void limit() throws SQLiteException {
    final Conn c = open();
    assertEquals(999, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER));
    assertEquals(999, c.setLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER, 9999));
    assertEquals(999, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER)); // SQLITE_MAX_VARIABLE_NUMBER
    assertEquals(999, c.setLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER, 99));
    assertEquals(99, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER));
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
    }, null);
    final String sql = "SELECT 1";
    c.fastExec(sql);
    assertArrayEquals("traces", new String[]{sql}, traces);
  }

  @Test
  public void createScalarFunction() throws SQLiteException {
    final Conn c = open();
    c.createScalarFunction("test", 0, new ScalarCallback() {
      @Override
      public void invoke(Pointer pCtx, int nArg, Pointer args) {
        assertNotNull(pCtx);
        assertEquals(0, nArg);
        //assertNull(args);
        SQLite.sqlite3_result_null(pCtx);
      }
    });
    c.fastExec("SELECT test()");
    c.createScalarFunction("test", 0, null);
    c.close();
  }

  @Test(expected = ConnException.class)
  public void closedConn() throws SQLiteException {
    final Conn c = open();
    c.close();
    c.getAutoCommit();
  }

  @Test
  public void virtualTable() throws SQLiteException {
    final Conn c = open();
    c.fastExec("CREATE VIRTUAL TABLE names USING fts4(name, desc, tokenize=porter)");
    c.closeAndCheck();
  }

  static void checkResult(int res) {
    assertEquals(0, res);
  }

  static Conn open() throws SQLiteException {
    return Conn.open(Conn.MEMORY, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null);
  }
}
