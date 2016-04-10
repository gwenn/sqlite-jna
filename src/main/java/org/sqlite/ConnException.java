/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

/**
 * Connection related exception.
 */
public class ConnException extends SQLiteException {
	private final transient Conn c;

	/**
	 * @param c a connection (may be null)
	 * @param reason a description of the exception
	 * @param errCode a database vendor-specific exception code
	 */
	public ConnException(Conn c, String reason, int errCode) {
		super(c, reason, errCode);
		this.c = c;
	}

	public String getFilename() {
		if (c == null) {
			return null;
		}
		return c.getFilename();
	}
}
