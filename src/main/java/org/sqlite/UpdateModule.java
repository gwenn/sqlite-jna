package org.sqlite;

import java.lang.foreign.MemorySegment;
import java.util.Map.Entry;

import static org.sqlite.ColTypes.SQLITE_NULL;
import static org.sqlite.SQLite.C_LONG_LONG;
import static org.sqlite.SQLite.SQLITE_OK;

public interface UpdateModule extends Module {
	/**
	 * @param vtab sqlite3_vtab *
	 * @param argc int
	 * @param argv sqlite3_value **argv
	 * @param p_rowid sqlite_int64 *
	 */
	default int update(MemorySegment vtab, int argc, MemorySegment argv, MemorySegment p_rowid) {
		vtab = vtab.reinterpret(vtab_layout().byteSize());
		sqlite3_values values = sqlite3_values.build(argc, argv);
		assert argc >= 1;
		if (argc == 1) {
			return delete(vtab, values);
		} else if (values.getType(0) == SQLITE_NULL) {
			Entry<Integer, Long> entry = insert(vtab, values);
			int rc = entry.getKey();
			if (rc == SQLITE_OK) {
				long rowId = entry.getValue();
				p_rowid = p_rowid.reinterpret(C_LONG_LONG.byteSize());
				p_rowid.set(C_LONG_LONG, 0, rowId);
			}
			return rc;
		} else {
			return update(vtab, values);
		}
	}
	int delete(MemorySegment vtab, sqlite3_values values);
	Entry<Integer, Long> insert(MemorySegment vtab, sqlite3_values values);
	int update(MemorySegment vtab, sqlite3_values values);
}
