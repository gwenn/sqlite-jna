/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

public class ConnException extends SQLiteException {
  private final Conn c;

  public ConnException(Conn c, String reason, int errCode) {
    super(reason, errCode);
    this.c = c;
  }

  public String getFilename() {
    if (c == null) {
      return null;
    }
    return c.getFilename();
  }

  @Override
  protected Conn getConn() {
    return c;
  }
}
