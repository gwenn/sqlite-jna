package org.sqlite;

public class ConnException extends SQLiteException {
  private final Conn c;

  public ConnException(Conn c, String message, int errCode) {
    super(message, errCode);
    this.c = c;
  }
  
  public String getFilename() {
    if (c == null) {
      return null; 
    }
    return c.getFilename();
  }

  @Override
  protected Conn getConn() {
    return c;
  }
}
