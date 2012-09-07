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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
    final int flags = OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE | OpenFlags.SQLITE_OPEN_FULLMUTEX;
    final org.sqlite.Conn conn = org.sqlite.Conn.open(url.substring(PREFIX.length()), flags, vfs);
    conn.setBusyTimeout(3000);
    return new Conn(conn);
  }
  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url.startsWith(PREFIX);
  }
  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    Util.trace("Driver.getPropertyInfo");
    return new DriverPropertyInfo[0];  // TODO vfs, mode (ro, rw, rwc), cache, encoding, foreign_keys, locking_mode, recursive_triggers, synchronous, load_extension
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
