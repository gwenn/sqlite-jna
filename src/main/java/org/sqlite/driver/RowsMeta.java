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
    switch (getStmt().getColumnAffinity(fixCol(column))) {
      case ColAffinities.INTEGER:
      case ColAffinities.NUMERIC:
      case ColAffinities.REAL:
        return false;
    }
    return true; // FIXME Collation 'NOCASE'
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
    return getStmt().getMetadata(fixCol(column))[0] ? columnNoNulls : columnNullable;
  }

  @Override
  public boolean isSigned(int column) throws SQLException {
    checkColumn(column);
    return true;
  }

  @Override
  public int getColumnDisplaySize(int column) throws SQLException {
    checkColumn(column);
    return 10; // Like in SQLite shell with column mode
  }

  @Override
  public String getColumnLabel(int column) throws SQLException {
    return getStmt().getColumnName(fixCol(column));
  }

  @Override
  public String getColumnName(int column) throws SQLException {
    String name = getStmt().getColumnOriginName(fixCol(column));
    if (name == null) {
      return getColumnLabel(column);
    }
    return name;
  }

  @Override
  public String getSchemaName(int column) throws SQLException {
    return getStmt().getColumnDatabaseName(fixCol(column));
  }

  @Override
  public int getPrecision(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getPrecision");
    checkColumn(column);
    return 0; // TODO based on column type
  }

  @Override
  public int getScale(int column) throws SQLException {
    Util.trace("ResultSetMetaData.getScale");
    checkColumn(column);
    return 0; // TODO based on column type
  }

  @Override
  public String getTableName(int column) throws SQLException {
    return getStmt().getColumnTableName(fixCol(column));
  }

  @Override
  public String getCatalogName(int column) throws SQLException {
    checkColumn(column);
    return "";
  }

  @Override
  public int getColumnType(int column) throws SQLException {
    return DbMeta.getJavaType(getColumnTypeName(column));
  }

  @Override
  public String getColumnTypeName(int column) throws SQLException {
    return getStmt().getColumnDeclType(fixCol(column));
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
  public boolean isDefinitelyWritable(int column) throws SQLException {
    return true;
  }

  @Override
  public String getColumnClassName(int column) throws SQLException {
    final int affinity = getStmt().getColumnAffinity(fixCol(column));
    switch (affinity) {
      case ColAffinities.TEXT:
        return "java.lang.String";
      case ColAffinities.INTEGER:
        return "java.lang.Long";
      case ColAffinities.REAL:
        return "java.lang.Double";
      case ColAffinities.NONE:
        return "[B";
      default:
        return "java.lang.Number";
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

  private void checkColumn(int column) throws SQLException {
    if (column > getColumnCount()) {
      throw new StmtException(stmt, String.format("column index (%d) > column count (%d)", column, getColumnCount()), ErrCodes.WRAPPER_SPECIFIC);
    }
  }
}
