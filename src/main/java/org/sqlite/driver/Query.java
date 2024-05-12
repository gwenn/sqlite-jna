package org.sqlite.driver;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link ResultSetIterable#iterator()}
 */
public interface Query extends Guard {
	static Query from(PreparedStatement stmt) {
		return new Query() {
			@Override
			public ResultSet executeQuery() throws SQLException {
				return stmt.executeQuery();
			}
			@Override
			public void close() throws SQLException {
				stmt.close();
			}
		};
	}
	/**
	 * @return {@link PreparedStatement#executeQuery()}
	 */
	ResultSet executeQuery() throws SQLException;
}
