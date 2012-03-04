package org.sqlite;

public interface Binder<P> {
  boolean useName();
  boolean bind(Stmt s, int paramIndex, String paramName, P params);
/*
  public <P> void bind(Binder<P> binder, P params) {
    for (int i = 1; i <= getBindParameterCount(); i++) {
      binder.bind(this, i, binder.useName() ? getBindParameterName(i) : null, params);
    }
  }
*/
}
