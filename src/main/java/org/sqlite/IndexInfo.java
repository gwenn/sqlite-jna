package org.sqlite;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	public Pointer aConstraint; // sqlite3_index_constraint * => IndexConstraint[nConstraint] (org.sqlite.SQLite.SQLite3Values.build ?)
	/**
	 * Number of terms in the ORDER BY clause
	 */
	public int nOrderBy;
	/**
	 * The ORDER BY clause
	 */
	public Pointer aOrderBy; // sqlite3_index_orderby * => IndexOrderBy[nOrderBy] (org.sqlite.SQLite.SQLite3Values.build ?)
	/* Outputs */
	public Pointer aConstraintUsage; // sqlite3_index_constraint_usage * => IndexConstraintUsage[nConstraint] (org.sqlite.SQLite.SQLite3Values.build ?)
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
	public int needToFreeIdxStr;
	/**
	 * True if output is already ordered
	 */
	public int orderByConsumed;
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
	public int idxFlags;
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
		public byte op; // unsigned char
		/**
		 * True if this constraint is usable
		 */
		public byte usable; // unsigned char
		/**
		 * Used internally - xBestIndex should ignore
		 */
		public int iTermOffset;

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

		public boolean is_desc() {
			return desc != 0;
		}
	}

	@FieldOrder({"argvIndex", "omit"})
	public static final class IndexConstraintUsage extends Structure implements ByReference {
		/**
		 * if >0, constraint is part of argv to xFilter
		 */
		public int argvIndex;
		/** Do not code a test for this constraint */
		public byte omit; // unsigned char

		public void omit(boolean omit) {
			this.omit = omit ? (byte)1 : (byte)0;
		}
	}

	public IndexConstraint[] constraints() {
		if (nConstraint == 0 || aConstraint == NULL) {
			return new IndexConstraint[0];
		}
		IndexConstraint first = Structure.newInstance(IndexConstraint.class, aConstraint);
		IndexConstraint[] constraints = (IndexConstraint[])first.toArray(nConstraint);
		return constraints;
	}
	public IndexOrderBy[] orderBys() {
		if (nOrderBy == 0 || aOrderBy == NULL) {
			return new IndexOrderBy[0];
		}
		IndexOrderBy first = Structure.newInstance(IndexOrderBy.class, aOrderBy);
		IndexOrderBy[] orderBys = (IndexOrderBy[])first.toArray(nOrderBy);
		return orderBys;
	}
	public IndexConstraintUsage[] constraintUsages() {
		if (nConstraint == 0 || aConstraintUsage == NULL) {
			return new IndexConstraintUsage[0];
		}
		IndexConstraintUsage first = Structure.newInstance(IndexConstraintUsage.class, aConstraintUsage);
		IndexConstraintUsage[] constraintUsages = (IndexConstraintUsage[])first.toArray(nConstraint);
		return constraintUsages;
	}
}
