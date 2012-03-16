/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.sql.SQLException;

public class SQLiteException extends SQLException {
  public SQLiteException(String reason, int errCode) {
    super(reason, null, errCode);
  }
  
  @Override
  public String getMessage() {
    final String errMsg = getErrMsg();
    if (errMsg != null && !errMsg.isEmpty()) {
      return errMsg;
    } else {
      if (getErrorCode() > 0) {
        return String.format("%s (code %d)", super.getMessage(), getErrorCode());
      } else {
        return super.getMessage();
      }
    }
  }

  public String getErrMsg() {
    final Conn c = getConn();
    if (c == null) {
      return null;
    }
    if (getErrorCode() >= 0) {
      return c.getErrMsg();
    }
    return null;
  }

  protected Conn getConn() {
    return null;
  }
}
