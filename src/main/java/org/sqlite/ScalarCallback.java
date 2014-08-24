/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

public interface ScalarCallback {
  //void (*)(sqlite3_context*,int,sqlite3_value**),
  @SuppressWarnings("unused")
  @Delegate
  void invoke(Pointer pCtx, int nArg, Pointer args);
}
