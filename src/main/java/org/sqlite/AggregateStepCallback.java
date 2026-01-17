package org.sqlite;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

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
 * @see Conn#createAggregateFunction(String, int, int, AggregateStepCallback, AggregateComputeCallback)
 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
 */
public abstract class AggregateStepCallback {
	protected final ValueLayout layout;
	protected final boolean inverse;

	protected AggregateStepCallback(ValueLayout layout, boolean inverse) {
		this.layout = layout;
		this.inverse = inverse;
	}

	//void (*)(sqlite3_context*,int,sqlite3_value**),
	/**
	 * @param ms <code>sqlite3_context*</code>
	 * @param nArg number of arguments
	 * @param args function arguments
	 */
	@SuppressWarnings("unused")
	public void callback(MemorySegment ms, int nArg, MemorySegment args) {
		sqlite3_context pCtx = new sqlite3_context(ms);
		long size = layout.byteSize();
		MemorySegment aggrCtx = sqlite3_aggregate_context(pCtx, (int) size);
		if (isNull(aggrCtx) && size > 0) {
			pCtx.setResultErrorNoMem();
			return;
		} else if (size > 0) {
			aggrCtx = aggrCtx.reinterpret(size);
		}
		step(pCtx, aggrCtx, sqlite3_values.build(nArg, args));
	}

	/**
	 * @param pCtx <code>sqlite3_context*</code>
	 * @param aggrCtx aggregate context
	 * @param args function arguments
	 */
	protected abstract void step(sqlite3_context pCtx, MemorySegment aggrCtx, sqlite3_values args);
}
