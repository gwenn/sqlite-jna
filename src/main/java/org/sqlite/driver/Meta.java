package org.sqlite.driver;

import java.sql.*;

public class Meta implements DatabaseMetaData {
  private Conn c;

  private PreparedStatement findTableByNamePattern;

  public Meta(Conn c) {
    this.c = c;
  }

  private void checkOpen() throws SQLException {
    if (c == null) throw new SQLException("connection closed");
  }

  private org.sqlite.Conn getConn() throws SQLException {
    checkOpen();
    return c.getConn();
  }

  void close() throws SQLException {
    if (c == null) return;
    if (findTableByNamePattern != null) findTableByNamePattern.close();
    c = null;
  }

  @Override
  public boolean allProceduresAreCallable() throws SQLException {
    return false;
  }
  @Override
  public boolean allTablesAreSelectable() throws SQLException {
    return true;
  }
  @Override
  public String getURL() throws SQLException {
    return getConn().getFilename();
  }
  @Override
  public String getUserName() throws SQLException {
    return null;
  }
  @Override
  public boolean isReadOnly() throws SQLException {
    return getConn().isReadOnly();
  }
  @Override
  public boolean nullsAreSortedHigh() throws SQLException {
    return false;
  }
  @Override
  public boolean nullsAreSortedLow() throws SQLException {
    return true;
  }
  @Override
  public boolean nullsAreSortedAtStart() throws SQLException {
    return false;
  }
  @Override
  public boolean nullsAreSortedAtEnd() throws SQLException {
    return false;
  }
  @Override
  public String getDatabaseProductName() throws SQLException {
    return "SQLite 3";
  }
  @Override
  public String getDatabaseProductVersion() throws SQLException {
    return org.sqlite.Conn.libversion();
  }
  @Override
  public String getDriverName() throws SQLException {
    return "SQLiteJNA";
  }
  @Override
  public String getDriverVersion() throws SQLException {
    return "1.0"; // FIXME
  }
  @Override
  public int getDriverMajorVersion() {
    return 1; // FIXME Keep in sync with Driver
  }
  @Override
  public int getDriverMinorVersion() {
    return 0; // FIXME Keep in sync with Driver
  }
  @Override
  public boolean usesLocalFiles() throws SQLException {
    return true;
  }
  @Override
  public boolean usesLocalFilePerTable() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsMixedCaseIdentifiers() throws SQLException {
    return true;
  }
  @Override
  public boolean storesUpperCaseIdentifiers() throws SQLException {
    return false;
  }
  @Override
  public boolean storesLowerCaseIdentifiers() throws SQLException {
    return false;
  }
  @Override
  public boolean storesMixedCaseIdentifiers() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
    return true;
  }
  @Override
  public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
    return false;
  }
  @Override
  public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
    return false;
  }
  @Override
  public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
    return true;
  }
  @Override
  public String getIdentifierQuoteString() throws SQLException {
    Util.trace("DatabaseMetaData.getIdentifierQuoteString");
    return "\""; // TODO Validate
  }
  @Override
  public String getSQLKeywords() throws SQLException {
    return ""; // TODO Validate
  }
  @Override
  public String getNumericFunctions() throws SQLException {
    return "abs,max,min,round,random";
  }
  @Override
  public String getStringFunctions() throws SQLException {
    return "glob,length,like,lower,ltrim,replace,rtrim,soundex,substr,trim,upper";
  }
  @Override
  public String getSystemFunctions() throws SQLException {
    return "last_insert_rowid,load_extension,sqlite_version";
  }
  @Override
  public String getTimeDateFunctions() throws SQLException {
    return "date,time,datetime,julianday,strftime";
  }
  @Override
  public String getSearchStringEscape() throws SQLException {
    Util.trace("DatabaseMetaData.getSearchStringEscape");
    return null; // TODO Validate
  }
  @Override
  public String getExtraNameCharacters() throws SQLException {
    return "";
  }
  @Override
  public boolean supportsAlterTableWithAddColumn() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsAlterTableWithDropColumn() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsColumnAliasing() throws SQLException {
    return true;
  }
  @Override
  public boolean nullPlusNonNullIsNull() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsConvert() throws SQLException {
    Util.trace("DatabaseMetaData.supportsConvert");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsConvert(int fromType, int toType) throws SQLException {
    Util.trace("DatabaseMetaData.supportsConvert");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsTableCorrelationNames() throws SQLException {
    Util.trace("DatabaseMetaData.supportsTableCorrelationNames");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsDifferentTableCorrelationNames() throws SQLException {
    Util.trace("DatabaseMetaData.supportsDifferentTableCorrelationNames");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsExpressionsInOrderBy() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsOrderByUnrelated() throws SQLException {
    return false; // TODO Validate
  }
  @Override
  public boolean supportsGroupBy() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsGroupByUnrelated() throws SQLException {
    return false; // TODO Validate
  }
  @Override
  public boolean supportsGroupByBeyondSelect() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsLikeEscapeClause() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsMultipleResultSets() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsMultipleTransactions() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsNonNullableColumns() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsMinimumSQLGrammar() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsCoreSQLGrammar() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsExtendedSQLGrammar() throws SQLException {
    Util.trace("DatabaseMetaData.supportsExtendedSQLGrammar");
    return false;
  }
  @Override
  public boolean supportsANSI92EntryLevelSQL() throws SQLException {
    Util.trace("DatabaseMetaData.supportsANSI92EntryLevelSQL");
    return false;
  }
  @Override
  public boolean supportsANSI92IntermediateSQL() throws SQLException {
    Util.trace("DatabaseMetaData.supportsANSI92IntermediateSQL");
    return false;
  }
  @Override
  public boolean supportsANSI92FullSQL() throws SQLException {
    Util.trace("DatabaseMetaData.supportsANSI92FullSQL");
    return false;
  }
  @Override
  public boolean supportsIntegrityEnhancementFacility() throws SQLException {
    return false; // TODO Validate
  }
  @Override
  public boolean supportsOuterJoins() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsFullOuterJoins() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsLimitedOuterJoins() throws SQLException {
    return true;
  }
  @Override
  public String getSchemaTerm() throws SQLException {
    return null; // TODO
  }
  @Override
  public String getProcedureTerm() throws SQLException {
    return null;
  }
  @Override
  public String getCatalogTerm() throws SQLException {
    return null;
  }
  @Override
  public boolean isCatalogAtStart() throws SQLException {
    return true;
  }
  @Override
  public String getCatalogSeparator() throws SQLException {
    return ".";
  }
  @Override
  public boolean supportsSchemasInDataManipulation() throws SQLException {
    return false; // TODO
  }
  @Override
  public boolean supportsSchemasInProcedureCalls() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsSchemasInTableDefinitions() throws SQLException {
    return false; // TODO
  }
  @Override
  public boolean supportsSchemasInIndexDefinitions() throws SQLException {
    return false; // TODO
  }
  @Override
  public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsCatalogsInDataManipulation() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsCatalogsInProcedureCalls() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsCatalogsInTableDefinitions() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsPositionedDelete() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsPositionedUpdate() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsSelectForUpdate() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsStoredProcedures() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsSubqueriesInComparisons() throws SQLException {
    Util.trace("DatabaseMetaData.supportsSubqueriesInComparisons");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsSubqueriesInExists() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsSubqueriesInIns() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsSubqueriesInQuantifieds() throws SQLException {
    Util.trace("DatabaseMetaData.supportsSubqueriesInQuantifieds");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsCorrelatedSubqueries() throws SQLException {
    Util.trace("DatabaseMetaData.supportsCorrelatedSubqueries");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsUnion() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsUnionAll() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
    Util.trace("DatabaseMetaData.supportsOpenCursorsAcrossCommit");
    return false;
  }
  @Override
  public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
    Util.trace("DatabaseMetaData.supportsOpenCursorsAcrossRollback");
    return false;
  }
  @Override
  public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
    Util.trace("DatabaseMetaData.supportsOpenStatementsAcrossCommit");
    return false; // TODO Validate
  }
  @Override
  public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
    Util.trace("DatabaseMetaData.supportsOpenStatementsAcrossRollback");
    return false; // TODO Validate
  }
  @Override
  public int getMaxBinaryLiteralLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxCharLiteralLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxColumnNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxColumnsInGroupBy() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxColumnsInIndex() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxColumnsInOrderBy() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxColumnsInSelect() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxColumnsInTable() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxConnections() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxCursorNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxIndexLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxSchemaNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxProcedureNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxCatalogNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxRowSize() throws SQLException {
    return 0;
  }
  @Override
  public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
    return false;
  }
  @Override
  public int getMaxStatementLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxStatements() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxTableNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxTablesInSelect() throws SQLException {
    return 0;
  }
  @Override
  public int getMaxUserNameLength() throws SQLException {
    return 0;
  }
  @Override
  public int getDefaultTransactionIsolation() throws SQLException {
    return Connection.TRANSACTION_SERIALIZABLE;
  }
  @Override
  public boolean supportsTransactions() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
    return level == Connection.TRANSACTION_SERIALIZABLE; // TODO TRANSACTION_READ_UNCOMMITTED
  }
  @Override
  public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
    return true; // tested
  }
  @Override
  public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
    return false;
  }
  @Override
  public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
    return false; // tested
  }
  @Override
  public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
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
      + "null as PROCEDURE_TYPE limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
    Util.trace("DatabaseMetaData.getProcedureColumns");
    return null;
  }
  @Override
  public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
    checkOpen();
    tableNamePattern = (tableNamePattern == null || "".equals(tableNamePattern)) ? "%" : tableNamePattern;

    final StringBuilder sql = new StringBuilder().append("select").
        append(" null as TABLE_CAT,").
        append(" null as TABLE_SCHEM,"). // TODO
        append(" name as TABLE_NAME,").
        append(" upper(type) as TABLE_TYPE,").
        append(" null as REMARKS,").
        append(" null as TYPE_CAT,").
        append(" null as TYPE_SCHEM,").
        append(" null as TYPE_NAME,").
        append(" null as SELF_REFERENCING_COL_NAME,").
        append(" null as REF_GENERATION").
        append(" from (select name, type from sqlite_master union all").
        append("       select name, type from sqlite_temp_master)").
        append(" where TABLE_NAME like ").append(quote(tableNamePattern));

    if (types != null) {
      sql.append(" and TABLE_TYPE in (");
      for (int i = 0; i < types.length; i++) {
        if (i > 0) sql.append(", ");
        sql.append("'").append(types[i].toUpperCase()).append("'");
      }
      sql.append(")");
    } else {
      sql.append(" and TABLE_TYPE in ('TABLE', 'VIEW')");
    }

    sql.append(" order by TABLE_TYPE, TABLE_SCHEM, TABLE_NAME");

    final PreparedStatement stmt = c.prepareStatement(sql.toString());
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getSchemas() throws SQLException { // TODO main, temp, attached dbs
    checkOpen();
    final PreparedStatement stmt = c.prepareStatement(
        "select "
      + "null as TABLE_SCHEM, "
      + "null as TABLE_CATALOG "
      + "limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getCatalogs() throws SQLException {
    checkOpen();
    final PreparedStatement stmt = c.prepareStatement(
      "select null as TABLE_CAT limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getTableTypes() throws SQLException {
    checkOpen();
    final PreparedStatement stmt = c.prepareStatement("select 'TABLE' as TABLE_TYPE " +
      "union select 'VIEW' as TABLE_TYPE");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  // TODO Support multi tables
  @Override
  public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
    checkOpen();
    ResultSet rs;
    final StringBuilder sql = new StringBuilder();

    checkOpen();

    if (findTableByNamePattern == null)
      findTableByNamePattern = c.prepareStatement(
          "select tbl_name from sqlite_master where tbl_name like ?");

    // determine exact table name
    findTableByNamePattern.setString(1, tableNamePattern);
    rs = findTableByNamePattern.executeQuery();
    final String tbl;
    if (rs.next()) {
      tbl = rs.getString(1);
    } else {
      tbl = null;
    }
    rs.close();

    sql.append("select ").
        append("null as TABLE_CAT, ").
        append("null as TABLE_SCHEM, ").
        append(quote(tbl)).append(" as TABLE_NAME, ").
        append("cn as COLUMN_NAME, ").
        append("ct as DATA_TYPE, ").
        append("tn as TYPE_NAME, ").
        append("10 as COLUMN_SIZE, "). // FIXME
        append("10 as BUFFER_LENGTH, ").
        append("10 as DECIMAL_DIGITS, ").
        append("10 as NUM_PREC_RADIX, ").
        append("colnullable as NULLABLE, ").
        append("null as REMARKS, ").
        append("null as COLUMN_DEF, "). // TODO
        append("0 as SQL_DATA_TYPE, ").
        append("0 as SQL_DATETIME_SUB, ").
        append("10 as CHAR_OCTET_LENGTH, "). // FIXME
        append("ordpos as ORDINAL_POSITION, ").
        append("(case colnullable when 0 then 'N' when 1 then 'Y' else '' end)").
        append(" as IS_NULLABLE, ").
        append("null as SCOPE_CATLOG, ").
        append("null as SCOPE_SCHEMA, ").
        append("null as SCOPE_TABLE, ").
        append("null as SOURCE_DATA_TYPE from (");

    boolean colFound = false;
    if (tbl != null) {
      // the command "pragma table_info('tablename')" does not embed
      // like a normal select statement so we must extract the information
      // and then build a resultset from unioned select statements
      final PreparedStatement table_info = c.prepareStatement("pragma table_info (" + quote(tbl) + ")");
      table_info.closeOnCompletion();
      rs = table_info.executeQuery();

      for (int i = 0; rs.next(); i++) {
        String colName = rs.getString(2);
        String colType = rs.getString(3);
        String colNotNull = rs.getString(4);

        int colNullable = 2;
        if (colNotNull != null) colNullable = colNotNull.equals("0") ? 1 : 0;
        if (colFound) sql.append(" union all ");
        colFound = true;

        colType = getSQLiteType(colType);
        int colJavaType = getJavaType(colType);

        sql.append("select ").
            append(i).append(" as ordpos, ").
            append(colNullable).append(" as colnullable, ").
            append(colJavaType).append(" as ct, ").
            append(quote(colName)).append(" as cn, ").
            append(quote(colType)).append(" as tn");

        if (columnNamePattern != null)
          sql.append(" where cn like ").append(quote(columnNamePattern));
      }
      rs.close();
    }

    sql.append(colFound ? ") order by TABLE_SCHEM, TABLE_NAME, ORDINAL_POSITION" :
        "select null as ordpos, null as colnullable, null as ct, "
            + "null as cn, null as tn) limit 0");
    final PreparedStatement columns = c.prepareStatement(sql.toString());
    columns.closeOnCompletion();
    return columns.executeQuery();
  }

  private String getSQLiteType(String colType) {
    return colType == null ? "TEXT" : colType.toUpperCase();
  }

  private static int getJavaType(String colType) { // FIXME http://sqlite.org/datatype3.html
    final int colJavaType;
    if ("INTEGER".equals(colType))
      colJavaType = Types.INTEGER;
    else if ("TEXT".equals(colType))
      colJavaType = Types.VARCHAR;
    else if ("FLOAT".equals(colType))
      colJavaType = Types.REAL;
    else
      colJavaType = Types.VARCHAR;
    return colJavaType;
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
      + "null as IS_GRANTABLE limit 0");
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
      + "null as IS_GRANTABLE limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
    Util.trace("DatabaseMetaData.getBestRowIdentifier");
    return null;
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
      + "null as PSEUDO_COLUMN limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
    Util.trace("DatabaseMetaData.getPrimaryKeys");
    return null;
  }
  @Override
  public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
    Util.trace("DatabaseMetaData.getImportedKeys");
    return null;
  }
  @Override
  public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
    Util.trace("DatabaseMetaData.getExportedKeys");
    return null;
  }
  @Override
  public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
    Util.trace("DatabaseMetaData.getCrossReference");
    return null;
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
      + "    select 'BLOB' as tn, " + Types.BLOB + " as dt union"
      + "    select 'NULL' as tn, " + Types.NULL + " as dt union"
      + "    select 'REAL' as tn, " + Types.REAL+ " as dt union"
      + "    select 'TEXT' as tn, " + Types.VARCHAR + " as dt union"
      + "    select 'INTEGER' as tn, "+ Types.INTEGER +" as dt"
      + ") order by DATA_TYPE, TYPE_NAME");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
    Util.trace("DatabaseMetaData.getIndexInfo");
    return null;
  }
  @Override
  public boolean supportsResultSetType(int type) throws SQLException {
    return type == ResultSet.TYPE_FORWARD_ONLY;
  }
  @Override
  public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
    return type == ResultSet.TYPE_FORWARD_ONLY && concurrency == ResultSet.CONCUR_READ_ONLY;
  }
  @Override
  public boolean ownUpdatesAreVisible(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean ownDeletesAreVisible(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean ownInsertsAreVisible(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean othersUpdatesAreVisible(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean othersDeletesAreVisible(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean othersInsertsAreVisible(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean updatesAreDetected(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean deletesAreDetected(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean insertsAreDetected(int type) throws SQLException {
    return false;
  }
  @Override
  public boolean supportsBatchUpdates() throws SQLException {
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
      + "limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public Connection getConnection() throws SQLException {
    return c;
  }
  @Override
  public boolean supportsSavepoints() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsNamedParameters() throws SQLException {
    return true;
  }
  @Override
  public boolean supportsMultipleOpenResults() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsGetGeneratedKeys() throws SQLException { // Used by Hibernate
    return true;
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
      + "null as SUPERTYPE_NAME limit 0");
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
      + "null as SUPERTABLE_NAME limit 0");
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
      + "null as SOURCE_DATA_TYPE limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public boolean supportsResultSetHoldability(int holdability) throws SQLException {
    return holdability == ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }
  @Override
  public int getResultSetHoldability() throws SQLException {
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }
  @Override
  public int getDatabaseMajorVersion() throws SQLException {
    return 3; // FIXME
  }
  @Override
  public int getDatabaseMinorVersion() throws SQLException {
    return 0; // FIXME
  }
  @Override
  public int getJDBCMajorVersion() throws SQLException {
    return 4;
  }
  @Override
  public int getJDBCMinorVersion() throws SQLException {
    return 1;
  }
  @Override
  public int getSQLStateType() throws SQLException {
    return sqlStateSQL99;
  }
  @Override
  public boolean locatorsUpdateCopy() throws SQLException {
    return false;
  }
  @Override
  public boolean supportsStatementPooling() throws SQLException {
    return false;
  }
  @Override
  public RowIdLifetime getRowIdLifetime() throws SQLException {
    Util.trace("DatabaseMetaData.getRowIdLifetime");
    return RowIdLifetime.ROWID_UNSUPPORTED; // TODO
  }
  @Override
  public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
    return getSchemas(); // TODO
  }
  @Override
  public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
    return false;
  }
  @Override
  public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
    return false;
  }
  @Override
  public ResultSet getClientInfoProperties() throws SQLException { // TODO
    checkOpen();
    final PreparedStatement stmt = c.prepareStatement(
        "select "
      + "null as NAME, "
      + "0 as MAX_LEN, "
      + "null as DEFAULT_VALUE, "
      + "null as DESCRIPTION "
      + "limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
    Util.trace("DatabaseMetaData.getFunctions");
    return null;
  }
  @Override
  public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
    Util.trace("DatabaseMetaData.getFunctionColumns");
    return null;
  }
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
      + "null as IS_NULLABLE limit 0");
    stmt.closeOnCompletion();
    return stmt.executeQuery();
  }
  @Override
  public boolean generatedKeyAlwaysReturned() throws SQLException {
    return false;
  }
  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw Util.error("not a wrapper");
  }
  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  private String quote(String data) {
    //if (data == null) return data;
    return Conn.mprintf("%Q", data);
  }
}
