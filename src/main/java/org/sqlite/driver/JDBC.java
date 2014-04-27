/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.OpenFlags;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class JDBC implements Driver {
  public static final String PREFIX;
  static {
    PREFIX = "jdbc:sqlite:";
    try {
      DriverManager.registerDriver(new JDBC());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static final String MEMORY = PREFIX + org.sqlite.Conn.MEMORY;
  public static final String TEMP_FILE = PREFIX + org.sqlite.Conn.TEMP_FILE;

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) return null;
    final String vfs = null;
    final int flags = OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE | OpenFlags.SQLITE_OPEN_FULLMUTEX |
        OpenFlags.SQLITE_OPEN_URI;
    final org.sqlite.Conn conn = org.sqlite.Conn.open(url.substring(PREFIX.length()), flags, vfs);
    conn.setBusyTimeout(3000);
    return new Conn(conn, info);
  }
  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url.startsWith(PREFIX);
  }
  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    final DriverPropertyInfo vfs = new DriverPropertyInfo("vfs", info == null ? null : info.getProperty("vfs"));
    vfs.description = "Specify the name of a VFS object that provides the operating system interface that should be used to access the database file on disk.";
    final DriverPropertyInfo mode = new DriverPropertyInfo("mode", info == null ? null : info.getProperty("mode"));
    mode.description = "The mode parameter may be set to either \"ro\", \"rw\", \"rwc\", or \"memory\"";
    mode.choices = new String[]{"ro", "rw", "rwc", "memory"};
    final DriverPropertyInfo cache = new DriverPropertyInfo("cache", info == null ? null : info.getProperty("cache"));
    cache.description = "The cache parameter may be set to either \"shared\" or \"private\"";
    cache.choices = new String[] {"shared", "private"};

    Util.trace("Driver.getPropertyInfo");
    return new DriverPropertyInfo[0];  // TODO encoding, foreign_keys, locking_mode, recursive_triggers, synchronous, load_extension
  }
  @Override
  public int getMajorVersion() {
    return 1;
  }
  @Override
  public int getMinorVersion() {
    return 0;
  }
  @Override
  public boolean jdbcCompliant() {
    return false;
  }
  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw Util.unsupported("Driver.getParentLogger");
  }
}
