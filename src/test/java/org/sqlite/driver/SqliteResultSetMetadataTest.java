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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;

import static org.junit.Assert.*;

public class SqliteResultSetMetadataTest extends SqliteTestHelper {
	@Test
	public void testColumnType() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery("SELECT * FROM type_table")) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(5, rsm.getColumnCount());

				assertEquals("main", rsm.getSchemaName(1));
				assertEquals("type_table", rsm.getTableName(1));
				assertFalse(rsm.isAutoIncrement(1));
				assertEquals(ResultSetMetaData.columnNullable, rsm.isNullable(1));
				assertEquals("name", rsm.getColumnLabel(1));
				assertEquals("name", rsm.getColumnName(1));
				assertEquals(Types.VARCHAR, rsm.getColumnType(1));

				assertFalse(rs.next());
				assertEquals(Types.VARCHAR, rsm.getColumnType(1));

				rs.close();
				assertEquals(Types.VARCHAR, rsm.getColumnType(1));
			}

			try (ResultSet rs = stmt.executeQuery("SELECT name as namelabel, width FROM type_table")) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(2, rsm.getColumnCount());

				assertEquals("main", rsm.getSchemaName(1));
				assertEquals("type_table", rsm.getTableName(1));
				assertFalse(rsm.isAutoIncrement(1));
				assertEquals(ResultSetMetaData.columnNullable, rsm.isNullable(1));
				assertEquals("namelabel", rsm.getColumnLabel(1));
				assertEquals("name", rsm.getColumnName(1));
				assertEquals(Types.VARCHAR, rsm.getColumnType(1));

				assertTrue(rsm.isSigned(2));
			}
		}
	}

	@Test
	public void testNoTable() throws Exception {
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
				ResultSetMetaData rsm = rs.getMetaData();

				assertEquals(1, rsm.getColumnCount());

				assertEquals("", rsm.getCatalogName(1));
				assertEquals("", rsm.getSchemaName(1));
				assertEquals("", rsm.getTableName(1));
				assertFalse(rsm.isAutoIncrement(1));
				assertEquals(ResultSetMetaData.columnNullable, rsm.isNullable(1));
				assertEquals("1", rsm.getColumnLabel(1));
				assertEquals("1", rsm.getColumnName(1));
				assertEquals(Types.OTHER, rsm.getColumnType(1));
			}
		}
	}
}
