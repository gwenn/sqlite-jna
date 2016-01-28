package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import org.sqlite.SQLite.SQLite3Context;

import static org.sqlite.SQLite.sqlite3_aggregate_context;

/**
 * User defined SQL aggregate function.
 * <pre>{@code
 * new AggregateFinalCallback() {
 *   \@Override
 *   public void callback(SQLite3Context pCtx) {
 *     Pointer aggr = getAggregateContext(pCtx);
 *     if (aggr == null {
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
public abstract class AggregateFinalCallback<A extends PointerType> implements Callback {
	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 */
	@SuppressWarnings("unused")
	public abstract void callback(SQLite3Context pCtx);

	protected abstract A wrap(Pointer p);

	/**
	 * Obtain aggregate function context.
	 *
	 * @return <code>null</code> when no rows match an aggregate query.
	 * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_get_auxdata</a>
	 */
	protected A getAggregateContext(SQLite3Context pCtx) {
		// Within the xFinal callback, it is customary to set N=0 in calls to sqlite3_aggregate_context(C,N)
		// so that no pointless memory allocations occur.
		final Pointer p = sqlite3_aggregate_context(pCtx, 0);
		if (p == null) {
			return null;
		}
		return wrap(p);
	}
}
