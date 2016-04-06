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
 * Type affinity of a column.
 * @see <a href="http://sqlite.org/datatype3.html#affinity">Type Affinity</a>
 */
public interface ColAffinities {
	int INTEGER = 0;
	int TEXT = 1;
	int NONE = 2;
	int REAL = 3;
	int NUMERIC = 4;
}
