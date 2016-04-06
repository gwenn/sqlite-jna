package org.sqlite;

public interface RowMapper<T> {
	T map(Row row) throws StmtException;
}
