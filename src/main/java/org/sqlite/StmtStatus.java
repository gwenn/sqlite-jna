package org.sqlite;

public enum StmtStatus {
	SQLITE_STMTSTATUS_FULLSCAN_STEP(1),
	SQLITE_STMTSTATUS_SORT(2),
	SQLITE_STMTSTATUS_AUTOINDEX(3),
	SQLITE_STMTSTATUS_VM_STEP(4);
	final int value;

	StmtStatus(int value) {
		this.value = value;
	}
}
