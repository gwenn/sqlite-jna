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
import org.sqlite.SQLite;

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
  public static final String ENABLE_TRIGGERS = "enable_triggers";
  public static final String ENABLE_LOAD_EXTENSION = "enable_load_extension";
  public static final String ENCODING = "encoding";

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) return null;
    final String vfs = info == null ? null : info.getProperty(VFS);
    final int flags = getOpenFlags(info == null ? null : info.getProperty(MODE),
        info == null ? null : info.getProperty(CACHE));
    final org.sqlite.Conn conn = org.sqlite.Conn.open(url.substring(PREFIX.length()), flags, vfs);
    conn.setBusyTimeout(3000);
    final String encoding = info == null ? null : info.getProperty(ENCODING);
    if (encoding != null && encoding.length() > 0) {
      conn.fastExec(org.sqlite.Conn.mprintf("PRAGMA encoding=\"%w\"", encoding));
    }
    final String fks = info == null ? null : info.getProperty(FOREIGN_KEYS);
    if ("on".equals(fks)) {
      if (!conn.enableForeignKeys(true)) {
        SQLite.sqlite3_log(-1, "cannot enable the enforcement of foreign key constraints."); // TODO warning?
      }
    } else if ("off".equals(fks)) {
      if (conn.enableForeignKeys(false)) {
        SQLite.sqlite3_log(-1, "cannot disable the enforcement of foreign key constraints."); // TODO warning?
      }
    }
    final String triggers = info == null ? null : info.getProperty(ENABLE_TRIGGERS);
    if ("on".equals(triggers)) {
      if (!conn.enableTriggers(true)) {
        SQLite.sqlite3_log(-1, "cannot enable triggers."); // TODO warning?
      }
    } else if ("off".equals(fks)) {
      if (conn.enableTriggers(false)) {
        SQLite.sqlite3_log(-1, "cannot disable triggers."); // TODO warning?
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
    mode.description = "Open the database for read-only/read-write (but not create) access.";
    mode.choices = new String[]{"ro", "rw", "rwc"};
    if (mode.value == null) mode.value = "rwc"; // default
    final DriverPropertyInfo cache = new DriverPropertyInfo(CACHE, info == null ? null : info.getProperty(CACHE));
    cache.description = "Choose to participate or not participate in shared cache mode.";
    cache.choices = new String[]{"shared", "private"};
    final DriverPropertyInfo fks = new DriverPropertyInfo(FOREIGN_KEYS, info == null ? null : info.getProperty(FOREIGN_KEYS));
    fks.description = "Enable or disable the enforcement of foreign key constraints.";
    fks.choices = new String[]{"on", "off"};
    final DriverPropertyInfo triggers = new DriverPropertyInfo(ENABLE_TRIGGERS, info == null ? null : info.getProperty(ENABLE_TRIGGERS));
    triggers.description = "Enable or disable triggers.";
    triggers.choices = new String[]{"on", "off"};
    final DriverPropertyInfo ele = new DriverPropertyInfo(ENABLE_LOAD_EXTENSION, info == null ? null : info.getProperty(ENABLE_LOAD_EXTENSION));
    ele.description = "Turn extension loading on or off.";
    ele.choices = new String[]{"on", "off"};
    if (ele.value == null) ele.value = "off"; // default
    final DriverPropertyInfo encoding = new DriverPropertyInfo(ENCODING, info == null ? null : info.getProperty(ENCODING));
    encoding.description = "Set the encoding.";
    encoding.choices = new String[]{"UTF-8", "UTF-16", "UTF-16le", "UTF-16be"};

    return new DriverPropertyInfo[] {vfs, mode, cache, fks, triggers, ele, encoding}; // TODO locking_mode, recursive_triggers, synchronous
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
