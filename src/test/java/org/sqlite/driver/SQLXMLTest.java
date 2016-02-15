package org.sqlite.driver;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class SQLXMLTest {
	private Connection c;
	private SQLXML sqlxml;

	@Before
	public void setUp() throws Exception {
		c = DriverManager.getConnection(JDBC.MEMORY);
		final Statement stmt = c.createStatement();
		stmt.execute("CREATE TABLE test (data TEXT)");
		stmt.close();

		sqlxml = c.createSQLXML();
	}

	@Test
	public void string() throws Exception {
		SQLXML sqlxml = new SQLXMLImpl(UTF_8);
		final String xml = "<root></root>";
		sqlxml.setString(xml);
		insert(sqlxml);
		sqlxml.free();

		final ResultSet rs = query();
		sqlxml = rs.getSQLXML(1);
		assertNotNull(sqlxml);
		assertEquals(xml, sqlxml.getString());
		set(sqlxml, "read-only", "not writable");
		free(sqlxml);
		rs.close();
	}

	@Test
	public void writer() throws Exception {
		SQLXML sqlxml = new SQLXMLImpl(UTF_8);
		final String xml = "<root></root>";
		final Writer writer = sqlxml.setCharacterStream();
		writer.write(xml);
		writer.close();
		insert(sqlxml);
		sqlxml.free();

		final ResultSet rs = query();
		sqlxml = rs.getSQLXML(1);
		assertNotNull(sqlxml);
		final Reader reader = sqlxml.getCharacterStream();
		assertNotNull(reader);
		//assertEquals(xml, reader.toString());
		sqlxml.free();
		rs.close();
	}

	@Test
	public void stream() throws Exception {
		SQLXML sqlxml = new SQLXMLImpl(UTF_8);
		final String xml = "<root></root>";
		final OutputStream out = sqlxml.setBinaryStream();
		final byte[] bytes = xml.getBytes(UTF_8);
		out.write(bytes);
		out.close();
		insert(sqlxml);
		sqlxml.free();

		final ResultSet rs = query();
		sqlxml = rs.getSQLXML(1);
		assertNotNull(sqlxml);
		final InputStream in = sqlxml.getBinaryStream();
		assertNotNull(in);
		//assertArrayEquals(bytes, in);
		sqlxml.free();
		rs.close();
	}

	@Test
	public void dom() throws Exception {
		SQLXML sqlxml = new SQLXMLImpl(UTF_8);
		final DOMResult result = sqlxml.setResult(DOMResult.class);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document doc = builder.newDocument();
		Element element = doc.createElement("root");
		doc.appendChild(element);

		result.setNode(doc);
		insert(sqlxml);
		sqlxml.free();

		final ResultSet rs = query();
		sqlxml = rs.getSQLXML(1);
		assertNotNull(sqlxml);
		final DOMSource src = sqlxml.getSource(DOMSource.class);
		assertNotNull(src);
		//assertEquals(doc, src.getNode());
		sqlxml.free();
		rs.close();
	}

	@Test
	public void free() throws SQLException {
		free(sqlxml);
	}
	private static void free(SQLXML sqlxml) throws SQLException {
		sqlxml.free();
		sqlxml.free();
		get(sqlxml, "freed", "has already been freed.");
		set(sqlxml, "freed", "has already been freed.");
	}

	@Test
	public void nil() throws SQLException {
		insert(null);
		final ResultSet rs = query();
		assertNull(rs.getSQLXML(1));
		rs.close();
	}

	private ResultSet query() throws SQLException {
		final PreparedStatement query = c.prepareStatement("SELECT data FROM test");
		query.closeOnCompletion();
		final ResultSet rs = query.executeQuery();
		assertTrue(rs.next());
		return rs;
	}

	private void insert(SQLXML sqlxml) throws SQLException {
		final PreparedStatement ins = c.prepareStatement("INSERT INTO test (data) VALUES (?)");
		ins.setSQLXML(1, sqlxml);
		assertEquals(1, ins.executeUpdate());
		ins.close();
	}

	@Test
	public void uninitialized() throws SQLException {
		get(sqlxml, "uninitialized", "has not been written.");
	}

	private static void get(SQLXML sqlxml, String message, String substring) {
		try {
			sqlxml.getString();
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
		try {
			sqlxml.getSource(null);
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
		try {
			sqlxml.getCharacterStream();
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
		try {
			sqlxml.getBinaryStream();
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
	}

	@Test
	public void doubleSet() throws SQLException {
		sqlxml.setString(null);
		set(sqlxml, "not writable", "has already been written");
	}

	private static void set(SQLXML sqlxml, String message, String substring) {
		try {
			sqlxml.setString(null);
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
		try {
			sqlxml.setBinaryStream();
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
		try {
			sqlxml.setCharacterStream();
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
		try {
			sqlxml.setResult(null);
			fail(message);
		} catch (SQLException e) {
			assertThat(e.getMessage(), CoreMatchers.containsString(substring));
		}
	}

	@After
	public void tearDown() throws Exception {
		if (sqlxml != null) {
			sqlxml.free();
		}
		if (c != null) {
			c.close();
		}
		c = null;

	}
}
