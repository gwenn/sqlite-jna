/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

class Util {
  private static PrintWriter writer;

  static {
    try {
      writer = new PrintWriter("/tmp/sqlite-jna.log");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
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
    writer.println(method);
    writer.flush();
  }
}
