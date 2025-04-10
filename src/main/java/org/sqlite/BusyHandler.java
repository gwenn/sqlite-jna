package org.sqlite;

import java.lang.foreign.MemorySegment;

/**
 * Callback to handle SQLITE_BUSY errors
 *
 * @see Conn#setBusyHandler(BusyHandler)
 * @see <a href="http://sqlite.org/c3ref/busy_handler.html">sqlite3_busy_handler</a>
 */
@FunctionalInterface
public interface BusyHandler {
	/**
	 * @param pArg  User data (<code>null</code>)
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	default int callback(MemorySegment pArg, int count) {
		return busy(count) ? 1 : 0;
	}

	/**
	 * @param count the number of times that the busy handler has been invoked previously for the same locking event.
	 * @return <code>true</code> to try again, <code>false</code> to abort.
	 */
	boolean busy(int count);
}
