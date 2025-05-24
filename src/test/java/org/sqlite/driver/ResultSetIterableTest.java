package org.sqlite.driver;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class ResultSetIterableTest {
	@Test
	public void close() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT ?");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			rsi.close();
		}
	}
	@Test
	public void iterator() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT 1 UNION ALL SELECT 2");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			long sum = 0;
			for (Long l : rsi) {
				sum += l;
			}
			assertEquals(3L, sum);
		}
	}
	@Test
	public void stream() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT 1 UNION ALL SELECT 2");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			assertEquals(3L, rsi.stream().mapToLong(l -> l.longValue()).sum());
		}
	}

	@Test
	public void empty() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT 1 LIMIT 0");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			assertEquals(0, rsi.stream().count());

			{
				Iterator<Long> iterator = rsi.iterator();
				try {
					iterator.next();
					fail("No such element");
				} catch (NoSuchElementException e) {
				}
				try {
					iterator.next();
					fail("Illegal state");
				} catch (IllegalStateException e) {
				}
			}
			{
				Iterator<Long> iterator = rsi.iterator();
				assertFalse(iterator.hasNext());
				assertFalse(iterator.hasNext());
			}
		}
	}

	@Test(expected = SQLException.class)
	public void map_error() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT 1 UNION ALL SELECT 'a'");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			for (Long l : rsi) {
				assertEquals(1L, l.longValue());
			}
		}
	}

	@Test(expected = SQLException.class)
	public void runtime_error() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT 1 UNION ALL SELECT '{[]}' -> 1");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			for (Long l : rsi) {
				assertEquals(1L, l.longValue());
			}
		}
	}

	@Test(expected = SQLException.class)
	public void invalid_query() throws SQLException {
		try (Connection c = DriverManager.getConnection(JDBC.MEMORY);
				 PreparedStatement s = c.prepareStatement("SELECT ?");
				 ResultSetIterable<Long> rsi = new ResultSetIterable<>(Query.from(s), rs -> rs.getLong(1))) {
			rsi.iterator();
		}
	}
}
