/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.ConnException;
import org.sqlite.ErrCodes;
import org.sqlite.OpenFlags;
import org.sqlite.SQLite;
import org.sqlite.parser.ast.LiteralExpr;
import org.sqlite.parser.ast.Pragma;
import org.sqlite.parser.ast.QualifiedName;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.util.Properties;
import java.util.logging.Logger;

import static org.sqlite.OpenQueryParameter.*;

/**
 * JDBC 4.x driver implementation for SQLite 3.
 * @see java.sql.Driver
 */
public class JDBC implements Driver {
	public static final String PREFIX;

	static {
		PREFIX = "jdbc:sqlite:";
		try {
			register();
		} catch (SQLException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static final String MEMORY = PREFIX + org.sqlite.Conn.MEMORY;
	public static final String TEMP_FILE = PREFIX + org.sqlite.Conn.TEMP_FILE;
	public static final String VFS = "vfs";
	public static final String MODE = "mode";
	public static final String CACHE = "cache";

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		if (!acceptsURL(url)) return null;
		final String vfs = info == null ? null : info.getProperty(VFS);
		final int flags = getOpenFlags(info == null ? null : info.getProperty(MODE),
				info == null ? null : info.getProperty(CACHE));
		final org.sqlite.Conn conn = org.sqlite.Conn.open(url.substring(PREFIX.length()), flags, vfs);
		final SQLWarning warnings;
		try {
			conn.setBusyTimeout(3000);
			warnings = setup(conn, info);
			// check database format (the pragma fails if the file header is not valid):
			conn.fastExec("PRAGMA schema_version");
		} catch (Throwable t) {
			conn.closeNoCheck();
			throw t;
		}
		return new Conn(conn, DateUtil.config(info), warnings);
	}

	private static int getOpenFlags(String mode, String cache) {
		int flags = OpenFlags.SQLITE_OPEN_FULLMUTEX | OpenFlags.SQLITE_OPEN_URI;
		if (mode == null || "rwc".equals(mode)) {
			flags |= OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_CREATE;
		} else if ("rw".equals(mode)) {
			flags |= OpenFlags.SQLITE_OPEN_READWRITE;
		} else if ("ro".equals(mode)) {
			flags |= OpenFlags.SQLITE_OPEN_READONLY;
		}
		if ("private".equals(cache)) {
			flags |= OpenFlags.SQLITE_OPEN_PRIVATECACHE;
		} else if ("shared".equals(cache)) {
			flags |= OpenFlags.SQLITE_OPEN_SHAREDCACHE;
		}
		return flags;
	}

	@Override
	public boolean acceptsURL(String url) {
		return url.startsWith(PREFIX);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
		// TODO parse url
		final DriverPropertyInfo vfs = new DriverPropertyInfo(VFS, info == null ? null : info.getProperty(VFS));
		vfs.description = "Specify the name of a VFS object that provides the operating system interface that should be used to access the database file on disk.";

		final DriverPropertyInfo mode = new DriverPropertyInfo(MODE, info == null ? null : info.getProperty(MODE));
		mode.description = "Open the database for read-only/read-write (but not create) access.";
		mode.choices = new String[]{"ro", "rw", "rwc"};
		if (mode.value == null) mode.value = "rwc"; // default

		final DriverPropertyInfo cache = new DriverPropertyInfo(CACHE, info == null ? null : info.getProperty(CACHE));
		cache.description = "Choose to participate or not participate in shared cache mode.";
		cache.choices = new String[]{"shared", "private"};

		final DriverPropertyInfo fks = new DriverPropertyInfo(FOREIGN_KEYS.name, info == null ? null : info.getProperty(FOREIGN_KEYS.name));
		fks.description = "Enable or disable the enforcement of foreign key constraints.";
		fks.choices = new String[]{"on", "off"};

		final DriverPropertyInfo triggers = new DriverPropertyInfo(ENABLE_TRIGGERS.name, info == null ? null : info.getProperty(ENABLE_TRIGGERS.name));
		triggers.description = "Enable or disable triggers.";
		triggers.choices = new String[]{"on", "off"};

		final DriverPropertyInfo ele = new DriverPropertyInfo(ENABLE_LOAD_EXTENSION.name, info == null ? null : info.getProperty(ENABLE_LOAD_EXTENSION.name));
		ele.description = "Turn extension loading on or off.";
		ele.choices = new String[]{"on", "off"};
		if (ele.value == null) ele.value = "off"; // default

		final DriverPropertyInfo encoding = new DriverPropertyInfo(ENCODING.name, info == null ? null : info.getProperty(ENCODING.name));
		encoding.description = "Set the encoding.";
		encoding.choices = new String[]{"UTF-8", "UTF-16", "UTF-16le", "UTF-16be"};

		final DriverPropertyInfo df = new DriverPropertyInfo(DateUtil.DATE_FORMAT, info == null ? null : info.getProperty(DateUtil.DATE_FORMAT));
		df.description = "Specify the format used to persist date ('" + DateUtil.JULIANDAY + "', '" + DateUtil.UNIXEPOCH + "', 'yyyy-MM-dd', '...').";
		final DriverPropertyInfo tf = new DriverPropertyInfo(DateUtil.TIME_FORMAT, info == null ? null : info.getProperty(DateUtil.TIME_FORMAT));
		tf.description = "Specify the format used to persist time ('" + DateUtil.JULIANDAY + "', '" + DateUtil.UNIXEPOCH + "', 'HH:mm:ss.SSSXXX', '...').";
		final DriverPropertyInfo tsf = new DriverPropertyInfo(DateUtil.TIMESTAMP_FORMAT, info == null ? null : info.getProperty(DateUtil.TIMESTAMP_FORMAT));
		tsf.description = "Specify the format used to persist timestamp ('" + DateUtil.JULIANDAY + "', '" + DateUtil.UNIXEPOCH + "', 'yyyy-MM-dd HH:mm:ss.SSSXXX', '...').";

		return new DriverPropertyInfo[]{vfs, mode, cache, fks, triggers, ele, encoding, df, tf, tsf}; // TODO locking_mode, recursive_triggers, synchronous
	}

	private static SQLWarning setup(org.sqlite.Conn conn, Properties info) throws ConnException {
		if (info == null) {
			return null;
		}
		final String encoding = info.getProperty(ENCODING.name);
		if (encoding != null && !encoding.isEmpty()) {
			Pragma pragma = new Pragma(new QualifiedName(null, "encoding"), LiteralExpr.string(encoding));
			conn.fastExec(pragma.toSql());
		}
		final String fks = info.getProperty(FOREIGN_KEYS.name);
		SQLWarning warnings = null;
		if ("on".equals(fks)) {
			if (!conn.enableForeignKeys(true)) {
				warnings = addWarning(warnings, new SQLWarning("cannot enable the enforcement of foreign key constraints.", null, ErrCodes.WRAPPER_SPECIFIC));
				SQLite.sqlite3_log(-1, "cannot enable the enforcement of foreign key constraints.");
			}
		} else if ("off".equals(fks)) {
			if (conn.enableForeignKeys(false)) {
				warnings = addWarning(warnings, new SQLWarning("cannot disable the enforcement of foreign key constraints.", null, ErrCodes.WRAPPER_SPECIFIC));
				SQLite.sqlite3_log(-1, "cannot disable the enforcement of foreign key constraints.");
			}
		}
		final String triggers = info.getProperty(ENABLE_TRIGGERS.name);
		if ("on".equals(triggers)) {
			if (!conn.enableTriggers(true)) {
				warnings = addWarning(warnings, new SQLWarning("cannot enable triggers.", null, ErrCodes.WRAPPER_SPECIFIC));
				SQLite.sqlite3_log(-1, "cannot enable triggers.");
			}
		} else if ("off".equals(fks)) {
			if (conn.enableTriggers(false)) {
				warnings = addWarning(warnings, new SQLWarning("cannot disable triggers.", null, ErrCodes.WRAPPER_SPECIFIC));
				SQLite.sqlite3_log(-1, "cannot disable triggers.");
			}
		}
		if ("on".equals(info.getProperty(ENABLE_LOAD_EXTENSION.name))) {
			conn.enableLoadExtension(true);
		} // disabled by default
		return warnings;
	}
	private static SQLWarning addWarning(SQLWarning current, SQLWarning next) {
		if (current != null) {
			current.setNextWarning(next);
		} else {
			current = next;
		}
		return current;
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}
#if jdbc.specification.version >= "4.1"
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw Util.unsupported("Driver.getParentLogger");
	}
#endif

	private static Driver registeredDriver;
	public static void register() throws SQLException {
		if (isRegistered()) {
			throw new IllegalStateException("Driver is already registered. It can only be registered once.");
		}
		registeredDriver = new JDBC();
		DriverManager.registerDriver(registeredDriver);
	}
	public static void deregister() throws SQLException {
		if (!isRegistered()) {
			throw new IllegalStateException("Driver is not registered (or it has not been registered using Driver.register() method)");
		}
		DriverManager.deregisterDriver(registeredDriver);
		registeredDriver = null;
	}
	public static boolean isRegistered() {
		return registeredDriver != null;
	}
}
