package org.sqlite;

import org.junit.Test;

import static org.junit.Assert.*;

public class VTabLogModuleTest {
	@Test
	public void basic() throws SQLiteException {
		try (Conn conn = Conn.open(Conn.MEMORY, OpenFlags.SQLITE_OPEN_READWRITE | OpenFlags.SQLITE_OPEN_FULLMUTEX, null)) {
			VTabLogModule.load_module(conn);
			conn.fastExec("""
				CREATE VIRTUAL TABLE temp.log USING vtablog(
				                    schema='CREATE TABLE x(a,b,c)',
				                    rows=5
				                );""");
			try (Stmt stmt = conn.prepare("SELECT *, rowId FROM log;", false)) {
				long count = 0;
				while (stmt.step(0)) {
					String a = stmt.getColumnText(0);
					assertTrue(a.startsWith("a"));
					count ++;
				}
				assertEquals(5, count);
			}
			conn.execDml("DELETE FROM log WHERE a = ?1", false, "a1");
			conn.execDml("INSERT INTO log (a, b, c) VALUES (?1, ?2, ?3)", false, "a", "b", "c");
			conn.execDml("UPDATE log SET b = ?1, c = ?2 WHERE a = ?3", false, "bn", "cn", "a1");
		}
	}
}
