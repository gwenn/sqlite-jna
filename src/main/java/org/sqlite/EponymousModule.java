package org.sqlite;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static org.sqlite.ErrCodes.SQLITE_NOMEM;
import static org.sqlite.ErrCodes.SQLITE_OK;
import static org.sqlite.SQLite.*;
import static org.sqlite.SQLite.C_POINTER;
import static org.sqlite.sqlite3_index_info.*;

public interface EponymousModule {
	/**
	 * @param db      sqlite3*
	 * @param aux     void*
	 * @param argc    int
	 * @param argv    const char**
	 * @param pp_vtab sqlite3_vtab**
	 * @param err_msg char**
	 */
	// TODO https://sqlite.org/c3ref/vtab_config.html
	default int connect(MemorySegment db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment pp_vtab, MemorySegment err_msg) {
		return create_connect(db, aux, argc, argv, pp_vtab, err_msg, false);
	}
	default int create_connect(MemorySegment db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment pp_vtab, MemorySegment err_msg, boolean isCreate) {
		sqlite3 sqlite3 = new sqlite3(db);
		argv = argv.reinterpret(C_POINTER.byteSize() * argc);
		err_msg = err_msg.reinterpret(C_POINTER.byteSize());
		Entry<Integer, MemorySegment> entry = connect(sqlite3, aux, argc, argv, err_msg, isCreate);
		int rc = entry.getKey();
		if (entry.getKey() == SQLITE_OK) {
			MemorySegment vtab = entry.getValue();
			if (isNull(vtab)) {
				return SQLITE_NOMEM;
			}
			pp_vtab = pp_vtab.reinterpret(C_POINTER.byteSize());
			pp_vtab.set(C_POINTER, 0, vtab); // *pp_vtab = vtab
		}
		return rc;
	}
	Entry<Integer,MemorySegment> connect(sqlite3 db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment errMsg, boolean isCreate);
	static Entry<Integer,MemorySegment> error(MemorySegment errMsg, int rc, String fmt, Object... args) {
		errMsg.set(C_POINTER, 0, sqlite3OwnedString(String.format(fmt, args)));
		return Map.entry(rc, MemorySegment.NULL);
	}

	/**
	 * @param vtab sqlite3_vtab*
	 * @param info sqlite3_index_info*
	 */
	// TODO https://sqlite.org/c3ref/vtab_collation.html
	// TODO https://sqlite.org/c3ref/vtab_in.html
	// TODO https://sqlite.org/c3ref/vtab_rhs_value.html
	// TODO https://sqlite.org/c3ref/vtab_distinct.html
	@SuppressWarnings("unused")
	default int bestIndex(MemorySegment vtab, MemorySegment info) {
		vtab = vtab.reinterpret(vtab_layout().byteSize());
		info = info.reinterpret(sqlite3_index_info.layout.byteSize());
		int nConstraint = nConstraint(info);
		Iterator<MemorySegment> aConstraint = aConstraint(info, nConstraint);
		Iterator<MemorySegment> aConstraintUsage = aConstraintUsage(info, nConstraint);
		return bestIndex(vtab, info, aConstraint, aConstraintUsage);
	}
	int bestIndex(MemorySegment vtab, MemorySegment info, Iterator<MemorySegment> aConstraint, Iterator<MemorySegment> aConstraintUsage);

	/**
	 * @param vtab sqlite3_vtab*
	 */
	@SuppressWarnings("unused")
	default int disconnect(MemorySegment vtab) {
		vtab = vtab.reinterpret(vtab_layout().byteSize());
		return disconnect(vtab, false);
	}
	default int disconnect(MemorySegment vtab, boolean isDestroy) {
		sqlite3_free(vtab);
		return SQLITE_OK;
	}

	/**
	 * @param vtab      sqlite3_vtab*
	 * @param pp_cursor sqlite3_vtab_cursor**
	 */
	default int open(MemorySegment vtab, MemorySegment pp_cursor) {
		vtab = vtab.reinterpret(vtab_layout().byteSize());
		MemorySegment cursor = open(vtab);
		if (cursor == null) return SQLITE_NOMEM;
		pp_cursor = pp_cursor.reinterpret(C_POINTER.byteSize());
		pp_cursor.set(C_POINTER, 0, cursor.asSlice(0, sqlite3_vtab_cursor.layout.byteSize())); // *ppCursor = &pCur->base;
		return SQLITE_OK;
	}
	default MemorySegment open(MemorySegment vtab) {
		MemorySegment cursor = sqlite3_malloc(layout());
		if (isNull(cursor)) {
			return null;
		}
		return cursor;
	}

	/**
	 * @param cursor sqlite3_vtab_cursor*
	 */
	default int close(MemorySegment cursor) {
		sqlite3_free(cursor);
		return SQLITE_OK;
	}
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param idx_num int
	 * @param idx_str const char*
	 * @param argc int
	 * @param argv sqlite3_value**
	 */
	@SuppressWarnings("unused")
	default int filter(MemorySegment cursor, int idx_num, MemorySegment idx_str, int argc, MemorySegment argv) {
		cursor = cursor.reinterpret(layout().byteSize());
		sqlite3_values values = sqlite3_values.build(argc, argv);
		//final String idx = getString(idx_str);
		return filter(cursor, idx_num, idx_str, values);
	}
	// TODO https://sqlite.org/c3ref/vtab_in_first.html
	int filter(MemorySegment cursor, int idxNum, MemorySegment idxStr, sqlite3_values values);

	/**
	 * @param cursor sqlite3_vtab_cursor*
	 */
	default int next(MemorySegment cursor) {
		cursor = cursor.reinterpret(layout().byteSize());
		return next(cursor, rowId(cursor));
	}
	int next(MemorySegment cursor, long rowId);

	/**
	 * @param cursor sqlite3_vtab_cursor*
	 */
	@SuppressWarnings("unused")
	default int eof(MemorySegment cursor) {
		cursor = cursor.reinterpret(layout.byteSize());
		return isEof(cursor) ? 1 : 0;
	}
	boolean isEof(MemorySegment cursor);

	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param ctx sqlite3_context*
	 * @param i int
	 */
	// TODO https://sqlite.org/c3ref/vtab_nochange.html
	default int column(MemorySegment cursor, MemorySegment ctx, int i) {
		cursor = cursor.reinterpret(layout().byteSize());
		sqlite3_context sqlite3_context = new sqlite3_context(ctx);
		return column(cursor, sqlite3_context, i);
	}
	int column(MemorySegment cursor, sqlite3_context sqlite3Context, int i);

	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param p_rowid sqlite3_int64*
	 */
	@SuppressWarnings("unused")
	default int rowId(MemorySegment cursor, MemorySegment p_rowid) {
		cursor = cursor.reinterpret(layout().byteSize());
		p_rowid = p_rowid.reinterpret(C_LONG_LONG.byteSize());
		p_rowid.set(C_LONG_LONG, 0, rowId(cursor));
		return SQLITE_OK;
	}
	long rowId(MemorySegment cursor);
	/**
	 * @return cursor layout
	 */
	MemoryLayout layout();
	/**
	 * @return vtab layout
	 */
	default MemoryLayout vtab_layout() {
		return sqlite3_vtab.layout;
	}

	static String dequote(String s) {
		if (s == null || s.isBlank() || s.length() < 2) {
			return s;
		}
		char quote = s.charAt(0);
		if (quote != '\'' && quote != '"') {
			return s;
		} else if (s.charAt(s.length()-1) != quote) {
			return s;
		}
		char[] data = s.toCharArray();
		int j = 0;
		for (int i = 1; i < data.length - 1; i++, j++) {
			if (data[i] == quote && data[i+1] == quote) i++;
			data[j] = data[i];
		}
		return new String(data, 0, j);
	}
}
