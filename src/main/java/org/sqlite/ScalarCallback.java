/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.bridj.Callback;
import org.bridj.Pointer;

public abstract class ScalarCallback<T> extends Callback<ScalarCallback<T>> {
  //void (*)(sqlite3_context*,int,sqlite3_value**),
  @SuppressWarnings("unused")
  public abstract void apply(Pointer pCtx, int nArg, Pointer<T> args);
}
