package org.sqlite;

public class Rows extends AbstractRows {
  private final Stmt stmt;
  private boolean open;
  private int row; // Initialized at -1 when there is no result otherwise 0

  public Rows(Stmt stmt, boolean hasRow) {
    this.stmt = stmt;
    this.row = hasRow ? 0 : -1;
    this.open = true;
  }

  @Override
  Stmt getStmt() {
    return stmt;
  }

  @Override
  int fixCol(int columnIndex) {
    return columnIndex - 1;
  }

  @Override
  boolean step() throws StmtException {
    checkOpen();
    if (row == -1) { // no result
      return false;
    }
    if (row == 0) {
      row++;
      return true;
    }
    final boolean hasRow = stmt.step();
    if (hasRow) {
      row++;
    } else {
      close();
    }
    return hasRow;
  }

  @Override
  int row() {
    return row;
  }

  @Override
  void checkOpen() throws StmtException {
    if (!open) {
      throw new StmtException(stmt, "resultSet closed", ErrCodes.WRAPPER_SPECIFIC);
    }
  }
  @Override
  public boolean isClosed() {
    return !open;
  }
  @Override
  void _close() throws StmtException {
    open = false;
    stmt.reset();
  }
}
