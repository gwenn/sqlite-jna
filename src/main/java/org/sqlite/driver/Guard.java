package org.sqlite.driver;

import java.sql.SQLException;

public interface Guard extends AutoCloseable {
	@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
	static <E extends Throwable> RuntimeException sneakyThrow(Exception e) throws E {
		throw (E) e;
	}

	static SQLException close(AutoCloseable ac, SQLException e) {
		if (ac != null) {
			try {
				ac.close();
			} catch (Exception x) {
				e.addSuppressed(x);
			}
		}
		return e;
	}

	static void closeAll(AutoCloseable... acs) throws SQLException {
		Exception e = null;
		for (AutoCloseable ac : acs) {
			if (ac != null) {
				try {
					ac.close();
				} catch (Exception x) {
					if (e != null) {
						e.addSuppressed(x);
					} else {
						e = x;
					}
				}
			}
		}
		if (e != null) {
			sneakyThrow(e);
		}
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
