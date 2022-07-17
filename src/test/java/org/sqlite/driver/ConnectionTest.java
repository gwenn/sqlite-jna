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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import static org.junit.Assert.*;

public class ConnectionTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void isValid() throws SQLException {
		Connection conn = DriverManager.getConnection(JDBC.MEMORY);
		assertTrue(conn.isValid(0));
		conn.close();
		assertFalse(conn.isValid(0));
	}

	@Test
	public void executeUpdateOnClosedDB() throws SQLException {
		Connection conn = DriverManager.getConnection(JDBC.MEMORY);
		Statement stat = conn.createStatement();
		conn.close();

		try {
			stat.executeUpdate("create table A(id, name)");
			fail("should not reach here");
		} catch (SQLException e) {
			assertEquals("Connection closed", e.getMessage()); // successfully detect the operation on the closed DB
		}
	}

    /*@Test
		public void readOnly() throws SQLException {

        // set read only mode
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);

        Connection conn = DriverManager.getConnection(JDBC.PREFIX, config.toProperties());
        Statement stat = conn.createStatement();
        try {
            assertTrue(conn.isReadOnly());
            // these updates must be forbidden in read-only mode
            stat.executeUpdate("create table A(id, name)");
            stat.executeUpdate("insert into A values(1, 'leo')");

            fail("read only flag is not properly set");
        }
        catch (SQLException e) {
            // success
        }
        finally {
            stat.close();
            conn.close();
        }

        config.setReadOnly(true); // should be a no-op

        try{
            conn.setReadOnly(false);
            fail("should not change read only flag after opening connection");
        }
        catch (SQLException e) {
           assert(e.getMessage().contains("Cannot change read-only flag after establishing a connection."));
        }
        finally {
            conn.close();
        }
    }*/

    /*@Test
    public void foreignKeys() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        Connection conn = DriverManager.getConnection(JDBC.PREFIX, config.toProperties());
        Statement stat = conn.createStatement();

        try {
            stat.executeUpdate("create table track(id integer primary key, name, aid, foreign key (aid) references artist(id))");
            stat.executeUpdate("create table artist(id integer primary key, name)");

            stat.executeUpdate("insert into artist values(10, 'leo')");
            stat.executeUpdate("insert into track values(1, 'first track', 10)"); // OK

            try {
                stat.executeUpdate("insert into track values(2, 'second track', 3)"); // invalid reference
            }
            catch (SQLException e) {
                return; // successfully detect violation of foreign key constraints
            }
            fail("foreign key constraint must be enforced");
        }
        finally {
            stat.close();
            conn.close();
        }
    }*/

    /*@Test
    public void canWrite() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        Connection conn = DriverManager.getConnection(JDBC.PREFIX, config.toProperties());
        Statement stat = conn.createStatement();

        try {
            assertFalse(conn.isReadOnly());
        }
        finally {
            stat.close();
            conn.close();
        }
    }*/

    /*@Test
    public void synchronous() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SynchronousMode.OFF);
        Connection conn = DriverManager.getConnection(JDBC.PREFIX, config.toProperties());
        Statement stat = conn.createStatement();

        try {
            ResultSet rs = stat.executeQuery("pragma synchronous");
            if (rs.next()) {
                ResultSetMetaData rm = rs.getMetaData();
                int i = rm.getColumnCount();
                int synchronous = rs.getInt(1);
                assertEquals(0, synchronous);
            }

        }
        finally {
            stat.close();
            conn.close();
        }
    }*/

	@Test
	public void openMemory() throws SQLException {
		Connection conn = DriverManager.getConnection(JDBC.MEMORY);
		conn.close();
	}

	@Test
	public void isClosed() throws SQLException {
		Connection conn = DriverManager.getConnection(JDBC.MEMORY);
		if (org.sqlite.Conn.libversionNumber() >= 3008000) {
			assertFalse(conn.isReadOnly());
		}
		conn.close();
		assertTrue(conn.isClosed());
	}

	@Test(expected = SQLException.class)
	public void closeTest() throws SQLException {
		Connection conn = DriverManager.getConnection(JDBC.MEMORY);
		PreparedStatement prep = conn.prepareStatement("select null;");
		ResultSet rs = prep.executeQuery();
		conn.close();
		prep.clearParameters();
	}

	@Test(expected = SQLException.class)
	public void openInvalidLocation() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:sqlite:/");
		conn.close();
	}

    /*@Test
    public void openResource() throws Exception {
        File testDB = copyToTemp("sample.db");
        assertTrue(testDB.exists());
        Connection conn = DriverManager
                .getConnection(String.format("jdbc:sqlite::resource:%s", testDB.toURI().toURL()));
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select * from coordinate");
        assertTrue(rs.next());
        rs.close();
        stat.close();
        conn.close();
    }*/

    /*@Test
    public void openJARResource() throws Exception {
        File testJAR = copyToTemp("testdb.jar");
        assertTrue(testJAR.exists());

        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite::resource:jar:%s!/sample.db", testJAR
                .toURI().toURL()));
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("select * from coordinate");
        assertTrue(rs.next());
        rs.close();
        stat.close();
        conn.close();
    }*/

    /*@Test
    public void openFile() throws Exception {

        File testDB = copyToTemp("sample.db");

        assertTrue(testDB.exists());
        Connection conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", testDB));
        conn.close();
    }

    public static File copyToTemp(String fileName) throws IOException {
        InputStream in = ConnectionTest.class.getResourceAsStream(fileName);
        File dir = new File("target");
        if (!dir.exists())
            dir.mkdirs();

        File tmp = File.createTempFile(fileName, "", new File("target"));
        tmp.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tmp);

        byte[] buf = new byte[8192];
        for (int readBytes = 0; (readBytes = in.read(buf)) != -1;) {
            out.write(buf, 0, readBytes);
        }
        out.flush();
        out.close();
        in.close();

        return tmp;
    }*/

	@Test
	public void openNewFile() throws SQLException, IOException {
		File testdb = new File(folder.getRoot(), "test.db");

		assertFalse(testdb.exists());
		Connection conn = DriverManager.getConnection(JDBC.PREFIX + testdb);
		if (org.sqlite.Conn.libversionNumber() >= 3008000) {
			assertFalse(conn.isReadOnly());
		}
		conn.close();

		assertTrue(testdb.exists());
		conn = DriverManager.getConnection(JDBC.PREFIX + testdb);
		if (org.sqlite.Conn.libversionNumber() >= 3008000) {
			assertFalse(conn.isReadOnly());
		}
		conn.close();

		assertTrue(testdb.exists());
	}

	@Test
	public void URIFilenames() throws SQLException {
		Connection conn1 = DriverManager.getConnection("jdbc:sqlite:file:memdb1?mode=memory&cache=shared");
		Statement stmt1 = conn1.createStatement();
		stmt1.executeUpdate("create table tbl (col int)");
		stmt1.executeUpdate("insert into tbl values(100)");
		stmt1.close();

		Connection conn2 = DriverManager.getConnection("jdbc:sqlite:file:memdb1?mode=memory&cache=shared");
		Statement stmt2 = conn2.createStatement();
		ResultSet rs = stmt2.executeQuery("select * from tbl");
		assertTrue(rs.next());
		assertEquals(100, rs.getInt(1));
		stmt2.close();

		Connection conn3 = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");
		Statement stmt3 = conn3.createStatement();
		stmt3.executeUpdate("attach 'file:memdb1?mode=memory&cache=shared' as memdb1");
		rs = stmt3.executeQuery("select * from memdb1.tbl");
		assertTrue(rs.next());
		assertEquals(100, rs.getInt(1));
		stmt3.executeUpdate("create table tbl2(col int)");
		stmt3.executeUpdate("insert into tbl2 values(200)");
		stmt3.close();

		Connection conn4 = DriverManager.getConnection("jdbc:sqlite:file::memory:?cache=shared");
		Statement stmt4 = conn4.createStatement();
		rs = stmt4.executeQuery("select * from tbl2");
		assertTrue(rs.next());
		assertEquals(200, rs.getInt(1));
		rs.close();
		stmt4.close();
		conn4.close();
	}

	@Test
	public void addWarning() throws Exception {
		try (Connection conn = DriverManager.getConnection(JDBC.MEMORY)) {
			((Conn) conn).addWarning(new SQLWarning("test"));
			((Conn) conn).addWarning(null);
		}
	}
}
