package org.sqlite;

import java.lang.foreign.MemorySegment;

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
	int connect(MemorySegment db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment pp_vtab, MemorySegment err_msg);
	/**
	 * @param vtab sqlite3_vtab*
	 * @param info sqlite3_index_info*
	 */
	// TODO https://sqlite.org/c3ref/vtab_collation.html
	// TODO https://sqlite.org/c3ref/vtab_in.html
	// TODO https://sqlite.org/c3ref/vtab_rhs_value.html
	// TODO https://sqlite.org/c3ref/vtab_distinct.html
	int bestIndex(MemorySegment vtab, MemorySegment info);
	/**
	 * @param vtab sqlite3_vtab*
	 */
	int disconnect(MemorySegment vtab);
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
	default int filter(MemorySegment cursor, int idx_num, MemorySegment idx_str, int argc, MemorySegment argv) {
		final sqlite3_values values = sqlite3_values.build(argc, argv);
		//final String idx = getString(idx_str);
		return filter(cursor, idx_num, idx_str, values);
	}
	// TODO https://sqlite.org/c3ref/vtab_in_first.html
	int filter(MemorySegment cursor, int idxNum, MemorySegment idxStr, sqlite3_values values);

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
	// TODO https://sqlite.org/c3ref/vtab_nochange.html
	default int column(MemorySegment cursor, MemorySegment ctx, int i) {
		sqlite3_context sqlite3_context = new sqlite3_context(ctx);
		return column(cursor, sqlite3_context, i);
	}
	int column(MemorySegment cursor, sqlite3_context sqlite3Context, int i);

	/**
	 * @param cursor sqlite3_vtab_cursor*
	 * @param p_rowid sqlite3_int64*
	 */
	int rowId(MemorySegment cursor, MemorySegment p_rowid);
}
