package org.sqlite;

import com.sun.jna.Pointer;

public class Backup {
  private Pointer pBackup;
  private final Conn dst, src;

  Backup(Pointer pBackup, Conn dst, Conn src) {
    this.pBackup = pBackup;
    this.dst = dst;
    this.src = src;
  }

  public boolean step(int nPage) throws ConnException {
    final int res = SQLite.sqlite3_backup_step(pBackup, nPage);
    if (res == SQLite.SQLITE_OK || res == ErrCodes.SQLITE_BUSY || res == ErrCodes.SQLITE_LOCKED) {
      SQLite.sqlite3_log(-1, "busy/locked error during backup.");
      return true;
    } else if (res == SQLite.SQLITE_DONE) {
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

  public int remaining() {
    return SQLite.sqlite3_backup_remaining(pBackup);
  }

  public int pageCount() {
    return SQLite.sqlite3_backup_pagecount(pBackup);
  }

  @Override
  protected void finalize() throws Throwable {
    if (pBackup != null) {
      SQLite.sqlite3_log(-1, "dangling SQLite backup.");
      finish();
    }
    super.finalize();
  }

  public int finish() {
    if (pBackup == null) {
      return SQLite.SQLITE_OK;
    }
    final int res = SQLite.sqlite3_backup_finish(pBackup); // must be called only once
    pBackup = null;
    return res;
  }

  public void finishAndCheck() throws ConnException {
    final int res = finish();
    if (res != SQLite.SQLITE_OK) {
      throw new ConnException(dst, "backup finish failed", res);
    }
  }
}
