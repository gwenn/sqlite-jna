package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.sqlite.IndexInfo.IndexConstraint;
import org.sqlite.IndexInfo.IndexConstraintUsage;
import org.sqlite.IndexInfo.IndexOrderBy;
import org.sqlite.SQLite.SQLite3;

import static com.sun.jna.Pointer.NULL;
import static org.sqlite.SQLite.SQLITE_OK;

public class Series {
	private static final Module<SeriesTab, SeriesTabCursor> MODULE = new Module<SeriesTab, SeriesTabCursor>(Series::connect, true) {
		@Override
		protected SeriesTab vtab(Pointer pVTab) {
			return new SeriesTab(pVTab);
		}
		@Override
		protected SeriesTabCursor cursor(Pointer cursor) {
			return new SeriesTabCursor(cursor);
		}
	};
	// Column numbers
	//private static final int SERIES_COLUMN_VALUE = 0;
	private static final int SERIES_COLUMN_START = 1;
	private static final int SERIES_COLUMN_STOP = 2;
	private static final int SERIES_COLUMN_STEP = 3;
	// start = $value  -- constraint exists
	private static final int START = 1;
	// stop = $value   -- constraint exists
	private static final int STOP = 2;
	// step = $value   -- constraint exists
	private static final int STEP = 4;
	// output in descending order
	private static final int DESC = 8;
	// Both start and stop
	private static final int BOTH = START | STOP;

	public static void loadModule(Conn conn) throws ConnException {
		conn.createModule("generate_series", MODULE, NULL, null);
	}

	private static SeriesTab connect(SQLite3 db, Pointer aux, String[] args) throws SQLiteException {
		final int rc = SQLite.sqlite3_declare_vtab(db, "CREATE TABLE x(value,start hidden,stop hidden,step hidden)");
		if (rc != SQLITE_OK) {
			throw new SQLiteException("SeriesTab.connect", rc);
		}
		Pointer init = Module.alloc(SeriesTab.ByValue.class);
		return new SeriesTab(init);
	}

	public static class SeriesTab extends VTab<SeriesTab, SeriesTabCursor> {
		// Used only to compute its size !!!
		public static class ByValue extends SeriesTab implements com.sun.jna.Structure.ByValue {
			public ByValue(Pointer pVTab) {
				super(pVTab);
			}
		}
		public SeriesTab(Pointer pVTab) {
			super(pVTab);
		}

		@Override
		protected void bestIndex(IndexInfo info) {
			// The query plan bitmask
			int idxNum = 0;
			// Index of the start= constraint
			int start_idx = 0;
			// Index of the stop= constraint
			int stop_idx = 0;
			// Index of the step= constraint
			int step_idx = 0;
			IndexConstraint[] constraints = info.constraints();
			for (int i = 0; i < constraints.length; i++) {
				IndexConstraint constraint = constraints[i];
				if (!constraint.isUsable()) {
					continue;
				}
				if (constraint.op != IndexConstraintOp.SQLITE_INDEX_CONSTRAINT_EQ) {
					continue;
				}
				switch (constraint.iColumn) {
					case SERIES_COLUMN_START:
						idxNum |= START;
						break;
					case SERIES_COLUMN_STOP:
						idxNum |= STOP;
						break;
					case SERIES_COLUMN_STEP:
						idxNum |= STEP;
						break;
				}
			}
			IndexConstraintUsage[] constraint_usages = info.constraintUsages();
			int num_of_arg = 0;
			if (start_idx > 0) {
				num_of_arg += 1;
				IndexConstraintUsage constraint_usage = constraint_usages[start_idx];
				constraint_usage.argvIndex = num_of_arg;
				constraint_usage.omit(true);
			}
			if (stop_idx > 0) {
				num_of_arg += 1;
				IndexConstraintUsage constraint_usage = constraint_usages[stop_idx];
				constraint_usage.argvIndex = num_of_arg;
				constraint_usage.omit(true);
			}
			if (step_idx > 0) {
				num_of_arg += 1;
				IndexConstraintUsage constraint_usage = constraint_usages[step_idx];
				constraint_usage.argvIndex = num_of_arg;
				constraint_usage.omit(true);
			}
			if ((idxNum & BOTH) != 0) {
				// Both start= and stop= boundaries are available.
				info.estimatedCost = 2 - (idxNum & STEP) != 0 ? 1 : 0;
				info.estimatedRows = 1000;
				boolean order_by_consumed;
				{
					IndexOrderBy[] orderBys = info.orderBys();
					if (orderBys.length > 0) {
						IndexOrderBy orderBy = orderBys[0];
						if (orderBy.is_desc()) {
							idxNum |= DESC;
						}
						order_by_consumed = true;
					} else {
						order_by_consumed = false;
					}
				}
				if (order_by_consumed) {
					info.orderByConsumed = 1;
				}
			} else {
				info.estimatedCost = 2_147_483_647d;
				info.estimatedRows = 2_147_483_647;
			}
			info.idxNum = idxNum;
		}

		@Override
		protected SeriesTabCursor open() {
			Pointer init = Module.alloc(SeriesTabCursor.ByValue.class);
			return new SeriesTabCursor(init);
		}
	}

	@Structure.FieldOrder({"pVtab"})
	public static class SeriesTabCursor extends VTabCursor<SeriesTab> {
		/**
		 * Virtual table of this cursor
		 */
		public SeriesTab pVtab; // sqlite3_vtab *
		// Used only to compute its size !!!
		public static class ByValue extends SeriesTabCursor implements com.sun.jna.Structure.ByValue {
			public ByValue(Pointer p) {
				super(p);
			}
		}
		public SeriesTabCursor(Pointer p) {
			super(p);
		}

		@Override
		public SeriesTab pVtab() {
			return pVtab;
		}

		@Override
		protected void filter(int idxNum, String idxStr, SQLite.SQLite3Values args) {
			// TODO
		}

		@Override
		protected void next() {
			// TODO
		}

		@Override
		protected boolean eof() {
			// TODO
			return true;
		}

		@Override
		protected void column(SQLite.SQLite3Context ctx, int i) {
			// TODO
		}

		@Override
		protected long rowId() {
			// TODO
			return 0;
		}
	}
}
