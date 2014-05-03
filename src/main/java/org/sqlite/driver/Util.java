/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import java.io.PrintStream;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

class Util {
  private static PrintStream out;

  static {
/*
    try {
      out = new PrintStream("/tmp/sqlite-jna-" + System.currentTimeMillis() + ".log");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
*/
    out = System.out;
  }

  private Util() {
  }

  static SQLException error(String msg) {
    trace(msg);
    return new SQLException(msg);
  }

  static SQLFeatureNotSupportedException caseUnsupported(String msg) {
    trace(msg);
    return new SQLFeatureNotSupportedException(msg);
  }

  static SQLFeatureNotSupportedException unsupported(String method) {
    trace(method);
    return new SQLFeatureNotSupportedException(String.format("%s not implemented by SQLite JDBC driver", method));
  }

  static void trace(String method) {
    out.println(method);
    out.flush();
  }

  static String escapeIdentifier(String identifier) {
    if (identifier == null) {
      return "";
    }
    if (identifier.indexOf('"') >= 0) { // escape quote by doubling them
      identifier = identifier.replaceAll("\"", "\"\"");
    }
    return identifier;
  }
  static String doubleQuote(String dbName) {
    if (dbName == null) {
      return "";
    }
    if ("main".equals(dbName) || "temp".equals(dbName)) {
      return dbName;
    }
    return '"' + escapeIdentifier(dbName) + '"'; // surround identifier with quote
  }
}
