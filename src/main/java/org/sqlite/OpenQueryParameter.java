/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Open URI parameters.
 * @see Conn#open(String, int, String)
 * @see <a href="http://sqlite.org/c3ref/open.html#urifilenamesinsqlite3open">URI Filenames</a>
 * @see <a href="http://sqlite.org/uri.html#coreqp">Query Parameters</a>
 */
public enum OpenQueryParameter {
	/*VFS("vfs") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
		}
	},
	MODE("mode") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
		}
	},
	CACHE("cache") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
		}
	},
	PSOW("psow") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
		}
	},
	NOLOCK("nolock") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
		}
	},
	IMMUTABLE("immutable") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
		}
	}*/
	/** @see <a href="https://www.sqlite.org/c3ref/enable_load_extension.html">enable load extension</a> */
	ENABLE_LOAD_EXTENSION("enable_load_extension") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			// Extension loading is off by default.
			final boolean enable = uri_boolean(params, this.name, false);
			if (enable) {
				conn.enableLoadExtension(enable);
			}
		}
	},
	ENABLE_TRIGGERS("enable_triggers") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			final boolean current = conn.areTriggersEnabled();
			final boolean enable = uri_boolean(params, this.name, current);
			if (enable != current) {
				if (enable != conn.enableTriggers(enable)) { // SQLITE_OMIT_TRIGGER
					throw new ConnException(conn, "Cannot enable or disable triggers", ErrCodes.WRAPPER_SPECIFIC);
				}
			}
		}
	},
	/** @see <a href="http://sqlite.org/pragma.html#pragma_encoding">pragma encoding</a> */
	ENCODING("encoding") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			final String encoding = params.get(this.name);
			if (encoding == null) {
				return;
			}
			final String current = conn.encoding(null);
			if (!current.equals(encoding)) { // Once an encoding has been set for a database, it cannot be changed.
				throw new ConnException(conn, String.format("'%s' <> '%s'", current, encoding), ErrCodes.WRAPPER_SPECIFIC); // TODO PRAGMA encoding = "..."
			}
		}
	},
	/** @see <a href="https://www.sqlite.org/c3ref/extended_result_codes.html">extended result codes</a> */
	EXTENDED_RESULT_CODES("extended_result_codes") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			// The extended result codes are disabled by default for historical compatibility.
			final boolean enable = uri_boolean(params, this.name, false);
			if (enable) {
				conn.setExtendedResultCodes(enable);
			}
		}
	},
	FOREIGN_KEYS("foreign_keys") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			final boolean current = conn.areForeignKeysEnabled();
			final boolean enable = uri_boolean(params, this.name, current);
			if (enable != current) {
				if (enable != conn.enableForeignKeys(enable)) {
					throw new ConnException(conn, "Cannot enable or disable the enforcement of foreign key constraints", ErrCodes.WRAPPER_SPECIFIC);
				}
			}
		}
	},
	/** @see <a href="https://www.sqlite.org/pragma.html#pragma_journal_mode">pragma journal mode</a> */
	JOURNAL_MODE("journal_mode") {
		private final String[] MODES = {"DELETE", "MEMORY", "OFF", "PERSIST", "TRUNCATE", "WAL"};
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			String mode = params.get(this.name);
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
	/** @see <a href="https://www.sqlite.org/pragma.html#pragma_locking_mode">pragma locking mode</a> */
	LOCKING_MODE("locking_mode") {
		private final String[] MODES = {"EXCLUSIVE", "NORMAL"};
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			String mode = params.get(this.name);
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
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			final String value = params.get(this.name);
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
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			final boolean current = conn.isQueryOnly(null);
			final boolean enable = uri_boolean(params, this.name, current);
			if (enable != current) {
				conn.setQueryOnly(null, enable);
			}
		}
	},
	/** @see <a href="https://www.sqlite.org/pragma.html#pragma_recursive_triggers">pragma recursive triggers</a> */
	RECURSIVE_TRIGGERS("recursive_triggers") {
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			// Support for recursive triggers was added in version 3.6.18 but was initially turned OFF by default, for compatibility.
			final boolean enable = uri_boolean(params, this.name, false);
			if (enable) {
				conn.pragma(null, this.name, enable);
				if (enable != conn.pragma(null, this.name)) {
					throw new ConnException(conn, "Cannot enable or disable recursive triggers", ErrCodes.WRAPPER_SPECIFIC);
				}
			}
		}
	},
	/** @see <a href="https://www.sqlite.org/pragma.html#pragma_synchronous">pragma synchronous</a> */
	SYNCHRONOUS("synchronous") {
		private final String[] FLAGS = {"0", "1", "2", "FULL", "NORMAL", "OFF"};
		@Override
		public void config(Map<String, String> params, Conn conn) throws SQLiteException {
			String mode = params.get(this.name);
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

	public abstract void config(Map<String, String> params, Conn conn) throws SQLiteException;

	private static final String[] TRUES = {"on", "true", "yes"};
	private static final String[] FALSES = {"false", "no", "off"};
	private static boolean uri_boolean(Map<String, String> params, String param, boolean defaultB) {
		String value = params.get(param);
		if (value == null || value.isEmpty()) {
			return defaultB;
		}
		value = value.toLowerCase();
		if (Arrays.binarySearch(TRUES, value) >= 0) {
			return true;
		} else if (Arrays.binarySearch(FALSES, value) >= 0) {
			return false;
		}
		final char c = value.charAt(0);
		if (Character.isDigit(c)) {
			return c != '0';
		}
		return defaultB;
	}

	public static Map<String, String> getQueryParams(String url) {
		final String[] urlParts = url.split("\\?");
		if (urlParts.length < 2) {
			return Collections.emptyMap();
		}
		try {
			final Map<String, String> params = new HashMap<>();

			final String query = urlParts[1];
			for (String param : query.split("&")) {
				String[] pair = param.split("=");
				String key = URLDecoder.decode(pair[0], SQLite.UTF_8_ECONDING);
				String value = "";
				if (pair.length > 1) {
					value = URLDecoder.decode(pair[1], SQLite.UTF_8_ECONDING);
				}

				// skip ?& and &&
				if ("".equals(key) && pair.length == 1) {
					continue;
				}

				params.put(key, value);
			}

			return params;
		} catch (UnsupportedEncodingException e) {
			return Collections.emptyMap();
		}
	}
}
