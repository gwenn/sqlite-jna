package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;

/**
 * Compile-time authorization callback
 *
 * @see Conn#setAuhtorizer(Authorizer, Pointer)
 * @see <a href="http://sqlite.org/c3ref/set_authorizer.html">sqlite3_set_authorizer</a>
 */
public abstract class Authorizer extends FunctionPointer {
	static { Loader.load(); }
	/**
	 * @param pArg       User data ({@link Conn#setAuhtorizer(Authorizer, Pointer)} second parameter)
	 * @param actionCode {@link ActionCodes}.*
	 * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
	 */
	public abstract int call(Pointer pArg, int actionCode, String arg1, String arg2, String dbName, String triggerName);

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
