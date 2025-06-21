/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.foreign.MemorySegment;

import static org.sqlite.sqlite3_context.sqlite3_get_auxdata;
import static org.sqlite.sqlite3_context.sqlite3_set_auxdata;

/**
 * User defined SQL scalar function.
 * <pre>{@code
 * new ScalarCallback() {
 *   \@Override
 *   protected void func(SQLite3Context pCtx, SQLite3Values args) {
 * 	   pCtx.setResultInt(0);
 *   }
 * }
 * }</pre>
 * @see Conn#createScalarFunction(String, int, int, ScalarCallback)
 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
 */
public abstract class ScalarCallback {
	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param ms <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(MemorySegment ms, int nArg, MemorySegment args) {
		sqlite3_context pCtx = new sqlite3_context(ms);
		func(pCtx, sqlite3_values.build(nArg, args));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	protected abstract void func(sqlite3_context pCtx, sqlite3_values args);

	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_set_auxdata</a>
	 */
	public static void setAuxData(@NonNull sqlite3_context pCtx, int n, MemorySegment auxData, Destructor free) {
		sqlite3_set_auxdata(pCtx, n, auxData, free);
	}
	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_get_auxdata</a>
	 */
	@NonNull
	public static MemorySegment getAuxData(sqlite3_context pCtx, int n) {
		return sqlite3_get_auxdata(pCtx, n);
	}
}
