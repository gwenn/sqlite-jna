package org.sqlite;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.annotation.Cast;

/**
 * Callback to handle SQLITE_BUSY errors
 *
 * @see Conn#setBusyHandler(BusyHandler)
 * @see <a href="http://sqlite.org/c3ref/busy_handler.html">sqlite3_busy_handler</a>
 */
public abstract class BusyHandler extends FunctionPointer {
	protected BusyHandler() {
		allocate();
	}
	private native void allocate();
	/**
	 * @param pArg  User data (<code>null</code>)
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	public
	@Cast("int")
	boolean call(Pointer pArg, int count) {
		return busy(count);
	}

	/**
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	protected abstract boolean busy(int count);
}
