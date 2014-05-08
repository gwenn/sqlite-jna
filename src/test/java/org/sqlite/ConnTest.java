package org.sqlite;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ConnTest {
  @Test
  public void checkLibversion() throws SQLiteException {
    Assert.assertTrue(Conn.libversion().startsWith("3"));
  }

  @Test
  public void checkOpenTempFile() throws SQLiteException {
    final Conn c = Conn.open(Conn.TEMP_FILE, OpenFlags.SQLITE_OPEN_READWRITE, null);
    Assert.assertNotNull(c);
    Assert.assertEquals(Conn.TEMP_FILE, c.getFilename());
    checkResult(c.close());
  }

  @Test
  public void checkOpenInMemoryDb() throws SQLiteException {
    final Conn c = open();
    Assert.assertNotNull(c);
    Assert.assertEquals("", c.getFilename());
    checkResult(c.close());
  }

  @Test
  public void checkInitialState() throws SQLiteException {
    final Conn c = open();
    Assert.assertEquals(0, c.getChanges());
    Assert.assertEquals(0, c.getTotalChanges());
    Assert.assertEquals(0, c.getLastInsertRowid());

    Assert.assertEquals(0, c.getErrCode());
    Assert.assertEquals(0, c.getExtendedErrcode());
    Assert.assertEquals("not an error", c.getErrMsg());
    checkResult(c.close());
  }

  @Test
  public void checkPrepare() throws SQLiteException {
    final Conn c = open();
    final Stmt s = c.prepare("SELECT 1");
    Assert.assertNotNull(s);
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
    Assert.assertTrue(metadata[0]);
    Assert.assertTrue(metadata[1]);
    Assert.assertTrue(metadata[2]);
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
    Assert.assertFalse(c.areForeignKeysEnabled());
    Assert.assertTrue(c.enableForeignKeys(true));
    Assert.assertTrue(c.areForeignKeysEnabled());
    checkResult(c.close());
  }

  @Test
  public void enableTriggers() throws SQLiteException {
    final Conn c = open();
    Assert.assertTrue(c.areTriggersEnabled());
    Assert.assertFalse(c.enableForeignKeys(false));
    Assert.assertFalse(c.areForeignKeysEnabled());
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
    Assert.assertNull(errMsg);
    checkResult(c.close());
  }

  @Test
  public void limit() throws SQLiteException {
    final Conn c = open();
    Assert.assertEquals(999, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER));
    Assert.assertEquals(999, c.setLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER, 9999));
    Assert.assertEquals(999, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER)); // SQLITE_MAX_VARIABLE_NUMBER
    Assert.assertEquals(999, c.setLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER, 99));
    Assert.assertEquals(99, c.getLimit(SQLite.SQLITE_LIMIT_VARIABLE_NUMBER));
  }

  @Test
  public void checkMprintf() throws SQLiteException {
    for (int i = 0; i < 100; i++) {
      Assert.assertEquals("'1'", Conn.mprintf("%Q", String.valueOf(1)));
    }
    Assert.assertEquals("tes\"\"t", Conn.mprintf("%w", "tes\"t"));
  }

  static void checkResult(int res) {
    Assert.assertEquals(0, res);
  }

  static Conn open() throws SQLiteException {
    return Conn.open(Conn.MEMORY, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null);
  }
}
