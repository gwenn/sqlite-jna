package org.sqlite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayModuleTest {
	@Test
	public void basic() throws SQLiteException {
		try (Conn conn = Conn.open(Conn.MEMORY, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null)) {
			ArrayModule.load_module(conn);
			try (Stmt stmt = conn.prepare("SELECT value FROM jarray(?)", false)) {
				stmt.bindArray(1, new long[]{1, 2, 3, 4});
				long total = 0;
				while (stmt.step(0)) {
					total += stmt.getColumnLong(0);
				}
				assertEquals(10, total);
			}
		}
	}
}
