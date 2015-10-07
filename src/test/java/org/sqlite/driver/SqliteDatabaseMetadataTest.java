/*
 * Copyright (c) 2013, Timothy Stack
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sqlite.driver;

import org.junit.Test;

import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static org.junit.Assert.*;

public class SqliteDatabaseMetadataTest extends SqliteTestHelper {
	@Test
	public void testGetSQLKeywords() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();
		String[] words = dmd.getSQLKeywords().split(",");

		assertEquals(words[0], "ABORT");
	}

	private Object[][] getMethodResults() {
		return new Object[][]{
				{"allProceduresAreCallable", false},
				{"allTablesAreSelectable", true},
				{"getURL", "jdbc:sqlite:" + dbFile.getAbsolutePath()},
				{"getUserName", null},
				{"nullsAreSortedHigh", false},
				{"nullsAreSortedLow", true},
				{"nullsAreSortedAtStart", false},
				{"nullsAreSortedAtEnd", false},
				{"getDatabaseProductName", "SQLite 3"},
				{"getDriverName", "SQLiteJNA"},
				{"getDriverVersion", "1.0"},
				{"getDriverMajorVersion", 1},
				{"getDriverMinorVersion", 0},
				{"usesLocalFiles", true},
				{"usesLocalFilePerTable", false},
				{"supportsMixedCaseIdentifiers", false},
				{"storesUpperCaseIdentifiers", false},
				{"storesLowerCaseIdentifiers", false},
				{"storesMixedCaseIdentifiers", true},
		};
	}

	@Test
	public void testFlags() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();
		Class<DatabaseMetaData> cl = DatabaseMetaData.class;

		for (Object[] pair : getMethodResults()) {
			Method method = cl.getMethod((String) pair[0]);
			Object result = method.invoke(dmd);

			assertEquals("Test of -- " + pair[0], pair[1], result);
		}
	}

	private static final String[] LIMITED_FUNCTIONS = {
			"getMaxBinaryLiteralLength",
			"getMaxCharLiteralLength",
			"getMaxColumnsInGroupBy",
			"getMaxColumnsInIndex",
			"getMaxColumnsInOrderBy",
			"getMaxColumnsInSelect",
			"getMaxColumnsInTable",
			"getMaxRowSize",
			"getMaxStatementLength",
			"getMaxTablesInSelect",
	};

	private static final String[] UNLIMITED_FUNCTIONS = {
			"getMaxColumnNameLength",
			"getMaxConnections",
			"getMaxIndexLength",
			"getMaxCatalogNameLength",
			"getMaxStatements",
			"getMaxTableNameLength",
	};

	@Test
	public void testLimits() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();
		Class<DatabaseMetaData> cl = DatabaseMetaData.class;

		for (String functionName : LIMITED_FUNCTIONS) {
			Method method = cl.getMethod(functionName);
			Object result = method.invoke(dmd);

			assertNotEquals("Test of -- " + functionName, 0, result);
		}
		for (String functionName : UNLIMITED_FUNCTIONS) {
			Method method = cl.getMethod(functionName);
			Object result = method.invoke(dmd);

			assertEquals("Test of -- " + functionName, 0, result);
		}
	}

	private static final String[] UNSUPPORTED_LIMIT_FUNCTIONS = {
			"getMaxCursorNameLength",
			"getMaxSchemaNameLength",
			"getMaxProcedureNameLength",
			"getMaxUserNameLength",
	};

	@Test
	public void testUnsupportedLimits() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();
		Class<DatabaseMetaData> cl = DatabaseMetaData.class;

		for (String functionName : UNSUPPORTED_LIMIT_FUNCTIONS) {
			Method method = cl.getMethod(functionName);
			Object result = method.invoke(dmd);

			assertEquals("Test of -- " + functionName, 0, result);
		}
	}

	private static final String[] NULL_SORT_ASC_RESULT_SET = {
			"|null|",
			"|1|",
	};

	private static final String[] NULL_SORT_DESC_RESULT_SET = {
			"|1|",
			"|null|",
	};

	@Test
	public void testNullSorting() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery(
					"SELECT null AS col0 UNION ALL SELECT 1 AS col0 ORDER BY col0 ASC")) {
				assertArrayEquals(NULL_SORT_ASC_RESULT_SET, formatResultSet(rs));
				assertEquals(dbMetadata.nullsAreSortedLow(),
						NULL_SORT_ASC_RESULT_SET[0].equals("|null|"));
				assertEquals(dbMetadata.nullsAreSortedAtEnd(),
						NULL_SORT_ASC_RESULT_SET[1].equals("|null|"));
				assertEquals(dbMetadata.nullsAreSortedHigh(),
						NULL_SORT_ASC_RESULT_SET[1].equals("|null|"));
			}
			try (ResultSet rs = stmt.executeQuery(
					"SELECT null AS col0 UNION ALL SELECT 1 AS col0 ORDER BY col0 DESC")) {
				assertArrayEquals(NULL_SORT_DESC_RESULT_SET, formatResultSet(rs));
				assertEquals(dbMetadata.nullsAreSortedHigh(),
						NULL_SORT_DESC_RESULT_SET[0].equals("|null|"));
				assertEquals(dbMetadata.nullsAreSortedAtStart(),
						NULL_SORT_DESC_RESULT_SET[0].equals("|null|"));
				assertEquals(dbMetadata.nullsAreSortedLow(),
						NULL_SORT_DESC_RESULT_SET[1].equals("|null|"));
			}
		}
	}

	@Test
	public void testVersion() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		assertTrue(dmd.getDatabaseProductVersion().matches("\\d+\\.\\d+\\.\\d+(\\.\\d+)?"));
	}

	private static final String MIXED_IDENT_HEADER = "|aBcD|";

	@Test
	public void testIdentifiers() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE TABLE mixed_IDENT (aBcD int)");
			try (ResultSet rs = stmt.executeQuery("SELECT abcd from mixed_ident")) {
				assertEquals(MIXED_IDENT_HEADER, formatResultSetHeader(rs.getMetaData()));
				assertFalse(dbMetadata.supportsMixedCaseIdentifiers());
				assertFalse(dbMetadata.storesUpperCaseIdentifiers());
				assertFalse(dbMetadata.storesLowerCaseIdentifiers());
				assertTrue(dbMetadata.storesMixedCaseIdentifiers());
			}
			stmt.execute("CREATE TABLE \"quote_IDENT\" (\"aBcD\" int)");
			try (ResultSet rs = stmt.executeQuery("SELECT abcd from \"quote_ident\"")) {
				assertEquals(MIXED_IDENT_HEADER, formatResultSetHeader(rs.getMetaData()));
				assertFalse(dbMetadata.supportsMixedCaseQuotedIdentifiers());
				assertFalse(dbMetadata.storesUpperCaseQuotedIdentifiers());
				assertFalse(dbMetadata.storesLowerCaseQuotedIdentifiers());
				assertTrue(dbMetadata.storesMixedCaseQuotedIdentifiers());
			}
		}
	}

	private static final String GET_PROCEDURES_HEADER =
			"|PROCEDURE_CAT|PROCEDURE_SCHEM|PROCEDURE_NAME|UNDEF1|UNDEF2|UNDEF3|REMARKS|PROCEDURE_TYPE|SPECIFIC_NAME|";
	private static final String[] EMPTY_RESULT_SET = {};

	@Test
	public void testGetProcedures() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getProcedures(null, null, null)) {
			assertEquals(GET_PROCEDURES_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(EMPTY_RESULT_SET, formatResultSet(rs));
		}
	}

	private static final String GET_PROCEDURE_COLUMNS_HEADER =
			"|PROCEDURE_CAT|PROCEDURE_SCHEM|PROCEDURE_NAME|COLUMN_NAME|COLUMN_TYPE|DATA_TYPE|TYPE_NAME|PRECISION|LENGTH|SCALE|RADIX|NULLABLE|REMARKS|COLUMN_DEF|SQL_DATA_TYPE|SQL_DATETIME_SUB|CHAR_OCTET_LENGTH|ORDINAL_POSITION|IS_NULLABLE|SPECIFIC_NAME|";

	@Test
	public void testGetProcedureColumns() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getProcedureColumns(null, null, null, null)) {
			assertEquals(GET_PROCEDURE_COLUMNS_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(EMPTY_RESULT_SET, formatResultSet(rs));
		}
	}

	private static final String GET_SCHEMAS_HEADER =
			"|TABLE_SCHEM|TABLE_CATALOG|";

	@Test
	public void testGetSchemas() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getSchemas()) {
			assertEquals(GET_SCHEMAS_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(EMPTY_RESULT_SET, formatResultSet(rs));
		}
		try (ResultSet rs = dmd.getSchemas(null, null)) {
			assertEquals(GET_SCHEMAS_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(EMPTY_RESULT_SET, formatResultSet(rs));
		}
	}

	@Test
	public void testGetCatalogs() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getCatalogs()) {
			assertTrue(rs.next());
			assertEquals("main", rs.getString(1));
			assertEquals("main", rs.getString("TABLE_CAT"));
			assertFalse(rs.next());
		}

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("ATTACH ':memory:' as extra_db");

			try (ResultSet rs = dmd.getCatalogs()) {
				assertTrue(rs.next());
				assertEquals("extra_db", rs.getString(1));
				assertEquals("extra_db", rs.getString("TABLE_CAT"));
				assertTrue(rs.next());
				assertEquals("main", rs.getString(1));
				assertEquals("main", rs.getString("TABLE_CAT"));
				assertFalse(rs.next());
			}
		}
	}

	private static final String TABLE_DUMP_HEADER =
			"|TABLE_CAT|TABLE_SCHEM|TABLE_NAME|TABLE_TYPE|REMARKS|TYPE_CAT|TYPE_SCHEM|TYPE_NAME|SELF_REFERENCING_COL_NAME|REF_GENERATION|";

	private static final String[] TABLE_DUMPS = {
			"|main|null|sqlite_master|SYSTEM TABLE|null|null|null|null|null|null|",
			"|main|null|prim_table|TABLE|null|null|null|null|null|null|",
			"|main|null|test_table|TABLE|null|null|null|null|null|null|",
			"|main|null|type_table|TABLE|null|null|null|null|null|null|",
	};

	@Test
	public void testGetTables() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getTables(null, null, "%", null)) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(TABLE_DUMP_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(TABLE_DUMPS, formatResultSet(rs));
		}

		try (ResultSet rs = dmd.getTables(null, null, "foo", null)) {
			assertFalse(rs.next());
		}

		try (ResultSet rs = dmd.getTables(null, null, "test_%", null)) {
			assertTrue(rs.next());
			assertEquals(TABLE_DUMPS[2], formatResultSetRow(rs));
			assertFalse(rs.next());
		}
	}

	private static final String COLUMN_DUMP_HEADER =
			"|TABLE_CAT|TABLE_SCHEM|TABLE_NAME|COLUMN_NAME|DATA_TYPE|TYPE_NAME|COLUMN_SIZE|" +
					"BUFFER_LENGTH|DECIMAL_DIGITS|NUM_PREC_RADIX|NULLABLE|REMARKS|COLUMN_DEF|" +
					"SQL_DATA_TYPE|SQL_DATETIME_SUB|CHAR_OCTET_LENGTH|ORDINAL_POSITION|IS_NULLABLE|SCOPE_CATLOG|" +
					"SCOPE_SCHEMA|SCOPE_TABLE|SOURCE_DATA_TYPE|IS_AUTOINCREMENT|IS_GENERATEDCOLUMN|";

	private static final String[] COLUMN_DUMP = {
			"|main|null|prim_table|id|4|INTEGER|10|null|10|10|1|null|null|null|null|10|1|YES|null|null|null|null|||",
			"|main|null|prim_table|b|2|BOOLEAN|10|null|10|10|1|null|null|null|null|10|2|YES|null|null|null|null|||",
			"|main|null|prim_table|bi|4|BIGINT|10|null|10|10|1|null|null|null|null|10|3|YES|null|null|null|null|||",
			"|main|null|prim_table|f|7|FLOAT|10|null|10|10|1|null|null|null|null|10|4|YES|null|null|null|null|||",
			"|main|null|prim_table|d|7|DOUBLE|10|null|10|10|1|null|null|null|null|10|5|YES|null|null|null|null|||",
			"|main|null|sqlite_master|type|12|TEXT|10|null|10|10|1|null|null|null|null|10|1|YES|null|null|null|null|||",
			"|main|null|sqlite_master|name|12|TEXT|10|null|10|10|1|null|null|null|null|10|2|YES|null|null|null|null|||",
			"|main|null|sqlite_master|tbl_name|12|TEXT|10|null|10|10|1|null|null|null|null|10|3|YES|null|null|null|null|||",
			"|main|null|sqlite_master|rootpage|4|INTEGER|10|null|10|10|1|null|null|null|null|10|4|YES|null|null|null|null|||",
			"|main|null|sqlite_master|sql|12|TEXT|10|null|10|10|1|null|null|null|null|10|5|YES|null|null|null|null|||",
			"|main|null|test_table|id|4|INTEGER|10|null|10|10|1|null|null|null|null|10|1|YES|null|null|null|null|||",
			"|main|null|test_table|name|12|VARCHAR|10|null|10|10|0|null|null|null|null|10|2|NO|null|null|null|null|||",
			"|main|null|type_table|name|12|VARCHAR|10|null|10|10|1|null|null|null|null|10|1|YES|null|null|null|null|||",
			"|main|null|type_table|birthdate|2|DATETIME|10|null|10|10|1|null|null|null|null|10|2|YES|null|null|null|null|||",
			"|main|null|type_table|height|7|REAL|10|null|10|10|1|null|null|null|null|10|3|YES|null|null|null|null|||",
			"|main|null|type_table|eyes|4|INTEGER|10|null|10|10|1|null|null|null|null|10|4|YES|null|null|null|null|||",
			"|main|null|type_table|width|2|DECIMAL(10,2)|10|null|10|10|1|null|null|null|null|10|5|YES|null|null|null|null|||",
	};
	@Test
	public void testGetColumns() throws Exception {
		try (ResultSet rs = dbMetadata.getColumns(null, null, null, null)) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(COLUMN_DUMP_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(COLUMN_DUMP, formatResultSet(rs));
		}
	}

	private static final String PK_DUMP_HEADER =
			"|TABLE_CAT|TABLE_SCHEM|TABLE_NAME|COLUMN_NAME|KEY_SEQ|PK_NAME|";

	private static final String[] PK_DUMP = {
			"|main|null|test_table|id|1|id|",
	};

	@Test
	public void testGetPrimaryKeys() throws Exception {
		try (ResultSet rs = dbMetadata.getPrimaryKeys(null, null, "test_table")) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(PK_DUMP_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(PK_DUMP, formatResultSet(rs));
		}

		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("CREATE TABLE nokey (foo TEXT, bar TEXT)");
		}

		try (ResultSet rs = dbMetadata.getPrimaryKeys(null, null, "nokey")) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(PK_DUMP_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(new String[0], formatResultSet(rs));
		}
	}

	private static final String INDEX_INFO_HEADER =
			"|TABLE_CAT|TABLE_SCHEM|TABLE_NAME|NON_UNIQUE|INDEX_QUALIFIER|INDEX_NAME|TYPE|ORDINAL_POSITION|COLUMN_NAME|ASC_OR_DESC|CARDINALITY|PAGES|FILTER_CONDITION|";
	private static final String[] INDEX_INFO_TEST_DUMP = {
			"|main|null|test_table|1|main|test_index|3|0|id|null|0|0|null|",
			"|main|null|test_table|1|main|test_index|3|1|name|null|0|0|null|",
	};
	private static final String[] INDEX_INFO_TYPE_DUMP = {
			"|main|null|type_table|0|main|sqlite_autoindex_type_table_1|3|0|name|null|0|0|null|",
			"|main|null|type_table|0|main|sqlite_autoindex_type_table_2|3|0|birthdate|null|0|0|null|",
	};

	@Test
	public void testGetIndexInfo() throws Exception {
		try (ResultSet rs = dbMetadata.getIndexInfo(null, null, "test_table", false, true)) {
			assertEquals(INDEX_INFO_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(INDEX_INFO_TEST_DUMP, formatResultSet(rs));
		}
		try (ResultSet rs = dbMetadata.getIndexInfo(null, null, "test_table", true, true)) {
			assertEquals(INDEX_INFO_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(EMPTY_RESULT_SET, formatResultSet(rs));
		}
		try (ResultSet rs = dbMetadata.getIndexInfo(null, null, "type_table", false, true)) {
			assertEquals(INDEX_INFO_HEADER, formatResultSetHeader(rs.getMetaData()));
			assertArrayEquals(INDEX_INFO_TYPE_DUMP, formatResultSet(rs));
		}
	}

	private static final String CLIENT_INFO_HEADER =
			"|NAME|MAX_LEN|DEFAULT_VALUE|DESCRIPTION|";

	@Test
	public void testGetClientInfo() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getClientInfoProperties()) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(CLIENT_INFO_HEADER, formatResultSetHeader(rsm));

			assertFalse(rs.next());
		}
	}

	private static final String TABLE_TYPE_HEADER = "|TABLE_TYPE|";

	private static final String[] TABLE_TYPE_DUMPS = {
			"|SYSTEM TABLE|",
			"|TABLE|",
			"|VIEW|",
	};

	@Test
	public void testGetTableTypes() throws Exception {
		DatabaseMetaData dmd = conn.getMetaData();

		try (ResultSet rs = dmd.getTableTypes()) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(TABLE_TYPE_HEADER, formatResultSetHeader(rsm));

			while (rs.next()) {
				assertEquals(TABLE_TYPE_DUMPS[rs.getRow() - 1], formatResultSetRow(rs));
			}
		}
	}

	private static final String IMPORTED_KEY_HEADER =
			"|PKTABLE_CAT|PKTABLE_SCHEM|PKTABLE_NAME|PKCOLUMN_NAME|FKTABLE_CAT|FKTABLE_SCHEM|" +
					"FKTABLE_NAME|FKCOLUMN_NAME|KEY_SEQ|UPDATE_RULE|DELETE_RULE|FK_NAME|PK_NAME|" +
					"DEFERRABILITY|";

	private static final String[] IMPORTED_KEY_DUMP = {
			"|main|null|artist|artistid|main|null|track|trackartist|1|3|3|track_artist_0|null|7|",
	};

	@Test
	public void testGetImportedKeys() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(
					"CREATE TABLE artist(" +
							" artistid    INTEGER PRIMARY KEY, " +
							" artistname  TEXT " +
							")"
			);
			stmt.executeUpdate(
					"CREATE TABLE track(" +
							"  trackid     INTEGER, " +
							"  trackname   TEXT, " +
							"  trackartist INTEGER," +
							"  FOREIGN KEY(trackartist) REFERENCES artist(artistid)" +
							")"
			);
		}

		try (ResultSet rs = dbMetadata.getImportedKeys("main", null, "track")) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(IMPORTED_KEY_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(IMPORTED_KEY_DUMP, formatResultSet(rs));
		}

		try (ResultSet rs = dbMetadata.getImportedKeys("main", null, "artist")) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(IMPORTED_KEY_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(new String[0], formatResultSet(rs));
		}

		try (ResultSet rs = dbMetadata.getExportedKeys("main", null, "artist")) {
			ResultSetMetaData rsm = rs.getMetaData();

			assertEquals(IMPORTED_KEY_HEADER, formatResultSetHeader(rsm));
			assertArrayEquals(IMPORTED_KEY_DUMP, formatResultSet(rs));
		}
	}

	private static final String[] TYPE_INFO_DUMP = {
			"|NULL|0|0|null|null|null|1|1|3|0|0|0|null|0|0|0|0|10|",
			"|INTEGER|4|0|null|null|null|1|1|3|0|0|0|null|0|0|0|0|10|",
			"|REAL|7|0|null|null|null|1|1|3|0|0|0|null|0|0|0|0|10|",
			"|TEXT|12|0|null|null|null|1|1|3|0|0|0|null|0|0|0|0|10|",
			"|BLOB|1111|0|null|null|null|1|1|3|0|0|0|null|0|0|0|0|10|",
	};

	@Test
	public void testTypeInfo() throws Exception {
		try (ResultSet rs = dbMetadata.getTypeInfo()) {
			assertArrayEquals(TYPE_INFO_DUMP, formatResultSet(rs));
		}
	}
/*
		private static final String GET_FUNCTION_COLUMNS_HEADER =
            "|FUNCTION_CAT|FUNCTION_SCHEM|FUNCTION_NAME|COLUMN_NAME|COLUMN_TYPE|DATA_TYPE|TYPE_NAME|PRECISION|LENGTH|SCALE|RADIX|NULLABLE|REMARKS|CHAR_OCTET_LENGTH|ORDINAL_POSITION|IS_NULLABLE|SPECIFIC_NAME|";

    private static final String[] GET_FUNCTION_COLUMNS_ABS_DUMP = {
            "|null|null|abs|X|1|2|NUMERIC|16|0|14|10|1|The absolute value of this argument will be returned|null|1|YES|null|",
            "|null|null|abs|return|5|2|NUMERIC|16|0|14|10|1|The absolute value of the X argument.|null|0|YES|null|",
    };

    @Test
    public void testGetFunctionColumns() throws Exception {
        try (ResultSet rs = dbMetadata.getFunctionColumns(null, null, null, null)) {
            assertEquals(GET_FUNCTION_COLUMNS_HEADER, formatResultSetHeader(rs.getMetaData()));
        }
        try (ResultSet rs = dbMetadata.getFunctionColumns(null, null, "abs", null)) {
            assertEquals(GET_FUNCTION_COLUMNS_HEADER, formatResultSetHeader(rs.getMetaData()));
            assertArrayEquals(GET_FUNCTION_COLUMNS_ABS_DUMP, formatResultSet(rs));
        }
    }
*/
}
