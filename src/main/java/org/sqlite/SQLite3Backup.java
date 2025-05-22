package org.sqlite;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static org.sqlite.SQLite.*;

/**
 * Online backup object
 *
 * @see <a href="http://sqlite.org/c3ref/backup.html">sqlite3_backup</a>
 */
final class SQLite3Backup {
	private MemorySegment p;
	int res;

	SQLite3Backup(MemorySegment p) {
		this.p = p;
	}

	private static final MethodHandle sqlite3_backup_init = downcallHandle(
		"sqlite3_backup_init", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static SQLite3Backup sqlite3_backup_init(SQLite3 pDst, String dstName, SQLite3 pSrc, String srcName) {
		try (Arena arena = Arena.ofConfined()) {
			return new SQLite3Backup((MemorySegment)sqlite3_backup_init.invokeExact(pDst.getPointer(),
				nativeString(arena, dstName), pSrc.getPointer(), nativeString(arena, srcName)));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_step = downcallHandle(
		"sqlite3_backup_step", IPI);
	static int sqlite3_backup_step(SQLite3Backup pBackup, int nPage) {
		try {
			return (int)sqlite3_backup_step.invokeExact(pBackup.p, nPage);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_remaining = downcallHandle(
		"sqlite3_backup_remaining", IP);
	static int sqlite3_backup_remaining(SQLite3Backup pBackup) {
		try {
			return (int)sqlite3_backup_remaining.invokeExact(pBackup.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_pagecount = downcallHandle(
		"sqlite3_backup_pagecount", IP);
	static int sqlite3_backup_pagecount(SQLite3Backup pBackup) {
		try {
			return (int)sqlite3_backup_pagecount.invokeExact(pBackup.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_finish = downcallHandle(
		"sqlite3_backup_finish", IP);

	void finish() {
		if (isNull(p)) {
			return;
		}
		int result;
		try {
			result = (int) sqlite3_backup_finish.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
		res = result; // must be called only once
		p = MemorySegment.NULL;
	}

	boolean isFinished() {
		return isNull(p);
	}
}
