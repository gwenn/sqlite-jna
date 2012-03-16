/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import java.sql.SQLFeatureNotSupportedException;

class Util {
    private Util() {
    }

    static SQLFeatureNotSupportedException unsupported() {
        return new SQLFeatureNotSupportedException("not implemented by SQLite JDBC driver");
    }
}
