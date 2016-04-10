/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

/**
 * Value storage class.
 * @see <a href="https://www.sqlite.org/c3ref/c_blob.html">Fundamental Datatypes</a>
 * @see <a href="http://sqlite.org/datatype3.html">Storage Classes and Datatypes</a>
 */
public interface ColTypes {
	/** 64-bit signed integer */
	int SQLITE_INTEGER = 1;
	/** 64-bit IEEE floating point number */
	int SQLITE_FLOAT = 2;
	int SQLITE_BLOB = 4;
	int SQLITE_NULL = 5;
	int SQLITE_TEXT = 3;
}
