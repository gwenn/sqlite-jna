/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.ColAffinities;
import org.sqlite.ErrCodes;
import org.sqlite.Stmt;
import org.sqlite.StmtException;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

class RowsMeta implements ResultSetMetaData {
	private final Stmt stmt;

	RowsMeta(Stmt stmt) {
		this.stmt = stmt;
	}

	private static int fixCol(int columnIndex) {
		return columnIndex - 1;
	}

	@Override
	public int getColumnCount() { // Used by Hibernate
		return stmt.getColumnCount();
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException {
		return stmt.getMetadata(fixCol(column))[2];
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException {
		return switch (stmt.getColumnAffinity(fixCol(column))) {
			case ColAffinities.INTEGER, ColAffinities.NUMERIC, ColAffinities.REAL -> false;
			default -> true;
		};
	}

	@Override
	public boolean isSearchable(int column) throws SQLException {
		checkColumn(column);
		return true;
	}

	@Override
	public boolean isCurrency(int column) throws SQLException {
		checkColumn(column);
		return false;
	}

	@Override
	public int isNullable(int column) throws SQLException {
		return stmt.getMetadata(fixCol(column))[0] ? columnNoNulls : columnNullable;
	}

	@Override
	public boolean isSigned(int column) throws SQLException {
		return switch (stmt.getColumnAffinity(fixCol(column))) {
			case ColAffinities.INTEGER, ColAffinities.NUMERIC, ColAffinities.REAL -> true;
			default -> false;
		};
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException {
		return switch (stmt.getColumnAffinity(fixCol(column))) {
			case ColAffinities.INTEGER -> 20;
			case ColAffinities.REAL -> 25;
			default -> 10;
		};
	}

	@Override
	public String getColumnLabel(int column) throws SQLException {
		return stmt.getColumnName(fixCol(column));
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		String name = stmt.getColumnOriginName(fixCol(column));
		if (name == null) {
			return getColumnLabel(column);
		}
		return name;
	}

	@Override
	public String getSchemaName(int column) throws SQLException {
		return nullToEmpty(stmt.getColumnDatabaseName(fixCol(column)));
	}

	@Override
	public int getPrecision(int column) throws SQLException {
		return switch (stmt.getColumnAffinity(fixCol(column))) {
			case ColAffinities.INTEGER -> 19;
			case ColAffinities.NUMERIC, ColAffinities.REAL -> 15;
			default -> 0;
		};
	}

	@Override
	public int getScale(int column) throws SQLException {
		return switch (stmt.getColumnAffinity(fixCol(column))) {
			case ColAffinities.INTEGER -> 0;
			case ColAffinities.NUMERIC, ColAffinities.REAL -> 15;
			default -> 0;
		};
	}

	@Override
	public String getTableName(int column) throws SQLException {
		return nullToEmpty(stmt.getColumnTableName(fixCol(column)));
	}

	@Override
	public String getCatalogName(int column) {
		return "";
	}

	@Override
	public int getColumnType(int column) throws SQLException {
		int affinity = stmt.getColumnAffinity(fixCol(column));
		return DbMeta.getJavaType(affinity);
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException {
		return stmt.getColumnDeclType(fixCol(column));
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException {
		checkColumn(column);
		return false;
	}

	@Override
	public boolean isWritable(int column) throws SQLException {
		checkColumn(column);
		return true;
	}

	@Override
	public boolean isDefinitelyWritable(int column) {
		return true;
	}

	@Override
	public String getColumnClassName(int column) throws SQLException {
		int affinity = stmt.getColumnAffinity(fixCol(column));
		return switch (affinity) {
			case ColAffinities.TEXT -> "java.lang.String";
			case ColAffinities.INTEGER -> "java.lang.Long";
			case ColAffinities.REAL -> "java.lang.Double";
			case ColAffinities.NONE -> "[B";
			default -> "java.lang.Number";
		};
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		}
		throw new SQLException("Cannot unwrap to " + iface.getName());
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(getClass());
	}

	private void checkColumn(int column) throws SQLException {
		if (column > getColumnCount()) {
			throw new StmtException(stmt, String.format("column index (%d) > column count (%d)", column, getColumnCount()), ErrCodes.WRAPPER_SPECIFIC);
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
