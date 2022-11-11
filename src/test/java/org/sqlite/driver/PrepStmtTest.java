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

import org.sqlite.ErrCodes;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

public class PrepStmtTest {
	private static final byte[] b1 = {1, 2, 7, 4, 2, 6, 2, 8, 5, 2, 3, 1, 5, 3, 6, 3, 3, 6, 2, 5};
	private static final byte[] b2 = "To be or not to be.".getBytes();
	private static final byte[] b3 = "Question!#$%".getBytes();
	private static final String utf01 = "\uD840\uDC40";
	private static final String utf02 = "\uD840\uDC47 ";
	private static final String utf03 = " \uD840\uDC43";
	private static final String utf04 = " \uD840\uDC42 ";
	private static final String utf05 = "\uD840\uDC40\uD840\uDC44";
	private static final String utf06 = "Hello World, \uD840\uDC40 \uD880\uDC99";
	private static final String utf07 = "\uD840\uDC41 testing \uD880\uDC99";
	private static final String utf08 = "\uD840\uDC40\uD840\uDC44 testing";

	private Connection conn;
	private Statement stat;

	@Before
	public void connect() throws Exception {
		conn = DriverManager.getConnection(JDBC.TEMP_FILE);
		stat = conn.createStatement();
	}

	@After
	public void close() throws SQLException {
		stat.close();
		conn.close();
	}

	@Test
	public void update() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("create table s1 (c1);");
		assertEquals(0, prep.executeUpdate());
		prep.close();
		prep = conn.prepareStatement("insert into s1 values (?);");
		prep.setInt(1, 3);
		assertEquals(1, prep.executeUpdate());
		assertNull(prep.getResultSet());
		prep.setInt(1, 5);
		assertEquals(1, prep.executeUpdate());
		prep.setInt(1, 7);
		assertEquals(1, prep.executeUpdate());
		prep.close();

		// check results with normal statement
		ResultSet rs = stat.executeQuery("select sum(c1) from s1;");
		assertTrue(rs.next());
		assertEquals(15, rs.getInt(1));
		rs.close();
	}

	@Test
	public void multiUpdate() throws SQLException {
		stat.executeUpdate("create table test (c1);");
		PreparedStatement prep = conn.prepareStatement("insert into test values (?);");

		for (int i = 0; i < 10; i++) {
			prep.setInt(1, i);
			prep.executeUpdate();
			prep.execute();
		}

		prep.close();
		stat.executeUpdate("drop table test;");
	}

	@Test
	public void emptyRS() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("select null limit 0;");
		ResultSet rs = prep.executeQuery();
		assertFalse(rs.next());
		rs.close();
		prep.close();
	}

	@Test
	public void singleRowRS() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("select ?;");
		prep.setInt(1, Integer.MAX_VALUE);
		ResultSet rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(Integer.MAX_VALUE, rs.getInt(1));
		assertEquals(Integer.toString(Integer.MAX_VALUE), rs.getString(1));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE).doubleValue(), rs.getDouble(1), 0.0001);
		assertFalse(rs.next());
		assertTrue(rs.isAfterLast());
		assertFalse(rs.next());
		rs.close();
		prep.close();
	}

	@Test
	public void twoRowRS() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("select ? union all select ?;");
		prep.setDouble(1, Double.MAX_VALUE);
		prep.setDouble(2, Double.MIN_VALUE);
		ResultSet rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(Double.MAX_VALUE, rs.getDouble(1), 0.0001);
		assertTrue(rs.next());
		assertEquals(Double.MIN_VALUE, rs.getDouble(1), 0.0001);
		assertFalse(rs.next());
		rs.close();
		prep.close();
	}

	@Test
	public void stringRS() throws SQLException {
		String name = "Gandhi";
		PreparedStatement prep = conn.prepareStatement("select ?;");
		prep.setString(1, name);
		ResultSet rs = prep.executeQuery();
		assertEquals(-1, prep.getUpdateCount());
		assertTrue(rs.next());
		assertEquals(name, rs.getString(1));
		assertFalse(rs.next());
		rs.close();
		prep.close();
	}

	@Test
	public void finalizePrep() throws SQLException {
		conn.prepareStatement("select null;");
		System.gc();
	}

	@Test
	public void set() throws SQLException, UnsupportedEncodingException {
		ResultSet rs;
		PreparedStatement prep = conn.prepareStatement("select ?, ?, ?;");

		// integers
		prep.setInt(1, Integer.MIN_VALUE);
		prep.setInt(2, Integer.MAX_VALUE);
		prep.setInt(3, 0);
		rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(Integer.MIN_VALUE, rs.getInt(1));
		assertEquals(Integer.MAX_VALUE, rs.getInt(2));
		assertEquals(0, rs.getInt(3));
		rs.close();

		// strings
		String name = "Winston Leonard Churchill";
		String fn = name.substring(0, 7), mn = name.substring(8, 15), sn = name.substring(16, 25);
		prep.clearParameters();
		prep.setString(1, fn);
		prep.setString(2, mn);
		prep.setString(3, sn);
		rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(fn, rs.getString(1));
		assertEquals(mn, rs.getString(2));
		assertEquals(sn, rs.getString(3));
		rs.close();

		// mixed
		prep.setString(1, name);
		prep.setString(2, null);
		prep.setLong(3, Long.MAX_VALUE);
		rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(name, rs.getString(1));
		assertNull(rs.getString(2));
		assertTrue(rs.wasNull());
		assertEquals(Long.MAX_VALUE, rs.getLong(3));
		rs.close();

		// bytes
		prep.setBytes(1, b1);
		prep.setBytes(2, b2);
		prep.setBytes(3, b3);
		rs = prep.executeQuery();
		assertTrue(rs.next());
		assertArrayEquals(b1, rs.getBytes(1));
		assertArrayEquals(b2, rs.getBytes(2));
		assertArrayEquals(b3, rs.getBytes(3));
		assertFalse(rs.next());
		rs.close();

		// streams
		ByteArrayInputStream inByte = new ByteArrayInputStream(b1);
		prep.setBinaryStream(1, inByte, b1.length);
		ByteArrayInputStream inAscii = new ByteArrayInputStream(b2);
		prep.setBinaryStream(2, inAscii, b2.length);
		byte[] b3 = utf08.getBytes(StandardCharsets.UTF_8);
		ByteArrayInputStream inUnicode = new ByteArrayInputStream(b3);
		prep.setBinaryStream(3, inUnicode, b3.length);

		rs = prep.executeQuery();
		assertTrue(rs.next());
		assertArrayEquals(b1, rs.getBytes(1));
		assertEquals(new String(b2, StandardCharsets.US_ASCII), rs.getString(2));
		assertEquals(utf08, rs.getString(3));
		assertFalse(rs.next());
		rs.close();
		prep.close();
	}

	@Test
	public void colNameAccess() throws SQLException {
		try (PreparedStatement prep = conn.prepareStatement("select ? as col1, ? as col2, ? as bingo;")) {
			prep.setNull(1, 0);
			prep.setFloat(2, Float.MIN_VALUE);
			prep.setShort(3, Short.MIN_VALUE);
			prep.executeQuery();
			try (ResultSet rs = prep.executeQuery()) {
				assertTrue(rs.next());
				assertNull(rs.getString("col1"));
				assertTrue(rs.wasNull());
				assertEquals(Float.MIN_VALUE, rs.getFloat("col2"), 0.0001);
				assertEquals(Short.MIN_VALUE, rs.getShort("bingo"));
			}
		}
	}

	@Test
	public void insert1000() throws SQLException {
		stat.executeUpdate("create table in1000 (a);");
		PreparedStatement prep = conn.prepareStatement("insert into in1000 values (?);");
		conn.setAutoCommit(false);
		for (int i = 0; i < 1000; i++) {
			prep.setInt(1, i);
			prep.executeUpdate();
		}
		prep.close();
		conn.commit();

		ResultSet rs = stat.executeQuery("select count(a) from in1000;");
		assertTrue(rs.next());
		assertEquals(1000, rs.getInt(1));
		rs.close();
	}

	@Test
	public void getObject() throws SQLException {
		stat.executeUpdate("create table testobj (" + "c1 integer, c2 float, c3, c4 numeric, c5 bit, c6, c7);");
		PreparedStatement prep = conn.prepareStatement("insert into testobj values (?,?,?,?,?,?,?);");

		prep.setInt(1, Integer.MAX_VALUE);
		prep.setFloat(2, Float.MAX_VALUE);
		prep.setDouble(3, Double.MAX_VALUE);
		prep.setLong(4, Long.MAX_VALUE);
		prep.setBoolean(5, false);
		prep.setByte(6, (byte) 7);
		prep.setBytes(7, b1);
		prep.executeUpdate();
		prep.close();

		ResultSet rs = stat.executeQuery("select c1,c2,c3,c4,c5,c6,c7 from testobj;");
		assertTrue(rs.next());

		assertEquals(Integer.MAX_VALUE, rs.getInt(1));
		assertEquals(Integer.MAX_VALUE, (int) rs.getLong(1));
		assertEquals(Float.MAX_VALUE, rs.getFloat(2), 0f);
		assertEquals(Double.MAX_VALUE, rs.getDouble(3), 0d);
		assertEquals(Long.MAX_VALUE, rs.getLong(4));
		assertFalse(rs.getBoolean(5));
		assertEquals((byte) 7, rs.getByte(6));
		assertArrayEquals(b1, rs.getBytes(7));

		assertNotNull(rs.getObject(1));
		assertNotNull(rs.getObject(2));
		assertNotNull(rs.getObject(3));
		assertNotNull(rs.getObject(4));
		assertNotNull(rs.getObject(5));
		assertNotNull(rs.getObject(6));
		assertNotNull(rs.getObject(7));
		assertTrue(rs.getObject(1) instanceof Integer);
		assertTrue(rs.getObject(2) instanceof Double);
		assertTrue(rs.getObject(3) instanceof Double);
		assertTrue(rs.getObject(4) instanceof Long);
		assertTrue(rs.getObject(5) instanceof Integer);
		assertTrue(rs.getObject(6) instanceof Integer);
		assertTrue(rs.getObject(7) instanceof byte[]);
		rs.close();
	}

	@Test
	public void tokens() throws SQLException {
				/* checks for a bug where a substring is read by the driver as the
         * full original string, caused by my idiocyin assuming the
         * pascal-style string was null terminated. Thanks Oliver Randschau. */
		StringTokenizer st = new StringTokenizer("one two three");
		st.nextToken();
		String substr = st.nextToken();

		PreparedStatement prep = conn.prepareStatement("select ?;");
		prep.setString(1, substr);
		ResultSet rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(substr, rs.getString(1));
		prep.close();
	}

	@Test
	public void utf() throws SQLException {
		ResultSet rs = stat.executeQuery("select '" + utf01 + "','" + utf02 + "','" + utf03 + "','" + utf04 + "','"
				+ utf05 + "','" + utf06 + "','" + utf07 + "','" + utf08 + "';");
		assertTrue(rs.next());
		assertEquals(utf01, rs.getString(1));
		assertEquals(utf02, rs.getString(2));
		assertEquals(utf03, rs.getString(3));
		assertEquals(utf04, rs.getString(4));
		assertEquals(utf05, rs.getString(5));
		assertEquals(utf06, rs.getString(6));
		assertEquals(utf07, rs.getString(7));
		assertEquals(utf08, rs.getString(8));
		rs.close();

		PreparedStatement prep = conn.prepareStatement("select ?,?,?,?,?,?,?,?;");
		prep.setString(1, utf01);
		prep.setString(2, utf02);
		prep.setString(3, utf03);
		prep.setString(4, utf04);
		prep.setString(5, utf05);
		prep.setString(6, utf06);
		prep.setString(7, utf07);
		prep.setString(8, utf08);
		rs = prep.executeQuery();
		assertTrue(rs.next());
		assertEquals(utf01, rs.getString(1));
		assertEquals(utf02, rs.getString(2));
		assertEquals(utf03, rs.getString(3));
		assertEquals(utf04, rs.getString(4));
		assertEquals(utf05, rs.getString(5));
		assertEquals(utf06, rs.getString(6));
		assertEquals(utf07, rs.getString(7));
		assertEquals(utf08, rs.getString(8));
		rs.close();
		prep.close();
	}

	@Test
	public void batch() throws SQLException {
		ResultSet rs;

		stat.executeUpdate("create table test (c1, c2, c3, c4);");
		PreparedStatement prep = conn.prepareStatement("insert into test values (?,?,?,?);");
		for (int i = 0; i < 10; i++) {
			prep.setInt(1, Integer.MIN_VALUE + i);
			prep.setFloat(2, Float.MIN_VALUE + i);
			prep.setString(3, "Hello " + i);
			prep.setDouble(4, Double.MAX_VALUE + i);
			prep.addBatch();
		}
		assertArrayEquals(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, prep.executeBatch());
		prep.close();

		rs = stat.executeQuery("select * from test;");
		for (int i = 0; i < 10; i++) {
			assertTrue(rs.next());
			assertEquals(Integer.MIN_VALUE + i, rs.getInt(1));
			assertEquals(Float.MIN_VALUE + i, rs.getFloat(2), 0.0001);
			assertEquals("Hello " + i, rs.getString(3));
			assertEquals(Double.MAX_VALUE + i, rs.getDouble(4), 0.0001);
		}
		rs.close();
		stat.executeUpdate("drop table test;");
	}

	@Test
	public void testExecuteBatch() throws Exception {
		stat.executeUpdate("create table t (c text);");
		PreparedStatement prep = conn.prepareStatement("insert into t values (?);");
		prep.setString(1, "a");
		prep.addBatch();
		int call1_length = prep.executeBatch().length;
		prep.setString(1, "b");
		prep.addBatch();
		int call2_length = prep.executeBatch().length;
		prep.close();

		assertEquals(1, call1_length);
		assertEquals(1, call2_length);

		ResultSet rs = stat.executeQuery("select * from t");
		rs.next();
		assertEquals("a", rs.getString(1));
		rs.next();
		assertEquals("b", rs.getString(1));
		rs.close();
	}

	@Test
	public void retainKeysInBatch() throws SQLException {
		stat.executeUpdate("create table test (c1, c2);");
		PreparedStatement prep = conn.prepareStatement(
				"insert into test values (?, ?);"
		);
		prep.setInt(1, 10);
		prep.setString(2, "ten");
		prep.addBatch();
		prep.setInt(1, 100);
		prep.setString(2, "hundred");
		prep.addBatch();
		prep.setString(2, "one hundred");
		prep.addBatch();
		prep.setInt(1, 1000);
		prep.setString(2, "thousand");
		prep.addBatch();
		prep.executeBatch();
		prep.close();

		ResultSet rs = stat.executeQuery("select * from test;");
		assertTrue(rs.next());
		assertEquals(10, rs.getInt(1));
		assertEquals("ten", rs.getString(2));
		assertTrue(rs.next());
		assertEquals(100, rs.getInt(1));
		assertEquals("hundred", rs.getString(2));
		assertTrue(rs.next());
		assertEquals(100, rs.getInt(1));
		assertEquals("one hundred", rs.getString(2));
		assertTrue(rs.next());
		assertEquals(1000, rs.getInt(1));
		assertEquals("thousand", rs.getString(2));
		assertFalse(rs.next());
		rs.close();
	}

	@Test
	public void dblock() throws SQLException {
		stat.executeUpdate("create table test (c1);");
		stat.executeUpdate("insert into test values (1);");
		final PreparedStatement prep = conn.prepareStatement("select * from test;");
		prep.executeQuery().close();
		stat.executeUpdate("drop table test;");
		prep.close();
	}

	@Test
	public void dbclose() throws SQLException {
		conn.prepareStatement("select ?;").setString(1, "Hello World");
		conn.prepareStatement("select null;").close();
		conn.prepareStatement("select null;").executeQuery().close();
		conn.prepareStatement("create table t (c);").executeUpdate();
		conn.prepareStatement("select null;");
	}

	@Test
	public void batchOneParam() throws SQLException {
		stat.executeUpdate("create table test (c1);");
		PreparedStatement prep = conn.prepareStatement("insert into test values (?);");
		for (int i = 0; i < 10; i++) {
			prep.setInt(1, Integer.MIN_VALUE + i);
			prep.addBatch();
		}
		assertArrayEquals(prep.executeBatch(), new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
		prep.close();
		ResultSet rs = stat.executeQuery("select count(*) from test;");
		assertTrue(rs.next());
		assertEquals(10, rs.getInt(1));
		rs.close();
	}

	@Test
	public void paramMetaData() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("select ?,?,?,?;");
		assertEquals(4, prep.getParameterMetaData().getParameterCount());
		prep.close();
	}

	//#if mvn.project.property.sqlite.enable.column.metadata == "true"
	@Test
	public void metaData() throws SQLException {
		try (PreparedStatement prep = conn.prepareStatement("select ? as col1, ? as col2, ? as delta;")) {
			ResultSetMetaData meta = prep.getMetaData();
			assertEquals(3, meta.getColumnCount());
			assertEquals("col1", meta.getColumnName(1));
			assertEquals("col2", meta.getColumnName(2));
			assertEquals("delta", meta.getColumnName(3));
        /*assertEquals(meta.getColumnType(1), Types.INTEGER);
        assertEquals(meta.getColumnType(2), Types.INTEGER);
        assertEquals(meta.getColumnType(3), Types.INTEGER);*/

			prep.setInt(1, 2);
			prep.setInt(2, 3);
			prep.setInt(3, -1);
			meta = prep.executeQuery().getMetaData();
			assertEquals(3, meta.getColumnCount());
		}
	}
	//#endif

	@Test
	public void date1() throws SQLException {
		close();
		final Properties info = new Properties();
		info.put(DateUtil.DATE_FORMAT, DateUtil.UNIXEPOCH);
		conn = DriverManager.getConnection(JDBC.TEMP_FILE, info);
		stat = conn.createStatement();

		Date d1 = new Date(987654321);

		stat.execute("create table t (c1);");
		PreparedStatement prep = conn.prepareStatement("insert into t values(?);");
		prep.setDate(1, d1);
		prep.executeUpdate();
		prep.setDate(1, null);
		prep.executeUpdate();
		prep.close();

		ResultSet rs = stat.executeQuery("select c1 from t;");
		assertTrue(rs.next());
		final long expected = DateUtil.normalizeDate(d1.getTime(), null);
		assertEquals(expected, rs.getLong(1));
		assertEquals(expected, rs.getDate(1).getTime());
		assertTrue(rs.next());
		assertNull(rs.getDate(1));
		rs.close();
	}

	@Test
	public void date2() throws SQLException {
		Date d1 = new Date(1092941466000L);
		stat.execute("create table t (c1);");
		PreparedStatement prep = conn.prepareStatement("insert into t values (datetime(?/1000, 'unixepoch'));");
		prep.setObject(1, d1, Types.INTEGER);
		prep.executeUpdate();
		prep.close();

		ResultSet rs = stat.executeQuery("select strftime('%s', c1) * 1000 from t;");
		assertTrue(rs.next());
		final long expected = DateUtil.normalizeDate(d1.getTime(), null);
		assertEquals(expected, rs.getLong(1));
		assertEquals(expected, rs.getDate(1).getTime());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void date() throws SQLException {
		Date d1 = new Date(System.currentTimeMillis());

		stat.execute("create table t (c1);");
		PreparedStatement prep = conn.prepareStatement(
				"insert into t values(?);");
		prep.setDate(1, d1);
		prep.executeUpdate();
		prep.close();

		ResultSet rs = stat.executeQuery("select c1 from t;");
		assertTrue(rs.next());
		final Date d2 = rs.getDate(1);
		assertEquals(d1.getYear(), d2.getYear());
		assertEquals(d1.getMonth(), d2.getMonth());
		assertEquals(d1.getDay(), d2.getDay());
		rs.close();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void time() throws SQLException {
		Time d1 = new Time(System.currentTimeMillis());

		stat.execute("create table t (c1);");
		PreparedStatement prep = conn.prepareStatement(
				"insert into t values (?);");
		prep.setTime(1, d1);
		prep.executeUpdate();
		prep.close();

		ResultSet rs = stat.executeQuery("select c1 from t;");
		assertTrue(rs.next());
		assertEquals(d1.getHours(), rs.getTime(1).getHours());
		assertEquals(d1.getMinutes(), rs.getTime(1).getMinutes());
		assertEquals(d1.getSeconds(), rs.getTime(1).getSeconds());
		rs.close();
	}

	@Test
	public void timestamp() throws SQLException {
		long now = System.currentTimeMillis();
		Timestamp d1 = new Timestamp(now);
		Date d2 = new Date(now);
		Time d3 = new Time(now);

		stat.execute("create table t (c1);");
		try (PreparedStatement prep = conn.prepareStatement("insert into t values (?);")) {
			prep.setTimestamp(1, d1);
			prep.executeUpdate();
		}

		try (ResultSet rs = stat.executeQuery("select c1 from t;")) {
			assertTrue(rs.next());
			assertEquals(d1, rs.getTimestamp(1));
		}

		try (ResultSet rs = stat.executeQuery("select date(c1, 'localtime') from t;")) {
			assertTrue(rs.next());
			assertEquals(d2.toString(), rs.getString(1));
		}

		try (ResultSet rs = stat.executeQuery("select time(c1, 'localtime') from t;")) {
			assertTrue(rs.next());
			assertEquals(d3.toString(), rs.getString(1));
		}

		try (ResultSet rs = stat.executeQuery("select strftime('%Y-%m-%d %H:%M:%f', c1, 'localtime') from t;")) {
			assertTrue(rs.next());
			// assertEquals(d1.toString(), rs.getString(1)); // ms are not occurate...
		}
	}

	@Test
	public void changeSchema() throws SQLException {
		stat.execute("create table t (c1);");
		try (PreparedStatement prep = conn.prepareStatement("insert into t values (?);")) {
			stat.execute("create table t2 (c2);");
			prep.setInt(1, 1000);
			prep.execute();
			prep.executeUpdate();
		}
	}

	//    @Ignore
	//    @Test
	//    public void multipleStatements() throws SQLException
	//    {
	//        PreparedStatement prep = conn
	//                .prepareStatement("create table person (id integer, name string); insert into person values(1, 'leo'); insert into person values(2, 'yui');");
	//        prep.executeUpdate();
	//
	//        ResultSet rs = conn.createStatement().executeQuery("select * from person");
	//        assertTrue(rs.next());
	//        assertTrue(rs.next());
	//    }

	@Test
	public void reusingSetValues() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("select ?,?;");
		prep.setInt(1, 9);

		for (int i = 0; i < 10; i++) {
			prep.setInt(2, i);
			try (ResultSet rs = prep.executeQuery()) {
				assertTrue(rs.next());
				assertEquals(9, rs.getInt(1));
				assertEquals(i, rs.getInt(2));
			}
		}

		for (int i = 0; i < 10; i++) {
			prep.setInt(2, i);
			try (ResultSet rs = prep.executeQuery()) {
				assertTrue(rs.next());
				assertEquals(9, rs.getInt(1));
				assertEquals(i, rs.getInt(2));
			}
		}

		prep.close();
	}

	@Test
	public void clearParameters() throws SQLException {
		stat.executeUpdate("create table tbl (colid integer primary key AUTOINCREMENT, col varchar)");
		stat.executeUpdate("insert into tbl(col) values (\"foo\")");

		PreparedStatement prep = conn.prepareStatement("select colid from tbl where col = ?");

		prep.setString(1, "foo");

		try (ResultSet rs = prep.executeQuery()) {

			prep.clearParameters();
			rs.next();

			assertEquals(1, rs.getInt(1));
		}

		try {
			prep.execute();
			fail("Returned result when values not bound to prepared statement");
		} catch (Exception e) {
			assertEquals("a value must be provided for each parameter marker in the PreparedStatement object before it can be executed.", e.getMessage());
		}

		try {
			try (ResultSet rs = prep.executeQuery()) {
				fail("Returned result when values not bound to prepared statement");
			}
		} catch (Exception e) {
			assertEquals("a value must be provided for each parameter marker in the PreparedStatement object before it can be executed.", e.getMessage());
		}

		prep.close();

		try {
			prep = conn.prepareStatement("insert into tbl(col) values (?)");
			prep.clearParameters();
			prep.executeUpdate();
			fail("Returned result when values not bound to prepared statement");
		} catch (Exception e) {
			assertEquals("a value must be provided for each parameter marker in the PreparedStatement object before it can be executed.", e.getMessage());
		} finally {
			prep.close();
		}
	}

	@Test
	public void setmaxrows() throws SQLException {
		try (PreparedStatement prep = conn.prepareStatement("select 1 union select 2;")) {
			prep.setMaxRows(1);
			try (ResultSet rs = prep.executeQuery()) {
				assertTrue(rs.next());
				assertEquals(1, rs.getInt(1));
				assertFalse(rs.next());
			}
		}
	}

	@Test
	public void doubleclose() throws SQLException {
		PreparedStatement prep = conn.prepareStatement("select null;");
		try (ResultSet rs = prep.executeQuery()) {
			rs.close();
		}
		prep.close();
		prep.close();
	}

	@Test(expected = SQLException.class)
	public void noSuchTable() throws SQLException {
		try (PreparedStatement prep = conn.prepareStatement("select * from doesnotexist;")) {
			prep.executeQuery();
		}
	}

	@Test(expected = SQLException.class)
	public void noSuchCol() throws SQLException {
		try (PreparedStatement prep = conn.prepareStatement("select notacol from (select 1);")) {
			prep.executeQuery();
		}
	}

	@Test(expected = SQLException.class)
	public void noSuchColName() throws SQLException {
		try (PreparedStatement stmt = conn.prepareStatement("select 1;")) {
			try (ResultSet rs = stmt.executeQuery()) {
				assertTrue(rs.next());
				rs.getInt("noSuchColName");
			}
		}
	}

	@Test
	public void constraintErrorCodeExecute() throws SQLException {
		assertEquals(0, stat.executeUpdate("create table foo (id integer, CONSTRAINT U_ID UNIQUE (id));"));
		assertEquals(1, stat.executeUpdate("insert into foo values(1);"));
		// try to insert a row with duplicate id
		try (PreparedStatement statement = conn.prepareStatement("insert into foo values(?);")) {
			statement.setInt(1, 1);
			statement.execute();
			fail("expected exception");
		} catch (SQLException e) {
			assertEquals(ErrCodes.SQLITE_CONSTRAINT, e.getErrorCode());
		}
	}

	@Test
	public void constraintErrorCodeExecuteUpdate() throws SQLException {
		assertEquals(0, stat.executeUpdate("create table foo (id integer, CONSTRAINT U_ID UNIQUE (id));"));
		assertEquals(1, stat.executeUpdate("insert into foo values(1);"));
		// try to insert a row with duplicate id
		try (PreparedStatement statement = conn.prepareStatement("insert into foo values(?);")) {
			statement.setInt(1, 1);
			statement.executeUpdate();
			fail("expected exception");
		} catch (SQLException e) {
			assertEquals(ErrCodes.SQLITE_CONSTRAINT, e.getErrorCode());
		}
	}

	@Test
	public void temporal() throws SQLException {
		try (PreparedStatement stmt = conn.prepareStatement("SELECT ?;")) {
			check(stmt, LocalDate.class, LocalDate.now());
			check(stmt, LocalDateTime.class, LocalDateTime.now());
			check(stmt, OffsetDateTime.class, OffsetDateTime.now());
			check(stmt, LocalTime.class, LocalTime.now());
		}
	}

	private static <T> void check(PreparedStatement stmt, Class<T> clazz, T expected) throws SQLException {
		stmt.setObject(1, expected);
		try (ResultSet rs = stmt.executeQuery()) {
			assertTrue(rs.next());
			T actual = rs.getObject(1, clazz);
			assertEquals(expected, actual);
		}
	}
}
