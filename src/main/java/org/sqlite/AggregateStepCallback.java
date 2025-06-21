package org.sqlite;

import java.lang.foreign.MemorySegment;

import static org.sqlite.SQLite.isNull;
import static org.sqlite.sqlite3_context.sqlite3_aggregate_context;

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
		sqlite3_context pCtx = new sqlite3_context(ms);
		int nBytes = numberOfBytes();
		MemorySegment p = sqlite3_aggregate_context(pCtx, nBytes);
		if (isNull(p) && nBytes > 0) {
			pCtx.setResultErrorNoMem();
			return;
		}
		step(pCtx, p, sqlite3_values.build(nArg, args));
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
	protected abstract void step(sqlite3_context pCtx, MemorySegment aggrCtx, sqlite3_values args);
}
