/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Tracing callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
 */
public interface TraceCallback extends Callback {
	/**
	 * @param sql SQL statement text.
	 */
	@SuppressWarnings("unused")
	default void callback(Pointer arg, String sql) {
		trace(sql);
	}

	/**
	 * @param sql SQL statement text.
	 */
	void trace(String sql);
}
