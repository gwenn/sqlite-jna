/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.util.Arrays;

/**
 * http://sqlite.org/c3ref/open.html#urifilenamesinsqlite3open
 * http://sqlite.org/uri.html#coreqp
 */
public enum OpenQueryParameter {
	/*VFS("vfs") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
		}
	},
	MODE("mode") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
		}
	},
	CACHE("cache") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
		}
	},
	PSOW("psow") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
		}
	},
	NOLOCK("nolock") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
		}
	},
	IMMUTABLE("immutable") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
		}
	}*/
	// https://www.sqlite.org/c3ref/enable_load_extension.html
	ENABLE_LOAD_EXTENSION("enable_load_extension") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			// Extension loading is off by default.
			final boolean enable = SQLite.sqlite3_uri_boolean(filename, this.name, false);
			if (enable) {
				conn.enableLoadExtension(enable);
			}
		}
	},
	ENABLE_TRIGGERS("enable_triggers") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			final boolean current = conn.areTriggersEnabled();
			final boolean enable = SQLite.sqlite3_uri_boolean(filename, this.name, current);
			if (enable != current) {
				if (enable != conn.enableTriggers(enable)) { // SQLITE_OMIT_TRIGGER
					throw new ConnException(conn, "Cannot enable or disable triggers", ErrCodes.WRAPPER_SPECIFIC);
				}
			}
		}
	},
	// http://sqlite.org/pragma.html#pragma_encoding
	ENCODING("encoding") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			final String encoding = SQLite.sqlite3_uri_parameter(filename, this.name);
			if (encoding == null) {
				return;
			}
			final String current = conn.encoding(null);
			if (!current.equals(encoding)) { // Once an encoding has been set for a database, it cannot be changed.
				throw new ConnException(conn, String.format("'%s' <> '%s'", current, encoding), ErrCodes.WRAPPER_SPECIFIC); // TODO PRAGMA encoding = "..."
			}
		}
	},
	// https://www.sqlite.org/c3ref/extended_result_codes.html
	EXTENDED_RESULT_CODES("extended_result_codes") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			// The extended result codes are disabled by default for historical compatibility.
			final boolean enable = SQLite.sqlite3_uri_boolean(filename, this.name, false);
			if (enable) {
				conn.setExtendedResultCodes(enable);
			}
		}
	},
	FOREIGN_KEYS("foreign_keys") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			final boolean current = conn.areForeignKeysEnabled();
			final boolean enable = SQLite.sqlite3_uri_boolean(filename, this.name, current);
			if (enable != current) {
				if (enable != conn.enableForeignKeys(enable)) {
					throw new ConnException(conn, "Cannot enable or disable the enforcement of foreign key constraints", ErrCodes.WRAPPER_SPECIFIC);
				}
			}
		}
	},
	// https://www.sqlite.org/pragma.html#pragma_journal_mode
	JOURNAL_MODE("journal_mode") {
		private final String[] MODES = new String[]{"DELETE", "MEMORY", "OFF", "PERSIST", "TRUNCATE", "WAL"};
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			String mode = SQLite.sqlite3_uri_parameter(filename, this.name);
			if (mode == null) {
				return;
			}
			if (Arrays.binarySearch(MODES, mode.toUpperCase()) < 0) {
				throw new ConnException(conn, String.format("Invalid journal_mode: '%s'", mode), ErrCodes.WRAPPER_SPECIFIC);
			}
			conn.fastExec("PRAGMA " + this.name + '=' + mode);
			// TODO check that change is effective
		}
	},
	// https://www.sqlite.org/pragma.html#pragma_locking_mode
	LOCKING_MODE("locking_mode") {
		private final String[] MODES = new String[]{"EXCLUSIVE", "NORMAL"};
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			String mode = SQLite.sqlite3_uri_parameter(filename, this.name);
			if (mode == null) {
				return;
			}
			if (Arrays.binarySearch(MODES, mode.toUpperCase()) < 0) {
				throw new ConnException(conn, String.format("Invalid locking_mode: '%s'", mode), ErrCodes.WRAPPER_SPECIFIC);
			}
			conn.fastExec("PRAGMA " + this.name + '=' + mode);
			// TODO check that change is effective
		}
	},
	//
	MMAP_SIZE("mmap_size") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			final String value = SQLite.sqlite3_uri_parameter(filename, this.name);
			if (value == null) {
				return;
			}
			final long size;
			try {
				size = Long.parseLong(value);
			} catch (NumberFormatException e) {
				throw new ConnException(conn, String.format("Invalid mmap_size: '%s'", value), ErrCodes.WRAPPER_SPECIFIC);
			}
			conn.fastExec("PRAGMA " + this.name + '=' + size);
			// TODO check that change is effective
		}
	},
	QUERY_ONLY("query_only") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			final boolean current = conn.isQueryOnly(null);
			final boolean enable = SQLite.sqlite3_uri_boolean(filename, this.name, current);
			if (enable != current) {
				conn.setQueryOnly(null, enable);
			}
		}
	},
	// https://www.sqlite.org/pragma.html#pragma_recursive_triggers
	RECURSIVE_TRIGGERS("recursive_triggers") {
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			// Support for recursive triggers was added in version 3.6.18 but was initially turned OFF by default, for compatibility.
			final boolean enable = SQLite.sqlite3_uri_boolean(filename, this.name, false);
			if (enable) {
				conn.pragma(null, this.name, enable);
				if (enable != conn.pragma(null, this.name)) {
					throw new ConnException(conn, "Cannot enable or disable recursive triggers", ErrCodes.WRAPPER_SPECIFIC);
				}
			}
		}
	},
	// https://www.sqlite.org/pragma.html#pragma_synchronous
	SYNCHRONOUS("synchronous") {
		private final String[] FLAGS = new String[]{"0", "1", "2", "FULL", "NORMAL", "OFF"};
		@Override
		public void config(String filename, Conn conn) throws SQLiteException {
			String mode = SQLite.sqlite3_uri_parameter(filename, this.name);
			if (mode == null) {
				return;
			}
			if (Arrays.binarySearch(FLAGS, mode.toUpperCase()) < 0) {
				throw new ConnException(conn, String.format("Invalid synchronous flag: '%s'", mode), ErrCodes.WRAPPER_SPECIFIC);
			}
			conn.fastExec("PRAGMA " + this.name + '=' + mode);
			// TODO check that change is effective
		}
	};

	public final String name;
	OpenQueryParameter(String name) {
		this.name = name;
	}

	public abstract void config(String filename, Conn conn) throws SQLiteException;
}
