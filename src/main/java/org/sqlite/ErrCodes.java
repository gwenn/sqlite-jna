package org.sqlite;

public interface ErrCodes {
  /**
   * Successful result
   */
  int SQLITE_OK = 0;

  /**
   * SQL error or missing database
   */
  int SQLITE_ERROR = 1;
  /**
   * Internal logic error in SQLite
   */
  int SQLITE_INTERNAL = 2;
  /**
   * Access permission denied
   */
  int SQLITE_PERM = 3;
  /**
   * Callback routine requested an abort
   */
  int SQLITE_ABORT = 4;
  /**
   * The database file is locked
   */
  int SQLITE_BUSY = 5;
  /**
   * A table in the database is locked
   */
  int SQLITE_LOCKED = 6;
  /**
   * A malloc() failed
   */
  int SQLITE_NOMEM = 7;
  /**
   * Attempt to write a readonly database
   */
  int SQLITE_READONLY = 8;
  /**
   * Operation terminated by sqlite_interrupt()
   */
  int SQLITE_INTERRUPT = 9;
  /**
   * Some kind of disk I/O error occurred
   */
  int SQLITE_IOERR = 10;
  /**
   * The database disk image is malformed
   */
  int SQLITE_CORRUPT = 11;
  /**
   * Unknown opcode in sqlite3_file_control()
   */
  int SQLITE_NOTFOUND = 12;
  /**
   * Insertion failed because database is full
   */
  int SQLITE_FULL = 13;
  /**
   * Unable to open the database file
   */
  int SQLITE_CANTOPEN = 14;
  /**
   * Database lock protocol error
   */
  int SQLITE_PROTOCOL = 15;
  /**
   * Database table is empty
   */
  int SQLITE_EMPTY = 16;
  /**
   * The database schema changed
   */
  int SQLITE_SCHEMA = 17;
  /**
   * String or BLOB exceeds size limit
   */
  int SQLITE_TOOBIG = 18;
  /**
   * Abort due to constraint violation
   */
  int SQLITE_CONSTRAINT = 19;
  /**
   * Data type mismatch
   */
  int SQLITE_MISMATCH = 20;
  /**
   * Library used incorrectly
   */
  int SQLITE_MISUSE = 21;
  /**
   * Uses OS features not supported on host
   */
  int SQLITE_NOLFS = 22;
  /**
   * Authorization denied
   */
  int SQLITE_AUTH = 23;
  /**
   *  Auxiliary database format error
   */
  int SQLITE_FORMAT = 24;
  /**
   * 2nd parameter to sqlite3_bind out of range
   */
  int SQLITE_RANGE = 25;
  /**
   * File opened that is not a database file
   */
  int SQLITE_NOTADB = 26;

  /** sqlite_step() has another row ready */
  //int SQLITE_ROW        =  100;
  /** sqlite_step() has finished executing */
  //int SQLITE_DONE       =  101;
}
