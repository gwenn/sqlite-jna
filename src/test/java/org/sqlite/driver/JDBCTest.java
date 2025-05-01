/*
This program follows the Apache License version 2.0 (<http://www.apache.org/licenses/> ) That means:

It allows you to:

*   freely download and use this software, in whole or in part, for personal, company internal, or commercial purposes;
*   use this software in packages or distributions that you create.

It forbids you to:

*   redistribute any piece of our originated software without proper attribution;
*   use any marks owned by us in any way that might state or imply that we xerial.org endorse your distribution;
*   use any marks owned by us in any way that might state or imply that you created this software in question.

It requires you to:

*   include a copy of the license in any redistribution you may make that includes this software;
*   provide clear attribution to us, xerial.org for any distributions that include this software

It does not require you to:

*   include the source of this software itself, or of any modifications you may have
    made to it, in any redistribution you may assemble that includes it;
*   submit changes that you make to the software back to this software (though such feedback is encouraged).

See License FAQ <http://www.apache.org/foundation/licence-FAQ.html> for more details.
*/

package org.sqlite.driver;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class JDBCTest {
	@Test
	public void enableLoadExtensionTest() throws Exception {
		Properties prop = new Properties();
		prop.setProperty("enable_load_extension", "true");

		try (Connection conn = DriverManager.getConnection(JDBC.MEMORY, prop);
			Statement stat = conn.createStatement()) {

			// How to build shared lib in Windows
			// # mingw32-gcc -fPIC -c extension-function.c
			// # mingw32-gcc -shared -Wl -o extension-function.dll extension-function.o

			//            stat.executeQuery("select load_extension('extension-function.dll')");
			//
			//            ResultSet rs = stat.executeQuery("select sqrt(4)");
			//            System.out.println(rs.getDouble(1));
		}
	}

	@Test
	public void majorVersion() throws Exception {
		assertEquals(DriverInfo.MAJOR_VERSION, DriverManager.getDriver(JDBC.MEMORY).getMajorVersion());
		assertEquals(DriverInfo.MINOR_VERSION, DriverManager.getDriver(JDBC.MEMORY).getMinorVersion());
	}

	@Test
	public void shouldReturnNullIfProtocolUnhandled() throws Exception {
		Assert.assertNull(new JDBC().connect("jdbc:anotherpopulardatabaseprotocol:", null));
	}
}
