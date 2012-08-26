package org.sqlite.driver;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BlobTest {
  @Test
  public void getBlob() throws SQLException {
    new JDBC();
    Connection c = null;
    try {
      c = DriverManager.getConnection("jdbc:sqlite::memory:");
      final Statement stmt = c.createStatement();
      stmt.execute("CREATE TABLE test (data BLOB)");
      Assert.assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(1024))"));
      ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
      Assert.assertTrue(rs.next());
      final long rowid = rs.getLong(1);
      Assert.assertTrue(rowid > 0);
      Assert.assertFalse(rs.next());

      rs = stmt.executeQuery("SELECT rowid, data FROM test");
      Assert.assertTrue(rs.next());
      Assert.assertEquals(rowid, rs.getLong(1));
      Assert.assertEquals(String.valueOf(rowid), rs.getRowId(1).toString());
      final Blob blob = rs.getBlob(2);
      Assert.assertNotNull(blob);
      final long length = blob.length();
      Assert.assertEquals(1024, length);
      final byte[] bytes = blob.getBytes(1, 1024);
      Assert.assertNotNull(bytes);
      Assert.assertEquals(length, bytes.length);
      Assert.assertFalse(rs.next());

      blob.free();
      rs.close();
      stmt.close();
    } finally {
      if (null != c) c.close();
    }
  }
}