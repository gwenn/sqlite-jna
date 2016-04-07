/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.sqlite.SQLite.SQLite3Context;
import org.sqlite.SQLite.SQLite3Values;

import static org.sqlite.SQLite.sqlite3_get_auxdata;
import static org.sqlite.SQLite.sqlite3_set_auxdata;

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
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(long pCtx, long[] args) {
		func(new SQLite3Context(pCtx), SQLite3Values.build(args));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	protected abstract void func(SQLite3Context pCtx, SQLite3Values args);

	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_set_auxdata</a>
	 */
	public void setAuxData(SQLite3Context pCtx, int n, Object auxData, Destructor free) {
		sqlite3_set_auxdata(pCtx.pCtx, n, auxData, free);
	}
	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_get_auxdata</a>
	 */
	public Object getAuxData(SQLite3Context pCtx, int n) {
		return sqlite3_get_auxdata(pCtx.pCtx, n);
	}
}
