package org.sqlite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SeriesTest {
	@Test
	public void loadModule() throws SQLiteException {
		try (Conn c = ConnTest.open()) {
			Series.loadModule(c);
			try (Stmt stmt = c.prepare("SELECT * FROM generate_series(0,20,5)", false)) {
				int expected = 0;
				while (stmt.step(0)) {
					int value = stmt.getColumnInt(0);
					assertEquals(expected, value);
					expected += 5;
				}
				assertEquals(25, expected);
			}
		}
	}
}
