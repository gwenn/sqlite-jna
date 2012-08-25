/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite.driver;

import org.sqlite.ColTypes;
import org.sqlite.Stmt;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class RowsMeta implements ResultSetMetaData {
  private org.sqlite.Stmt stmt;

  RowsMeta(Stmt stmt) {
    this.stmt = stmt;
  }

  private org.sqlite.Stmt getStmt() throws SQLException {
    return stmt;
  }
  private int fixCol(int columnIndex) {
    return columnIndex - 1;
  }

  @Override
  public int getColumnCount() throws SQLException { // Used by Hibernate
    return getStmt().getColumnCount();
  }
  @Override
  public boolean isAutoIncrement(int column) throws SQLException {
    return getStmt().getMetadata(fixCol(column))[2];
  }
  @Override
  public boolean isCaseSensitive(int column) throws SQLException {
    return true; // FIXME Collation 'NOCASE'
  }
  @Override
  public boolean isSearchable(int column) throws SQLException {
    return true;
  }
  @Override
  public boolean isCurrency(int column) throws SQLException {
    return false;
  }
  @Override
  public int isNullable(int column) throws SQLException {
    return getStmt().getMetadata(fixCol(column))[0] ? columnNoNulls : columnNullable;
  }
  @Override
  public boolean isSigned(int column) throws SQLException {
    return true;
  }
  @Override
  public int getColumnDisplaySize(int column) throws SQLException {
    return 10; // Like in SQLite shell with column mode
  }
  @Override
  public String getColumnLabel(int column) throws SQLException {
    return getColumnName(column);
  }
  @Override
  public String getColumnName(int column) throws SQLException {
    return getStmt().getColumnName(fixCol(column)); // TODO sqlite3_column_origin_name ?
  }
  @Override
  public String getSchemaName(int column) throws SQLException {
    return ""; // TODO sqlite3_column_database_name
  }
  @Override
  public int getPrecision(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getPrecision");
    return 0; // TODO based on column type
  }
  @Override
  public int getScale(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getScale");
    return 0; // TODO based on column type
  }
  @Override
  public String getTableName(int column) throws SQLException {
    return getStmt().getColumnTableName(fixCol(column));
  }
  @Override
  public String getCatalogName(int column) throws SQLException {
    return "";
  }
  @Override
  public int getColumnType(int column) throws SQLException {
    // After a type conversion, the value returned by sqlite3_column_type() is undefined.
    final int sourceType = getStmt().getColumnType(fixCol(column));
    switch (sourceType) {
      case ColTypes.SQLITE_TEXT:
        return Types.VARCHAR;
      case ColTypes.SQLITE_INTEGER:
        return Types.INTEGER;
      case ColTypes.SQLITE_FLOAT:
        return Types.REAL;
      case ColTypes.SQLITE_BLOB:
        return Types.BLOB;
      case ColTypes.SQLITE_NULL:
        return Types.NULL;
      default:
        throw new AssertionError(String.format("Unknown column type %d", sourceType));
    }
  }
  @Override
  public String getColumnTypeName(int column) throws SQLException {
    // After a type conversion, the value returned by sqlite3_column_type() is undefined.
    final int sourceType = getStmt().getColumnType(fixCol(column));
    switch (sourceType) {
      case ColTypes.SQLITE_TEXT:
        return "text";
      case ColTypes.SQLITE_INTEGER:
        return "integer";
      case ColTypes.SQLITE_FLOAT:
        return "real";
      case ColTypes.SQLITE_BLOB:
        return "blob";
      case ColTypes.SQLITE_NULL:
        return "null";
      default:
        throw new AssertionError(String.format("Unknown column type %d", sourceType));
    }
  }
  @Override
  public boolean isReadOnly(int column) throws SQLException {
    return false;
  }
  @Override
  public boolean isWritable(int column) throws SQLException {
    return true;
  }
  @Override
  public boolean isDefinitelyWritable(int column) throws SQLException {
    return true;
  }
  @Override
  public String getColumnClassName(int column) throws SQLException {
    // After a type conversion, the value returned by sqlite3_column_type() is undefined.
    final int sourceType = getStmt().getColumnType(fixCol(column));
    switch (sourceType) {
      case ColTypes.SQLITE_TEXT:
        return "java.lang.String";
      case ColTypes.SQLITE_INTEGER:
        return "java.lang.Long";
      case ColTypes.SQLITE_FLOAT:
        return "java.lang.Double";
      case ColTypes.SQLITE_BLOB:
        return "[B";
      case ColTypes.SQLITE_NULL:
        return null;
      default:
        throw new AssertionError(String.format("Unknown column type %d", sourceType));
    }
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw Util.error("not a wrapper");
  }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }
}