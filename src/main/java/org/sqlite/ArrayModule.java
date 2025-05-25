package org.sqlite;

import java.lang.foreign.*;
import java.lang.foreign.ValueLayout.OfLong;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.ErrCodes.SQLITE_NOMEM;
import static org.sqlite.ErrCodes.SQLITE_OK;
import static org.sqlite.SQLite.*;
import static org.sqlite.sqlite3_index_info.*;
import static org.sqlite.sqlite3_index_info.sqlite3_index_constraint.SQLITE_INDEX_CONSTRAINT_EQ;
import static org.sqlite.sqlite3_module.sqlite3_declare_vtab;

/**
 * Port of <a href="https://www.sqlite.org/src/file?name=ext/misc/carray.c">carray</a>
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
	private static MemorySegment bind_ptr(MemorySegment bind) {
		return bind.get(bind_ptr, 0);
	}
	private static void bind_ptr(MemorySegment bind, MemorySegment ms) {
		bind.set(bind_ptr, 0, ms);
	}
	private static final OfLong bind_len = (OfLong)bind_layout.select(groupElement("len"));
	private static long bind_len(MemorySegment bind) {
		return bind.get(bind_len, 8);
	}
	private static void bind_len(MemorySegment bind, long n) {
		bind.set(bind_len, 8, n);
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
	public Entry<Integer, MemorySegment> connect(sqlite3 db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment err_msg, boolean isCreate) {
		int rc = sqlite3_declare_vtab(db, "CREATE TABLE x(value,pointer hidden)");
		MemorySegment vtab = MemorySegment.NULL;
		if (rc == SQLITE_OK) {
			vtab = sqlite3_malloc(vtab_layout());
		}
		return Map.entry(rc, vtab);
	}

	@Override
	public int bestIndex(MemorySegment vtab, MemorySegment info, Iterator<MemorySegment> aConstraint, Iterator<MemorySegment> aConstraintUsage) {
		// Index of the pointer= constraint
		boolean ptr_idx = false;
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

	private static final GroupLayout layout = MemoryLayout.structLayout(
		sqlite3_vtab_cursor.layout.withName("base"),
		C_LONG_LONG.withName("rowId"),
		C_POINTER.withName("ptr"),
		C_LONG_LONG.withName("len")
	).withName("jarray_cursor");
	private static final OfLong rowId = (OfLong)layout.select(groupElement("rowId"));
	@Override
	public long rowId(MemorySegment cursor) {
		return cursor.get(rowId, 8);
	}
	private static void rowId(MemorySegment cursor, long id) {
		cursor.set(rowId, 8, id);
	}
	private static final AddressLayout ptr = (AddressLayout)layout.select(groupElement("ptr"));
	private static MemorySegment ptr(MemorySegment cursor) {
		return cursor.get(ptr, 16);
	}
	private static void ptr(MemorySegment cursor, MemorySegment ms) {
		cursor.set(ptr, 16, ms);
	}
	private static final OfLong len = (OfLong)layout.select(groupElement("len"));
	private static long len(MemorySegment cursor) {
		return cursor.get(len, 24);
	}
	private static void len(MemorySegment cursor, long n) {
		cursor.set(len, 24, n);
	}

	@Override
	public int filter(MemorySegment cursor, int idxNum, MemorySegment idxStr, sqlite3_values values) {
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
	public int next(MemorySegment cursor, long rowId) {
		rowId(cursor, rowId + 1);
		return SQLITE_OK;
	}

	@Override
	public boolean isEof(MemorySegment cursor) {
		long id = rowId(cursor);
		long len = len(cursor);
		return id > len;
	}

	@Override
	public int column(MemorySegment cursor, sqlite3_context sqlite3Context, int i) {
		if (i == COLUMN_POINTER) {
			return SQLITE_OK;
		}
		MemorySegment ptr = ptr(cursor);
		ptr = ptr.reinterpret(len(cursor) * C_LONG_LONG.byteSize());
		long value = ptr.getAtIndex(C_LONG_LONG, rowId(cursor) - 1);
		sqlite3Context.setResultLong(value);
		return SQLITE_OK;
	}

	@Override
	public MemoryLayout layout() {
		return layout;
	}
}
