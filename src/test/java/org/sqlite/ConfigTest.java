package org.sqlite;

import org.junit.Assert;
import org.junit.Test;

public class ConfigTest {
  @Test
  public void testThreadingMode() {
    Assert.assertEquals(SQLite.SQLITE_OK, SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_SINGLETHREAD));
    Assert.assertEquals(SQLite.SQLITE_OK, SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_MULTITHREAD));
    Assert.assertEquals(SQLite.SQLITE_OK, SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_SERIALIZED));
  }

  @Test
  public void testURIHandling() {
    Assert.assertEquals(SQLite.SQLITE_OK, SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_URI, false));
    Assert.assertEquals(SQLite.SQLITE_OK, SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_URI, true));
  }

  @Test
  public void testMemoryStatus() {
    Assert.assertEquals(SQLite.SQLITE_OK, SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_MEMSTATUS, false));
  }

  @Test
  public void testLog() {
    /*int rv = SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_LOG, new SQLite.LogCallback() {
      @Override
      public void invoke(Pointer udp, int err, String msg) {
        System.out.printf("%d: %s\n", err, msg);
      }
    }, null);
    Assert.assertEquals(SQLite.SQLITE_OK, rv);*/
    SQLite.sqlite3_log(-1, "testLog");
    /*rv = SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_LOG, null, null);
    Assert.assertEquals(SQLite.SQLITE_OK, rv);*/
  }
}
