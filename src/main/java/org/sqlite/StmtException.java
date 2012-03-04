package org.sqlite;

public class StmtException extends SQLiteException {
  private final Stmt stmt;

  public StmtException(Stmt stmt, String name, int errCode) {
    super(name, errCode);
    this.stmt = stmt;
  }

  public String getSql() {
    if (stmt == null) {
      return null;
    }
    return stmt.getSql();
  }
  
  @Override
  protected Conn getConn() {
    if (stmt == null) {
      return null;
    }
    return stmt.c;
  }
}
