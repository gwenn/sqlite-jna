package org.sqlite;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;

/**
 * Profiling callback.
 *
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_profile</a>
 */
public abstract class ProfileCallback extends FunctionPointer {
	protected ProfileCallback() {
		allocate();
	}
	private native void allocate();
	/**
	 * @param sql SQL statement text.
	 * @param ns  time in nanoseconds
	 */
	@SuppressWarnings("unused")
	public void call(Pointer arg, @Cast("const char*") BytePointer sql, @Cast("sqlite3_uint64") long ns) {
		profile(SQLite.getString(sql), ns);
	}

	/**
	 * @param sql SQL statement text.
	 * @param ns  time in nanoseconds
	 */
	protected abstract void profile(String sql, long ns);
}
