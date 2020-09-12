package org.sqlite;

/**
 * Status Parameters for prepared statements
 * @see Stmt#status(StmtStatus, boolean)
 * @see <a href="http://sqlite.org/c3ref/c_stmtstatus_counter.html">Status Parameters</a>
 */
public enum StmtStatus {
	SQLITE_STMTSTATUS_FULLSCAN_STEP(1),
	SQLITE_STMTSTATUS_SORT(2),
	SQLITE_STMTSTATUS_AUTOINDEX(3),
	SQLITE_STMTSTATUS_VM_STEP(4),
	SQLITE_STMTSTATUS_REPREPARE(5),
	SQLITE_STMTSTATUS_RUN(6),
	SQLITE_STMTSTATUS_MEMUSED(99);
	final int value;

	StmtStatus(int value) {
		this.value = value;
	}
}
