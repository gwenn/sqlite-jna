/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.SQLite;

import java.sql.RowId;

record RowIdImpl(long value) implements RowId {

	@Override
	public byte[] getBytes() {
		return toString().getBytes(SQLite.UTF_8);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RowIdImpl rowId = (RowIdImpl) o;

		return value == rowId.value;

	}

	@Override
	public int hashCode() {
		return Long.hashCode(value);
	}

	static long getValue(RowId rowId) {
		if (rowId instanceof RowIdImpl) {
			return ((RowIdImpl) rowId).value;
		} else {
			return Long.parseLong(rowId.toString());
		}
	}
}
