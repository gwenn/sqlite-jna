package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

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
final class sqlite3_backup {
	@NonNull
	private MemorySegment p;
	int res;

	sqlite3_backup(@NonNull MemorySegment p) {
		this.p = p.asReadOnly();
	}

	private static final MethodHandle sqlite3_backup_init = downcallHandle(
		"sqlite3_backup_init", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static sqlite3_backup sqlite3_backup_init(@NonNull sqlite3 pDst, @NonNull String dstName, @NonNull sqlite3 pSrc, @NonNull String srcName) {
		try (Arena arena = Arena.ofConfined()) {
			return new sqlite3_backup((MemorySegment)sqlite3_backup_init.invokeExact(pDst.getPointer(),
				nativeString(arena, dstName), pSrc.getPointer(), nativeString(arena, srcName)));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_step = downcallHandle(
		"sqlite3_backup_step", IPI);
	static int sqlite3_backup_step(@NonNull sqlite3_backup pBackup, int nPage) {
		try {
			return (int)sqlite3_backup_step.invokeExact(pBackup.p, nPage);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_remaining = downcallHandle(
		"sqlite3_backup_remaining", IP);
	static int sqlite3_backup_remaining(@NonNull sqlite3_backup pBackup) {
		try {
			return (int)sqlite3_backup_remaining.invokeExact(pBackup.p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_backup_pagecount = downcallHandle(
		"sqlite3_backup_pagecount", IP);
	static int sqlite3_backup_pagecount(@NonNull sqlite3_backup pBackup) {
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
