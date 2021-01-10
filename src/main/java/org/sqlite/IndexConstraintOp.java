package org.sqlite;

/**
 * @see <a href="https://sqlite.org/c3ref/c_index_constraint_eq.html">Virtual Table Constraint Operator Codes</a>
 */
public interface IndexConstraintOp {
	int SQLITE_INDEX_CONSTRAINT_EQ = 2;
	int SQLITE_INDEX_CONSTRAINT_GT = 4;
	int SQLITE_INDEX_CONSTRAINT_LE = 8;
	int SQLITE_INDEX_CONSTRAINT_LT = 16;
	int SQLITE_INDEX_CONSTRAINT_GE = 32;
	int SQLITE_INDEX_CONSTRAINT_MATCH = 64;
	int SQLITE_INDEX_CONSTRAINT_LIKE = 65;      // 3.10.0
	int SQLITE_INDEX_CONSTRAINT_GLOB = 66;      // 3.10.0
	int SQLITE_INDEX_CONSTRAINT_REGEXP = 67;    // 3.10.0
	int SQLITE_INDEX_CONSTRAINT_NE = 68;        // 3.21.0
	int SQLITE_INDEX_CONSTRAINT_ISNOT = 69;     // 3.21.0
	int SQLITE_INDEX_CONSTRAINT_ISNOTNULL = 70; // 3.21.0
	int SQLITE_INDEX_CONSTRAINT_ISNULL = 71;    // 3.21.0
	int SQLITE_INDEX_CONSTRAINT_IS = 72;        // 3.21.0
	int SQLITE_INDEX_CONSTRAINT_FUNCTION = 150; // 3.25.0
}
