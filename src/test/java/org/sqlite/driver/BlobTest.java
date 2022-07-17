package org.sqlite.driver;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.*;

public class BlobTest {
	//#if mvn.project.property.sqlite.enable.column.metadata == "true"
	@Test
	public void getBlob() throws SQLException {
		Connection c = null;
		try {
			c = DriverManager.getConnection(JDBC.MEMORY);
			final Statement stmt = c.createStatement();
			stmt.execute("CREATE TABLE test (data BLOB)");
			assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(1024))"));
			ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
			assertTrue(rs.next());
			final long rowid = rs.getLong(1);
			assertTrue(rowid > 0);
			assertFalse(rs.next());

			rs = stmt.executeQuery("SELECT rowid, data FROM test");
			assertTrue(rs.next());
			assertEquals(rowid, rs.getLong(1));
			assertEquals(String.valueOf(rowid), rs.getRowId(1).toString());
			final Blob blob = rs.getBlob(2);
			assertNotNull(blob);
			final long length = blob.length();
			assertEquals(1024, length);
			final byte[] bytes = blob.getBytes(1, 1024);
			assertNotNull(bytes);
			assertEquals(length, bytes.length);
			assertFalse(rs.next());

			blob.free();
			rs.close();
			stmt.close();
		} finally {
			if (null != c) c.close();
		}
	}
	//#endif

	@Test
	public void getNullBlob() throws SQLException {
		Connection c = null;
		try {
			c = DriverManager.getConnection(JDBC.MEMORY);
			final Statement stmt = c.createStatement();
			stmt.execute("CREATE TABLE test (data BLOB)");
			assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (NULL)"));
			ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
			assertTrue(rs.next());
			final long rowid = rs.getLong(1);
			assertTrue(rowid > 0);
			assertFalse(rs.next());

			rs = stmt.executeQuery("SELECT rowid, data FROM test");
			assertTrue(rs.next());
			assertEquals(rowid, rs.getLong(1));
			assertEquals(String.valueOf(rowid), rs.getRowId(1).toString());
			final Blob blob = rs.getBlob(2);
			assertNull(blob);
			final InputStream stream = rs.getBinaryStream(2);
			assertNull(stream);
			assertFalse(rs.next());

			rs.close();
			stmt.close();
		} finally {
			if (null != c) c.close();
		}
	}

	@Test
	@Ignore
	public void setBinaryStream() throws SQLException {
		Connection c = null;
		try {
			c = DriverManager.getConnection(JDBC.MEMORY);
			final Statement stmt = c.createStatement();
			stmt.execute("CREATE TABLE test (data BLOB)");
			assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(1024))"));
			ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
			assertTrue(rs.next());
			final long rowid = rs.getLong(1);
			assertTrue(rowid > 0);
			assertFalse(rs.next());
			stmt.close();

			final PreparedStatement pstmt = c.prepareStatement("UPDATE test SET data = :blob WHERE rowid = :rowid");
			pstmt.setRowId(2, new RowIdImpl(rowid));
			// pstmt.setBytes(1, new byte[] {1, 2, 3, 4, 5, 6});
			pstmt.setBinaryStream(1, new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6}));
			assertEquals(1, pstmt.executeUpdate());
		} finally {
			if (null != c) c.close();
		}
	}
}
