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
  public static final String VFS = "vfs";
  public static final String MODE = "mode";
  public static final String CACHE = "cache";
  public static final String FOREIGN_KEYS = "foreign_keys";
  public static final String ENABLE_LOAD_EXTENSION = "enable_load_extension";

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) return null;
    final String vfs = info == null ? null : info.getProperty(VFS);
    final int flags = getOpenFlags(info == null ? null : info.getProperty(MODE),
        info == null ? null : info.getProperty(CACHE));
    final org.sqlite.Conn conn = org.sqlite.Conn.open(url.substring(PREFIX.length()), flags, vfs);
    conn.setBusyTimeout(3000);
    final String fks = info == null ? null : info.getProperty(FOREIGN_KEYS);
    if ("on".equals(fks)) {
      if (!conn.enableForeignKeys(true)) {
        // TODO warning?
      }
    } else if ("off".equals(fks)) {
      if (conn.enableForeignKeys(false)) {
        // TODO warning?
      }
    }
    if ("on".equals(info == null ? null : info.getProperty(ENABLE_LOAD_EXTENSION))) {
      conn.enableLoadExtension(true);
    } // disabled by default
    return new Conn(conn, info);
  }

  private static int getOpenFlags(String mode, String cache) {
    int flags = OpenFlags.SQLITE_OPEN_FULLMUTEX | OpenFlags.SQLITE_OPEN_URI;
    if (mode == null || "rwc".equals(mode)) {
      flags |= OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE;
    } else if ("rw".equals(mode)) {
      flags |= OpenFlags.SQLITE_OPEN_READWRITE;
    } else if ("ro".equals(mode)) {
      flags |= OpenFlags.SQLITE_OPEN_READONLY;
    }
    if ("private".equals(cache)) {
      flags |= OpenFlags.SQLITE_OPEN_PRIVATECACHE;
    } else if ("shared".equals(cache)) {
      flags |= OpenFlags.SQLITE_OPEN_SHAREDCACHE;
    }
    return flags;
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url.startsWith(PREFIX);
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    final DriverPropertyInfo vfs = new DriverPropertyInfo(VFS, info == null ? null : info.getProperty(VFS));
    vfs.description = "Specify the name of a VFS object that provides the operating system interface that should be used to access the database file on disk.";
    final DriverPropertyInfo mode = new DriverPropertyInfo(MODE, info == null ? null : info.getProperty(MODE));
    mode.description = "The mode parameter may be set to either \"ro\", \"rw\" or \"rwc\"";
    mode.choices = new String[]{"ro", "rw", "rwc"};
    if (mode.value == null) mode.value = "rwc"; // default
    final DriverPropertyInfo cache = new DriverPropertyInfo(CACHE, info == null ? null : info.getProperty(CACHE));
    cache.description = "The cache parameter may be set to either \"shared\" or \"private\"";
    cache.choices = new String[]{"shared", "private"};
    final DriverPropertyInfo fks = new DriverPropertyInfo(FOREIGN_KEYS, info == null ? null : info.getProperty(FOREIGN_KEYS));
    fks.description = "The foreign_keys parameter may be set to either \"on\" or \"off\"";
    fks.choices = new String[]{"on", "off"};
    final DriverPropertyInfo ele = new DriverPropertyInfo(ENABLE_LOAD_EXTENSION, info == null ? null : info.getProperty(ENABLE_LOAD_EXTENSION));
    ele.description = "The enable_load_extension parameter may be set to either \"on\" or \"off\"";
    ele.choices = new String[]{"on", "off"};

    return new DriverPropertyInfo[] {vfs, mode, cache, fks, ele}; // TODO encoding, locking_mode, recursive_triggers, synchronous
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
