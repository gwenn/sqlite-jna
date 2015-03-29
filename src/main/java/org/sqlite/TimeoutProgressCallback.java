/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import jnr.ffi.Pointer;
import org.sqlite.SQLite.ProgressCallback;

public class TimeoutProgressCallback implements ProgressCallback {
  private long expiration;

  @Override
  public boolean invoke(Pointer arg) {
    if (expiration == 0 || System.currentTimeMillis() <= expiration) {
      return false;
    }
    return true;
  }

  /**
   * @param timeout in millis
   */
  public void setTimeout(long timeout) {
    if (timeout == 0) {
      expiration = 0L;
      return;
    }
    expiration = System.currentTimeMillis() + timeout;
  }
}
