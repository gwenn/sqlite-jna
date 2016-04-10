package org.sqlite;

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
	 * @throws StmtException
	 */
	int getColumnType(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Column declared type
	 * @throws StmtException
	 */
	String getColumnDeclType(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return {@link org.sqlite.ColAffinities}.*
	 * @throws StmtException
	 */
	int getColumnAffinity(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Column name
	 * @throws StmtException
	 */
	String getColumnName(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Column origin name
	 * @throws StmtException
	 */
	String getColumnOriginName(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Table name
	 * @throws StmtException
	 */
	String getColumnTableName(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return Database name
	 * @throws StmtException
	 */
	String getColumnDatabaseName(int iCol) throws StmtException;

	/**
	 * @param iCol The leftmost column is number 0.
	 * @return BLOB value
	 * @throws StmtException
	 */
	byte[] getColumnBlob(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return the number of bytes in that BLOB or string.
	 * @throws StmtException
	 */
	int getColumnBytes(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return double value
	 * @throws StmtException
	 */
	double getColumnDouble(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return int value
	 * @throws StmtException
	 */
	int getColumnInt(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return long value
	 * @throws StmtException
	 */
	long getColumnLong(int iCol) throws StmtException;
	/**
	 * @param iCol The leftmost column is number 0.
	 * @return text value
	 * @throws StmtException
	 */
	String getColumnText(int iCol) throws StmtException;
}
