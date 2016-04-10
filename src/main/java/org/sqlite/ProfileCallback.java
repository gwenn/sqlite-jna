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
 * Profiling callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_profile</a>
 */
public interface ProfileCallback {
	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	@SuppressWarnings("unused")
	@Delegate
	default void callback(Pointer arg,@Encoding("UTF-8") String sql, long ns) {
		profile(sql, ns);
	}

	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	void profile(String sql, long ns);
}
