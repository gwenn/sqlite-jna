/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public abstract class TraceCallback implements Callback {
  @SuppressWarnings("unused")
  public void invoke(Pointer arg, String sql) {
    trace(sql);
  }

  protected abstract void trace(String sql);
}
