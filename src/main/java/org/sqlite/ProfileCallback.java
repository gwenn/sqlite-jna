/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.annotations.Encoding;

public abstract class ProfileCallback {
	@SuppressWarnings("unused")
	@Delegate
	public void invoke(Pointer arg, @Encoding("UTF-8") String sql, long ns) {
		profile(sql, ns);
	}

	protected abstract void profile(String sql, long ns);
}
