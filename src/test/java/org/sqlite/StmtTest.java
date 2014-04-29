package org.sqlite;

import org.junit.Assert;
import org.junit.Test;

public class StmtTest {
  @Test
  public void checkPrepare() throws SQLiteException {
    final Conn c = ConnTest.open();
    for (int i = 0; i < 100; i++) {
      final Stmt s = c.prepare("SELECT 1");
      Assert.assertNotNull(s);
      checkResult(s.close());
    }
    checkResult(c.close());
  }

  @Test
  public void checkBind() throws SQLiteException {
    final Conn c = ConnTest.open();
    final Stmt s = c.prepare("SELECT ?");
    Assert.assertNotNull(s);
    for (int i = 0; i < 100; i++) {
      s.bind("TEST");
      if (s.step()) {
        Assert.assertEquals("TEST", s.getColumnText(0));
      } else {
        Assert.fail("No result");
      }
      s.reset();
    }
    checkResult(s.close());
    checkResult(c.close());
  }

  @Test
  public void checkMissingBind() throws SQLiteException {
    final Conn c = ConnTest.open();
    final Stmt s = c.prepare("SELECT ?");
    Assert.assertNotNull(s);
    if (s.step()) {
      Assert.assertNull(s.getColumnText(0));
    } else {
      Assert.fail("No result");
    }
    checkResult(s.close());
    checkResult(c.close());
  }

  static void checkResult(int res) {
    Assert.assertEquals(0, res);
  }
}
