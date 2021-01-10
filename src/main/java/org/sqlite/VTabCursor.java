package org.sqlite;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import org.sqlite.SQLite.SQLite3Context;
import org.sqlite.SQLite.SQLite3Values;

/**
 * Implementations must
 * <ul>
 *   <li>declared {@code pVtab} as the first field,</li>
 *   <li>have memory allocated manually because the lifetime should not be handled by the JVM GC.</li>
 * </ul>
 * @see <a href="https://sqlite.org/c3ref/vtab_cursor.html">sqlite3_vtab_cursor</a>
 */
public abstract class VTabCursor extends Structure {
	/**
	 * Virtual table of this cursor
	 */
	public abstract VTab pVtab(); // sqlite3_vtab *
	/* Virtual table implementations will typically add additional fields */

	protected VTabCursor(Pointer p) {
		super(p);
	}
	protected abstract void filter(int idxNum, String idxStr, SQLite3Values args) throws SQLiteException;
	protected abstract void next() throws SQLiteException;
	protected abstract boolean eof();
	/**
	 * @param i is zero-based so the first column is numbered 0
	 */
	protected abstract void column(SQLite3Context ctx, int i) throws SQLiteException;
	protected abstract long rowId() throws SQLiteException;
	/**
	 * Overriding method must call the super implementation of the method.
	 */
	protected void close() throws SQLiteException {
		SQLite.sqlite3_free(getPointer());
	}
}
