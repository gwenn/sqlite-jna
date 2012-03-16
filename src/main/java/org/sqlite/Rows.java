package org.sqlite;

import java.sql.SQLException;

public class Rows extends AbstractResultSet {
  
  @Override
  void checkOpen() throws SQLException {
    if (!isOpen()) {
      throw new SQLException("resultSet closed");
    }
  }

  private boolean isOpen() {
    return false; // TODO
  }
}
