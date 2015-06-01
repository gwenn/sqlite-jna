/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

public class ZeroBlob {
	// length of BLOB
	public final int n;

	/**
	 * @param n length of BLOB
	 */
	public ZeroBlob(int n) {
		this.n = n;
	}
}
