package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.sqlite.IndexInfo.IndexConstraint;
import org.sqlite.IndexInfo.IndexConstraintUsage;
import org.sqlite.IndexInfo.IndexOrderBy;
import org.sqlite.SQLite.SQLite3;

import static com.sun.jna.Pointer.NULL;
import static java.util.Arrays.fill;
import static org.sqlite.ErrCodes.SQLITE_CONSTRAINT;
import static org.sqlite.IndexConstraintOp.SQLITE_INDEX_CONSTRAINT_EQ;
import static org.sqlite.SQLite.SQLITE_OK;

public class Series {
	private static final Module<SeriesTab, SeriesTabCursor> MODULE = new Module<SeriesTab, SeriesTabCursor>(Series::connect, true) {
		@Override
		protected SeriesTab vtab(Pointer pVTab) {
			final SeriesTab seriesTab = new SeriesTab(pVTab);
			seriesTab.read();
			return seriesTab;
		}
		@Override
		protected SeriesTabCursor cursor(Pointer cursor) {
			final SeriesTabCursor c = new SeriesTabCursor(cursor);
			c.read();
			return c;
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
		//sqlite3_vtab_config(db, SQLITE_VTAB_INNOCUOUS);
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
		protected int bestIndex(IndexInfo info) {
			int idxNum = 0;           /* The query plan bitmask */
			int unusableMask = 0;     /* Mask of unusable constraints */
			int[] aIdx = new int[3]; /* Constraints on start, stop, and step */
			fill(aIdx, -1);
			IndexConstraint[] constraints = info.constraints();
			for (int i = 0; i < constraints.length; i++) {
				IndexConstraint constraint = constraints[i];
				if (constraint.iColumn < SERIES_COLUMN_START) {
					continue;
				}
				/* 0 for start, 1 for stop, 2 for step */
				int iCol = constraint.iColumn - SERIES_COLUMN_START;
				assert( iCol>=0 && iCol<=2 );
				/* bitmask for those column */
				int iMask = 1 << iCol;
				if (!constraint.isUsable()) {
					unusableMask |=  iMask;
				} else if (constraint.op == SQLITE_INDEX_CONSTRAINT_EQ) {
					idxNum |= iMask;
					aIdx[iCol] = i;
				}
			}
			IndexConstraintUsage[] usages = info.constraintUsages();
			int nArg = 0;
			for (int i = 0; i < aIdx.length; i++) {
				int j = aIdx[i];
				if (j >= 0) {
					final IndexConstraintUsage usage = usages[i];
					usage.argvIndex = ++nArg;
					usage.omit(false);
					usage.write();
				}
			}
			if ((unusableMask & ~idxNum) != 0) {
				/* The start, stop, and step columns are inputs.  Therefore if there
				 ** are unusable constraints on any of start, stop, or step then
				 ** this plan is unusable */
				return SQLITE_CONSTRAINT;
			}
			if ((idxNum & BOTH) == BOTH) {
				/* Both start= and stop= boundaries are available.  This is the
				 ** the preferred case */
				info.estimatedCost = 2 - (idxNum & STEP) != 0 ? 1 : 0;
				info.estimatedRows = 1000;
				IndexOrderBy[] orderBys = info.orderBys();
				if (orderBys.length == 1) {
					if (orderBys[0].is_desc()) {
						idxNum |= DESC;
					} else {
						idxNum |= 16;
					}
					info.orderByConsumed = true;
					info.write();
				}
			} else {
				/* If either boundary is missing, we have to generate a huge span
				 ** of numbers.  Make this case very expensive so that the query
				 ** planner will work hard to avoid it. */
				info.estimatedRows = 2_147_483_647;
			}
			info.idxNum = idxNum;
			return SQLITE_OK;
		}

		@Override
		protected SeriesTabCursor open() throws SQLiteException {
			Pointer init = Module.alloc(SeriesTabCursor.ByValue.class);
			return new SeriesTabCursor(init);
		}
	}

	@Structure.FieldOrder({"pVtab", "isDesc", "iRowid", "iValue", "mnValue", "mxValue", "iStep"})
	public static class SeriesTabCursor extends VTabCursor<SeriesTab> {
		/**
		 * Virtual table of this cursor
		 */
		public SeriesTab pVtab; // sqlite3_vtab *
		/* True to count down rather than up */
		public boolean isDesc; // int
		/* The rowid */
		public long iRowid;
		/* Current value ("value") */
		public long iValue;
		/* Mimimum value ("start") */
		public long mnValue;
		/* Maximum value ("stop") */
		public long mxValue;
		/* Increment ("step") */
		public long iStep;

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
			int i = 0;
			if ((idxNum & START) != 0) {
				mnValue = args.getLong(i++);
			} else {
				mnValue = 0;
			}
			if ((idxNum & STOP) != 0) {
				mxValue = args.getLong(i++);
			} else {
				mxValue = 0xffffffff;
			}
			if ((idxNum & STEP) != 0) {
				iStep = args.getLong(i);
				if (iStep == 0) {
					iStep = 1;
				} else if (iStep < 0) {
					iStep = -iStep;
					if ((idxNum & 16) == 0) {
						idxNum |= DESC;
					}
				}
			} else {
				iStep = 1;
			}
			for (i = 0; i < args.getCount(); i++) {
				if (args.getType(i) == ColTypes.SQLITE_NULL) {
					mnValue = 1;
					mxValue = 0;
					break;
				}
			}
			isDesc = (idxNum & DESC) != 0;
			if (isDesc) {
				iValue = mxValue;
				if (iStep > 0) {
					iValue -= (mxValue - mnValue) % iStep;
				}
			} else {
				iValue = mnValue;
			}
			iRowid = 1;
			write();
		}

		@Override
		protected void next() {
			if (isDesc) {
				iValue -= iStep;
			} else {
				iValue += iStep;
			}
			iRowid++;
			write();
		}

		@Override
		protected boolean eof() {
			if (isDesc) {
				return iValue < mnValue;
			} else {
				return iValue > mxValue;
			}
		}

		@Override
		protected void column(SQLite.SQLite3Context ctx, int i) {
			long x;
			if (i == SERIES_COLUMN_START) {
				x = mnValue;
			} else if (i == SERIES_COLUMN_STOP) {
				x = mxValue;
			} else if (i == SERIES_COLUMN_STEP) {
				x = iStep;
			} else {
				x = iValue;
			}
			ctx.setResultLong(x);
		}

		@Override
		protected long rowId() {
			return iRowid;
		}
	}
}
