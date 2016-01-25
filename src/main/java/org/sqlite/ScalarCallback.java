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

/**
 * User defined SQL scalar function.
 * <pre>{@code
 * new ScalarCallback() {
 *   \@Override
 *   public void invoke(Pointer pCtx, int nArg, Pointer args) {
 * 			SQLite.sqlite3_result_int(pCtx, 0);
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
	public void invoke(SQLite3Context pCtx, int nArg, Pointer args) {
		invoke(pCtx, args.getPointerArray(0, nArg));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	protected abstract void invoke(SQLite3Context pCtx, Pointer[] args);
}
