package org.sqlite;

import org.junit.Assert;
import org.junit.Test;

public class ConnTest {
  @Test
  public void checkLibversion() throws SQLiteException {
    final Conn c = open();
    Assert.assertTrue(c.libversion().startsWith("3"));
  }

  @Test
  public void checkOpenTempFile() throws SQLiteException {
    final Conn c = Conn.open(Conn.TEMP_FILE, OpenFlags.SQLITE_OPEN_READWRITE, null);
    Assert.assertNotNull(c);
    Assert.assertEquals(Conn.TEMP_FILE, c.getFilename());
    checkResult(c._close());
  }

  @Test
  public void checkOpenInMemoryDb() throws SQLiteException {
    final Conn c = open();
    Assert.assertNotNull(c);
    Assert.assertEquals(Conn.MEMORY, c.getFilename());
    checkResult(c._close());
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
  }

  @Test
  public void checkPrepare() throws SQLiteException {
    final Conn c = open();
    final Stmt s = c.prepare("SELECT 1");
    Assert.assertNotNull(s);
    s.close();
    checkResult(c._close());
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
    checkResult(c._close());
  }

  @Test
  public void checkGetTableColumnMetadata() {
    // TODO
  }

  @Test
  public void checkMprintf() {
    Assert.assertEquals("'1'", SQLite.sqlite3_mprintf("%Q", String.valueOf(1)));
  }

  static void checkResult(int res) {
    Assert.assertEquals(0, res);
  }

  static Conn open() throws SQLiteException {
    return Conn.open(Conn.MEMORY, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null);
  }
}
