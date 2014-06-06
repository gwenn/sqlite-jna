/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import static org.sqlite.SQLite.*;

public class Backup {
  private sqlite3_backup pBackup;
  private final Conn dst, src;

  Backup(sqlite3_backup pBackup, Conn dst, Conn src) {
    assert pBackup != null && dst != null && src != null;
    this.pBackup = pBackup;
    this.dst = dst;
    this.src = src;
  }

  public boolean step(int nPage) throws ConnException {
    checkInit();
    final int res = sqlite3_backup_step(pBackup, nPage);
    if (res == SQLITE_OK || res == ErrCodes.SQLITE_BUSY || res == ErrCodes.SQLITE_LOCKED) {
      sqlite3_log(-1, "busy/locked error during backup.");
      return true;
    } else if (res == SQLITE_DONE) {
      return false;
    }
    throw new ConnException(dst, "backup step failed", res);
  }

  // TODO progression callback

  /**
   * Run starts the backup:
   * <ul>
   * <li>copying up to 'nPage' pages between the source and destination at each step,</li>
   * <li>sleeping 'millis' between steps,</li>
   * <li>closing the backup when done or when an error happens.</li>
   * </ul>
   * Sleeping is disabled if 'sleepNs' is zero or negative.
   */
  public void run(int nPage, long millis) throws ConnException, InterruptedException {
    try {
      while (true) {
        if (!step(nPage)) {
          break;
        }
        if (millis > 0) {
          Thread.sleep(millis);
        }
      }
    } finally {
      finish();
    }
  }

  public int remaining() throws ConnException {
    checkInit();
    return sqlite3_backup_remaining(pBackup);
  }

  public int pageCount() throws ConnException {
    checkInit();
    return sqlite3_backup_pagecount(pBackup);
  }

  @Override
  protected void finalize() throws Throwable {
    if (pBackup != null) {
      sqlite3_log(-1, "dangling SQLite backup.");
      finish();
    }
    super.finalize();
  }

  public int finish() {
    if (pBackup == null) {
      return SQLITE_OK;
    }
    final int res = sqlite3_backup_finish(pBackup); // must be called only once
    pBackup = null;
    return res;
  }

  public void finishAndCheck() throws ConnException {
    final int res = finish();
    if (res != SQLITE_OK) {
      throw new ConnException(dst, "backup finish failed", res);
    }
  }

  public boolean isFinished() {
    return pBackup == null;
  }

  public void checkInit() throws ConnException {
    if (isFinished()) {
      throw new ConnException(dst, "backup already finished", ErrCodes.WRAPPER_SPECIFIC);
    }
  }
}
