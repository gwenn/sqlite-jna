package org.sqlite;

public class ConnException extends SQLiteException {
  private final Conn c;

  public ConnException(Conn c, String name, int errCode) {
    super(name, errCode);
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
