package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.foreign.*;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.SQLite.*;

public class sqlite3_module {
	@NonNull
	public static MemorySegment eponymousOnly(@NonNull EponymousModule m, @NonNull Arena arena) {
		MemorySegment struct = arena.allocate(layout);
		iVersion(struct, 2);
		//  For eponymous-only virtual tables, the xCreate method is NULL
		xConnect(struct, m, arena);
		xBestIndex(struct, m, arena);
		xDisconnect(struct, m, arena);
		xOpen(struct, m, arena);
		xClose(struct, m, arena);
		xFilter(struct, m, arena);
		xNext(struct, m, arena);
		xEof(struct, m, arena);
		xColumn(struct, m, arena);
		xRowid(struct, m, arena);
		return struct;
	}
	@NonNull
	public static MemorySegment eponymous(@NonNull EponymousModule m, @NonNull Arena arena) {
		MemorySegment struct = eponymousOnly(m, arena);
		// A virtual table is eponymous if its xCreate method is the exact same function
		// as the xConnect method
		struct.set(xCreate, xCreate_offset, struct.get(xConnect, xConnect_offset));
		struct.set(xDestroy, xDestroy_offset, struct.get(xDisconnect, xDisconnect_offset));
		return struct;
	}
	@NonNull
	public static MemorySegment readOnly(@NonNull Module m, @NonNull Arena arena) {
		MemorySegment struct = eponymousOnly(m, arena);
		xCreate(struct, m, arena);
		xDestroy(struct, m, arena);
		return struct;
	}
	@NonNull
	public static MemorySegment update(@NonNull UpdateModule m, @NonNull Arena arena) {
		MemorySegment struct = readOnly(m, arena);
		xUpdate(struct, m, arena);
		return struct.asReadOnly();
	}

	private static final MethodHandle sqlite3_declare_vtab = downcallHandle(
		"sqlite3_declare_vtab", IPP);
	public static int sqlite3_declare_vtab(@NonNull sqlite3 pDb, @NonNull String sql) {
		try (Arena arena = Arena.ofConfined()) {
			return (int)sqlite3_declare_vtab.invokeExact(pDb.getPointer(), nativeString(arena, sql));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final GroupLayout layout = MemoryLayout.structLayout(
		C_INT.withName("iVersion"),
		MemoryLayout.paddingLayout(4),
		C_POINTER.withName("xCreate"),
		C_POINTER.withName("xConnect"),
		C_POINTER.withName("xBestIndex"),
		C_POINTER.withName("xDisconnect"),
		C_POINTER.withName("xDestroy"),
		C_POINTER.withName("xOpen"),
		C_POINTER.withName("xClose"),
		C_POINTER.withName("xFilter"),
		C_POINTER.withName("xNext"),
		C_POINTER.withName("xEof"),
		C_POINTER.withName("xColumn"),
		C_POINTER.withName("xRowid"),
		C_POINTER.withName("xUpdate"),
		C_POINTER.withName("xBegin"),
		C_POINTER.withName("xSync"),
		C_POINTER.withName("xCommit"),
		C_POINTER.withName("xRollback"),
		C_POINTER.withName("xFindFunction"),
		C_POINTER.withName("xRename"),
		C_POINTER.withName("xSavepoint"),
		C_POINTER.withName("xRelease"),
		C_POINTER.withName("xRollbackTo"),
		C_POINTER.withName("xShadowName"),
		C_POINTER.withName("xIntegrity")
	).withName("sqlite3_module");

	private static final OfInt iVersion = (OfInt)layout.select(groupElement("iVersion"));

	private static void iVersion(MemorySegment struct, int fieldValue) {
		struct.set(iVersion, 0, fieldValue);
	}

	private static final AddressLayout xCreate = (AddressLayout)layout.select(groupElement("xCreate"));
	private static final int xCreate_offset = 8;
	private static final FunctionDescriptor xCreate_or_xConnect_desc = FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT, C_POINTER, C_POINTER, C_POINTER);
	private static final MethodHandle xCreate_handler = upcallHandle(Module.class, "create",
		xCreate_or_xConnect_desc);
	private static void xCreate(MemorySegment struct, Module fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xCreate_handler, fi, xCreate_or_xConnect_desc, arena);
		struct.set(xCreate, xCreate_offset, fieldValue);
	}

	private static final AddressLayout xConnect = (AddressLayout)layout.select(groupElement("xConnect"));
	private static final int xConnect_offset = 16;
	private static final MethodHandle xConnect_handler = upcallHandle(EponymousModule.class, "connect",
		xCreate_or_xConnect_desc);
	private static void xConnect(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xConnect_handler, fi, xCreate_or_xConnect_desc, arena);
		struct.set(xConnect, xConnect_offset, fieldValue);
	}

	private static final AddressLayout xBestIndex = (AddressLayout)layout.select(groupElement("xBestIndex"));
	private static final MethodHandle xBestIndex_handler = upcallHandle(EponymousModule.class, "bestIndex",
		IPP);
	private static void xBestIndex(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xBestIndex_handler, fi, IPP, arena);
		struct.set(xBestIndex, 24, fieldValue);
	}

	private static final AddressLayout xDisconnect = (AddressLayout)layout.select(groupElement("xDisconnect"));
	private static final int xDisconnect_offset = 32;
	private static final MethodHandle xDisconnect_handler = upcallHandle(EponymousModule.class, "disconnect",
		IP);
	private static void xDisconnect(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xDisconnect_handler, fi, IP, arena);
		struct.set(xDisconnect, xDisconnect_offset, fieldValue);
	}

	private static final AddressLayout xDestroy = (AddressLayout)layout.select(groupElement("xDestroy"));
	private static final int xDestroy_offset = 40;
	private static final MethodHandle xDestroy_handler = upcallHandle(Module.class, "destroy",
		IP);
	private static void xDestroy(MemorySegment struct, Module fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xDestroy_handler, fi, IP, arena);
		struct.set(xDestroy, xDestroy_offset, fieldValue);
	}

	private static final AddressLayout xOpen = (AddressLayout)layout.select(groupElement("xOpen"));
	private static final MethodHandle xOpen_handler = upcallHandle(EponymousModule.class, "open",
		IPP);
	private static void xOpen(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xOpen_handler, fi, IPP, arena);
		struct.set(xOpen, 48, fieldValue);
	}

	private static final AddressLayout xClose = (AddressLayout)layout.select(groupElement("xClose"));
	private static final MethodHandle xClose_handler = upcallHandle(EponymousModule.class, "close",
		IP);
	private static void xClose(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xClose_handler, fi, IP, arena);
		struct.set(xClose, 56, fieldValue);
	}

	private static final AddressLayout xFilter = (AddressLayout)layout.select(groupElement("xFilter"));
	private static final MethodHandle xFilter_handler = upcallHandle(EponymousModule.class, "filter",
		IPIPIP);
	private static void xFilter(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xFilter_handler, fi, IPIPIP, arena);
		struct.set(xFilter, 64, fieldValue);
	}

	private static final AddressLayout xNext = (AddressLayout)layout.select(groupElement("xNext"));
	private static final MethodHandle xNext_handler = upcallHandle(EponymousModule.class, "next",
		IP);
	private static void xNext(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xNext_handler, fi, IP, arena);
		struct.set(xNext, 72, fieldValue);
	}

	private static final AddressLayout xEof = (AddressLayout)layout.select(groupElement("xEof"));
	private static final MethodHandle xEof_handler = upcallHandle(EponymousModule.class, "eof",
		IP);
	private static void xEof(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xEof_handler, fi, IP, arena);
		struct.set(xEof, 80, fieldValue);
	}

	private static final AddressLayout xColumn = (AddressLayout)layout.select(groupElement("xColumn"));
	public static final FunctionDescriptor column_desc = FunctionDescriptor.of(C_INT, C_POINTER, C_POINTER, C_INT);
	private static final MethodHandle xColumn_handler = upcallHandle(EponymousModule.class, "column",
		column_desc);
	private static void xColumn(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xColumn_handler, fi, column_desc, arena);
		struct.set(xColumn, 88, fieldValue);
	}

	private static final AddressLayout xRowid = (AddressLayout)layout.select(groupElement("xRowid"));
	private static final MethodHandle xRowid_handler = upcallHandle(EponymousModule.class, "rowId",
		IPP);
	private static void xRowid(MemorySegment struct, EponymousModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xRowid_handler, fi, IPP, arena);
		struct.set(xRowid, 96, fieldValue);
	}

	private static final AddressLayout xUpdate = (AddressLayout)layout.select(groupElement("xUpdate"));
	private static final MethodHandle xUpdate_handler = upcallHandle(UpdateModule.class, "update",
		IPIPP);
	private static void xUpdate(MemorySegment struct, UpdateModule fi, Arena arena) {
		MemorySegment fieldValue = upcallStub(xUpdate_handler, fi, IPIPP, arena);
		struct.set(xUpdate, 104, fieldValue);
	}
}
