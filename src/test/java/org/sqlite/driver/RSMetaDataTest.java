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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import static org.junit.Assert.*;

public class RSMetaDataTest {
	private Connection conn;
	private Statement stat;
	private ResultSetMetaData meta;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.MEMORY);
		stat = conn.createStatement();
		stat.executeUpdate("create table People (pid integer primary key autoincrement, "
				+ " firstname text, surname text, dob date);");
		stat.executeUpdate("insert into people values (null, 'Mohandas', 'Gandhi', " + " '1869-10-02');");
		meta = stat.executeQuery("select pid, firstname, surname from people;").getMetaData();
	}

	@After
	public void close() throws SQLException {
		stat.executeUpdate("drop table people;");
		stat.close();
		conn.close();
	}

	@Test
	public void catalogName() throws SQLException {
		assertEquals("main", meta.getCatalogName(1));
	}

	@Test
	public void columns() throws SQLException {
		assertEquals(3, meta.getColumnCount());
		assertEquals("pid", meta.getColumnName(1));
		assertEquals("firstname", meta.getColumnName(2));
		assertEquals("surname", meta.getColumnName(3));
		assertEquals(Types.INTEGER, meta.getColumnType(1));
		assertEquals(Types.VARCHAR, meta.getColumnType(2));
		assertEquals(Types.VARCHAR, meta.getColumnType(3));
		assertEquals("integer", meta.getColumnTypeName(1));
		assertEquals("text", meta.getColumnTypeName(2));
		assertEquals("text", meta.getColumnTypeName(3));
		assertTrue(meta.isAutoIncrement(1));
		assertFalse(meta.isAutoIncrement(2));
		assertFalse(meta.isAutoIncrement(3));
		assertEquals(ResultSetMetaData.columnNullable, meta.isNullable(1));
		assertEquals(ResultSetMetaData.columnNullable, meta.isNullable(2));
		assertEquals(ResultSetMetaData.columnNullable, meta.isNullable(3));
	}

	@Test
	public void expression() throws SQLException {
		final ResultSet rs = stat.executeQuery("SELECT NULL UNION SELECT 1");
		meta = rs.getMetaData();
		assertNull(meta.getColumnTypeName(1));
		rs.next();
		assertNull(meta.getColumnTypeName(1));
		rs.next();
		assertNull(meta.getColumnTypeName(1));
		rs.close();
	}

	@Test
	public void columnTypes() throws SQLException {
		stat.executeUpdate(
				"create table tbl (col1 INT, col2 INTEGER, col3 TINYINT, " +
						"col4 SMALLINT, col5 MEDIUMINT, col6 BIGINT, col7 UNSIGNED BIG INT, " +
						"col8 INT2, col9 INT8, col10 CHARACTER(20), col11 VARCHAR(255), " +
						"col12 VARYING CHARACTER(255), col13 NCHAR(55), " +
						"col14 NATIVE CHARACTER(70), col15 NVARCHAR(100), col16 TEXT, " +
						"col17 CLOB, col18 BLOB, col19 REAL, col20 DOUBLE, " +
						"col21 DOUBLE PRECISION, col22 FLOAT, col23 NUMERIC, " +
						"col24 DECIMAL(10,5), col25 BOOLEAN, col26 DATE, col27 DATETIME)"
		);
		// insert empty data into table otherwise getColumnType returns null
		stat.executeUpdate(
				"insert into tbl values (1, 2, 3, 4, 5, 6, 7, 8, 9," +
						"'c', 'varchar', 'varying', 'n', 'n','nvarchar', 'text', 'clob'," +
						"null, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 0, 12345, 123456)"
		);
		meta = stat.executeQuery(
				"select col1, col2, col3, col4, col5, col6, col7, col8, col9, " +
						"col10, col11, col12, col13, col14, col15, col16, col17, col18, " +
						"col19, col20, col21, col22, col23, col24, col25, col26, col27, " +
						"cast(col1 as boolean) from tbl"
		).getMetaData();

		assertEquals(Types.INTEGER, meta.getColumnType(1));
		assertEquals(Types.INTEGER, meta.getColumnType(2));
		assertEquals(Types.INTEGER, meta.getColumnType(3));
		assertEquals(Types.INTEGER, meta.getColumnType(4));
		assertEquals(Types.INTEGER, meta.getColumnType(5));
		assertEquals(Types.INTEGER, meta.getColumnType(6));
		assertEquals(Types.INTEGER, meta.getColumnType(7));
		assertEquals(Types.INTEGER, meta.getColumnType(8));
		assertEquals(Types.INTEGER, meta.getColumnType(9));

		assertEquals(Types.VARCHAR, meta.getColumnType(10));
		assertEquals(Types.VARCHAR, meta.getColumnType(11));
		assertEquals(Types.VARCHAR, meta.getColumnType(12));
		assertEquals(Types.VARCHAR, meta.getColumnType(13));
		assertEquals(Types.VARCHAR, meta.getColumnType(14));
		assertEquals(Types.VARCHAR, meta.getColumnType(15));
		assertEquals(Types.VARCHAR, meta.getColumnType(16));
		assertEquals(Types.VARCHAR, meta.getColumnType(17));

		assertEquals(Types.OTHER, meta.getColumnType(18));

		assertEquals(Types.REAL, meta.getColumnType(19));
		assertEquals(Types.REAL, meta.getColumnType(20));
		assertEquals(Types.REAL, meta.getColumnType(21));
		assertEquals(Types.REAL, meta.getColumnType(22));
		assertEquals(Types.NUMERIC, meta.getColumnType(23));
		assertEquals(Types.NUMERIC, meta.getColumnType(24));
		assertEquals(Types.NUMERIC, meta.getColumnType(25));

		assertEquals(Types.NUMERIC, meta.getColumnType(26));
		assertEquals(Types.NUMERIC, meta.getColumnType(27));

		assertEquals(Types.OTHER, meta.getColumnType(28));

		assertEquals(15, meta.getPrecision(24));
		assertEquals(15, meta.getScale(24));
	}

	@Test
	public void differentRS() throws SQLException {
		meta = stat.executeQuery("select * from people;").getMetaData();
		assertEquals(4, meta.getColumnCount());
		assertEquals("pid", meta.getColumnName(1));
		assertEquals("firstname", meta.getColumnName(2));
		assertEquals("surname", meta.getColumnName(3));
		assertEquals("dob", meta.getColumnName(4));
	}

	@Test
	public void nullable() throws SQLException {
		meta = stat.executeQuery("select null;").getMetaData();
		assertEquals(ResultSetMetaData.columnNullable, meta.isNullable(1));
	}

	@Test(expected = SQLException.class)
	public void badCatalogIndex() throws SQLException {
		meta.getCatalogName(4);
	}

	@Test(expected = SQLException.class)
	public void badColumnIndex() throws SQLException {
		meta.getColumnName(4);
	}

	@Test
	public void scale() throws SQLException {
		assertEquals(0, meta.getScale(2));
		assertEquals(0, meta.getScale(3));
	}
}
