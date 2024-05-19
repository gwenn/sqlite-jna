package org.sqlite.driver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.sqlite.driver.Guard.sneakyThrow;

/**
 * <pre>{@code
 * 		try (Connection conn = DriverManager.getConnection(JDBC.MEMORY);
 * 				 PreparedStatement stmt = conn.prepareStatement("SELECT 1");
 * 				 ResultSetIterable<String> rsi = new ResultSetIterable<String>(Query.from(stmt), rs -> rs.getString(1))) {
 * 			for (String s : rsi) {
 * 				System.out.println("s = " + s);
 * 			}
 * 	  }
 * }</pre>
 */
public class ResultSetIterable<T> implements Iterable<T>, Guard {
	private final Query query;
	private final Mapper<T> mapper;
	private ResultSet rs;

	protected ResultSetIterable(Query query, Mapper<T> mapper) {
		this.query = query;
		this.mapper = mapper;
	}

	@Override
	public void close() throws SQLException {
		Guard.closeAll(rs, query);
	}

	@Override
	public Iterator<T> iterator() {
		try {
			if (rs != null) {
				rs.close(); // previous result set
			}
			rs = query.executeQuery();
		} catch (SQLException e) {
			return sneakyThrow(e);
		}
		return new Iterator<T>() {
			private State state = State.NOT_READY;
			@Override
			public boolean hasNext() {
				if (State.FAILED == state) {
					throw new IllegalStateException();
				}
				if (State.DONE == state) {
					return false;
				} else if (State.READY == state) {
					return true;
				}
				state = State.FAILED;
				try {
					if (rs.next()) {
						state = State.READY;
						return true;
					} else {
						rs.close(); // close as soon as possible
						state = State.DONE;
						return false;
					}
				} catch (SQLException e) {
					return sneakyThrow(Guard.close(rs, e));
				}
			}
			@Override
			public T next() {
				if (!hasNext()) {
					state = State.FAILED;
					throw new NoSuchElementException();
				}
				state = State.NOT_READY;
				try {
					return mapper.map(rs);
				} catch (SQLException e) {
					state = State.FAILED;
					return sneakyThrow(e);
				}
			}
			@Override
			public void forEachRemaining(Consumer<? super T> action) {
				try (SneakyGuard ignored = rs != null ? new SneakyGuard(rs::close) : null) {
					Iterator.super.forEachRemaining(action);
				}
			}
		};
	}

	/*@Override
	public Spliterator<T> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED); // ORDERED ?
	}*/
	public Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false).onClose(new SneakyGuard(this));
	}

	private enum State {
		READY, NOT_READY, DONE, FAILED,
	}
}
