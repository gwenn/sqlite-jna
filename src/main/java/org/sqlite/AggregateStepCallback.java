package org.sqlite;

import org.sqlite.SQLite.SQLite3Context;
import org.sqlite.SQLite.SQLite3Values;

import java.lang.foreign.MemorySegment;

import static org.sqlite.SQLite.sqlite3_aggregate_context;
import static org.sqlite.SQLite.sqlite3_result_error_nomem;

/**
 * User defined SQL aggregate function.
 * <pre>{@code
 * new AggregateStepCallback() {
 *   \@Override
 *   public void step(SQLite3Context pCtx, MemorySegment aggrCtx, SQLite3Values args) {
 *     args.getX(...);
 *     ...
 *     aggrCtx.setX(...);
 *   }
 * }
 * }</pre>
 *
 * @see Conn#createAggregateFunction(String, int, int, AggregateStepCallback, AggregateFinalCallback)
 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
 */
public abstract class AggregateStepCallback {
	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param ms <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(MemorySegment ms, int nArg, MemorySegment args) {
		SQLite3Context pCtx = new SQLite3Context(ms);
		final int nBytes = numberOfBytes();
		final MemorySegment p = sqlite3_aggregate_context(pCtx, nBytes);
		if (p == null && nBytes > 0) {
			sqlite3_result_error_nomem(pCtx);
			return;
		}
		step(pCtx, p, SQLite3Values.build(nArg, args));
	}

	/**
	 * @return number of bytes to allocate.
	 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_aggregate_context</a>
	 */
	protected abstract int numberOfBytes();

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param aggrCtx aggregate context
	 * @param args function arguments
	 */
	protected abstract void step(SQLite3Context pCtx, MemorySegment aggrCtx, SQLite3Values args);
}
