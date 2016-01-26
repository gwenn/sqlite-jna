package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import org.sqlite.SQLite.SQLite3Context;
import org.sqlite.SQLite.SQLite3Values;

import static org.sqlite.SQLite.sqlite3_aggregate_context;

/**
 * User defined SQL aggregate function.
 * <pre>{@code
 * new AggregateStepCallback() {
 *   \@Override
 *   public void step(SQLite3Context pCtx, SQLite3Values args) {
 *     Pointer aggr = getAggregateContext(pCtx, ...);
 *     ...
 *   }
 * }
 * }</pre>
 * @see Conn#createAggregateFunction(String, int, int, AggregateStepCallback, AggregateFinalCallback)
 * @see <a href="http://sqlite.org/c3ref/create_function.html">sqlite3_create_function_v2</a>
 */
public abstract class AggregateStepCallback implements Callback {
    //void (*)(sqlite3_context*,int,sqlite3_value**),
    /**
     * @param pCtx <code>sqlite3_context*</code>
     * @param nArg number of arguments
     * @param args function arguments
     */
    @SuppressWarnings("unused")
    public void callback(SQLite3Context pCtx, int nArg, Pointer args) {
        step(pCtx, SQLite3Values.build(nArg, args));
    }

    /**
     * @param pCtx <code>sqlite3_context*</code>
     * @param args function arguments
     */
    protected abstract void step(SQLite3Context pCtx, SQLite3Values args);

    /**
     * Obtain aggregate function context.
     * @param nBytes number of bytes to allocate.
     * @return <code>null</code> if <code>nBytes</code> is <= 0 or if a memory allocate error occurs.
     * @see <a href="http://sqlite.org/c3ref/aggregate_context.html">sqlite3_get_auxdata</a>
     */
    public Pointer getAggregateContext(SQLite3Context pCtx, int nBytes) {
        return sqlite3_aggregate_context(pCtx, nBytes);
    }
}
