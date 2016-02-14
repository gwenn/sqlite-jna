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

/**
 * Tracing callback.
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
 */
public abstract class TraceCallback extends FunctionPointer {
	static { Loader.load(); }
	/**
	 * @param sql SQL statement text.
	 */
	@SuppressWarnings("unused")
	public void call(Pointer arg, String sql) {
		trace(sql);
	}

	/**
	 * @param sql SQL statement text.
	 */
	protected abstract void trace(String sql);
}
