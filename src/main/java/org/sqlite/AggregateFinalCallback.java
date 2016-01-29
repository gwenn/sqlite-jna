package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import org.sqlite.SQLite.SQLite3Context;

import static org.sqlite.SQLite.sqlite3_aggregate_context;

/**
 * User defined SQL aggregate function.
 * <pre>{@code
 * new AggregateFinalCallback() {
 *   \@Override
 *   public void finalStep(SQLite3Context pCtx, Pointer aggrCtx) {
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
public abstract class AggregateFinalCallback implements Callback {
	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 */
	@SuppressWarnings("unused")
	public void callback(SQLite3Context pCtx) {
		finalStep(pCtx, getAggregateContext(pCtx));
	}

	protected abstract void finalStep(SQLite3Context pCtx, Pointer aggrCtx);

	/**
	 * Obtain aggregate function context.
	 *
	 * @return <code>null</code> when no rows match an aggregate query.
	 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_get_auxdata</a>
	 */
	protected Pointer getAggregateContext(SQLite3Context pCtx) {
		// Within the xFinal callback, it is customary to set N=0 in calls to sqlite3_aggregate_context(C,N)
		// so that no pointless memory allocations occur.
		return sqlite3_aggregate_context(pCtx, 0);
	}
}
