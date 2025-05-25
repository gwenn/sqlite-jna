package org.sqlite;

import java.lang.foreign.MemorySegment;

/**
 * Read-only module
 */
public interface Module extends EponymousModule {
	/**
	 * @param db      sqlite3*
	 * @param aux     void*
	 * @param argc    int
	 * @param argv    const char**
	 * @param pp_vtab sqlite3_vtab**
	 * @param err_msg char**
	 */
	default int create(MemorySegment db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment pp_vtab, MemorySegment err_msg) {
		return create_connect(db, aux, argc, argv, pp_vtab, err_msg, true);
	}
	/**
	 * @param vtab sqlite3_vtab*
	 */
	default int destroy(MemorySegment vtab) {
		vtab = vtab.reinterpret(vtab_layout().byteSize());
		return disconnect(vtab, true);
	}
}
