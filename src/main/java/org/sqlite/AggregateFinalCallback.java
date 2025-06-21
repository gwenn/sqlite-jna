package org.sqlite;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.foreign.MemorySegment;

import static org.sqlite.sqlite3_context.sqlite3_aggregate_context;

/**
 * User defined SQL aggregate function.
 * <pre>{@code
 * new AggregateFinalCallback() {
 *   \@Override
 *   public void finalStep(SQLite3Context pCtx, MemorySegment aggrCtx) {
 *     if (aggrCtx == null) {
 *       pCtx.setResultNull();
 *       return;
 *     }
 *     ...
 *     pCtx.setResultX(...);
 *   }
 * }
 * }</pre>
 *
 * @see Conn#createAggregateFunction(String, int, int, AggregateStepCallback, AggregateFinalCallback)
 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
 */
public abstract class AggregateFinalCallback {
	/**
	 * @param ms <code>sqlite3_context*</code>
	 */
	@SuppressWarnings("unused")
	public void callback(MemorySegment ms) {
		sqlite3_context pCtx = new sqlite3_context(ms);
		finalStep(pCtx, getAggregateContext(pCtx));
	}

	protected abstract void finalStep(@NonNull sqlite3_context pCtx, @NonNull MemorySegment aggrCtx);

	/**
	 * Obtain aggregate function context.
	 *
	 * @return <code>null</code> when no rows match an aggregate query.
	 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_aggregate_context</a>
	 */
	@NonNull
	protected static MemorySegment getAggregateContext(sqlite3_context pCtx) {
		// Within the xFinal callback, it is customary to set N=0 in calls to sqlite3_aggregate_context(C,N)
		// so that no pointless memory allocations occur.
		return sqlite3_aggregate_context(pCtx, 0);
	}
}
