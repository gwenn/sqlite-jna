/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
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
