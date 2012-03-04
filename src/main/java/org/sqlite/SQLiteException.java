package org.sqlite;

public class SQLiteException extends RuntimeException {
  protected final int errCode;
  private String msg;

  public SQLiteException(String msg, int errCode) {
    super();
    this.msg = msg;
    this.errCode = errCode;
  }
  
  @Override
  public String getMessage() {
    final String errMsg = getErrMsg();
    if (errMsg != null && !errMsg.isEmpty()) {
      return errMsg;
    } else {
      if (errCode > 0) {
        return String.format("%s (code %d)", msg, errCode);
      } else {
        return msg;
      }
    }
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
    if (errCode >= 0) {
      return c.getErrMsg();
    }
    return null;
  }

  protected Conn getConn() {
    return null;
  }
}
