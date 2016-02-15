/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.annotation.Cast;
import org.sqlite.SQLite.sqlite3_context;
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
public abstract class ScalarCallback extends FunctionPointer {
	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void call(sqlite3_context pCtx, int nArg,@Cast("sqlite3_value**") PointerPointer args) {
		func(pCtx, SQLite3Values.build(nArg, args));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param args function arguments
	 */
	protected abstract void func(sqlite3_context pCtx, SQLite3Values args);

	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_set_auxdata</a>
	 */
	public void setAuxData(sqlite3_context pCtx, int n, Pointer auxData, Destructor free) {
		sqlite3_set_auxdata(pCtx, n, auxData, free);
	}
	/**
	 * @see <a href="http://sqlite.org/c3ref/get_auxdata.html">sqlite3_get_auxdata</a>
	 */
	public Pointer getAuxData(sqlite3_context pCtx, int n) {
		return sqlite3_get_auxdata(pCtx, n);
	}
}
