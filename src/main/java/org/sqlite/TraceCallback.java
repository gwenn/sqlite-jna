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

public abstract class TraceCallback<T> extends Callback<TraceCallback<T>> {
  @SuppressWarnings("unused")
  public abstract void apply(Pointer<T> arg, Pointer<Byte> sql);
}
