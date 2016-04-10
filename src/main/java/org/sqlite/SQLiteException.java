/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.sql.SQLException;

public class SQLiteException extends SQLException {
	private final String errMsg;

	/**
	 * @param reason a description of the exception
	 * @param errCode a database vendor-specific exception code
	 */
	public SQLiteException(String reason, int errCode) {
		this(null, reason, errCode);
	}

	/**
	 * @param c a connection (may be null)
	 * @param reason a description of the exception
	 * @param errCode a database vendor-specific exception code
	 */
	public SQLiteException(Conn c, String reason, int errCode) {
		this(c, reason, errCode, null);
	}
	/**
	 * @param c a connection (may be null)
	 * @param reason a description of the exception
	 * @param errCode a database vendor-specific exception code
	 * @param cause the underlying reason for this <code>SQLiteException</code>
	 */
	public SQLiteException(Conn c, String reason, int errCode, Throwable cause) {
		super(reason, null, errCode, cause);
		if (c == null) {
			errMsg = null;
		} else if (getErrorCode() >= 0) {
			errMsg = c.getErrMsg();
		} else {
			errMsg = null;
		}
	}

	@Override
	public String getMessage() {
		if (errMsg != null && !errMsg.isEmpty()) {
			return String.format("%s (%s)", super.getMessage(), errMsg);
		} else {
			if (getErrorCode() > 0) {
				return String.format("%s (code %d)", super.getMessage(), getErrorCode());
			} else {
				return super.getMessage();
			}
		}
	}

	public String getErrMsg() {
		return errMsg;
	}
}
