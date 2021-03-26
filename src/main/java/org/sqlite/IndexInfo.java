package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import static com.sun.jna.Pointer.NULL;

/**
 * @see <a href="https://sqlite.org/c3ref/index_info.html">sqlite3_index_info</a>
 */
@FieldOrder({"nConstraint", "aConstraint", "nOrderBy", "aOrderBy", "aConstraintUsage", "idxNum", "idxStr", "needToFreeIdxStr", "orderByConsumed", "estimatedCost", "estimatedRows", "idxFlags", "colUsed"})
public class IndexInfo extends Structure {
	/* Inputs */
	/**
	 * Number of entries in aConstraint
	 */
	public int nConstraint;
	/**
	 * Table of WHERE clause constraints
	 */
	public Pointer aConstraint; // sqlite3_index_constraint * => IndexConstraint[nConstraint]
	/**
	 * Number of terms in the ORDER BY clause
	 */
	public int nOrderBy;
	/**
	 * The ORDER BY clause
	 */
	public Pointer aOrderBy; // sqlite3_index_orderby * => IndexOrderBy[nOrderBy]
	/* Outputs */
	public Pointer aConstraintUsage; // sqlite3_index_constraint_usage * => IndexConstraintUsage[nConstraint]
	/**
	 * Number used to identify the index
	 */
	public int idxNum;
	/**
	 * String, possibly obtained from sqlite3_malloc
	 */
	public Pointer idxStr; // char *
	/**
	 * Free idxStr using sqlite3_free() if true
	 */
	public boolean needToFreeIdxStr; // int
	/**
	 * True if output is already ordered
	 */
	public boolean orderByConsumed; // int
	/**
	 * Estimated cost of using this index
	 */
	public double estimatedCost;
	/* Fields below are only available in SQLite 3.8.2 and later */
	/**
	 * Estimated number of rows returned
	 */
	public long estimatedRows; // sqlite3_int64
	/* Fields below are only available in SQLite 3.9.0 and later */
	/**
	 * Mask of SQLITE_INDEX_SCAN_* flags
	 */
	public int idxFlags; // TODO EnumConverter
	/* Fields below are only available in SQLite 3.10.0 and later */

	/**
	 * Input: Mask of columns used by statement
	 */
	public long colUsed; // sqlite3_uint64

	@FieldOrder({"iColumn", "op", "usable", "iTermOffset"})
	public static final class IndexConstraint extends Structure {
		/**
		 * Column constrained.  -1 for ROWID
		 */
		public int iColumn;
		/**
		 * Constraint operator
		 */
		public byte op; // unsigned char, TODO EnumConverter IndexConstraintOp
		/**
		 * True if this constraint is usable
		 */
		public byte usable; // unsigned char
		/**
		 * Used internally - xBestIndex should ignore
		 */
		public int iTermOffset;

		public IndexConstraint(Pointer p) {
			super(p);
			read();
		}
		public boolean isUsable() {
			return usable != 0;
		}
	}

	@FieldOrder({"iColumn", "desc"})
	public static final class IndexOrderBy extends Structure {
		/**
		 * Column number
		 */
		public int iColumn;
		/**
		 * True for DESC.  False for ASC.
		 */
		public byte desc; // unsigned char

		public IndexOrderBy(Pointer p) {
			super(p);
			read();
		}
		public boolean is_desc() {
			return desc != 0;
		}
	}

	@FieldOrder({"argvIndex", "omit"})
	public static final class IndexConstraintUsage extends Structure {
		/**
		 * if >0, constraint is part of argv to xFilter
		 */
		public int argvIndex;
		/** Do not code a test for this constraint */
		public byte omit; // unsigned char

		public IndexConstraintUsage(Pointer p) {
			super(p);
			//read();
		}
		public void omit(boolean omit) {
			this.omit = omit ? (byte)1 : (byte)0;
		}
	}

	public IndexConstraint[] constraints() {
		if (nConstraint == 0 || aConstraint == NULL) {
			return new IndexConstraint[0];
		}
		IndexConstraint first = new IndexConstraint(aConstraint);
		IndexConstraint[] constraints = (IndexConstraint[])first.toArray(nConstraint);
		return constraints;
	}
	public IndexOrderBy[] orderBys() {
		if (nOrderBy == 0 || aOrderBy == NULL) {
			return new IndexOrderBy[0];
		}
		IndexOrderBy first = new IndexOrderBy(aOrderBy);
		IndexOrderBy[] orderBys = (IndexOrderBy[])first.toArray(nOrderBy);
		return orderBys;
	}
	public IndexConstraintUsage[] constraintUsages() {
		if (nConstraint == 0 || aConstraintUsage == NULL) {
			return new IndexConstraintUsage[0];
		}
		IndexConstraintUsage first = new IndexConstraintUsage(aConstraintUsage);
		IndexConstraintUsage[] constraintUsages = (IndexConstraintUsage[])first.toArray(nConstraint);
		return constraintUsages;
	}
}
