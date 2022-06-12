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

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sqlite.SQLiteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SqliteDriverTest {
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	private Driver driver;

	@Before
	public void newDriver() {
		driver = new JDBC();
	}

	@Test
	public void testAcceptsUrl() throws Exception {
		assertFalse(driver.acceptsURL(""));
		assertFalse(driver.acceptsURL("jdbc:sqlite"));
		assertTrue(driver.acceptsURL(JDBC.PREFIX));
		assertTrue(driver.acceptsURL("jdbc:sqlite::memory:"));
		assertTrue(driver.acceptsURL("jdbc:sqlite:/tmp/test.db"));
	}

	@Test
	public void testInvalidUrl() throws Exception {
		assertNull(driver.connect("jdbc:mysql:", null));
	}

	@Test
	public void testVersion() {
		assertEquals(1, driver.getMajorVersion());
		assertEquals(0, driver.getMinorVersion());
	}

	@Test(expected = SQLiteException.class)
	public void testUrlDoesNotExist() throws Exception {
		driver.connect("jdbc:sqlite:/non-existent/path/to/db", null);
	}

	@Test(expected = SQLiteException.class)
	public void testNotADB() throws Exception {
		File tempFile = testFolder.newFile("file.txt");

		try (FileWriter fw = new FileWriter(tempFile)) {
			fw.write("Hello, World!");
			fw.flush();
		}
		driver.connect(JDBC.PREFIX + tempFile.getAbsolutePath(), null);
	}

	@Test(expected = SQLiteException.class)
	public void testDirectory() throws Exception {
		driver.connect(JDBC.PREFIX + testFolder.getRoot().getAbsolutePath(), null);
	}

	@Test
	public void testEmptyUrl() throws Exception {
		assertNull(driver.connect("", null));
	}

	@Test(expected = SQLiteException.class)
	public void testNoPermissions() throws Exception {
		Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
		File tempFile = testFolder.newFile("test.db");

		assertTrue(tempFile.setReadable(false));
		driver.connect(JDBC.PREFIX + tempFile.getAbsolutePath(), null);
	}

	@Test
	public void testWorking() throws Exception {
		try (Connection conn = driver.connect(JDBC.PREFIX, null)) {
			assertNull(conn.getWarnings());
		}
	}

	@Test
	public void testCompliance() throws Exception {
		assertFalse(driver.jdbcCompliant());
	}

	@Test(expected = SQLFeatureNotSupportedException.class)
	public void testLogger() throws Exception {
		assertNotNull(driver.getParentLogger());
	}

	@Test
	public void testProperties() throws Exception {
		assertEquals(10, driver.getPropertyInfo("jdbc:sqlite::memory:", new Properties()).length);
		//assertArrayEquals(new DriverPropertyInfo[10], driver.getPropertyInfo("jdbc:sqlite::memory:", new Properties()));
	}
}
