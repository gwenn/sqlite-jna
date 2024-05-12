package org.sqlite.driver;

import java.sql.SQLException;

public interface Guard extends AutoCloseable {
	@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
	static <E extends Throwable, T> T sneakyThrow(SQLException e) throws E {
		throw (E) e;
	}

	void close() throws SQLException;

	class SneakyGuard<T> implements AutoCloseable, Runnable {
		private final Guard guard;
		public SneakyGuard(Guard guard) {
			this.guard = guard;
		}
		@Override
		public void close() {
			try {
				guard.close();
			} catch (SQLException e) {
				sneakyThrow(e);
			}
		}
		@Override
		public void run() {
			close();
		}
	}
}
