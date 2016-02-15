package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;

/**
 * Callback to handle SQLITE_BUSY errors
 *
 * @see Conn#setBusyHandler(BusyHandler, Pointer)
 * @see <a href="http://sqlite.org/c3ref/busy_handler.html">sqlite3_busy_handler</a>
 */
public abstract class BusyHandler extends FunctionPointer {
	/**
	 * @param pArg  User data ({@link Conn#setBusyHandler(BusyHandler, Pointer)} second argument)
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	public int call(Pointer pArg, int count) {
		return busy(pArg, count) ? 1 : 0;
	}
	/**
	 * @param pArg  User data ({@link Conn#setBusyHandler(BusyHandler, Pointer)} second argument)
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	protected abstract boolean busy(Pointer pArg, int count);
}
