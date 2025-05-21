package org.sqlite;

import java.lang.foreign.MemorySegment;

import static org.sqlite.ErrCodes.SQLITE_OK;

public interface EponymousModule {
	/**
	 * TODO sqlite3_declare_vtab
	 * @param db      sqlite3*
	 * @param aux     void*
	 * @param argc    int
	 * @param argv    const char**
	 * @param pp_vtab sqlite3_vtab**
	 * @param err_msg char**
	 */
	int connect(MemorySegment db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment pp_vtab, MemorySegment err_msg);
	/**
	 * @param vtab sqlite3_vtab*
	 * @param info sqlite3_index_info*
	 */
	default int bestIndex(MemorySegment vtab, MemorySegment info) {
		return SQLITE_OK;
	}
	/**
	 * @param vtab sqlite3_vtab*
	 */
	default int disconnect(MemorySegment vtab) {
		return SQLITE_OK;

	}
	/**
	 * @param vtab      sqlite3_vtab*
	 * @param pp_cursor sqlite3_vtab_cursor**
	 */
	int open(MemorySegment vtab, MemorySegment pp_cursor);
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 */
	int close(MemorySegment cursor);
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param idx_num int
	 * @param idx_str const char*
	 * @param argc int
	 * @param argv sqlite3_value**
	 */
	int filter(MemorySegment cursor, int idx_num, MemorySegment idx_str, int argc, MemorySegment argv);
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 */
	int next(MemorySegment cursor);
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 */
	int eof(MemorySegment cursor);
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param ctx sqlite3_context*
	 * @param i int
	 */
	int column(MemorySegment cursor, MemorySegment ctx, int i);
	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param p_rowid sqlite3_int64*
	 */
	int rowId(MemorySegment cursor, MemorySegment p_rowid);
}
