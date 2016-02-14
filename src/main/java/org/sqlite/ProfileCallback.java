/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;

/**
 * Profiling callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_profile</a>
 */
public abstract class ProfileCallback extends FunctionPointer {
	static { Loader.load(); }
	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	@SuppressWarnings("unused")
	public void call(Pointer arg, String sql,@Cast("sqlite3_uint64") long ns) {
		profile(sql, ns);
	}

	/**
	 * @param sql SQL statement text.
	 * @param ns time in nanoseconds
	 */
	protected abstract void profile(String sql, long ns);
}
