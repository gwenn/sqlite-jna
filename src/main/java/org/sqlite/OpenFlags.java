/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

public interface OpenFlags {
  int SQLITE_OPEN_READONLY = 0x00000001;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_READWRITE = 0x00000002;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_CREATE = 0x00000004;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_DELETEONCLOSE = 0x00000008;  /* VFS only */
  int SQLITE_OPEN_EXCLUSIVE = 0x00000010;  /* VFS only */
  int SQLITE_OPEN_AUTOPROXY = 0x00000020;  /* VFS only */
  int SQLITE_OPEN_URI = 0x00000040;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_MAIN_DB = 0x00000100;  /* VFS only */
  int SQLITE_OPEN_TEMP_DB = 0x00000200;  /* VFS only */
  int SQLITE_OPEN_TRANSIENT_DB = 0x00000400;  /* VFS only */
  int SQLITE_OPEN_MAIN_JOURNAL = 0x00000800;  /* VFS only */
  int SQLITE_OPEN_TEMP_JOURNAL = 0x00001000;  /* VFS only */
  int SQLITE_OPEN_SUBJOURNAL = 0x00002000;  /* VFS only */
  int SQLITE_OPEN_MASTER_JOURNAL = 0x00004000;  /* VFS only */
  int SQLITE_OPEN_NOMUTEX = 0x00008000;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_FULLMUTEX = 0x00010000;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_SHAREDCACHE = 0x00020000;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_PRIVATECACHE = 0x00040000;  /* Ok for sqlite3_open_v2() */
  int SQLITE_OPEN_WAL = 0x00080000;  /* VFS only */
}
