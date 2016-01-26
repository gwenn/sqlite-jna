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
public abstract class ScalarCallback implements Callback {
	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(SQLite3Context pCtx, int nArg, Pointer args) {
		func(pCtx, SQLite3Values.build(nArg, args));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	protected abstract void func(SQLite3Context pCtx, SQLite3Values args);

	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_set_auxdata</a>
	 */
	public void setAuxData(SQLite3Context pCtx, int n, Pointer auxData, Callback free) {
		sqlite3_set_auxdata(pCtx, n, auxData, free);
	}
	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_get_auxdata</a>
	 */
	public Pointer getAuxData(SQLite3Context pCtx, int n) {
		return sqlite3_get_auxdata(pCtx, n);
	}
}
