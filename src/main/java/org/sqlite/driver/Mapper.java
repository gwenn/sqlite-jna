package org.sqlite.driver;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Row mapper used by {@link ResultSetIterable#iterator()}
 */
public interface Mapper<T> {
	T map(ResultSet rs) throws SQLException;
}
