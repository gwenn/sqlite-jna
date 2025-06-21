package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

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
	default int create(@NonNull MemorySegment db, @NonNull MemorySegment aux, int argc, @NonNull MemorySegment argv, @NonNull MemorySegment pp_vtab, @NonNull MemorySegment err_msg) {
		return create_connect(db, aux, argc, argv, pp_vtab, err_msg, true);
	}
	/**
	 * @param vtab sqlite3_vtab*
	 */
	default int destroy(@NonNull MemorySegment vtab) {
		vtab = vtab.reinterpret(vtab_layout().byteSize());
		return disconnect(vtab, true);
	}
}
