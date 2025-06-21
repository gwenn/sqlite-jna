package org.sqlite;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A single result row of a query.
 */
public interface Row {
	/**
	 * @return column count
	 */
	int getColumnCount();
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return {@link org.sqlite.ColTypes}.*
	 */
	int getColumnType(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Column declared type
	 */
	String getColumnDeclType(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return {@link org.sqlite.ColAffinities}.*
	 */
	int getColumnAffinity(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Column name
	 */
	String getColumnName(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Column origin name
	 */
	String getColumnOriginName(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Table name
	 */
	String getColumnTableName(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Database name
	 */
	String getColumnDatabaseName(int iCol) throws StmtException;

	/**
	 * @param iCol The leftmost column is number 0.
	 * @return BLOB value
	 */
	byte@Nullable[] getColumnBlob(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return the number of bytes in that BLOB or string.
	 */
	int getColumnBytes(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return double value
	 */
	double getColumnDouble(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return int value
	 */
	int getColumnInt(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return long value
	 */
	long getColumnLong(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return text value
	 */
	@Nullable
	String getColumnText(int iCol) throws StmtException;
}
