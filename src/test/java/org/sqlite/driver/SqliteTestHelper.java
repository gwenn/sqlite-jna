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
 * THIS SOFTWARE IS PROVIDED BY Tim Stack AND CONTRIBUTORS ''AS IS'' AND ANY
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteTestHelper {
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	protected File dbFile;
	protected Connection conn;
	protected DatabaseMetaData dbMetadata;

	@Before
	public void openConnection() throws Exception {
		dbFile = testFolder.newFile("test.db");
		conn = DriverManager.getConnection(JDBC.PREFIX + dbFile.getAbsolutePath(), null);
		dbMetadata = conn.getMetaData();
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate("CREATE TABLE test_table (id INTEGER PRIMARY KEY, name VARCHAR NOT NULL)");
			stmt.executeUpdate("CREATE INDEX test_index ON test_table (id, name)");
			stmt.executeUpdate("INSERT INTO test_table VALUES (1, 'test')");

			stmt.executeUpdate("CREATE TABLE type_table (name VARCHAR PRIMARY KEY, " +
					"birthdate DATETIME UNIQUE, height REAL, eyes INTEGER, width DECIMAL(10,2))");
			stmt.executeUpdate("CREATE TABLE prim_table (id INTEGER PRIMARY KEY, b BOOLEAN, bi BIGINT, f FLOAT, d DOUBLE)");
		}
	}

	@After
	public void closeConnection() throws SQLException {
		if (conn != null)
			conn.close();
		conn = null;
		dbFile.delete();
		dbFile = null;
	}

	protected String formatResultSetHeader(ResultSetMetaData rsm) throws SQLException {
		String retval = "|";

		for (int lpc = 1; lpc <= rsm.getColumnCount(); lpc++) {
			retval += rsm.getColumnLabel(lpc) + "|";
		}
		return retval;
	}

	protected String formatResultSetRow(ResultSet rs) throws SQLException {
		ResultSetMetaData rsm = rs.getMetaData();
		String retval = "|";

		for (int lpc = 1; lpc <= rsm.getColumnCount(); lpc++) {
			retval += rs.getString(lpc) + "|";
		}

		return retval;
	}

	protected String[] formatResultSet(ResultSet rs) throws SQLException {
		List<String> rows = new ArrayList<>();

		while (rs.next()) {
			rows.add(formatResultSetRow(rs));
		}

		return rows.toArray(new String[rows.size()]);
	}
}
