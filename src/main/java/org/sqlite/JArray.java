package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import static com.sun.jna.Pointer.NULL;
import static org.sqlite.SQLite.SQLITE_OK;

/**
 * Java port of <a href="https://www.sqlite.org/cgi/src/file?name=ext/misc/carray.c&ci=tip">carray()</a> table-valued-function.
 * See https://github.com/java-native-access/jna/blob/master/www/PointersAndArrays.md
 */
public class JArray {
	private static final Module<JArrayTab, JArrayTabCursor> MODULE = new Module<JArrayTab, JArrayTabCursor>(JArray::connect, true) {
		@Override
		protected JArrayTab vtab(Pointer pVTab) {
			final JArrayTab arrayTab = new JArrayTab(pVTab);
			arrayTab.read();
			return arrayTab;
		}
		@Override
		protected JArrayTabCursor cursor(Pointer cursor) {
			final JArrayTabCursor c = new JArrayTabCursor(cursor);
			c.read();
			return c;
		}
	};
	// Column numbers
	//private static final int CARRAY_COLUMN_VALUE = 0;
	private static final int CARRAY_COLUMN_POINTER = 1;
	private static final int CARRAY_COLUMN_COUNT = 2;
	private static final int CARRAY_COLUMN_CTYPE = 3;

	public static void loadModule(Conn conn) throws ConnException {
		conn.createModule("jarray", MODULE, NULL, null);
	}

	private static JArrayTab connect(SQLite.SQLite3 db, Pointer aux, String[] args) throws SQLiteException {
		final int rc = SQLite.sqlite3_declare_vtab(db, "CREATE TABLE x(value,pointer hidden,count hidden,ctype hidden)");
		if (rc != SQLITE_OK) {
			throw new SQLiteException("JArrayTab.connect", rc);
		}
		Pointer init = Module.alloc(JArrayTab.ByValue.class);
		//sqlite3_vtab_config(db, SQLITE_VTAB_INNOCUOUS);
		return new JArrayTab(init);
	}

	public static class JArrayTab extends VTab<JArrayTab, JArrayTabCursor> {
		// Used only to compute its size !!!
		public static class ByValue extends JArrayTab implements com.sun.jna.Structure.ByValue {
			public ByValue(Pointer pVTab) {
				super(pVTab);
			}
		}
		public JArrayTab(Pointer pVTab) {
			super(pVTab);
		}

		@Override
		protected int bestIndex(IndexInfo info) {
			return 0; // FIXME
		}

		@Override
		protected JArrayTabCursor open() throws SQLiteException {
			Pointer init = Module.alloc(JArrayTabCursor.ByValue.class);
			return new JArrayTabCursor(init);
		}
	}

	@Structure.FieldOrder({"pVtab"})
	public static class JArrayTabCursor extends VTabCursor<JArrayTab> {
		/**
		 * Virtual table of this cursor
		 */
		public JArrayTab pVtab; // sqlite3_vtab *
		/* The rowid */
		public long iRowid;

		// Used only to compute its size !!!
		public static class ByValue extends JArrayTabCursor implements com.sun.jna.Structure.ByValue {
			public ByValue(Pointer p) {
				super(p);
			}
		}
		public JArrayTabCursor(Pointer p) {
			super(p);
		}

		@Override
		public JArrayTab pVtab() {
			return pVtab;
		}

		@Override
		protected void filter(int idxNum, String idxStr, SQLite.SQLite3Values args) throws SQLiteException {
			// FIXME
		}

		@Override
		protected void next() throws SQLiteException {
			iRowid++;
			write();
		}

		@Override
		protected boolean eof() {
			return false; // FIXME
		}

		@Override
		protected void column(SQLite.SQLite3Context ctx, int i) throws SQLiteException {
			// FIXME
		}

		@Override
		protected long rowId() throws SQLiteException {
			return iRowid;
		}
	}
}
