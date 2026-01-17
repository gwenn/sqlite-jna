package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.sql.SQLFeatureNotSupportedException;

import static org.sqlite.SQLite.*;
import static org.sqlite.sqlite3_stmt.*;

/**
 * Database connection handle
 *
 * @see <a href="http://sqlite.org/c3ref/sqlite3.html">sqlite3</a>
 */
public final class sqlite3 {
	@NonNull
	private MemorySegment p;
	int res;
	// To avoid upcallStub(s) from being GCed before connection is closed
	private Arena arena;
	// Make sure a stmt is not finalized while current conn is being closed
	final Object lock = new Object();

	private static final MethodHandle sqlite3_errmsg = downcallHandle(
		"sqlite3_errmsg", PP);
	static String sqlite3_errmsg(@NonNull sqlite3 pDb) { // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
		try {
			return getString((MemorySegment) sqlite3_errmsg.invokeExact(pDb.getPointer()));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_errcode = downcallHandle(
		"sqlite3_errcode", IP);
	static int sqlite3_errcode(@NonNull sqlite3 pDb) {
		try {
			return (int) sqlite3_errcode.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_extended_result_codes = downcallHandle(
		"sqlite3_extended_result_codes", IPI);
	static int sqlite3_extended_result_codes(@NonNull sqlite3 pDb, boolean onoff) {
		try {
			return (int) sqlite3_extended_result_codes.invokeExact(pDb.getPointer(), onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_extended_errcode = downcallHandle(
		"sqlite3_extended_errcode", IP);
	static int sqlite3_extended_errcode(@NonNull sqlite3 pDb) {
		try {
			return (int) sqlite3_extended_errcode.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_open_v2 = downcallHandle(
		"sqlite3_open_v2", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_POINTER));
	static int sqlite3_open_v2(@NonNull String filename, @NonNull MemorySegment ppDb, int flags, @Nullable String vfs) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment filenameSegment = arena.allocateFrom(filename);
			MemorySegment vfsSegment = vfs == null ? MemorySegment.NULL : arena.allocateFrom(vfs);
			return (int) sqlite3_open_v2.invokeExact(filenameSegment, ppDb, flags, vfsSegment);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_close = downcallHandle(
		"sqlite3_close", IP);
	static int sqlite3_close(@NonNull sqlite3 pDb) {
		try {
			return (int) sqlite3_close.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_close_v2 = downcallHandle(
		"sqlite3_close_v2", IP);
	private static int sqlite3_close_v2(@NonNull sqlite3 pDb) { // since 3.7.14
		try {
			return (int) sqlite3_close_v2.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_interrupt = downcallHandle(
		"sqlite3_interrupt", VP);
	static void sqlite3_interrupt(@NonNull sqlite3 pDb) {
		try {
			sqlite3_interrupt.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_busy_handler = downcallHandle(
		"sqlite3_busy_handler", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final MethodHandle busy_handler = upcallHandle(BusyHandler.class, "callback",
		IPI);
	static int sqlite3_busy_handler(@NonNull sqlite3 pDb, BusyHandler bh, MemorySegment pArg) {
		try {
			// FIXME previous busyHandler will not be freed until pDb is closed & gced
			MemorySegment busyHandler = upcallStub(busy_handler, bh, IPI, pDb.getArena());
			return (int) sqlite3_busy_handler.invokeExact(pDb.getPointer(), busyHandler, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_busy_timeout = downcallHandle(
		"sqlite3_busy_timeout", IPI);
	static int sqlite3_busy_timeout(@NonNull sqlite3 pDb, int ms) {
		try {
			// TODO How to free previous busyHandler ?
			return (int) sqlite3_busy_timeout.invokeExact(pDb.getPointer(), ms);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_db_status = downcallHandle(
		"sqlite3_db_status", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_POINTER, C_INT));
	static int sqlite3_db_status(@NonNull sqlite3 pDb, int op, MemorySegment pCur, MemorySegment pHiwtr, boolean resetFlg) {
		try {
			return (int) sqlite3_db_status.invokeExact(pDb.getPointer(), op, pCur, pCur, resetFlg ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	// TODO https://sqlite.org/c3ref/c_dbconfig_defensive.html#sqlitedbconfiglookaside constants
	private static final MethodHandle sqlite3_db_config = downcallHandle(
		"sqlite3_db_config", FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_INT, C_POINTER));
	static int sqlite3_db_config(@NonNull sqlite3 pDb, int op, int v, MemorySegment pOk) {
		try {
			return (int) sqlite3_db_config.invokeExact(pDb.getPointer(), op, v, pOk);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_enable_load_extension = OMIT_LOAD_EXTENSION ? null : downcallHandle(
		"sqlite3_enable_load_extension", IPI);
	static int sqlite3_enable_load_extension(@NonNull sqlite3 pDb, boolean onoff) {
		checkActivated(sqlite3_enable_load_extension, "SQLITE_OMIT_LOAD_EXTENSION activated");
		try {
			return (int) sqlite3_enable_load_extension.invokeExact(pDb.getPointer(), onoff ? 1 : 0);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_load_extension = OMIT_LOAD_EXTENSION ? null : downcallHandle(
		"sqlite3_load_extension", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_load_extension(@NonNull sqlite3 pDb, @NonNull String file, @Nullable String proc, MemorySegment errMsg) {
		checkActivated(sqlite3_load_extension, "SQLITE_OMIT_LOAD_EXTENSION activated");
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_load_extension.invokeExact(pDb.getPointer(), nativeString(arena, file), nativeString(arena, proc), errMsg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_limit = downcallHandle(
		"sqlite3_limit", IPII);
	static int sqlite3_limit(@NonNull sqlite3 pDb, int id, int newVal) {
		try {
			return (int) sqlite3_limit.invokeExact(pDb.getPointer(), id, newVal);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_get_autocommit = downcallHandle(
		"sqlite3_get_autocommit", IP);
	static boolean sqlite3_get_autocommit(@NonNull sqlite3 pDb) {
		try {
			return ((int) sqlite3_get_autocommit.invokeExact(pDb.getPointer())) != 0;
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_changes = downcallHandle(
		"sqlite3_changes", IP);
	static int sqlite3_changes(@NonNull sqlite3 pDb) {
		try {
			return (int) sqlite3_changes.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_changes64 = versionAtLeast(3037000) ? downcallHandle(
		"sqlite3_changes64", LP) : null;
	static long sqlite3_changes64(@NonNull sqlite3 pDb) throws SQLFeatureNotSupportedException { // 3.37.0
		if (sqlite3_changes64 == null) {
			throw new SQLFeatureNotSupportedException("LargeUpdate not implemented for " + sqlite3_libversion());
		}
		try {
			return (long) sqlite3_changes64.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_total_changes = downcallHandle(
		"sqlite3_total_changes", IP);
	static int sqlite3_total_changes(@NonNull sqlite3 pDb) {
		try {
			return (int) sqlite3_total_changes.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_total_changes64 = versionAtLeast(3037000) ? downcallHandle(
		"sqlite3_total_changes64", LP) : null;
	static long sqlite3_total_changes64(@NonNull sqlite3 pDb) throws SQLFeatureNotSupportedException { // 3.37.0
		if (sqlite3_total_changes64 == null) {
			throw new SQLFeatureNotSupportedException("LargeUpdate not implemented for " + sqlite3_libversion());
		}
		try {
			return (long) sqlite3_total_changes64.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_last_insert_rowid = downcallHandle(
		"sqlite3_last_insert_rowid", LP);
	static long sqlite3_last_insert_rowid(@NonNull sqlite3 pDb) {
		try {
			return (long) sqlite3_last_insert_rowid.invokeExact(pDb.getPointer());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_db_filename = downcallHandle(
		"sqlite3_db_filename", PPP);
	@Nullable
	static String sqlite3_db_filename(@NonNull sqlite3 pDb, String dbName) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return getString((MemorySegment) sqlite3_db_filename.invokeExact(pDb.getPointer(), nativeString(arena, dbName)));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_db_readonly = downcallHandle(
		"sqlite3_db_readonly", IPP);
	static int sqlite3_db_readonly(@NonNull sqlite3 pDb, String dbName) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int) sqlite3_db_readonly.invokeExact(pDb.getPointer(), nativeString(arena, dbName));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_next_stmt = downcallHandle(
		"sqlite3_next_stmt", PPP);
	@NonNull
	private static MemorySegment sqlite3_next_stmt(@NonNull sqlite3 pDb, @NonNull MemorySegment stmt) {
		try {
			return (MemorySegment) sqlite3_next_stmt.invokeExact(pDb.getPointer(), stmt);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_table_column_metadata = downcallHandle(
		"sqlite3_table_column_metadata", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER,
			C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_table_column_metadata(@NonNull sqlite3 pDb, @Nullable String dbName, @NonNull String tableName, @NonNull String columnName,
											 MemorySegment pzDataType, MemorySegment pzCollSeq,
											 MemorySegment pNotNull, MemorySegment pPrimaryKey, MemorySegment pAutoinc) { // no copy needed
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_table_column_metadata.invokeExact(pDb.getPointer(),
				nativeString(arena, dbName), nativeString(arena, tableName), nativeString(arena, columnName),
				pzDataType, pzCollSeq, pNotNull, pPrimaryKey, pAutoinc);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_exec = downcallHandle(
		"sqlite3_exec", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_exec(@NonNull sqlite3 pDb, @NonNull String cmd, MemorySegment c, MemorySegment udp, MemorySegment errMsg) {
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_exec.invokeExact(pDb.getPointer(), nativeString(arena, cmd), c, udp, errMsg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	// https://sqlite.org/c3ref/c_prepare_normalize.html
	public static final int SQLITE_PREPARE_PERSISTENT = 0x01/*, SQLITE_PREPARE_NORMALIZE = 0x02*/, SQLITE_PREPARE_NO_VTAB = 0x04;
	private static final MethodHandle sqlite3_prepare_v3 = downcallHandle(
		"sqlite3_prepare_v3", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT, C_POINTER, C_POINTER));
	static int sqlite3_prepare_v3(@NonNull sqlite3 pDb, @NonNull MemorySegment sql, int nByte, int prepFlags, @NonNull MemorySegment ppStmt,
								  MemorySegment pTail) {
		try {
			return (int)sqlite3_prepare_v3.invokeExact(pDb.getPointer(), sql, nByte, prepFlags, ppStmt, pTail);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	// As there is only one ProgressCallback by connection, and it is used to implement query timeout,
	// the method visibility is restricted.
	private static final MethodHandle sqlite3_progress_handler = sqlite3_compileoption_used("OMIT_PROGRESS_CALLBACK") ? null :
		downcallHandle("sqlite3_progress_handler", FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER));
	private static final MethodHandle progress_callback = upcallHandle(SQLite.ProgressCallback.class, "callback",
		IP);
	static void sqlite3_progress_handler(@NonNull sqlite3 pDb, int nOps, SQLite.ProgressCallback xProgress, MemorySegment pArg) {
		checkActivated(sqlite3_progress_handler, "SQLITE_OMIT_PROGRESS_CALLBACK activated");
		try {
			// FIXME previous pc will not be freed until pDb is closed & gced
			MemorySegment pc = upcallStub(progress_callback, xProgress, IP, pDb.getArena());
			sqlite3_progress_handler.invokeExact(pDb.getPointer(), nOps, pc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_trace = OMIT_TRACE ? null : downcallHandle(
		"sqlite3_trace", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor trace_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_POINTER);
	private static final MethodHandle trace_callback = upcallHandle(TraceCallback.class, "callback",
		trace_callback_desc);
	static void sqlite3_trace(@NonNull sqlite3 pDb, TraceCallback xTrace, MemorySegment pArg) {
		checkActivated(sqlite3_trace, "SQLITE_OMIT_TRACE activated");
		try {
			// FIXME previous tc will not be freed until pDb is closed & gced
			MemorySegment tc = upcallStub(trace_callback, xTrace, trace_callback_desc, pDb.getArena());
			sqlite3_trace.invokeExact(pDb.getPointer(), tc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_profile = OMIT_TRACE ? null : downcallHandle(
		"sqlite3_profile", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor profile_callback_desc = FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_LONG_LONG);
	private static final MethodHandle profile_callback = upcallHandle(ProfileCallback.class, "callback",
		profile_callback_desc);
	static void sqlite3_profile(@NonNull sqlite3 pDb, ProfileCallback xProfile, MemorySegment pArg) {
		checkActivated(sqlite3_profile, "SQLITE_OMIT_TRACE activated");
		try {
			// FIXME previous pc will not be freed until pDb is closed & gced
			MemorySegment pc = upcallStub(profile_callback, xProfile, profile_callback_desc, pDb.getArena());
			sqlite3_profile.invokeExact(pDb.getPointer(), pc, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	// TODO sqlite3_commit_hook, sqlite3_rollback_hook
	private static final MethodHandle sqlite3_update_hook = downcallHandle(
		"sqlite3_update_hook", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor update_hook_desc = FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER, C_LONG_LONG);
	private static final MethodHandle update_hook = upcallHandle(UpdateHook.class, "callback",
		update_hook_desc);
	@NonNull
	static MemorySegment sqlite3_update_hook(@NonNull sqlite3 pDb, UpdateHook xUpdate, MemorySegment pArg) {
		try {
			// FIXME previous uh will not be freed until pDb is closed & gced
			MemorySegment uh = upcallStub(update_hook, xUpdate, update_hook_desc, pDb.getArena());
			return (MemorySegment)sqlite3_update_hook.invokeExact(pDb.getPointer(), uh, pArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_set_authorizer = sqlite3_compileoption_used("OMIT_AUTHORIZATION") ? null :
		downcallHandle("sqlite3_set_authorizer", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER));
	private static final FunctionDescriptor authorizer_desc = FunctionDescriptor.of(C_INT, C_POINTER, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER);
	private static final MethodHandle authorizer_up = upcallHandle(Authorizer.class, "callback",
		authorizer_desc);
	static int sqlite3_set_authorizer(@NonNull sqlite3 pDb, Authorizer authorizer, MemorySegment pUserData) {
		checkActivated(sqlite3_set_authorizer, "SQLITE_OMIT_AUTHORIZATION activated");
		try {
			// FIXME previous auth will not be freed until pDb is closed & gced
			MemorySegment auth = upcallStub(authorizer_up, authorizer, authorizer_desc, pDb.getArena());
			return (int)sqlite3_set_authorizer.invokeExact(pDb.getPointer(), auth, pUserData);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_unlock_notify = ENABLE_UNLOCK_NOTIFY ? downcallHandle(
		"sqlite3_unlock_notify", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER)) : null;
	private static final MethodHandle unlock_notify_up = upcallHandle(UnlockNotifyCallback.class, "callback",
		VPI);
	static int sqlite3_unlock_notify(@NonNull sqlite3 pBlocked, UnlockNotifyCallback xNotify, MemorySegment pNotifyArg) {
		checkActivated(sqlite3_unlock_notify, "SQLITE_ENABLE_UNLOCK_NOTIFY not activated");
		try {
			// FIXME previous unc will not be freed until pDb is closed & gced
			MemorySegment unc = upcallStub(unlock_notify_up, xNotify, VPI, pBlocked.getArena());
			return (int)sqlite3_unlock_notify.invokeExact(pBlocked.getPointer(), unc, pNotifyArg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	/*
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*,int,sqlite3_value**),
	void (*)(sqlite3_context*),
	void(*)(void*)
	*/
	// eTextRep: SQLITE_UTF8 => 1, ...
	private static final MethodHandle sqlite3_create_function_v2 = downcallHandle(
		"sqlite3_create_function_v2", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	private static final MethodHandle scalar_callback = upcallHandle(ScalarCallback.class, "callback",
		VPIP);
	private static final MethodHandle aggregate_step_callback = upcallHandle(AggregateStepCallback.class, "callback",
		VPIP);
	private static final MethodHandle aggregate_compute_callback = upcallHandle(AggregateComputeCallback.class, "callback",
		VP);
	static int sqlite3_create_function_v2(@NonNull sqlite3 pDb, @NonNull String functionName, int nArg, int eTextRep,
										  MemorySegment pApp, ScalarCallback xFunc, AggregateStepCallback xStep, AggregateComputeCallback xFinal, MemorySegment xDestroy) {
		assert (xStep == null || !xStep.inverse) && (xFinal == null || xFinal.isFinal);
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment xFu = upcallStub(scalar_callback, xFunc, VPIP, pDb.getArena());
			MemorySegment xS = upcallStub(aggregate_step_callback, xStep, VPIP, pDb.getArena());
			MemorySegment xFi = upcallStub(aggregate_compute_callback, xFinal, VP, pDb.getArena());
			return (int)sqlite3_create_function_v2.invokeExact(pDb.getPointer(), nativeString(arena, functionName), nArg, eTextRep, pApp, xFu, xS, xFi, xDestroy);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_create_window_function = downcallHandle(
		"sqlite3_create_window_function", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_create_window_function(@NonNull sqlite3 pDb, @NonNull String functionName, int nArg, int eTextRep,
											  MemorySegment pApp, AggregateStepCallback xStep, AggregateComputeCallback xFinal,
											  AggregateComputeCallback xValue, AggregateStepCallback xInverse, MemorySegment xDestroy) {
		assert !xStep.inverse && xFinal.isFinal && !xValue.isFinal && xInverse.inverse;
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment xS = upcallStub(aggregate_step_callback, xStep, VPIP, pDb.getArena());
			MemorySegment xFi = upcallStub(aggregate_compute_callback, xFinal, VP, pDb.getArena());
			MemorySegment xV = upcallStub(aggregate_compute_callback, xValue, VP, pDb.getArena());
			MemorySegment xI = upcallStub(aggregate_step_callback, xInverse, VPIP, pDb.getArena());
			return (int)sqlite3_create_window_function.invokeExact(pDb.getPointer(), nativeString(arena, functionName), nArg, eTextRep, pApp, xS, xFi, xV, xI, xDestroy);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_create_module_v2 = downcallHandle(
		"sqlite3_create_module_v2", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_POINTER));
	static int sqlite3_create_module_v2(@NonNull sqlite3 pDb, @NonNull String moduleName, @NonNull EponymousModule module, boolean eponymousOnly, MemorySegment pClientData, MemorySegment xDestroy) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment ms;
			if (module instanceof UpdateModule) {
				ms = sqlite3_module.update((UpdateModule) module, pDb.getArena());
			} else if (module instanceof Module) {
				ms = sqlite3_module.readOnly((Module) module, pDb.getArena());
			} else if (eponymousOnly) {
				ms = sqlite3_module.eponymousOnly(module, pDb.getArena());
			} else {
				ms = sqlite3_module.eponymous(module, pDb.getArena());
			}
			return (int)sqlite3_create_module_v2.invokeExact(pDb.getPointer(), nativeString(arena, moduleName), ms, pClientData, xDestroy);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	/** Do no memory allocations */
	public static int SQLITE_SERIALIZE_NOCOPY = 0x001;
	private static final MethodHandle sqlite3_serialize = downcallHandle(
		"sqlite3_serialize", FunctionDescriptor.of(C_POINTER, C_POINTER, C_POINTER, C_POINTER, C_INT));
	static @Nullable Serialized sqlite3_serialize(@NonNull sqlite3 pDb, @Nullable String dbName) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment piSize = arena.allocate(C_POINTER);
			MemorySegment ptr = (MemorySegment) sqlite3_serialize.invokeExact(pDb.getPointer(), nativeString(arena, dbName), piSize, SQLITE_SERIALIZE_NOCOPY);
			boolean shared;
			if (isNull(ptr)) {
				ptr = (MemorySegment) sqlite3_serialize.invokeExact(pDb.getPointer(), nativeString(arena, dbName), piSize, 0);
				if (isNull(ptr)) {
					return null;
				}
				shared = false;
			} else {
				shared = true;
			}
			long size = piSize.reinterpret(C_LONG_LONG.byteSize()).get(C_LONG_LONG, 0);
			return new Serialized(shared, ptr.reinterpret(size));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	private static final MethodHandle sqlite3_deserialize = downcallHandle(
		"sqlite3_deserialize", FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_POINTER, C_LONG_LONG, C_LONG_LONG, C_INT));
	static int sqlite3_deserialize(@NonNull sqlite3 pDb, @Nullable String dbName, Serialized data, int flags) {
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment ptr = data.ptr();
			return (int)sqlite3_deserialize.invokeExact(pDb.getPointer(), nativeString(arena, dbName), ptr, ptr.byteSize(), ptr.byteSize(), flags);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	sqlite3(@NonNull MemorySegment p) {
		this.p = p.asReadOnly();
	}

	MemorySegment getPointer() {
		return p;
	}

	void close() {
		synchronized (lock) {
			if (SQLite.isNull(p)) {
				return;
			}
			// Dangling statements
			MemorySegment stmt = sqlite3_next_stmt(this, MemorySegment.NULL);
			while (!SQLite.isNull(stmt)) {
				if (sqlite3_stmt_busy(stmt)) {
					SQLite.sqlite3_log(ErrCodes.SQLITE_MISUSE, "Dangling statement (not reset): \"" + sqlite3_sql(stmt) + "\"");
				} else {
					SQLite.sqlite3_log(ErrCodes.SQLITE_MISUSE, "Dangling statement (not finalize): \"" + sqlite3_sql(stmt) + "\"");
				}
				stmt = sqlite3_next_stmt(this, stmt);
			}
			res = sqlite3_close_v2(this); // must be called only once
			p = MemorySegment.NULL;
		}
	}

	boolean isClosed() {
		return SQLite.isNull(p);
	}

	private Arena getArena() {
		if (arena == null) { // lazy loading
			arena = Arena.ofAuto();
		}
		return arena;
	}
}
