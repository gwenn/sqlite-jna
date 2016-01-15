/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Pointer;

import static org.sqlite.SQLite.*;

/**
 * Online Backup Object
 * <a href="https://www.sqlite.org/c3ref/backup.html">sqlite3_backup</a>
 */
public class Backup {
	private Pointer pBackup;
	private final Conn dst, src;

	Backup(Pointer pBackup, Conn dst, Conn src) {
		assert pBackup != null && dst != null && src != null;
		this.pBackup = pBackup;
		this.dst = dst;
		this.src = src;
	}

	/**
	 * Copy up to N pages between the source and destination databases
	 * @param nPage number of pages to copy
	 * @return <code>true</code> when successfully finishes copying all pages from source to destination.
	 * @see <a href="https://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupstep">sqlite3_backup_step</a>
	 */
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
			while (step(nPage)) {
				if (millis > 0L) {
					Thread.sleep(millis);
				}
			}
		} finally {
			finish();
		}
	}

	/**
	 * @return the number of pages still to be backed up
	 * @see <a href="https://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupremaining">sqlite3_backup_remaining</a>
	 */
	public int remaining() throws ConnException {
		checkInit();
		return sqlite3_backup_remaining(pBackup);
	}

	/**
	 * @return the total number of pages in the source database
	 * @see <a href="https://www.sqlite.org/c3ref/backup_finish.html#sqlite3backuppagecount">sqlite3_backup_pagecount</a>
	 */
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

	/**
	 * Destroy this backup.
	 * @return result code (No exception is thrown).
	 * @see <a href="https://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupfinish">sqlite3_backup_finish</a>
	 */
	public int finish() {
		if (pBackup == null) {
			return SQLITE_OK;
		}
		final int res = sqlite3_backup_finish(pBackup); // must be called only once
		pBackup = null;
		return res;
	}
	/**
	 * Destroy this backup and throw an exception if an error occured.
	 */
	public void finishAndCheck() throws ConnException {
		final int res = finish();
		if (res != SQLITE_OK) {
			throw new ConnException(dst, "backup finish failed", res);
		}
	}
	/**
	 * @return whether or not this backup is finished
	 */
	public boolean isFinished() {
		return pBackup == null;
	}

	void checkInit() throws ConnException {
		if (isFinished()) {
			throw new ConnException(dst, "backup already finished", ErrCodes.WRAPPER_SPECIFIC);
		}
	}
}
