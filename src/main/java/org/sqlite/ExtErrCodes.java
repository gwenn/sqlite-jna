/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

/**
 * Extended Result Codes.
 * @see <a href="http://sqlite.org/c3ref/c_abort_rollback.html">Extended Result Codes</a>
 * @see <a href="http://sqlite.org/rescode.html#extrc">Extended Result Code List</a>
 */
public interface ExtErrCodes {
	int SQLITE_IOERR_READ = ErrCodes.SQLITE_IOERR | 1 << 8;
	int SQLITE_IOERR_SHORT_READ = ErrCodes.SQLITE_IOERR | 2 << 8;
	int SQLITE_IOERR_WRITE = ErrCodes.SQLITE_IOERR | 3 << 8;
	int SQLITE_IOERR_FSYNC = ErrCodes.SQLITE_IOERR | 4 << 8;
	int SQLITE_IOERR_DIR_FSYNC = ErrCodes.SQLITE_IOERR | 5 << 8;
	int SQLITE_IOERR_TRUNCATE = ErrCodes.SQLITE_IOERR | 6 << 8;
	int SQLITE_IOERR_FSTAT = ErrCodes.SQLITE_IOERR | 7 << 8;
	int SQLITE_IOERR_UNLOCK = ErrCodes.SQLITE_IOERR | 8 << 8;
	int SQLITE_IOERR_RDLOCK = ErrCodes.SQLITE_IOERR | 9 << 8;
	int SQLITE_IOERR_DELETE = ErrCodes.SQLITE_IOERR | 10 << 8;
	int SQLITE_IOERR_BLOCKED = ErrCodes.SQLITE_IOERR | 11 << 8;
	int SQLITE_IOERR_NOMEM = ErrCodes.SQLITE_IOERR | 12 << 8;
	int SQLITE_IOERR_ACCESS = ErrCodes.SQLITE_IOERR | 13 << 8;
	int SQLITE_IOERR_CHECKRESERVEDLOCK = ErrCodes.SQLITE_IOERR | 14 << 8;
	int SQLITE_IOERR_LOCK = ErrCodes.SQLITE_IOERR | 15 << 8;
	int SQLITE_IOERR_CLOSE = ErrCodes.SQLITE_IOERR | 16 << 8;
	int SQLITE_IOERR_DIR_CLOSE = ErrCodes.SQLITE_IOERR | 17 << 8;
	int SQLITE_IOERR_SHMOPEN = ErrCodes.SQLITE_IOERR | 18 << 8;
	int SQLITE_IOERR_SHMSIZE = ErrCodes.SQLITE_IOERR | 19 << 8;
	int SQLITE_IOERR_SHMLOCK = ErrCodes.SQLITE_IOERR | 20 << 8;
	int SQLITE_IOERR_SHMMAP = ErrCodes.SQLITE_IOERR | 21 << 8;
	int SQLITE_IOERR_SEEK = ErrCodes.SQLITE_IOERR | 22 << 8;
	int SQLITE_LOCKED_SHAREDCACHE = ErrCodes.SQLITE_LOCKED | 1 << 8;
	int SQLITE_BUSY_RECOVERY = ErrCodes.SQLITE_BUSY | 1 << 8;
	int SQLITE_CANTOPEN_NOTEMPDIR = ErrCodes.SQLITE_CANTOPEN | 1 << 8;
	int SQLITE_CORRUPT_VTAB = ErrCodes.SQLITE_CORRUPT | 1 << 8;
	int SQLITE_READONLY_RECOVERY = ErrCodes.SQLITE_READONLY | 1 << 8;
	int SQLITE_READONLY_CANTLOCK = ErrCodes.SQLITE_READONLY | 2 << 8;
	int SQLITE_READONLY_ROLLBACK = ErrCodes.SQLITE_READONLY | 3 << 8;
	int SQLITE_READONLY_DBMOVED = ErrCodes.SQLITE_READONLY | 4 << 8;
	int SQLITE_ABORT_ROLLBACK = ErrCodes.SQLITE_ABORT | 2 << 8;
	int SQLITE_CONSTRAINT_CHECK = ErrCodes.SQLITE_CONSTRAINT | 1 << 8;
	int SQLITE_CONSTRAINT_COMMITHOOK = ErrCodes.SQLITE_CONSTRAINT | 2 << 8;
	int SQLITE_CONSTRAINT_FOREIGNKEY = ErrCodes.SQLITE_CONSTRAINT | 3 << 8;
	int SQLITE_CONSTRAINT_FUNCTION = ErrCodes.SQLITE_CONSTRAINT | 4 << 8;
	int SQLITE_CONSTRAINT_NOTNULL = ErrCodes.SQLITE_CONSTRAINT | 5 << 8;
	int SQLITE_CONSTRAINT_PRIMARYKEY = ErrCodes.SQLITE_CONSTRAINT | 6 << 8;
	int SQLITE_CONSTRAINT_TRIGGER = ErrCodes.SQLITE_CONSTRAINT | 7 << 8;
	int SQLITE_CONSTRAINT_UNIQUE = ErrCodes.SQLITE_CONSTRAINT | 8 << 8;
	int SQLITE_CONSTRAINT_VTAB = ErrCodes.SQLITE_CONSTRAINT | 9 << 8;
	int SQLITE_CONSTRAINT_ROWID = ErrCodes.SQLITE_CONSTRAINT |10 << 8;
	//int SQLITE_NOTICE_RECOVER_WAL = ErrCodes.SQLITE_NOTICE | 1 << 8;
	//int SQLITE_NOTICE_RECOVER_ROLLBACK = ErrCodes.SQLITE_NOTICE | 2 << 8;
	//int SQLITE_WARNING_AUTOINDEX = ErrCodes.SQLITE_WARNING | 1 << 8;
	int SQLITE_AUTH_USER = ErrCodes.SQLITE_AUTH | 1 << 8;
}
