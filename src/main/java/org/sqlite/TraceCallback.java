package org.sqlite;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;

/**
 * Tracing callback.
 *
 * @see <a href="http://sqlite.org/c3ref/profile.html">sqlite3_trace</a>
 */
public abstract class TraceCallback extends FunctionPointer {
	protected TraceCallback() {
		allocate();
	}
	private native void allocate();
	/**
	 * @param sql SQL statement text.
	 */
	@SuppressWarnings("unused")
	public void call(Pointer arg, @Cast("const char*") BytePointer sql) {
		trace(SQLite.getString(sql));
	}

	/**
	 * @param sql SQL statement text.
	 */
	protected abstract void trace(String sql);
}
