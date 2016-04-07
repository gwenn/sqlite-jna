package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Data change notification callback.
 * <ul>
 * <li>The update hook is not invoked when internal system tables are modified (i.e. sqlite_master and sqlite_sequence).</li>
 * <li>The update hook is not invoked when WITHOUT ROWID tables are modified.</li>
 * <li>In the current implementation, the update hook is not invoked when duplication rows are deleted because of an ON CONFLICT REPLACE clause.</li>
 * <li>Nor is the update hook invoked when rows are deleted using the truncate optimization.</li>
 * </ul>
 * @see Conn#updateHook(UpdateHook)
 * @see <a href="http://sqlite.org/c3ref/update_hook.html">sqlite3_update_hook</a>
 */
public interface UpdateHook extends Callback {
	/**
	 * Data Change Notification Callback
	 * @param pArg <code>null</code>.
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
	 * @param dbName database name containing the affected row.
	 * @param tblName table name containing the affected row.
	 * @param rowId id of the affected row.
	 */
	default void callback(Pointer pArg, int actionCode, String dbName, String tblName, long rowId) {
		update(actionCode, dbName, tblName, rowId);
	}
	/**
	 * Data Change Notification Callback
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
	 * @param dbName database name containing the affected row.
	 * @param tblName table name containing the affected row.
	 * @param rowId id of the affected row.
	 */
	void update(int actionCode, String dbName, String tblName, long rowId);
}
