package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

// The update hook is not invoked when internal system tables are modified (i.e. sqlite_master and sqlite_sequence).
// The update hook is not invoked when WITHOUT ROWID tables are modified.
// In the current implementation, the update hook is not invoked when duplication rows are deleted because of an ON CONFLICT REPLACE clause.
// Nor is the update hook invoked when rows are deleted using the truncate optimization.
public interface UpdateHook {
	/**
	 * @param actionCode org.sqlite.ActionCodes.SQLITE_INSERT | SQLITE_UPDATE | SQLITE_DELETE
	 */
	@Delegate
	void invoke(Pointer pArg, int actionCode, String dbName, String tblName, long rowId);
}
