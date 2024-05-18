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
#if sqlite.enable.column.metadata == "true"
	@Test
	public void getBlob() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 Statement stmt = c.createStatement()) {
			stmt.execute("CREATE TABLE test (data BLOB)");
			assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(1024))"));
			long rowid;
			try (ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
				assertTrue(rs.next());
				rowid = rs.getLong(1);
				assertTrue(rowid > 0);
				assertFalse(rs.next());
			}

			try (ResultSet rs = stmt.executeQuery("SELECT rowid, data FROM test")) {
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
			}
		}
	}
#endif

	@Test
	public void getNullBlob() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 Statement stmt = c.createStatement()) {
			stmt.execute("CREATE TABLE test (data BLOB)");
			assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (NULL)"));
			long rowid;
			try (ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
				assertTrue(rs.next());
				rowid = rs.getLong(1);
				assertTrue(rowid > 0);
				assertFalse(rs.next());
			}

			try (ResultSet rs = stmt.executeQuery("SELECT rowid, data FROM test")) {
				assertTrue(rs.next());
				assertEquals(rowid, rs.getLong(1));
				assertEquals(String.valueOf(rowid), rs.getRowId(1).toString());
				final Blob blob = rs.getBlob(2);
				assertNull(blob);
				final InputStream stream = rs.getBinaryStream(2);
				assertNull(stream);
				assertFalse(rs.next());
			}
		}
	}

	@Test
	@Ignore
	public void setBinaryStream() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY)) {
			long rowid;
			try (Statement stmt = c.createStatement()) {
				stmt.execute("CREATE TABLE test (data BLOB)");
				assertEquals(1, stmt.executeUpdate("INSERT INTO test (data) VALUES (zeroblob(1024))"));
				ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()");
				assertTrue(rs.next());
				rowid = rs.getLong(1);
				assertTrue(rowid > 0);
				assertFalse(rs.next());
			}

			try (PreparedStatement pstmt = c.prepareStatement("UPDATE test SET data = :blob WHERE rowid = :rowid")) {
				pstmt.setRowId(2, new RowIdImpl(rowid));
				// pstmt.setBytes(1, new byte[] {1, 2, 3, 4, 5, 6});
				pstmt.setBinaryStream(1, new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6}));
				assertEquals(1, pstmt.executeUpdate());
			}
		}
	}
}
