package org.sqlite;

public class StmtException extends SQLiteException {
  private final Stmt stmt;

  public StmtException(Stmt stmt, String message, int errCode) {
    super(message, errCode);
    this.stmt = stmt;
  }

  @Override
  protected Conn getConn() {
    if (stmt == null) {
      return null;
    }
    return stmt.c;
  }
}
