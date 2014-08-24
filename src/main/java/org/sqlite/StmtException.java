/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

public class StmtException extends SQLiteException {
  private final transient Stmt stmt;

  public StmtException(Stmt stmt, String reason, int errCode) {
    super(stmt == null ? null : stmt.c, reason, errCode);
    this.stmt = stmt;
  }

  public String getSql() {
    if (stmt == null) {
      return null;
    }
    return stmt.getSql();
  }
}
