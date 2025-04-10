package org.sqlite;

import java.lang.foreign.MemorySegment;

import static org.sqlite.SQLite.getString;

/**
 * Compile-time authorization callback
 *
 * @see Conn#setAuhtorizer(Authorizer)
 * @see <a href="http://sqlite.org/c3ref/set_authorizer.html">sqlite3_set_authorizer</a>
 */
@FunctionalInterface
public interface Authorizer {
	/**
	 * @param pArg       User data (<code>null</code>)
	 * @param actionCode {@link ActionCodes}.*
	 * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
	 */
	default int callback(MemorySegment pArg, int actionCode, MemorySegment arg1, MemorySegment arg2, MemorySegment dbName, MemorySegment triggerName) {
		return authorize(actionCode, getString(arg1), getString(arg2), getString(dbName), getString(triggerName));
	}

	/**
	 * @param actionCode {@link ActionCodes}.*
	 * @return {@link #SQLITE_OK} or {@link #SQLITE_DENY} or {@link #SQLITE_IGNORE}
	 */
	int authorize(int actionCode, String arg1, String arg2, String dbName, String triggerName);

	int SQLITE_OK = ErrCodes.SQLITE_OK;
	/**
	 * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
	 */
	int SQLITE_DENY = 1;
	/**
	 * @see <a href="http://sqlite.org/c3ref/c_deny.html">Authorizer Return Codes</a>
	 */
	int SQLITE_IGNORE = 2;
}
