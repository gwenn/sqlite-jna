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
import org.sqlite.SQLite;
import org.sqlite.StmtException;
import org.sqlite.parser.DefaultSchemaProvider;
import org.sqlite.parser.EnhancedPragma;
import org.sqlite.parser.SchemaProvider;
import org.sqlite.parser.ast.IdExpr;
import org.sqlite.parser.ast.Pragma;
import org.sqlite.parser.ast.QualifiedName;
import org.sqlite.parser.ast.Select;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.sqlite.SQLite.escapeIdentifier;

class DbMeta implements DatabaseMetaData {
	private final Conn c;
	private final SchemaProvider schemaProvider;

	DbMeta(Conn c) {
		this.c = c;
		this.schemaProvider = new DefaultSchemaProvider(c);
	}

	private void checkOpen() throws SQLException {
		if (c == null) throw new SQLException("connection closed");
	}

	private org.sqlite.Conn getConn() throws SQLException {
		checkOpen();
		return c.getConn();
	}

	@Override
	public boolean allProceduresAreCallable() {
		return false;
	}

	@Override
	public boolean allTablesAreSelectable() {
		return true;
	}

	@Override
	public String getURL() throws SQLException {
		return JDBC.PREFIX + getConn().getFilename();
	}

	@Override
	public String getUserName() {
		return null;
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return getConn().isReadOnly(null);
	}

	@Override
	public boolean nullsAreSortedHigh() {
		return false;
	}

	@Override
	public boolean nullsAreSortedLow() {
		return true;
	}

	@Override
	public boolean nullsAreSortedAtStart() {
		return false;
	}

	@Override
	public boolean nullsAreSortedAtEnd() {
		return false;
	}

	@Override
	public String getDatabaseProductName() {
		return "SQLite 3";
	}

	@Override
	public String getDatabaseProductVersion() {
		return org.sqlite.Conn.libversion();
	}

	@Override
	public String getDriverName() {
		return "SQLiteJNA";
	}

	@Override
	public String getDriverVersion() {
		return "1.0"; // FIXME
	}

	/** @see JDBC#getMajorVersion() */
	@Override
	public int getDriverMajorVersion() {
		return 1; // FIXME Keep in sync with Driver
	}

	/** @see JDBC#getMinorVersion() */
	@Override
	public int getDriverMinorVersion() {
		return 0; // FIXME Keep in sync with Driver
	}

	@Override
	public boolean usesLocalFiles() {
		return true;
	}

	@Override
	public boolean usesLocalFilePerTable() {
		return false;
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() {
		return false;
	}

	@Override
	public boolean storesUpperCaseIdentifiers() {
		return false;
	}

	@Override
	public boolean storesLowerCaseIdentifiers() {
		return false;
	}

	@Override
	public boolean storesMixedCaseIdentifiers() {
		return true;
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() {
		return false;
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() {
		return false;
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() {
		return false;
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() {
		return true;
	}

	@Override
	public String getIdentifierQuoteString() {
		return "\""; // http://sqlite.org/lang_keywords.html
	}

	@Override
	public String getSQLKeywords() {
		return "ABORT,ANALYZE,ATTACH,AUTOINCREMENT,CONFLICT,DATABASE,DETACH,EXCLUSIVE,EXPLAIN,FAIL,GLOB,IF,IGNORE,INDEX,INDEXED,INSTEAD,ISNULL,LIMIT,NOTNULL,OFFSET,PLAN,PRAGMA,QUERY,RAISE,REGEXP,REINDEX,RENAME,REPLACE,RESTRICT,TEMP,VACUUM,VIRTUAL";
	}

	@Override
	public String getNumericFunctions() {
		return "abs,max,min,round,random";
	}

	@Override
	public String getStringFunctions() {
		return "glob,length,like,lower,ltrim,replace,rtrim,soundex,substr,trim,upper";
	}

	@Override
	public String getSystemFunctions() {
		return "last_insert_rowid,load_extension,sqlite_version";
	}

	@Override
	public String getTimeDateFunctions() {
		return "date,time,datetime,julianday,strftime";
	}

	@Override
	public String getSearchStringEscape() {
		Util.trace("DatabaseMetaData.getSearchStringEscape");
		return null; // TODO Validate (Y LIKE X [ESCAPE Z])
	}

	@Override
	public String getExtraNameCharacters() {
		return "";
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() {
		return true;
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() {
		return false;
	}

	@Override
	public boolean supportsColumnAliasing() {
		return true;
	}

	@Override
	public boolean nullPlusNonNullIsNull() {
		return true;
	}

	@Override
	public boolean supportsConvert() {
		Util.trace("DatabaseMetaData.supportsConvert");
		return false; // TODO Validate
	}

	@Override
	public boolean supportsConvert(int fromType, int toType) {
		Util.trace("DatabaseMetaData.supportsConvert");
		return false; // TODO Validate
	}

	@Override
	public boolean supportsTableCorrelationNames() {
		return true; // table alias
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() {
		return false; // table alias can be the same
	}

	@Override
	public boolean supportsExpressionsInOrderBy() {
		return true;
	}

	@Override
	public boolean supportsOrderByUnrelated() {
		return true; // select name from sqlite_master order by type;
	}

	@Override
	public boolean supportsGroupBy() {
		return true;
	}

	@Override
	public boolean supportsGroupByUnrelated() {
		return true; // select name from sqlite_master group by type;
	}

	@Override
	public boolean supportsGroupByBeyondSelect() {
		return true; // TODO Validate
	}

	@Override
	public boolean supportsLikeEscapeClause() {
		return true;
	}

	/** @see Stmt#getMoreResults() */
	@Override
	public boolean supportsMultipleResultSets() {
		return true;
	}

	@Override
	public boolean supportsMultipleTransactions() {
		return true;
	}

	@Override
	public boolean supportsNonNullableColumns() {
		return true;
	}

	@Override
	public boolean supportsMinimumSQLGrammar() {
		return true;
	}

	@Override
	public boolean supportsCoreSQLGrammar() {
		return true;
	}

	@Override
	public boolean supportsExtendedSQLGrammar() {
		Util.trace("DatabaseMetaData.supportsExtendedSQLGrammar");
		return false;
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() {
		Util.trace("DatabaseMetaData.supportsANSI92EntryLevelSQL");
		return false;
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() {
		Util.trace("DatabaseMetaData.supportsANSI92IntermediateSQL");
		return false;
	}

	@Override
	public boolean supportsANSI92FullSQL() {
		Util.trace("DatabaseMetaData.supportsANSI92FullSQL");
		return false;
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() {
		return false; // TODO Validate
	}

	@Override
	public boolean supportsOuterJoins() {
		return true;
	}

	@Override
	public boolean supportsFullOuterJoins() {
		return false;
	}

	@Override
	public boolean supportsLimitedOuterJoins() {
		return true;
	}

	@Override
	public String getSchemaTerm() {
		return "";
	}

	@Override
	public String getProcedureTerm() {
		return null;
	}

	@Override
	public String getCatalogTerm() {
		return "dbName";
	}

	@Override
	public boolean isCatalogAtStart() {
		return true;
	}

	@Override
	public String getCatalogSeparator() {
		return ".";
	}

	@Override
	public boolean supportsSchemasInDataManipulation() {
		return false;
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() {
		return false;
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() {
		return false;
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() {
		return false;
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() { // http://sqlite.org/syntaxdiagrams.html#qualified-table-name
		return true;
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() {
		return false;
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() { // http://sqlite.org/lang_createtable.html
		return true; // ~ temporary table name must be unqualified
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() { // http://sqlite.org/lang_createindex.html
		return true;
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() {
		return false;
	}

	@Override
	public boolean supportsPositionedDelete() {
		return false;
	}

	@Override
	public boolean supportsPositionedUpdate() {
		return false;
	}

	@Override
	public boolean supportsSelectForUpdate() {
		return false;
	}

	@Override
	public boolean supportsStoredProcedures() {
		return false;
	}

	@Override
	public boolean supportsSubqueriesInComparisons() {
		return true; // select name from sqlite_master where rootpage > (select 3);
	}

	@Override
	public boolean supportsSubqueriesInExists() {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInIns() {
		return true;
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() {
		Util.trace("DatabaseMetaData.supportsSubqueriesInQuantifieds");
		return false; // TODO Validate
	}

	@Override
	public boolean supportsCorrelatedSubqueries() {
		return true; // select * from sqlite_master sm where not exists (select 1 from sqlite_temp_master stm where stm.name = sm.name);
	}

	@Override
	public boolean supportsUnion() {
		return true;
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() {
		Util.trace("DatabaseMetaData.supportsOpenCursorsAcrossCommit");
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() {
		Util.trace("DatabaseMetaData.supportsOpenCursorsAcrossRollback");
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() {
		Util.trace("DatabaseMetaData.supportsOpenStatementsAcrossCommit");
		return false; // TODO Validate
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() {
		Util.trace("DatabaseMetaData.supportsOpenStatementsAcrossRollback");
		return false; // TODO Validate
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_LENGTH); // http://sqlite.org/limits.html#max_length
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_LENGTH); // http://sqlite.org/limits.html#max_length
	}

	@Override
	public int getMaxColumnNameLength() {
		return 0;
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_COLUMN); // http://sqlite.org/limits.html#max_column
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_COLUMN); // http://sqlite.org/limits.html#max_column
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_COLUMN); // http://sqlite.org/limits.html#max_column
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_COLUMN); // http://sqlite.org/limits.html#max_column
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_COLUMN); // http://sqlite.org/limits.html#max_column
	}

	@Override
	public int getMaxConnections() {
		return 0;
	}

	@Override
	public int getMaxCursorNameLength() {
		return 0;
	}

	@Override
	public int getMaxIndexLength() {
		return 0;
	}

	@Override
	public int getMaxSchemaNameLength() {
		return 0;
	}

	@Override
	public int getMaxProcedureNameLength() {
		return 0;
	}

	@Override
	public int getMaxCatalogNameLength() {
		return 0;
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_LENGTH); // http://sqlite.org/limits.html#max_length
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() {
		return true;
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		return getConn().getLimit(SQLite.SQLITE_LIMIT_SQL_LENGTH); // http://sqlite.org/limits.html#max_sql_length
	}

	@Override
	public int getMaxStatements() {
		return 0;
	}

	@Override
	public int getMaxTableNameLength() {
		return 0;
	}

	@Override
	public int getMaxTablesInSelect() {
		return 64; // ~ http://www.sqlite.org/limits.html: Maximum Number Of Tables In A Join
	}

	@Override
	public int getMaxUserNameLength() {
		return 0;
	}

	/** @see Conn#transactionIsolation */
	@Override
	public int getDefaultTransactionIsolation() {
		return Connection.TRANSACTION_SERIALIZABLE;
	}

	@Override
	public boolean supportsTransactions() {
		return true;
	}

	/** @see Conn#setTransactionIsolation(int) */
	@Override
	public boolean supportsTransactionIsolationLevel(int level) {
		return level == Connection.TRANSACTION_SERIALIZABLE || level == Connection.TRANSACTION_READ_UNCOMMITTED;
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions() {
		return true; // tested
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly() {
		return false;
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() {
		return false; // tested
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() {
		return false; // tested
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as PROCEDURE_CAT, "
						+ "null as PROCEDURE_SCHEM, "
						+ "null as PROCEDURE_NAME, "
						+ "null as UNDEF1, "
						+ "null as UNDEF2, "
						+ "null as UNDEF3, "
						+ "null as REMARKS, "
						+ "null as PROCEDURE_TYPE, "
						+ "null as SPECIFIC_NAME limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as PROCEDURE_CAT, "
						+ "null as PROCEDURE_SCHEM, "
						+ "null as PROCEDURE_NAME, "
						+ "null as COLUMN_NAME, "
						+ "null as COLUMN_TYPE, "
						+ "null as DATA_TYPE, "
						+ "null as TYPE_NAME, "
						+ "null as PRECISION, "
						+ "null as LENGTH, "
						+ "null as SCALE, "
						+ "null as RADIX, "
						+ "null as NULLABLE, "
						+ "null as REMARKS, "
						+ "null as COLUMN_DEF, "
						+ "null as SQL_DATA_TYPE, "
						+ "null as SQL_DATETIME_SUB, "
						+ "null as CHAR_OCTET_LENGTH, "
						+ "null as ORDINAL_POSITION, "
						+ "null as IS_NULLABLE, "
						+ "null as SPECIFIC_NAME limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		checkOpen();
		tableNamePattern = tableNamePattern == null || tableNamePattern.isEmpty() ? "%" : tableNamePattern;

		final StringBuilder sql = new StringBuilder().append("select").
				append(" cat as TABLE_CAT,").
				append(" null as TABLE_SCHEM,").
				append(" name as TABLE_NAME,").
				append(" upper(type) as TABLE_TYPE,").
				append(" null as REMARKS,").
				append(" null as TYPE_CAT,").
				append(" null as TYPE_SCHEM,").
				append(" null as TYPE_NAME,").
				append(" null as SELF_REFERENCING_COL_NAME,").
				append(" null as REF_GENERATION").
				append(" from (");

		final List<String> catalogs = schemaProvider.getDbNames(catalog);
		boolean match = false;
		final String typeExpr = "CASE WHEN like('sqlite_%', name) THEN 'system ' || type ELSE type END as type";
		for (String s : catalogs) {
			if (match) sql.append(" UNION ALL ");
			if ("main".equalsIgnoreCase(s)) {
				sql.append("SELECT 'main' as cat, name, ").append(typeExpr).append(" FROM sqlite_master UNION ALL ");
				sql.append("SELECT 'main', 'sqlite_master', 'SYSTEM TABLE'");
			} else if ("temp".equalsIgnoreCase(s)) {
				sql.append("SELECT 'temp' as cat, name, ").append(typeExpr).append(" FROM sqlite_temp_master UNION ALL ");
				sql.append("SELECT 'temp', 'sqlite_temp_master', 'SYSTEM TABLE'");
			} else {
				sql.append("SELECT ").append(quote(catalog)).append(" as cat, name, ").append(typeExpr).append(" FROM \"").
						append(escapeIdentifier(catalog)).append("\".sqlite_master UNION ALL ");
				sql.append("SELECT ").append(quote(catalog)).append(", 'sqlite_master', 'SYSTEM TABLE'");
			}
			match = true;
		}
		sql.append(") where TABLE_NAME like ").append(quote(tableNamePattern));

		if (types != null) {
			sql.append(" and TABLE_TYPE in (");
			for (int i = 0; i < types.length; i++) {
				if (i > 0) sql.append(", ");
				sql.append(quote(types[i].toUpperCase()));
			}
			sql.append(')');
		} else {
			sql.append(" and TABLE_TYPE in ('SYSTEM TABLE', 'TABLE', 'VIEW')");
		}

		sql.append(" order by TABLE_TYPE, TABLE_SCHEM, TABLE_NAME");

		final PreparedStatement stmt = c.prepareStatement(sql.toString());
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TABLE_SCHEM, "
						+ "null as TABLE_CATALOG "
						+ "limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		checkOpen();
		final StringBuilder sql = new StringBuilder("select dbName as TABLE_CAT from (");
		// Pragma cannot be used as subquery...
		final List<String> catalogs = schemaProvider.getDbNames(null);
		for (int i = 0; i < catalogs.size(); i++) {
			if (i > 0) {
				sql.append(" UNION ");
			}
			sql.append("SELECT ").append(quote(catalogs.get(i))).append(" AS dbName");
		}
		sql.append(") order by TABLE_CAT");
		final PreparedStatement stmt = c.prepareStatement(sql.toString());
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement("select 'TABLE' as TABLE_TYPE " +
				"union select 'VIEW' union select 'SYSTEM TABLE'");
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		checkOpen();
		final StringBuilder sql = new StringBuilder();

		final List<QualifiedName> tbls = schemaProvider.findTables(catalog, tableNamePattern);

		sql.append("select ").
				append("cat as TABLE_CAT, ").
				append("null as TABLE_SCHEM, ").
				append("tbl as TABLE_NAME, ").
				append("cn as COLUMN_NAME, ").
				append("ct as DATA_TYPE, ").
				append("tn as TYPE_NAME, ").
				append("10 as COLUMN_SIZE, "). // FIXME precision or display size
				append("null as BUFFER_LENGTH, "). // not used
				append("10 as DECIMAL_DIGITS, "). // FIXME scale or null
				append("10 as NUM_PREC_RADIX, ").
				append("colnullable as NULLABLE, ").
				append("null as REMARKS, ").
				append("cdflt as COLUMN_DEF, ").
				append("null as SQL_DATA_TYPE, "). // unused
				append("null as SQL_DATETIME_SUB, "). // unused
				append("10 as CHAR_OCTET_LENGTH, "). // FIXME same as COLUMN_SIZE
				append("ordpos as ORDINAL_POSITION, ").
				append("(case colnullable when 0 then 'NO' when 1 then 'YES' else '' end)").
				append(" as IS_NULLABLE, ").
				append("null as SCOPE_CATLOG, ").
				append("null as SCOPE_SCHEMA, ").
				append("null as SCOPE_TABLE, ").
				append("null as SOURCE_DATA_TYPE, ").
				append("'' as IS_AUTOINCREMENT, "). // TODO http://sqlite.org/autoinc.html
				append("'' as IS_GENERATEDCOLUMN from (");

		boolean colFound = false;
		for (QualifiedName tbl : tbls) {
			Pragma pragma = new Pragma(new QualifiedName(tbl.dbName, "table_info"), new IdExpr(tbl.name));
			// Pragma cannot be used as subquery...
			try (PreparedStatement table_info = c.prepareStatement(pragma.toSql());
					 ResultSet rs = table_info.executeQuery()) {
				// 1:cid|2:name|3:type|4:notnull|5:dflt_value|6:pk
				while (rs.next()) {
					if (colFound) sql.append(" UNION ALL ");
					colFound = true;

					final String colType = getSQLiteType(rs.getString(3));
					final int colJavaType = getJavaType(colType);

					sql.append("SELECT ").
							append(quote(tbl.dbName)).append(" AS cat, ").
							append(quote(tbl.name)).append(" AS tbl, ").
							append(rs.getInt(1) + 1).append(" AS ordpos, ").
							append(rs.getBoolean(4) ? columnNoNulls : columnNullable).append(" AS colnullable, ").
							append(colJavaType).append(" AS ct, ").
							append(quote(rs.getString(2))).append(" AS cn, ").
							append(quote(colType)).append(" AS tn, ").
							append(quote(rs.getString(5))).append(" AS cdflt");

					if (columnNamePattern != null && !"%".equals(columnNamePattern)) {
						sql.append(" WHERE cn LIKE ").append(quote(columnNamePattern));
					}
				}
			} catch (StmtException e) { // query does not return ResultSet
				assert e.getErrorCode() == ErrCodes.WRAPPER_SPECIFIC;
			}
		}

		sql.append(colFound ? ") order by TABLE_SCHEM, TABLE_NAME, ORDINAL_POSITION" :
				"SELECT NULL AS cat, NULL AS tbl, NULL AS ordpos, NULL AS colnullable, NULL AS ct, "
						+ "NULL AS cn, NULL AS tn, NULL AS cdflt) limit 0");
		final PreparedStatement columns = c.prepareStatement(sql.toString());
		columns.closeOnCompletion();
		return columns.executeQuery();
	}

	private List<String[]> getExactTableNames(String[] catalogs, String tableNamePattern) throws SQLException {
		tableNamePattern = tableNamePattern == null || tableNamePattern.isEmpty() ? "%" : tableNamePattern;
		final List<String[]> tbls = new ArrayList<>();
		for (String catalog : catalogs) {
			final String sql;
			if ("main".equalsIgnoreCase(catalog)) {
				sql = "SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name LIKE ? UNION SELECT 'sqlite_master' WHERE 'sqlite_master' LIKE ?";
			} else if ("temp".equalsIgnoreCase(catalog)) {
				sql = "SELECT name FROM sqlite_temp_master WHERE type IN ('table','view') AND name LIKE ? UNION SELECT 'sqlite_temp_master' WHERE 'sqlite_temp_master' LIKE ?";
			} else {
				sql = "SELECT name FROM \"" + escapeIdentifier(catalog) + "\".sqlite_master WHERE type IN ('table','view') AND name LIKE ? UNION SELECT 'sqlite_master' WHERE 'sqlite_master' LIKE ?";
			}
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				// determine exact table name
				ps.setString(1, tableNamePattern);
				ps.setString(2, tableNamePattern);
				try (ResultSet rs = ps.executeQuery()){
					while (rs.next()) {
						tbls.add(new String[]{catalog, rs.getString(1)});
					}
				}
			}
		}
		return tbls;
	}

	private static String getSQLiteType(String colType) {
		return colType == null ? "" : colType.toUpperCase();
	}

	// TODO Validate affinity vs java type
	private static int getJavaType(String colType) {
		return getJavaType(SQLite.getAffinity(colType));
	}
	public static int getJavaType(int affinity) {
		switch (affinity) {
			case ColAffinities.TEXT:
				return Types.VARCHAR;
			case ColAffinities.INTEGER:
				return Types.INTEGER;
			case ColAffinities.REAL:
				return Types.REAL;
			case ColAffinities.NONE:
				return Types.OTHER;
			default:
				return Types.NUMERIC;
		}
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TABLE_CAT, "
						+ "null as TABLE_SCHEM, "
						+ "null as TABLE_NAME, "
						+ "null as COLUMN_NAME, "
						+ "null as GRANTOR, "
						+ "null as GRANTEE, "
						+ "null as PRIVILEGE, "
						+ "null as IS_GRANTABLE limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TABLE_CAT, "
						+ "null as TABLE_SCHEM, "
						+ "null as TABLE_NAME, "
						+ "null as GRANTOR, "
						+ "null as GRANTEE, "
						+ "null as PRIVILEGE, "
						+ "null as IS_GRANTABLE limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
		checkOpen();
		final StringBuilder sql = new StringBuilder();

		catalog = schemaProvider.getDbName(catalog, table);
		sql.append("select ").
				append(scope).append(" as SCOPE, ").
				append("cn as COLUMN_NAME, ").
				append("ct as DATA_TYPE, ").
				append("tn as TYPE_NAME, ").
				append("10 as COLUMN_SIZE, "). // FIXME precision (19 for LONG, 15 for REAL) or display size (20 for LONG, 25 for REAL)
				append("0 as BUFFER_LENGTH, ").
				append("0 as DECIMAL_DIGITS, "). // FIXME scale (0 for LONG, 15 for REAL)
				append("pc as PSEUDO_COLUMN from (");

		// Pragma cannot be used as subquery...
		int count = -1;
		String colName = null;
		String colType = null;
		Pragma pragma = new Pragma(new QualifiedName(catalog, "table_info"), new IdExpr(table));
		try (PreparedStatement table_info = c.prepareStatement(pragma.toSql());
				 ResultSet rs = table_info.executeQuery()
		) {
			// 1:cid|2:name|3:type|4:notnull|5:dflt_value|6:pk
			while (count < 2 && rs.next()) {
				if (count < 0) {
					count = 0; // table exists
				}
				if (rs.getBoolean(6) && (nullable || rs.getBoolean(4))) {
					colName = rs.getString(2);
					colType = getSQLiteType(rs.getString(3));
					count++;
				}
			}
		} catch (StmtException e) { // query does not return ResultSet
			assert e.getErrorCode() == ErrCodes.WRAPPER_SPECIFIC;
			count = -1;
		}

		if (count == 1) {
			sql.append("SELECT ").
					append(quote(colName)).append(" as cn, ").
					append(getJavaType(colType)).append(" as ct, ").
					append("'").append(colType).append("' as tn, ").
					append(bestRowNotPseudo).append(" as pc) order by SCOPE");
		} else {
			sql.append("SELECT ").
					append("'ROWID' AS cn, ").
					append(Types.INTEGER).append(" AS ct, ").
					append("'INTEGER' AS tn, ").
					append(bestRowPseudo).append(" AS pc) order by SCOPE");
			if (count < 0) {
				sql.append(" limit 0");
			}
		}
		final PreparedStatement columns = c.prepareStatement(sql.toString());
		columns.closeOnCompletion();
		return columns.executeQuery();
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as SCOPE, "
						+ "null as COLUMN_NAME, "
						+ "null as DATA_TYPE, "
						+ "null as TYPE_NAME, "
						+ "null as COLUMN_SIZE, "
						+ "null as BUFFER_LENGTH, "
						+ "null as DECIMAL_DIGITS, "
						+ "null as PSEUDO_COLUMN limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		checkOpen();
		Select select = EnhancedPragma.getPrimaryKeys(catalog, table, schemaProvider);
		final PreparedStatement columns = c.prepareStatement(select.toSql());
		columns.closeOnCompletion();
		return columns.executeQuery();
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		checkOpen();
		Select select = EnhancedPragma.getImportedKeys(catalog, table, schemaProvider);
		final PreparedStatement fks = c.prepareStatement(select.toSql());
		fks.closeOnCompletion();
		return fks.executeQuery();
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		checkOpen();
		final StringBuilder sql = new StringBuilder();

		catalog = schemaProvider.getDbName(catalog, table);
		sql.append("select ").
				append(quote(catalog)).append(" as PKTABLE_CAT, ").
				append("null as PKTABLE_SCHEM, ").
				append(quote(table)).append(" as PKTABLE_NAME, ").
				append("pc as PKCOLUMN_NAME, ").
				append(quote(catalog)).append(" as FKTABLE_CAT, ").
				append("null as FKTABLE_SCHEM, ").
				append("ft as FKTABLE_NAME, ").
				append("fc as FKCOLUMN_NAME, ").
				append("seq as KEY_SEQ, ").
				append(importedKeyNoAction).append(" as UPDATE_RULE, "). // FIXME on_update (6) NO ACTION, CASCADE
				append(importedKeyNoAction).append(" as DELETE_RULE, "). // FIXME on_delete (7) NO ACTION, CASCADE
				append("fk as FK_NAME, ").
				append("null as PK_NAME, ").
				append(importedKeyNotDeferrable).append(" as DEFERRABILITY "). // FIXME
				append("from (");

		final Collection<String> fkTables = new ArrayList<>();
		final String s;
		if ("main".equalsIgnoreCase(catalog)) {
			s = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE ? AND sql LIKE ?";
		} else if ("temp".equalsIgnoreCase(catalog)) {
			s = "SELECT name FROM sqlite_temp_master WHERE type = 'table' AND name NOT LIKE ? AND sql LIKE ?";
		} else {
			s = "SELECT name FROM \"" + escapeIdentifier(catalog) + "\".sqlite_master WHERE type = 'table' AND name NOT LIKE ? AND sql LIKE ?";
		}

		try (PreparedStatement fks = c.prepareStatement(s)) {
			fks.setString(1, table);
			fks.setString(2, "%REFERENCES%" + table + "%(%");
			try (ResultSet rs = fks.executeQuery()){
				while (rs.next()) {
					fkTables.add(rs.getString(1));
				}
			}
		}

		int count = 0;
		if (!fkTables.isEmpty()) {
			for (String fkTable : fkTables) {
				Pragma pragma = new Pragma(new QualifiedName(catalog, "foreign_key_list"), new IdExpr(fkTable));
				// Pragma cannot be used as subquery...
				try (PreparedStatement foreign_key_list = c.prepareStatement(pragma.toSql());
						 ResultSet rs = foreign_key_list.executeQuery()) {
					// 1:id|2:seq|3:table|4:from|5:to|6:on_update|7:on_delete|8:match
					while (rs.next()) {
						if (!rs.getString(3).equalsIgnoreCase(table)) {
							continue;
						}
						if (count > 0) {
							sql.append(" UNION ALL ");
						}
						sql.append("SELECT ").
								append(quote(fkTable + '_' + table + '_' + rs.getString(1))).append(" AS fk, "). // to be kept in sync with getForeignKeys
								append(quote(fkTable)).append(" AS ft, ").
								append(quote(rs.getString(5))).append(" AS pc, ").
								append(quote(rs.getString(4))).append(" AS fc, ").
								append(rs.getShort(2) + 1).append(" AS seq");
						count++;
					}
				} catch (StmtException e) { // query does not return ResultSet
					assert e.getErrorCode() == ErrCodes.WRAPPER_SPECIFIC;
				}
			}
		}

		if (count == 0) {
			sql.append("SELECT NULL AS fk, NULL AS ft, NULL AS pc, NULL AS fc, NULL AS seq) limit 0");
		} else {
			sql.append(") order by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, KEY_SEQ");
		}
		final PreparedStatement eks = c.prepareStatement(sql.toString());
		eks.closeOnCompletion();
		return eks.executeQuery();
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "tn as TYPE_NAME, "
						+ "dt as DATA_TYPE, "
						+ "0 as PRECISION, "
						+ "null as LITERAL_PREFIX, "
						+ "null as LITERAL_SUFFIX, "
						+ "null as CREATE_PARAMS, "
						+ typeNullable + " as NULLABLE, "
						+ "1 as CASE_SENSITIVE, "
						+ typeSearchable + " as SEARCHABLE, "
						+ "0 as UNSIGNED_ATTRIBUTE, "
						+ "0 as FIXED_PREC_SCALE, "
						+ "0 as AUTO_INCREMENT, "
						+ "null as LOCAL_TYPE_NAME, "
						+ "0 as MINIMUM_SCALE, "
						+ "0 as MAXIMUM_SCALE, "
						+ "0 as SQL_DATA_TYPE, "
						+ "0 as SQL_DATETIME_SUB, "
						+ "10 as NUM_PREC_RADIX from ("
						+ "    select 'BLOB' as tn, " + Types.OTHER + " as dt union"
						+ "    select 'NULL' as tn, " + Types.NULL + " as dt union"
						+ "    select 'REAL' as tn, " + Types.REAL + " as dt union"
						+ "    select 'TEXT' as tn, " + Types.VARCHAR + " as dt union"
						+ "    select 'INTEGER' as tn, " + Types.INTEGER + " as dt"
						+ ") order by DATA_TYPE, TYPE_NAME"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
		checkOpen();
		Select select = EnhancedPragma.getCrossReference(parentCatalog, parentTable, foreignCatalog, foreignTable, schemaProvider);
		final PreparedStatement fks = c.prepareStatement(select.toSql());
		fks.closeOnCompletion();
		return fks.executeQuery();
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
		checkOpen();
		catalog = schemaProvider.getDbName(catalog, table);
		final StringBuilder sql = new StringBuilder();
		sql.append("select ").
				append(quote(catalog)).append(" as TABLE_CAT, ").
				append("null as TABLE_SCHEM, ").
				append(quote(table)).append(" as TABLE_NAME, ").
				append("nu as NON_UNIQUE, ").
				append(quote(catalog)).append(" as INDEX_QUALIFIER, ").
				append("idx as INDEX_NAME, ").
				append(tableIndexOther).append(" as TYPE, ").
				append("seqno as ORDINAL_POSITION, ").
				append("cn as COLUMN_NAME, ").
				append("null as ASC_OR_DESC, ").
				append("0 as CARDINALITY, ").
				append("0 as PAGES, ").
				append("null as FILTER_CONDITION ").
				append("from (");

		final Map<String, Boolean> indexes = new HashMap<>();
		Pragma pragma = new Pragma(new QualifiedName(catalog, "index_list"), new IdExpr(table));
		try (PreparedStatement index_list = c.prepareStatement(pragma.toSql());
				 ResultSet rs = index_list.executeQuery()) {
			// 1:seq|2:name|3:unique
			while (rs.next()) {
				final boolean notuniq = !rs.getBoolean(3);
				if (unique && notuniq) {
					continue;
				}
				indexes.put(rs.getString(2), notuniq);
			}
		} catch (StmtException e) { // query does not return ResultSet
			assert e.getErrorCode() == ErrCodes.WRAPPER_SPECIFIC;
		}

		if (indexes.isEmpty()) {
			sql.append("SELECT NULL AS nu, NULL AS idx, NULL AS seqno, NULL AS cn) limit 0");
		} else {
			boolean found = false;
			for (final Entry<String, Boolean> index : indexes.entrySet()) {
				Pragma _pragma = new Pragma(new QualifiedName(null, "index_info"), new IdExpr(index.getKey()));
				try (PreparedStatement index_info = c.prepareStatement(_pragma.toSql());
						 ResultSet rs = index_info.executeQuery()) {
					// 1:seqno|2:cid|3:name
					while (rs.next()) {
						if (found) {
							sql.append(" UNION ALL ");
						}
						sql.append("SELECT ").
								append(index.getValue() ? 1 : 0).append(" AS nu, ").
								append(quote(index.getKey())).append(" AS idx, ").
								append(rs.getInt(1)).append(" AS seqno, ").
								append(quote(rs.getString(3))).append(" AS cn");
						found = true;
					}
				//} catch(StmtException e) { // query does not return ResultSet
				}
			}
			if (found) {
				sql.append(") order by NON_UNIQUE, TYPE, INDEX_NAME, ORDINAL_POSITION");
			} else {
				sql.append("SELECT NULL AS nu, NULL AS idx, NULL AS seqno, NULL AS cn) limit 0");
			}
		}

		final PreparedStatement idx = c.prepareStatement(sql.toString());
		idx.closeOnCompletion();
		return idx.executeQuery();
	}

	/** @see Stmt#getResultSetType()
	 * @see Rows#getType() */
	@Override
	public boolean supportsResultSetType(int type) {
		return type == ResultSet.TYPE_FORWARD_ONLY;
	}

	/** @see Stmt#getResultSetConcurrency()
	 * @see Rows#getConcurrency() */
	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency) {
		return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) {
		return false;
	}

	@Override
	public boolean ownDeletesAreVisible(int type) {
		return false;
	}

	@Override
	public boolean ownInsertsAreVisible(int type) {
		return false;
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) {
		return false;
	}

	@Override
	public boolean othersDeletesAreVisible(int type) {
		return false;
	}

	@Override
	public boolean othersInsertsAreVisible(int type) {
		return false;
	}

	@Override
	public boolean updatesAreDetected(int type) {
		return false;
	}

	@Override
	public boolean deletesAreDetected(int type) {
		return false;
	}

	@Override
	public boolean insertsAreDetected(int type) {
		return false;
	}

	/** @see Stmt#executeBatch() */
	@Override
	public boolean supportsBatchUpdates() {
		return true;
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TYPE_CAT, "
						+ "null as TYPE_SCHEM, "
						+ "null as TYPE_NAME, "
						+ "null as CLASS_NAME, "
						+ "null as DATA_TYPE, "
						+ "null as REMARKS, "
						+ "null as BASE_TYPE "
						+ "limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public Connection getConnection() {
		return c;
	}

	/** @see Conn#setSavepoint() */
	@Override
	public boolean supportsSavepoints() {
		return true;
	}

	@Override
	public boolean supportsNamedParameters() { // but no callable statement...
		return true;
	}

	@Override
	public boolean supportsMultipleOpenResults() {
		return false;
	}

	/** @see Stmt#getGeneratedKeys() */
	@Override
	public boolean supportsGetGeneratedKeys() { // Used by Hibernate
		return true; // partial support only
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TYPE_CAT, "
						+ "null as TYPE_SCHEM, "
						+ "null as TYPE_NAME, "
						+ "null as SUPERTYPE_CAT, "
						+ "null as SUPERTYPE_SCHEM, "
						+ "null as SUPERTYPE_NAME limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TABLE_CAT, "
						+ "null as TABLE_SCHEM, "
						+ "null as TABLE_NAME, "
						+ "null as SUPERTABLE_NAME limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as TYPE_CAT, "
						+ "null as TYPE_SCHEM, "
						+ "null as TYPE_NAME, "
						+ "null as ATTR_NAME, "
						+ "null as DATA_TYPE, "
						+ "null as ATTR_TYPE_NAME, "
						+ "null as ATTR_SIZE, "
						+ "null as DECIMAL_DIGITS, "
						+ "null as NUM_PREC_RADIX, "
						+ "null as NULLABLE, "
						+ "null as REMARKS, "
						+ "null as ATTR_DEF, "
						+ "null as SQL_DATA_TYPE, "
						+ "null as SQL_DATETIME_SUB, "
						+ "null as CHAR_OCTET_LENGTH, "
						+ "null as ORDINAL_POSITION, "
						+ "null as IS_NULLABLE, "
						+ "null as SCOPE_CATALOG, "
						+ "null as SCOPE_SCHEMA, "
						+ "null as SCOPE_TABLE, "
						+ "null as SOURCE_DATA_TYPE limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	/** @see Rows#getHoldability()
	 * @see Stmt#getResultSetHoldability()
	 * @see #getResultSetHoldability()
	 * @see Conn#getHoldability() */
	@Override
	public boolean supportsResultSetHoldability(int holdability) {
		return holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT; // TODO Validate sqlite3_reset & lock
	}

	@Override
	public int getResultSetHoldability() {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT; // TODO Validate sqlite3_reset & lock
	}

	@Override
	public int getDatabaseMajorVersion() {
		return 3; // FIXME
	}

	@Override
	public int getDatabaseMinorVersion() {
		return 0; // FIXME
	}

	@Override
	public int getJDBCMajorVersion() {
		return 4;
	}

	@Override
	public int getJDBCMinorVersion() {
		return 1;
	}

	@Override
	public int getSQLStateType() {
		return sqlStateSQL99;
	}

	@Override
	public boolean locatorsUpdateCopy() {
		return false;
	}

	/** @see Stmt#setPoolable(boolean) */
	@Override
	public boolean supportsStatementPooling() {
		return true; // Statement cache
	}

	@Override
	public RowIdLifetime getRowIdLifetime() {
		Util.trace("DatabaseMetaData.getRowIdLifetime");
		return RowIdLifetime.ROWID_VALID_FOREVER; // TODO http://www.sqlite.org/autoinc.html
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return getSchemas();
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() {
		return false;
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() {
		return false;
	}

	/** @see Conn#getClientInfo() */
	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		// TODO http://sqlite.org/pragma.html#pragma_application_id
		// http://sqlite.org/pragma.html#pragma_user_version
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement(
				"select "
						+ "null as NAME, "
						+ "0 as MAX_LEN, "
						+ "null as DEFAULT_VALUE, "
						+ "null as DESCRIPTION "
						+ "limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) {
		Util.trace("DatabaseMetaData.getFunctions");
		return null;
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) {
		Util.trace("DatabaseMetaData.getFunctionColumns");
		return null;
	}

	//#if mvn.project.property.jdbc.specification.version >= "4.1"
	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		checkOpen();
		final PreparedStatement stmt = c.prepareStatement( // TODO rowId?
				"select "
						+ "null as TABLE_CAT, "
						+ "null as TABLE_SCHEM, "
						+ "null as TABLE_NAME, "
						+ "null as COLUMN_NAME, "
						+ "null as DATA_TYPE, "
						+ "null as COLUMN_SIZE, "
						+ "null as DECIMAL_DIGITS, "
						+ "null as NUM_PREC_RADIX, "
						+ "null as COLUMN_USAGE, "
						+ "null as REMARKS, "
						+ "null as CHAR_OCTET_LENGTH, "
						+ "null as IS_NULLABLE limit 0"
		);
		stmt.closeOnCompletion();
		return stmt.executeQuery();
	}

	@Override
	public boolean generatedKeyAlwaysReturned() {
		return false;
	}
	//#endif

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

	//#if mvn.project.property.jdbc.specification.version >= "4.2"
	@Override
	public long getMaxLogicalLobSize() {
		return Integer.MAX_VALUE;
	}
	//#endif

	private static String quote(String data) {
		if (data == null) {
			return "NULL";
		}
		if (data.indexOf('\'') >= 0) { // escape quote by doubling them
			data = data.replaceAll("'", "''");
		}
		return '\'' + data + '\'';
	}
}
