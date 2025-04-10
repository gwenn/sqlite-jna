/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.lang.foreign.MemorySegment;

/**
 * Profiling callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_profile</a>
 */
@FunctionalInterface
public interface ProfileCallback {
	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	@SuppressWarnings("unused")
	default void callback(MemorySegment arg, String sql, long ns) {
		profile(sql, ns);
	}

	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	void profile(String sql, long ns);
}
