/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

public class TimeoutProgressCallback implements SQLite.ProgressCallback {
  private long expiration;

  @Override
  public boolean progress() {
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
      this.expiration = 0L;
      return;
    }
    this.expiration = System.currentTimeMillis() + timeout;
  }
}
