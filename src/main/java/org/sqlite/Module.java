package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.ByReference;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.sqlite.SQLite.SQLite3;
import org.sqlite.SQLite.SQLite3Context;
import org.sqlite.SQLite.SQLite3Values;

import static com.sun.jna.Pointer.NULL;
import static java.util.Objects.requireNonNull;
import static org.sqlite.ErrCodes.SQLITE_ERROR;
import static org.sqlite.ErrCodes.SQLITE_NOMEM;
import static org.sqlite.SQLite.SQLITE_OK;
import static org.sqlite.SQLite.UTF_8_ECONDING;
import static org.sqlite.SQLite.nativeString;
import static org.sqlite.SQLite.sqlite3_free;

/**
 * @see <a href="https://sqlite.org/c3ref/module.html">sqlite3_module</a>
 */
@FieldOrder({"iVersion", "xCreate", "xConnect", "xBestIndex", "xDisconnect", "xDestroy", "xOpen", "xClose", "xFilter", "xNext", "xEof", "xColumn", "xRowid", "xUpdate", "xBegin", "xSync", "xCommit", "xRollback", "xFindFunction", "xRename"})
public abstract class Module<T extends VTab<T,C>, C extends VTabCursor<T>> extends Structure implements ByReference { // TODO generic over Aux
	public final int iVersion = 1;
	public Connect<T, C> xCreate;
	public Connect<T, C> xConnect;
	@SuppressWarnings("unused")
	public BestIndex<T, C> xBestIndex = new BestIndex<T, C>() {
		@Override
		public int invoke(Pointer pVTab, IndexInfo info) {
			T tab = vtab(pVTab);
			return tab.bestIndex(info);
		}
	};

	public Disconnect<T, C> xDisconnect;
	public Disconnect<T, C> xDestroy;
	@SuppressWarnings("unused")
	public Open<T, C> xOpen = new Open<T, C>() {
		@Override
		public int invoke(Pointer pVTab, PointerByReference ppCursor) {
			T tab = vtab(pVTab);
			try {
				C cursor = tab.open();
				if (cursor == null) {
					tab.setErrMsg(String.format("%s.open returned null", tab));
					return SQLITE_ERROR;
				}
				ppCursor.setValue(cursor.getPointer());
				return SQLITE_OK;
			} catch (SQLiteException e) {
				tab.setErrMsg(e.getErrMsg());
				return e.getErrorCode();
			}
		}
	};
	@SuppressWarnings("unused")
	public Close<C> xClose = new Close<C>() {
		@Override
		public int invoke(Pointer cursor) {
			C c = cursor(cursor);
			try {
				c.close();
				return SQLITE_OK;
			} catch (SQLiteException e) {
				c.pVtab().setErrMsg(e.getErrMsg());
				return e.getErrorCode();
			}
		}
	};
	@SuppressWarnings("unused")
	public Filter<C> xFilter = new Filter<C>() {
		@Override
		public int invoke(Pointer cursor, int idxNum, String idxStr, int argc, Pointer argv) {
			C c = cursor(cursor);
			try {
				c.filter(idxNum, idxStr, SQLite3Values.build(argc, argv));
				return SQLITE_OK;
			} catch (SQLiteException e) {
				c.pVtab().setErrMsg(e.getErrMsg());
				return e.getErrorCode();
			}
		}
	};
	@SuppressWarnings("unused")
	public Next<C> xNext = new Next<C>() {
		@Override
		public int invoke(Pointer cursor) {
			C c = cursor(cursor);
			try {
				c.next();
				return SQLITE_OK;
			} catch (SQLiteException e) {
				c.pVtab().setErrMsg(e.getErrMsg());
				return e.getErrorCode();
			}
		}
	};
	@SuppressWarnings("unused")
	public Eof<C> xEof = new Eof<C>() {
		@Override
		public boolean invoke(Pointer cursor) {
			C c = cursor(cursor);
			return c.eof();
		}
	};
	@SuppressWarnings("unused")
	public Column<C> xColumn = new Column<C>() {
		@Override
		public int invoke(Pointer cursor, SQLite3Context ctx, int i) {
			C c = cursor(cursor);
			try {
				c.column(ctx, i);
				return SQLITE_OK;
			} catch (SQLiteException e) {
				c.pVtab().setErrMsg(e.getErrMsg());
				return e.getErrorCode();
			}
		}
	};
	@SuppressWarnings("unused")
	public Rowid<C> xRowid = new Rowid<C>() {
		@Override
		public int invoke(Pointer cursor, LongByReference pRowid) {
			C c = cursor(cursor);
			try {
				pRowid.setValue(c.rowId());
				return SQLITE_OK;
			} catch (SQLiteException e) {
				c.pVtab().setErrMsg(e.getErrMsg());
				return e.getErrorCode();
			}
		}
	};
	// TODO
	public final Callback xUpdate = null;
	public final Callback xBegin = null;
	public final Callback xSync = null;
	public final Callback xCommit = null;
	public final Callback xRollback = null;
	public final Callback xFindFunction = null;
	public final Callback xRename = null;
	/*
	int (*xCreate)(sqlite3*, void *pAux, int argc, const char *const*argv, sqlite3_vtab **ppVTab, char**);
	int (*xConnect)(sqlite3*, void *pAux, int argc, const char *const*argv, sqlite3_vtab **ppVTab, char**);
	int (*xBestIndex)(sqlite3_vtab *pVTab, sqlite3_index_info*);
	int (*xDisconnect)(sqlite3_vtab *pVTab);
	int (*xDestroy)(sqlite3_vtab *pVTab);
	int (*xOpen)(sqlite3_vtab *pVTab, sqlite3_vtab_cursor **ppCursor);
	int (*xClose)(sqlite3_vtab_cursor*);
	int (*xFilter)(sqlite3_vtab_cursor*, int idxNum, const char *idxStr, int argc, sqlite3_value **argv);
	int (*xNext)(sqlite3_vtab_cursor*);
	int (*xEof)(sqlite3_vtab_cursor*);
	int (*xColumn)(sqlite3_vtab_cursor*, sqlite3_context*, int);
	int (*xRowid)(sqlite3_vtab_cursor*, sqlite3_int64 *pRowid);
	int (*xUpdate)(sqlite3_vtab *, int, sqlite3_value **, sqlite3_int64 *);
	int (*xBegin)(sqlite3_vtab *pVTab);
	int (*xSync)(sqlite3_vtab *pVTab);
	int (*xCommit)(sqlite3_vtab *pVTab);
	int (*xRollback)(sqlite3_vtab *pVTab);
	int (*xFindFunction)(sqlite3_vtab *pVtab, int nArg, const char *zName, void (**pxFunc)(sqlite3_context*,int,sqlite3_value**), void **ppArg);
	int (*xRename)(sqlite3_vtab *pVtab, const char *zNew);
	// The methods above are in version 1 of the sqlite_module object. Those below are for version 2 and greater.
    int (*xSavepoint)(sqlite3_vtab *pVTab, int);
	int (*xRelease)(sqlite3_vtab *pVTab, int);
	int (*xRollbackTo)(sqlite3_vtab *pVTab, int);
	// The methods above are in versions 1 and 2 of the sqlite_module object. Those below are for version 3 and greater.
    int (*xShadowName)(const char*);
	 */

	public Module(
			CreateOrConnect<T, C> create,
			CreateOrConnect<T, C> connect
	) {
		xCreate = xConnect(create);
		xConnect = xConnect(connect);
		xDisconnect = xDisconnect();
		xDestroy = xDestroy();
	}

	public Module(CreateOrConnect<T, C> connect, boolean eponymousOnly) {
		xConnect = xConnect(connect);
		xDisconnect = xDisconnect();
		if (eponymousOnly) {
			xCreate = null;
			xDestroy = null;
		} else {
			xCreate = xConnect;
			xDestroy = xDisconnect;
		}
	}

	private Disconnect<T, C> xDisconnect() {
		return new Disconnect<T, C>() {
			@Override
			public int invoke(Pointer pVTab) {
				T tab = vtab(pVTab);
				try {
					tab.disconnect();
					return SQLITE_OK;
				} catch (SQLiteException e) {
					tab.setErrMsg(e.getErrMsg());
					return e.getErrorCode();
				}
			}
		};
	}
	private Disconnect<T, C> xDestroy() {
		return new Disconnect<T, C>() {
			@Override
			public int invoke(Pointer pVTab) {
				T tab = vtab(pVTab);
				try {
					tab.destroy();
					return SQLITE_OK;
				} catch (SQLiteException e) {
					tab.setErrMsg(e.getErrMsg());
					return e.getErrorCode();
				}
			}
		};
	}

	protected abstract T vtab(Pointer pVTab);
	protected abstract C cursor(Pointer cursor);

	// size is correct only if ByValue
	public static <S extends Structure & Structure.ByValue> Pointer alloc(Class<S> cls) throws SQLiteException {
		int size = Native.getNativeSize(cls);
		Pointer init = requireNonNull(SQLite.sqlite3_malloc(size));
		if (init == NULL) {
			throw new SQLiteException("Cannot allocate " + size + " bytes", SQLITE_NOMEM);
		}
		return init;
	}

	@FunctionalInterface
	public interface CreateOrConnect<T extends VTab<T, C>, C extends VTabCursor<T>> {
		T invoke(SQLite3 db, Pointer aux, String[] args) throws SQLiteException;
	}

	@FunctionalInterface
	public interface DestroyOrDisconnect<T extends VTab<T, C>, C extends VTabCursor<T>> {
		void invoke(T pVTab) throws SQLiteException;
	}

	private static <T extends VTab<T, C>, C extends VTabCursor<T>> Connect<T, C> xConnect(CreateOrConnect<T, C> connect) {
		return new Connect<T, C>() {
			@Override
			public int invoke(SQLite3 db, Pointer aux, int argc, Pointer argv, PointerByReference ppVTab, PointerByReference err_msg) {
				String[] args;
				if (argc == 0 || argv == NULL) {
					args = new String[0];
				} else {
					args = argv.getStringArray(0, argc, UTF_8_ECONDING);
				}
				try {
					T vTab = connect.invoke(db, aux, args);
					if (vTab != null) {
						ppVTab.setValue(vTab.getPointer());
					}
					return SQLITE_OK;
				} catch (SQLiteException e) {
					setErrMsg(err_msg, e.getErrMsg());
					return e.getErrorCode();
				}
			}
		};
	}

	private static void setErrMsg(PointerByReference err_msg, String msg) {
		if (err_msg.getValue() != NULL) {
			sqlite3_free(err_msg.getValue());
		}
		err_msg.setValue(nativeString(msg, SQLite::sqlite3_malloc));
	}

	public interface Connect<T extends VTab<T, C>, C extends VTabCursor<T>> extends Callback {
		int invoke(SQLite3 db, Pointer aux, int argc, Pointer argv, PointerByReference ppVTab, PointerByReference err_msg);
	}

	public interface Disconnect<T extends VTab<T, C>, C extends VTabCursor<T>> extends Callback {
		int invoke(Pointer pVTab);
	}

	public interface BestIndex<T extends VTab<T, C>, C extends VTabCursor<T>> extends Callback {
		int invoke(Pointer pVTab, IndexInfo info);
	}

	public interface Open<T extends VTab<T, C>, C extends VTabCursor<T>> extends Callback {
		int invoke(Pointer pVTab, PointerByReference ppCursor);
	}

	public interface Close<C extends VTabCursor<?>> extends Callback {
		int invoke(Pointer cursor);
	}

	public interface Filter<C extends VTabCursor<?>> extends Callback {
		int invoke(Pointer cursor, int idxNum, String idxStr, int argc, Pointer argv);
	}

	public interface Next<C extends VTabCursor<?>> extends Callback {
		int invoke(Pointer cursor);
	}

	public interface Eof<C extends VTabCursor<?>> extends Callback {
		boolean invoke(Pointer cursor);
	}

	public interface Column<C extends VTabCursor<?>> extends Callback {
		int invoke(Pointer cursor, SQLite3Context ctx, int i);
	}

	public interface Rowid<C extends VTabCursor<?>> extends Callback {
		int invoke(Pointer cursor, LongByReference pRowid);
	}
}
