package org.sqlite;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;

/**
 * Compile-time authorization callback
 *
 * @see Conn#setAuhtorizer(Authorizer)
 * @see <a href="http://sqlite.org/c3ref/set_authorizer.html">sqlite3_set_authorizer</a>
 */
public abstract class Authorizer extends FunctionPointer {
	protected Authorizer() {
		allocate();
	}
	private native void allocate();
	/**
	 * @param pArg       User data (<code>null</code>)
	 * @param actionCode {@link ActionCodes}.*
	 * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
	 */
	public int call(Pointer pArg, int actionCode,
			@Cast("const char*") BytePointer arg1, @Cast("const char*") BytePointer arg2,
			@Cast("const char*") BytePointer dbName, @Cast("const char*") BytePointer triggerName) {
		return authorize(actionCode, SQLite.getString(arg1), SQLite.getString(arg2), SQLite.getString(dbName), SQLite.getString(triggerName));
	}
	protected abstract int authorize(int actionCode, String arg1, String arg2, String dbName, String triggerName);

	public static final int SQLITE_OK = ErrCodes.SQLITE_OK;
	/**
	 * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
	 */
	public static final int SQLITE_DENY = 1;
	/**
	 * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
	 */
	public static final int SQLITE_IGNORE = 2;
}
