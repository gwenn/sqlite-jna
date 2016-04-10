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
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.Encoding;

/**
 * Tracing callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
 */
public interface TraceCallback {
	/**
	 * @param sql SQL statement text.
	 */
	@SuppressWarnings("unused")
	@Delegate
	default void callback(Pointer arg,@Encoding("UTF-8") String sql) {
		trace(sql);
	}

	/**
	 * @param sql SQL statement text.
	 */
	void trace(String sql);
}
