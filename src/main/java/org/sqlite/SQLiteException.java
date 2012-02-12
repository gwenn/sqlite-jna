package org.sqlite;

public class SQLiteException extends RuntimeException {
  protected final int errCode;

  public SQLiteException(String message, int errCode) {
    super(message);
    this.errCode = errCode;
  }

  /**
   * @return org.sqlite.ErrCodes.*
   */
  public int getErrCode() {
    return errCode;
  }
  
  public String getErrMsg() {
    final Conn c = getConn();
    if (c == null) {
      return null;
    }
    return c.getErrMsg();
  }

  protected Conn getConn() {
    return null;
  }
}
