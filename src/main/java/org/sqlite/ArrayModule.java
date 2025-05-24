package org.sqlite;

import java.lang.foreign.*;
import java.lang.foreign.ValueLayout.OfLong;
import java.util.Iterator;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.ErrCodes.SQLITE_NOMEM;
import static org.sqlite.ErrCodes.SQLITE_OK;
import static org.sqlite.SQLite.*;
import static org.sqlite.sqlite3_index_info.*;
import static org.sqlite.sqlite3_index_info.sqlite3_index_constraint.SQLITE_INDEX_CONSTRAINT_EQ;
import static org.sqlite.sqlite3_module.sqlite3_declare_vtab;

/**
 * Port of <a href="http://www.sqlite.org/cgi/src/finfo?name=ext/misc/carray.c">carray</a>
 * C extension: <a href="https://www.sqlite.org/carray.html">carray</a>
 */
public class ArrayModule implements EponymousModule {
	public static final ArrayModule INSTANCE = new ArrayModule();
	private static final int COLUMN_POINTER = 1;
	private static final MemorySegment POINTER_NAME = nativeString(Arena.global(), "jarray");

	public static void load_module(Conn conn) throws ConnException {
		conn.createModule("jarray", INSTANCE, true);
	}

	private static final GroupLayout bind_layout = MemoryLayout.structLayout(
		C_POINTER.withName("ptr"),
		C_LONG_LONG.withName("len")
	).withName("jarray_bind");
	private static final AddressLayout bind_ptr = (AddressLayout)bind_layout.select(groupElement("ptr"));
	private static final long bind_ptrOffset = 0;
	private static MemorySegment bind_ptr(MemorySegment bind) {
		return bind.get(bind_ptr, bind_ptrOffset);
	}
	private static void bind_ptr(MemorySegment bind, MemorySegment ms) {
		bind.set(bind_ptr, bind_ptrOffset, ms);
	}
	private static final OfLong bind_len = (OfLong)bind_layout.select(groupElement("len"));
	private static final long bind_lenOffset = bind_ptr.byteSize();
	private static long bind_len(MemorySegment bind) {
		return bind.get(bind_len, bind_lenOffset);
	}
	private static void bind_len(MemorySegment bind, long n) {
		bind.set(bind_len, bind_lenOffset, n);
	}
	@SuppressWarnings("unused")
	public static void bindDel(MemorySegment bind) {
		bind = bind.reinterpret(bind_layout.byteSize());
		sqlite3_free(bind_ptr(bind));
		sqlite3_free(bind);
	}
	private static final MemorySegment xDel = upcallStub(ArrayModule.class, "bindDel", VP);
	static int bind_array(sqlite3_stmt pStmt, int i, long[] array) {
		MemorySegment ptr = sqlite3_malloc(array.length * C_LONG_LONG.byteSize());
		if (isNull(ptr)) {
			return SQLITE_NOMEM;
		}
		MemorySegment.copy(array, 0, ptr, C_LONG_LONG, 0, array.length);
		MemorySegment bind = sqlite3_malloc(bind_layout.byteSize());
		if (isNull(bind)) {
			return SQLITE_NOMEM;
		}
		bind_ptr(bind, ptr);
		bind_len(bind, array.length);
		return sqlite3_stmt.sqlite3_bind_pointer(pStmt, i, bind, POINTER_NAME, xDel);
	}

	private ArrayModule() {
	}

	@Override
	public int connect(MemorySegment db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment pp_vtab, MemorySegment err_msg) {
		sqlite3 sqlite3 = new sqlite3(db);
		int rc = sqlite3_declare_vtab(sqlite3, "CREATE TABLE x(value,pointer hidden)");
		if (rc == SQLITE_OK) {
			//argv = argv.reinterpret(argc * C_POINTER.byteSize());
			MemorySegment vtab = sqlite3_malloc(sqlite3_vtab.layout);
			if (isNull(vtab)) {
				return SQLITE_NOMEM;
			}
			pp_vtab = pp_vtab.reinterpret(C_POINTER.byteSize());
			pp_vtab.set(C_POINTER, 0, vtab); // *pp_vtab = vtab
		}
		return rc;
	}

	@Override
	public int bestIndex(MemorySegment vtab, MemorySegment info) {
		// Index of the pointer= constraint
		boolean ptr_idx = false;
		info = info.reinterpret(sqlite3_index_info.layout.byteSize());
		int nConstraint = nConstraint(info);
		Iterator<MemorySegment> aConstraint = aConstraint(info, nConstraint);
		Iterator<MemorySegment> aConstraintUsage = aConstraintUsage(info, nConstraint);
		while (aConstraint.hasNext()) {
			MemorySegment constraint = aConstraint.next();
			MemorySegment constraint_usage = aConstraintUsage.next();
			if (!sqlite3_index_constraint.usable(constraint)) {
				continue;
			}
			if (sqlite3_index_constraint.op(constraint) != SQLITE_INDEX_CONSTRAINT_EQ) {
				continue;
			}
			if (COLUMN_POINTER == sqlite3_index_constraint.iColumn(constraint)) {
				ptr_idx = true;
				sqlite3_index_constraint_usage.argvIndex(constraint_usage, 1);
				sqlite3_index_constraint_usage.omit(constraint_usage, (byte)1);
			}
		}
		if (ptr_idx) {
			sqlite3_index_info.estimatedCost(info, 1);
			sqlite3_index_info.estimatedRows(info, 100);
			sqlite3_index_info.idxNum(info, 1);
		} else {
			sqlite3_index_info.estimatedCost(info, 2_147_483_647);
			sqlite3_index_info.estimatedRows(info, 2_147_483_647);
			sqlite3_index_info.idxNum(info, 0);
		}
		return SQLITE_OK;
	}

	@Override
	public int disconnect(MemorySegment vtab) {
		sqlite3_free(vtab);
		return SQLITE_OK;
	}

	private static final GroupLayout layout = MemoryLayout.structLayout(
		sqlite3_vtab_cursor.layout.withName("base"),
		C_LONG_LONG.withName("rowId"),
		C_POINTER.withName("ptr"),
		C_LONG_LONG.withName("len")
	).withName("jarray_cursor");
	private static final GroupLayout base = (GroupLayout)layout.select(groupElement("base"));
	private static final OfLong rowId = (OfLong)layout.select(groupElement("rowId"));
	private static final long rowIdOffset = sqlite3_vtab_cursor.layout.byteSize();
	private static long rowId(MemorySegment cursor) {
		return cursor.get(rowId, rowIdOffset);
	}
	private static void rowId(MemorySegment cursor, long id) {
		cursor.set(rowId, rowIdOffset, id);
	}
	private static final AddressLayout ptr = (AddressLayout)layout.select(groupElement("ptr"));
	private static final long ptrOffset = sqlite3_vtab_cursor.layout.byteSize() + rowId.byteSize();
	private static MemorySegment ptr(MemorySegment cursor) {
		return cursor.get(ptr, ptrOffset);
	}
	private static void ptr(MemorySegment cursor, MemorySegment ms) {
		cursor.set(ptr, ptrOffset, ms);
	}
	private static final OfLong len = (OfLong)layout.select(groupElement("len"));
	private static final long lenOffset = ptrOffset + ptr.byteSize();
	private static long len(MemorySegment cursor) {
		return cursor.get(len, lenOffset);
	}
	private static void len(MemorySegment cursor, long n) {
		cursor.set(len, lenOffset, n);
	}

	@Override
	public int open(MemorySegment vtab, MemorySegment pp_cursor) {
		MemorySegment cursor = sqlite3_malloc(layout);
		if (isNull(cursor)) {
			return SQLITE_NOMEM;
		}
		pp_cursor = pp_cursor.reinterpret(C_POINTER.byteSize());
		pp_cursor.set(C_POINTER, 0, cursor.asSlice(0, base.byteSize())); // *ppCursor = &pCur->base;
		return SQLITE_OK;
	}

	@Override
	public int close(MemorySegment cursor) {
		sqlite3_free(cursor);
		return SQLITE_OK;
	}

	@Override
	public int filter(MemorySegment cursor, int idxNum, MemorySegment idxStr, sqlite3_values values) {
		cursor = cursor.reinterpret(layout.byteSize());
		if (idxNum > 0) {
			MemorySegment bind = values.getPointer(0, POINTER_NAME, bind_layout);
			ptr(cursor, bind_ptr(bind));
			len(cursor, bind_len(bind));
		} else {
			ptr(cursor, MemorySegment.NULL);
			len(cursor, 0);
		}
		rowId(cursor, 1);
		return SQLITE_OK;
	}

	@Override
	public int next(MemorySegment cursor) {
		cursor = cursor.reinterpret(layout.byteSize());
		long id = rowId(cursor);
		rowId(cursor, id + 1);
		return SQLITE_OK;
	}

	@Override
	public int eof(MemorySegment cursor) {
		cursor = cursor.reinterpret(layout.byteSize());
		long id = rowId(cursor);
		long len = len(cursor);
		return id > len ? 1 : 0;
	}

	@Override
	public int column(MemorySegment cursor, sqlite3_context sqlite3Context, int i) {
		if (i == COLUMN_POINTER) {
			return SQLITE_OK;
		}
		cursor = cursor.reinterpret(layout.byteSize());
		MemorySegment ptr = ptr(cursor);
		ptr = ptr.reinterpret(len(cursor) * C_LONG_LONG.byteSize());
		long value = ptr.getAtIndex(C_LONG_LONG, rowId(cursor) - 1);
		sqlite3Context.setResultLong(value);
		return SQLITE_OK;
	}

	@Override
	public int rowId(MemorySegment cursor, MemorySegment p_rowid) {
		cursor = cursor.reinterpret(layout.byteSize());
		long id = rowId(cursor);
		p_rowid.set(C_LONG_LONG, 0, id);
		return SQLITE_OK;
	}
}
