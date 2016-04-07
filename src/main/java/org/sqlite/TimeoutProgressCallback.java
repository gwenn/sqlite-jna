/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.sqlite.SQLite.ProgressCallback;

/**
 * Query Progress Callback.
 * @see <a href="http://sqlite.org/c3ref/progress_handler.html">sqlite3_progress_handler</a>
 */
public class TimeoutProgressCallback extends ProgressCallback {
	private long expiration;

	/**
	 * @return <code>true</code> when the operation times out.
	 */
	@Override
	protected boolean progress() {
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
