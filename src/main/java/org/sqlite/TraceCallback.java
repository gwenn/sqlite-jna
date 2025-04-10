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

import static org.sqlite.SQLite.getString;

/**
 * Tracing callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
 */
@FunctionalInterface
public interface TraceCallback {
	/**
	 * @param sql SQL statement text.
	 */
	@SuppressWarnings("unused")
	default void callback(MemorySegment arg, MemorySegment sql) {
		trace(getString(sql));
	}

	/**
	 * @param sql SQL statement text.
	 */
	void trace(String sql);
}
