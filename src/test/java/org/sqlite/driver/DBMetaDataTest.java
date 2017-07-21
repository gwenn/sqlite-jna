/*
Copyright (c) 2006, David Crawshaw.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
*/

package org.sqlite.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import static org.junit.Assert.*;

public class DBMetaDataTest {
	private Connection conn;
	private Statement stat;
	private DatabaseMetaData meta;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.MEMORY);
		stat = conn.createStatement();
		stat.executeUpdate("create table test (id integer primary key, fn float default 0.0, sn not null);");
		stat.executeUpdate("create view testView as select * from test;");
		meta = conn.getMetaData();
	}

	@After
	public void close() throws SQLException {
		meta = null;
		stat.close();
		conn.close();
	}

	@Test
	public void getTables() throws SQLException {
		ResultSet rs = meta.getTables(null, null, null, null);
		assertNotNull(rs);

		stat.getGeneratedKeys().close();
		stat.close();

		assertTrue(rs.next());
		assertEquals("sqlite_master", rs.getString("TABLE_NAME")); // 3
		assertEquals("SYSTEM TABLE", rs.getString("TABLE_TYPE")); // 4
		assertEquals("main", rs.getString("TABLE_CAT")); // 4
		assertTrue(rs.next());
		assertEquals("test", rs.getString("TABLE_NAME")); // 3
		assertEquals("TABLE", rs.getString("TABLE_TYPE")); // 4
		assertEquals("main", rs.getString("TABLE_CAT")); // 4
		assertTrue(rs.next());
		assertEquals("testView", rs.getString("TABLE_NAME"));
		assertEquals("VIEW", rs.getString("TABLE_TYPE"));
		assertEquals("main", rs.getString("TABLE_CAT")); // 4
		rs.close();

		rs = meta.getTables(null, null, "bob", null);
		assertFalse(rs.next());
		rs.close();
		rs = meta.getTables(null, null, "test", null);
		assertTrue(rs.next());
		assertFalse(rs.next());
		rs.close();
		rs = meta.getTables(null, null, "test%", null);
		assertTrue(rs.next());
		assertTrue(rs.next());
		rs.close();

		rs = meta.getTables(null, null, null, new String[]{"table"});
		assertTrue(rs.next());
		assertEquals("test", rs.getString("TABLE_NAME"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getTables(null, null, null, new String[]{"view"});
		assertTrue(rs.next());
		assertEquals("testView", rs.getString("TABLE_NAME"));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void getTableTypes() throws SQLException {
		ResultSet rs = meta.getTableTypes();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("SYSTEM TABLE", rs.getString("TABLE_TYPE"));
		assertTrue(rs.next());
		assertEquals("TABLE", rs.getString("TABLE_TYPE"));
		assertTrue(rs.next());
		assertEquals("VIEW", rs.getString("TABLE_TYPE"));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void getTypeInfo() throws SQLException {
		ResultSet rs = meta.getTypeInfo();
		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("NULL", rs.getString("TYPE_NAME"));
		assertTrue(rs.next());
		assertEquals("INTEGER", rs.getString("TYPE_NAME"));
		assertTrue(rs.next());
		assertEquals("REAL", rs.getString("TYPE_NAME"));
		assertTrue(rs.next());
		assertEquals("TEXT", rs.getString("TYPE_NAME"));
		assertTrue(rs.next());
		assertEquals("BLOB", rs.getString("TYPE_NAME"));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void getColumns() throws SQLException {
		ResultSet rs = meta.getColumns(null, null, "test", "id");
		assertTrue(rs.next());
		assertEquals("test", rs.getString("TABLE_NAME"));
		assertEquals("id", rs.getString("COLUMN_NAME"));
		assertEquals("YES", rs.getString("IS_NULLABLE"));
		assertEquals(null, rs.getString("COLUMN_DEF"));
		assertEquals(Types.INTEGER, rs.getInt("DATA_TYPE"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "test", "fn");
		assertTrue(rs.next());
		assertEquals("fn", rs.getString("COLUMN_NAME"));
		assertEquals(Types.REAL, rs.getInt("DATA_TYPE"));
		assertEquals("YES", rs.getString("IS_NULLABLE"));
		assertEquals("0.0", rs.getString("COLUMN_DEF"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "test", "sn");
		assertTrue(rs.next());
		assertEquals("sn", rs.getString("COLUMN_NAME"));
		assertEquals("NO", rs.getString("IS_NULLABLE"));
		assertEquals(null, rs.getString("COLUMN_DEF"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "test", "%");
		assertTrue(rs.next());
		assertEquals("id", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("fn", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("sn", rs.getString("COLUMN_NAME"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "test", "%n");
		assertTrue(rs.next());
		assertEquals("fn", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("sn", rs.getString("COLUMN_NAME"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "tes%t", "%");
		assertTrue(rs.next());
		assertEquals("id", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("fn", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("sn", rs.getString("COLUMN_NAME"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "t%", "%");
		assertTrue(rs.next());
		assertEquals("test", rs.getString("TABLE_NAME"));
		assertEquals("id", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("fn", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		assertEquals("sn", rs.getString("COLUMN_NAME"));
		assertTrue(rs.next());
		rs.close();

		rs = meta.getColumns(null, null, "doesnotexist", "%");
		assertFalse(rs.next());
		assertEquals(24, rs.getMetaData().getColumnCount());
		rs.close();
	}

	@Test
	public void numberOfgetImportedKeysCols() throws SQLException {

		stat.executeUpdate("create table parent (id1 integer, id2 integer, primary key(id1, id2))");
		stat.executeUpdate("create table child1 (id1 integer, id2 integer, foreign key(id1) references parent(id1), foreign key(id2) references parent(id2))");
		stat.executeUpdate("create table child2 (id1 integer, id2 integer, foreign key(id2, id1) references parent(id2, id1))");

		ResultSet importedKeys = meta.getImportedKeys(null, null, "child1");

		//child1: 1st fk (simple)
		assertTrue(importedKeys.next());
		assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
		assertEquals("id1", importedKeys.getString("PKCOLUMN_NAME"));
		// assertNotNull(importedKeys.getString("PK_NAME")); FIXME
		assertNotNull(importedKeys.getString("FK_NAME"));
		assertEquals("child1", importedKeys.getString("FKTABLE_NAME"));
		assertEquals("id1", importedKeys.getString("FKCOLUMN_NAME"));

		//child1: 2nd fk (simple)
		assertTrue(importedKeys.next());
		assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
		assertEquals("id2", importedKeys.getString("PKCOLUMN_NAME"));
		// assertNotNull(importedKeys.getString("PK_NAME")); FIXME
		assertNotNull(importedKeys.getString("FK_NAME"));
		assertEquals("child1", importedKeys.getString("FKTABLE_NAME"));
		assertEquals("id2", importedKeys.getString("FKCOLUMN_NAME"));

		assertFalse(importedKeys.next());
		importedKeys.close();

		importedKeys = meta.getImportedKeys(null, null, "child2");

		//child2: 1st fk (composite)
		assertTrue(importedKeys.next());
		assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
		assertEquals("id2", importedKeys.getString("PKCOLUMN_NAME"));
		//assertNotNull(importedKeys.getString("PK_NAME")); FIXME
		assertNotNull(importedKeys.getString("FK_NAME"));
		assertEquals("child2", importedKeys.getString("FKTABLE_NAME"));
		assertEquals("id2", importedKeys.getString("FKCOLUMN_NAME"));

		assertTrue(importedKeys.next());
		assertEquals("parent", importedKeys.getString("PKTABLE_NAME"));
		assertEquals("id1", importedKeys.getString("PKCOLUMN_NAME"));
		//assertNotNull(importedKeys.getString("PK_NAME")); FIXME
		assertNotNull(importedKeys.getString("FK_NAME"));
		assertEquals("child2", importedKeys.getString("FKTABLE_NAME"));
		assertEquals("id1", importedKeys.getString("FKCOLUMN_NAME"));

		assertFalse(importedKeys.next());

		importedKeys.close();
	}

/*    @Test
		public void columnOrderOfgetExportedKeys() throws SQLException {

    exportedKeys.close();

    }*/

	@Test
	public void numberOfgetExportedKeysCols() throws SQLException {

		stat.executeUpdate("create table parent (id1 integer, id2 integer, primary key(id1, id2))");
		stat.executeUpdate("create table child1 (id1 integer, id2 integer,\r\n foreign\tkey(id1) references parent(id1), foreign key(id2) references parent(id2))");
		stat.executeUpdate("create table child2 (id1 integer, id2 integer, foreign key(id2, id1) references parent(id2, id1))");

		ResultSet exportedKeys = meta.getExportedKeys(null, null, "parent");

		//1st fk (simple) - child1
		assertTrue(exportedKeys.next());
		assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("id2", exportedKeys.getString("PKCOLUMN_NAME"));
		//assertNotNull(exportedKeys.getString("PK_NAME")); FIXME
		assertNotNull(exportedKeys.getString("FK_NAME"));
		assertEquals("child1", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("id2", exportedKeys.getString("FKCOLUMN_NAME"));

		//2nd fk (simple) - child1
		assertTrue(exportedKeys.next());
		assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("id1", exportedKeys.getString("PKCOLUMN_NAME"));
		// assertNotNull(exportedKeys.getString("PK_NAME")); FIXME
		assertNotNull(exportedKeys.getString("FK_NAME"));
		assertEquals("child1", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("id1", exportedKeys.getString("FKCOLUMN_NAME"));

		//3rd fk (composite) - child2
		assertTrue(exportedKeys.next());
		assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("id2", exportedKeys.getString("PKCOLUMN_NAME"));
		//assertNotNull(exportedKeys.getString("PK_NAME")); FIXME
		assertNotNull(exportedKeys.getString("FK_NAME"));
		assertEquals("child2", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("id2", exportedKeys.getString("FKCOLUMN_NAME"));

		assertTrue(exportedKeys.next());
		assertEquals("parent", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("id1", exportedKeys.getString("PKCOLUMN_NAME"));
		// assertNotNull(exportedKeys.getString("PK_NAME")); FIXME
		assertNotNull(exportedKeys.getString("FK_NAME"));
		assertEquals("child2", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("id1", exportedKeys.getString("FKCOLUMN_NAME"));

		assertFalse(exportedKeys.next());

		exportedKeys.close();
	}

	@Test
	public void columnOrderOfgetTables() throws SQLException {
		ResultSet rs = meta.getTables(null, null, null, null);
		assertTrue(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(10, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("TABLE_TYPE", rsmeta.getColumnName(4));
		assertEquals("REMARKS", rsmeta.getColumnName(5));
		assertEquals("TYPE_CAT", rsmeta.getColumnName(6));
		assertEquals("TYPE_SCHEM", rsmeta.getColumnName(7));
		assertEquals("TYPE_NAME", rsmeta.getColumnName(8));
		assertEquals("SELF_REFERENCING_COL_NAME", rsmeta.getColumnName(9));
		assertEquals("REF_GENERATION", rsmeta.getColumnName(10));
		rs.close();
	}

	@Test
	public void columnOrderOfgetTableTypes() throws SQLException {
		ResultSet rs = meta.getTableTypes();
		assertTrue(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(1, rsmeta.getColumnCount());
		assertEquals("TABLE_TYPE", rsmeta.getColumnName(1));
		rs.close();
	}

	@Test
	public void columnOrderOfgetTypeInfo() throws SQLException {
		ResultSet rs = meta.getTypeInfo();
		assertTrue(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(18, rsmeta.getColumnCount());
		assertEquals("TYPE_NAME", rsmeta.getColumnName(1));
		assertEquals("DATA_TYPE", rsmeta.getColumnName(2));
		assertEquals("PRECISION", rsmeta.getColumnName(3));
		assertEquals("LITERAL_PREFIX", rsmeta.getColumnName(4));
		assertEquals("LITERAL_SUFFIX", rsmeta.getColumnName(5));
		assertEquals("CREATE_PARAMS", rsmeta.getColumnName(6));
		assertEquals("NULLABLE", rsmeta.getColumnName(7));
		assertEquals("CASE_SENSITIVE", rsmeta.getColumnName(8));
		assertEquals("SEARCHABLE", rsmeta.getColumnName(9));
		assertEquals("UNSIGNED_ATTRIBUTE", rsmeta.getColumnName(10));
		assertEquals("FIXED_PREC_SCALE", rsmeta.getColumnName(11));
		assertEquals("AUTO_INCREMENT", rsmeta.getColumnName(12));
		assertEquals("LOCAL_TYPE_NAME", rsmeta.getColumnName(13));
		assertEquals("MINIMUM_SCALE", rsmeta.getColumnName(14));
		assertEquals("MAXIMUM_SCALE", rsmeta.getColumnName(15));
		assertEquals("SQL_DATA_TYPE", rsmeta.getColumnName(16));
		assertEquals("SQL_DATETIME_SUB", rsmeta.getColumnName(17));
		assertEquals("NUM_PREC_RADIX", rsmeta.getColumnName(18));
		rs.close();
	}

	@Test
	public void columnOrderOfgetColumns() throws SQLException {
		ResultSet rs = meta.getColumns(null, null, "test", null);
		assertTrue(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(24, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(4));
		assertEquals("DATA_TYPE", rsmeta.getColumnName(5));
		assertEquals("TYPE_NAME", rsmeta.getColumnName(6));
		assertEquals("COLUMN_SIZE", rsmeta.getColumnName(7));
		assertEquals("BUFFER_LENGTH", rsmeta.getColumnName(8));
		assertEquals("DECIMAL_DIGITS", rsmeta.getColumnName(9));
		assertEquals("NUM_PREC_RADIX", rsmeta.getColumnName(10));
		assertEquals("NULLABLE", rsmeta.getColumnName(11));
		assertEquals("REMARKS", rsmeta.getColumnName(12));
		assertEquals("COLUMN_DEF", rsmeta.getColumnName(13));
		assertEquals("SQL_DATA_TYPE", rsmeta.getColumnName(14));
		assertEquals("SQL_DATETIME_SUB", rsmeta.getColumnName(15));
		assertEquals("CHAR_OCTET_LENGTH", rsmeta.getColumnName(16));
		assertEquals("ORDINAL_POSITION", rsmeta.getColumnName(17));
		assertEquals("IS_NULLABLE", rsmeta.getColumnName(18));
		// should be SCOPE_CATALOG, but misspelt in the standard
		assertEquals("SCOPE_CATLOG", rsmeta.getColumnName(19));
		assertEquals("SCOPE_SCHEMA", rsmeta.getColumnName(20));
		assertEquals("SCOPE_TABLE", rsmeta.getColumnName(21));
		assertEquals("SOURCE_DATA_TYPE", rsmeta.getColumnName(22));
		assertEquals("IS_AUTOINCREMENT", rsmeta.getColumnName(23));
		assertEquals("IS_GENERATEDCOLUMN", rsmeta.getColumnName(24));
		rs.close();
	}

	// the following functions always return an empty resultset, so
	// do not bother testing their parameters, only the column types

	@Test
	public void columnOrderOfgetProcedures() throws SQLException {
		ResultSet rs = meta.getProcedures(null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(9, rsmeta.getColumnCount());
		assertEquals("PROCEDURE_CAT", rsmeta.getColumnName(1));
		assertEquals("PROCEDURE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("PROCEDURE_NAME", rsmeta.getColumnName(3));
		// currently (Java 1.5), cols 4,5,6 are undefined
		assertEquals("REMARKS", rsmeta.getColumnName(7));
		assertEquals("PROCEDURE_TYPE", rsmeta.getColumnName(8));
		assertEquals("SPECIFIC_NAME", rsmeta.getColumnName(9));
		rs.close();
	}

	@Test
	public void columnOrderOfgetProcedurColumns() throws SQLException {
		ResultSet rs = meta.getProcedureColumns(null, null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(20, rsmeta.getColumnCount());
		assertEquals("PROCEDURE_CAT", rsmeta.getColumnName(1));
		assertEquals("PROCEDURE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("PROCEDURE_NAME", rsmeta.getColumnName(3));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(4));
		assertEquals("COLUMN_TYPE", rsmeta.getColumnName(5));
		assertEquals("DATA_TYPE", rsmeta.getColumnName(6));
		assertEquals("TYPE_NAME", rsmeta.getColumnName(7));
		assertEquals("PRECISION", rsmeta.getColumnName(8));
		assertEquals("LENGTH", rsmeta.getColumnName(9));
		assertEquals("SCALE", rsmeta.getColumnName(10));
		assertEquals("RADIX", rsmeta.getColumnName(11));
		assertEquals("NULLABLE", rsmeta.getColumnName(12));
		assertEquals("REMARKS", rsmeta.getColumnName(13));
		assertEquals("COLUMN_DEF", rsmeta.getColumnName(14));
		assertEquals("SQL_DATA_TYPE", rsmeta.getColumnName(15));
		assertEquals("SQL_DATETIME_SUB", rsmeta.getColumnName(16));
		assertEquals("CHAR_OCTET_LENGTH", rsmeta.getColumnName(17));
		assertEquals("ORDINAL_POSITION", rsmeta.getColumnName(18));
		assertEquals("IS_NULLABLE", rsmeta.getColumnName(19));
		assertEquals("SPECIFIC_NAME", rsmeta.getColumnName(20));
		rs.close();
	}

	@Test
	public void columnOrderOfgetSchemas() throws SQLException {
		ResultSet rs = meta.getSchemas();
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(2, rsmeta.getColumnCount());
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(1));
		assertEquals("TABLE_CATALOG", rsmeta.getColumnName(2));
		rs.close();
	}

	@Test
	public void columnOrderOfgetCatalogs() throws SQLException {
		ResultSet rs = meta.getCatalogs();
		assertTrue(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(1, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		rs.close();
	}

	@Test
	public void columnOrderOfgetColumnPrivileges() throws SQLException {
		ResultSet rs = meta.getColumnPrivileges(null, null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(8, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(4));
		assertEquals("GRANTOR", rsmeta.getColumnName(5));
		assertEquals("GRANTEE", rsmeta.getColumnName(6));
		assertEquals("PRIVILEGE", rsmeta.getColumnName(7));
		assertEquals("IS_GRANTABLE", rsmeta.getColumnName(8));
		rs.close();
	}

	@Test
	public void columnOrderOfgetTablePrivileges() throws SQLException {
		ResultSet rs = meta.getTablePrivileges(null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(7, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("GRANTOR", rsmeta.getColumnName(4));
		assertEquals("GRANTEE", rsmeta.getColumnName(5));
		assertEquals("PRIVILEGE", rsmeta.getColumnName(6));
		assertEquals("IS_GRANTABLE", rsmeta.getColumnName(7));
		rs.close();
	}

	@Test
	public void columnOrderOfgetBestRowIdentifier() throws SQLException {
		ResultSet rs = meta.getBestRowIdentifier(null, null, null, 0, false);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(8, rsmeta.getColumnCount());
		assertEquals("SCOPE", rsmeta.getColumnName(1));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(2));
		assertEquals("DATA_TYPE", rsmeta.getColumnName(3));
		assertEquals("TYPE_NAME", rsmeta.getColumnName(4));
		assertEquals("COLUMN_SIZE", rsmeta.getColumnName(5));
		assertEquals("BUFFER_LENGTH", rsmeta.getColumnName(6));
		assertEquals("DECIMAL_DIGITS", rsmeta.getColumnName(7));
		assertEquals("PSEUDO_COLUMN", rsmeta.getColumnName(8));
		rs.close();
		rs = meta.getBestRowIdentifier(null, null, "test", 0, false);
		assertTrue(rs.next());
		assertEquals(0, rs.getInt(1));
		assertEquals("ROWID", rs.getString(2));
		assertEquals(Types.INTEGER, rs.getInt(3));
		assertEquals("INTEGER", rs.getString(4));
		assertEquals(10, rs.getInt(5));
		assertEquals(0, rs.getInt(6));
		assertEquals(0, rs.getInt(7));
		assertEquals(DatabaseMetaData.bestRowPseudo, rs.getInt(8));
		rs.close();
	}

	@Test
	public void columnOrderOfgetVersionColumns() throws SQLException {
		ResultSet rs = meta.getVersionColumns(null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(8, rsmeta.getColumnCount());
		assertEquals("SCOPE", rsmeta.getColumnName(1));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(2));
		assertEquals("DATA_TYPE", rsmeta.getColumnName(3));
		assertEquals("TYPE_NAME", rsmeta.getColumnName(4));
		assertEquals("COLUMN_SIZE", rsmeta.getColumnName(5));
		assertEquals("BUFFER_LENGTH", rsmeta.getColumnName(6));
		assertEquals("DECIMAL_DIGITS", rsmeta.getColumnName(7));
		assertEquals("PSEUDO_COLUMN", rsmeta.getColumnName(8));
		rs.close();
	}

	@Test
	public void columnOrderOfgetPrimaryKeys() throws SQLException {
		if (org.sqlite.Conn.libversionNumber() < 3007016) {
			return;
		}
		ResultSet rs;
		ResultSetMetaData rsmeta;

		stat.executeUpdate("create table nopk (c1, c2, c3, c4);");
		stat.executeUpdate("create table pk1 (col1 primary key, col2, col3);");
		stat.executeUpdate("create table pk2 (col1, col2 primary key, col3);");
		stat.executeUpdate("create table pk3 (col1, col2, col3, col4, primary key (col3, col2  ));");
		// extra spaces and mixed case are intentional, do not remove!
		stat.executeUpdate("create table pk4 (col1, col2, col3, col4, " +
				"\r\nCONSTraint\r\nnamed  primary\r\n\t\t key   (col3, col2  ));");

		rs = meta.getPrimaryKeys(null, null, "nopk");
		assertFalse(rs.next());
		rsmeta = rs.getMetaData();
		assertEquals(6, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(4));
		assertEquals("KEY_SEQ", rsmeta.getColumnName(5));
		assertEquals("PK_NAME", rsmeta.getColumnName(6));
		rs.close();

		rs = meta.getPrimaryKeys(null, null, "pk1");
		assertTrue(rs.next());
		assertEquals("col1", rs.getString("PK_NAME"));
		assertEquals("col1", rs.getString("COLUMN_NAME"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getPrimaryKeys(null, null, "pk2");
		assertTrue(rs.next());
		assertEquals("col2", rs.getString("PK_NAME"));
		assertEquals("col2", rs.getString("COLUMN_NAME"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getPrimaryKeys(null, null, "pk3");
		assertTrue(rs.next());
		assertEquals("col2", rs.getString("COLUMN_NAME"));
		assertEquals("PK", rs.getString("PK_NAME"));
		assertEquals(2, rs.getInt("KEY_SEQ"));
		assertTrue(rs.next());
		assertEquals("col3", rs.getString("COLUMN_NAME"));
		assertEquals("PK", rs.getString("PK_NAME"));
		assertEquals(1, rs.getInt("KEY_SEQ"));
		assertFalse(rs.next());
		rs.close();

		rs = meta.getPrimaryKeys(null, null, "pk4");
		assertTrue(rs.next());
		assertEquals("col2", rs.getString("COLUMN_NAME"));
		assertEquals("PK", rs.getString("PK_NAME"));
		assertEquals(2, rs.getInt("KEY_SEQ"));
		assertTrue(rs.next());
		assertEquals("col3", rs.getString("COLUMN_NAME"));
		assertEquals("PK", rs.getString("PK_NAME"));
		assertEquals(1, rs.getInt("KEY_SEQ"));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void columnOrderOfgetImportedKeys() throws SQLException {

		stat.executeUpdate("create table person (id integer)");
		stat.executeUpdate("create table address (pid integer, name, foreign key(pid) references person(id))");

		ResultSet importedKeys = meta.getImportedKeys("main", "global", "address");
		assertTrue(importedKeys.next());
		assertEquals("main", importedKeys.getString("PKTABLE_CAT"));
		assertNull(importedKeys.getString("PKTABLE_SCHEM"));
		assertEquals("main", importedKeys.getString("FKTABLE_CAT"));
		assertEquals("person", importedKeys.getString("PKTABLE_NAME"));
		assertEquals("id", importedKeys.getString("PKCOLUMN_NAME"));
		//assertNotNull(importedKeys.getString("PK_NAME")); // FIXME
		assertNotNull(importedKeys.getString("FK_NAME"));
		assertEquals("address", importedKeys.getString("FKTABLE_NAME"));
		assertEquals("pid", importedKeys.getString("FKCOLUMN_NAME"));
		importedKeys.close();

		importedKeys = meta.getImportedKeys(null, null, "person");
		assertTrue(!importedKeys.next());
		importedKeys.close();
	}

	@Test
	public void columnOrderOfgetExportedKeys() throws SQLException {

		stat.executeUpdate("create table person (id integer primary key)");
		stat.executeUpdate("create table address (pid integer, name, foreign key(pid) references person(id))");

		ResultSet exportedKeys = meta.getExportedKeys("main", "global", "person");
		assertTrue(exportedKeys.next());
		assertEquals("main", exportedKeys.getString("PKTABLE_CAT"));
		assertNull(exportedKeys.getString("PKTABLE_SCHEM"));
		assertEquals("main", exportedKeys.getString("FKTABLE_CAT"));
		assertNull(exportedKeys.getString("FKTABLE_SCHEM"));
		// assertNotNull(exportedKeys.getString("PK_NAME")); FIXME
		assertNotNull(exportedKeys.getString("FK_NAME"));

		assertEquals("person", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("id", exportedKeys.getString("PKCOLUMN_NAME"));
		assertEquals("address", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("pid", exportedKeys.getString("FKCOLUMN_NAME"));

		exportedKeys.close();

		exportedKeys = meta.getExportedKeys(null, null, "address");
		assertFalse(exportedKeys.next());
		exportedKeys.close();

		// With explicit primary column defined.
		stat.executeUpdate("create table REFERRED (ID integer primary key not null)");
		stat.executeUpdate("create table REFERRING (ID integer, RID integer, constraint fk\r\n foreign\tkey\r\n(RID) references REFERRED(id))");

		exportedKeys = meta.getExportedKeys(null, null, "referred");
		assertTrue(exportedKeys.next());
		assertEquals("referred", exportedKeys.getString("PKTABLE_NAME"));
		assertEquals("REFERRING", exportedKeys.getString("FKTABLE_NAME"));
		assertEquals("REFERRING_referred_0", exportedKeys.getString("FK_NAME"));
		exportedKeys.close();
	}

	@Test
	public void columnOrderOfgetCrossReference() throws SQLException {
		stat.executeUpdate("create table person (id integer)");
		stat.executeUpdate("create table address (pid integer, name, foreign key(pid) references person(id))");

		ResultSet cr = meta.getCrossReference(null, null, "person", null, null, "address");
		//assertTrue(cr.next());
		cr.close();
	}

    /* TODO
    @Test public void columnOrderOfgetSuperTypes() throws SQLException {
    @Test public void columnOrderOfgetSuperTables() throws SQLException {
    @Test public void columnOrderOfgetAttributes() throws SQLException {*/

	@Test
	public void columnOrderOfgetUDTs() throws SQLException {
		ResultSet rs = meta.getUDTs(null, null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(7, rsmeta.getColumnCount());
		assertEquals("TYPE_CAT", rsmeta.getColumnName(1));
		assertEquals("TYPE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TYPE_NAME", rsmeta.getColumnName(3));
		assertEquals("CLASS_NAME", rsmeta.getColumnName(4));
		assertEquals("DATA_TYPE", rsmeta.getColumnName(5));
		assertEquals("REMARKS", rsmeta.getColumnName(6));
		assertEquals("BASE_TYPE", rsmeta.getColumnName(7));
		rs.close();
	}

	@Test
	public void getIndexInfoOnTest() throws SQLException {
		ResultSet rs = meta.getIndexInfo(null,null,"test",false,false);

		assertNotNull(rs);
		rs.close();
	}

	@Test
	public void getIndexInfoIndexedSingle() throws SQLException {
		stat.executeUpdate("create table testindex (id integer primary key, fn float default 0.0, sn not null);");
		stat.executeUpdate("create index testindex_idx on testindex (sn);");

		ResultSet rs = meta.getIndexInfo(null,null,"testindex",false,false);

		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("testindex", rs.getString(3));
		assertTrue("not uniq", rs.getBoolean(4));
		assertEquals("testindex_idx", rs.getString(6));
		assertEquals(0, rs.getInt(8));
		assertEquals("sn", rs.getString(9));
		assertFalse(rs.next());
		rs.close();
	}


	@Test
	public void getIndexInfoIndexedSingleExpr() throws SQLException {
		if (org.sqlite.Conn.libversionNumber() < 3009000) {
			return;
		}
		stat.executeUpdate("create table testindex (id integer primary key, fn float default 0.0, sn not null);");
		stat.executeUpdate("create index testindex_idx on testindex (sn, fn/2);");

		ResultSet rs = meta.getIndexInfo(null,null,"testindex",false,false);

		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("testindex", rs.getString(3));
		assertTrue("not uniq", rs.getBoolean(4));
		assertEquals("testindex_idx", rs.getString(6));
		assertEquals(0, rs.getInt(8));
		assertEquals("sn", rs.getString(9));

		assertTrue(rs.next());
		assertEquals("testindex", rs.getString(3));
		assertTrue("not uniq", rs.getBoolean(4));
		assertEquals("testindex_idx", rs.getString(6));
		assertEquals(1, rs.getInt(8));
		assertNull(rs.getString(9));
		assertFalse(rs.next());
		rs.close();
	}


	@Test
	public void getIndexInfoIndexedMulti() throws SQLException {
		stat.executeUpdate("create table testindex (id integer primary key, fn float default 0.0, sn not null);");
		stat.executeUpdate("create index testindex_idx on testindex (sn);");
		stat.executeUpdate("create index testindex_pk_idx on testindex (id);");

		ResultSet rs = meta.getIndexInfo(null,null,"testindex",false,false);

		assertNotNull(rs);
		assertTrue(rs.next());
		assertEquals("testindex", rs.getString(3));
		assertTrue("not uniq", rs.getBoolean(4));
		assertEquals("testindex_idx", rs.getString(6));
		assertEquals(0, rs.getInt(8));
		assertEquals("sn", rs.getString(9));

		assertTrue(rs.next());
		assertEquals("testindex", rs.getString(3));
		assertTrue("not uniq", rs.getBoolean(4));
		assertEquals("testindex_pk_idx", rs.getString(6));
		assertEquals(0, rs.getInt(8));
		assertEquals("id", rs.getString(9));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void version() throws SQLException {
		assertNotNull(meta.getDatabaseProductVersion());
		assertTrue("1.0".equals(meta.getDriverVersion()));
	}

	@Test
	public void indexInfo() throws SQLException {
		ResultSet rs = meta.getIndexInfo(null, null, null, false, false);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(13, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("NON_UNIQUE", rsmeta.getColumnName(4));
		assertEquals("INDEX_QUALIFIER", rsmeta.getColumnName(5));
		assertEquals("INDEX_NAME", rsmeta.getColumnName(6));
		assertEquals("TYPE", rsmeta.getColumnName(7));
		assertEquals("ORDINAL_POSITION", rsmeta.getColumnName(8));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(9));
		assertEquals("ASC_OR_DESC", rsmeta.getColumnName(10));
		assertEquals("CARDINALITY", rsmeta.getColumnName(11));
		assertEquals("PAGES", rsmeta.getColumnName(12));
		assertEquals("FILTER_CONDITION", rsmeta.getColumnName(13));
		rs.close();

		rs = meta.getIndexInfo(null, null, "test", true, false);
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void primaryKeys() throws SQLException {
		ResultSet rs = meta.getPrimaryKeys(null, null, null);
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(6, rsmeta.getColumnCount());
		assertEquals("TABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("TABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("TABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("COLUMN_NAME", rsmeta.getColumnName(4));
		assertEquals("KEY_SEQ", rsmeta.getColumnName(5));
		assertEquals("PK_NAME", rsmeta.getColumnName(6));
		rs.close();

		rs = meta.getPrimaryKeys(null, null, "test");
		assertTrue(rs.next());
		assertEquals("test", rs.getString(3));
		assertEquals("id", rs.getString(4));
		assertEquals(1, rs.getInt(5));
		assertEquals("id", rs.getString(6));
		rs.close();
	}

	@Test
	public void crossReference() throws SQLException {
		ResultSet rs = meta.getCrossReference(null, null, null, null, null, "test");
		assertFalse(rs.next());
		ResultSetMetaData rsmeta = rs.getMetaData();
		assertEquals(14, rsmeta.getColumnCount());
		assertEquals("PKTABLE_CAT", rsmeta.getColumnName(1));
		assertEquals("PKTABLE_SCHEM", rsmeta.getColumnName(2));
		assertEquals("PKTABLE_NAME", rsmeta.getColumnName(3));
		assertEquals("PKCOLUMN_NAME", rsmeta.getColumnName(4));
		assertEquals("FKTABLE_CAT", rsmeta.getColumnName(5));
		assertEquals("FKTABLE_SCHEM", rsmeta.getColumnName(6));
		assertEquals("FKTABLE_NAME", rsmeta.getColumnName(7));
		assertEquals("FKCOLUMN_NAME", rsmeta.getColumnName(8));
		assertEquals("KEY_SEQ", rsmeta.getColumnName(9));
		assertEquals("UPDATE_RULE", rsmeta.getColumnName(10));
		assertEquals("DELETE_RULE", rsmeta.getColumnName(11));
		assertEquals("FK_NAME", rsmeta.getColumnName(12));
		assertEquals("PK_NAME", rsmeta.getColumnName(13));
		assertEquals("DEFERRABILITY", rsmeta.getColumnName(14));
		rs.close();
	}

	@Test
	public void virtualTable() throws SQLException {
		stat.execute("CREATE VIRTUAL TABLE vt USING fts4(name, tokenize=porter, matchinfo=fts3)");
		ResultSet rs = meta.getTables("main", null, "vt", new String[]{"table"});
		while (rs.next()) {
			System.out.println(rs.getString("TABLE_CAT") + ", " + rs.getString("TABLE_NAME"));
		}
		rs.close();
	}
}
