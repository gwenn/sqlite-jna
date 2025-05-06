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
 * Zeroblobs are intended to serve as placeholders for BLOBs whose content is later written using incremental BLOB I/O routines.
 *
 * @param n length of BLOB
 * @see Stmt#bindZeroblob(int, int)
 * @see Stmt#bindByIndex(int, Object)
 */
public record ZeroBlob(int n) {
}
