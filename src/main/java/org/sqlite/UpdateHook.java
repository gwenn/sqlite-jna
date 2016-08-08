package org.sqlite;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;

/**
 * Data change notification callback.
 * <ul>
 * <li>The update hook is not invoked when internal system tables are modified (i.e. sqlite_master and sqlite_sequence).</li>
 * <li>The update hook is not invoked when WITHOUT ROWID tables are modified.</li>
 * <li>In the current implementation, the update hook is not invoked when duplication rows are deleted because of an ON CONFLICT REPLACE clause.</li>
 * <li>Nor is the update hook invoked when rows are deleted using the truncate optimization.</li>
 * </ul>
 *
 * @see Conn#updateHook(UpdateHook)
 * @see <a href="http://sqlite.org/c3ref/update_hook.html">sqlite3_update_hook</a>
 */
public abstract class UpdateHook extends FunctionPointer {
	protected UpdateHook() {
		allocate();
	}
	private native void allocate();
	/**
	 * Data Change Notification Callback
	 *
	 * @param pArg       <code>null</code>.
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
	 * @param dbName     database name containing the affected row.
	 * @param tblName    table name containing the affected row.
	 * @param rowId      id of the affected row.
	 */
	public void call(Pointer pArg, int actionCode, @Cast("const char*") BytePointer dbName,
			@Cast("const char*") BytePointer tblName, @Cast("sqlite3_int64") long rowId) {
		update(actionCode, SQLite.getString(dbName), SQLite.getString(tblName), rowId);
	}
	/**
	 * Data Change Notification Callback
	 *
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE.
	 * @param dbName     database name containing the affected row.
	 * @param tblName    table name containing the affected row.
	 * @param rowId      id of the affected row.
	 */
	protected abstract void update(int actionCode, String dbName, String tblName, long rowId);
}
