/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

public final class SQLite {
  static {
    System.loadLibrary("sqlite_jni");
  }

  public static final int SQLITE_OK = 0;

  public static final int SQLITE_ROW = 100;
  public static final int SQLITE_DONE = 101;

  static final int SQLITE_TRANSIENT = -1;

  static native String sqlite3_libversion(); // no copy needed
  static native int sqlite3_libversion_number();
  static native boolean sqlite3_threadsafe();
  static native boolean sqlite3_compileoption_used(String optName);

  public static final int SQLITE_CONFIG_SINGLETHREAD = 1,
      SQLITE_CONFIG_MULTITHREAD = 2, SQLITE_CONFIG_SERIALIZED = 3,
      SQLITE_CONFIG_MEMSTATUS = 9,
      SQLITE_CONFIG_LOG = 16,
      SQLITE_CONFIG_URI = 17;
  //sqlite3_config(SQLITE_CONFIG_SINGLETHREAD|SQLITE_CONFIG_MULTITHREAD|SQLITE_CONFIG_SERIALIZED)
  static native int sqlite3_config(int op);
  //sqlite3_config(SQLITE_CONFIG_URI, int onoff)
  //sqlite3_config(SQLITE_CONFIG_MEMSTATUS, int onoff)
  static native int sqlite3_config(int op, boolean onoff);
  //sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
  public static native int sqlite3_config(int op, LogCallback xLog);
  // Applications can use the sqlite3_log(E,F,..) API to send new messages to the log, if desired, but this is discouraged.
  public static native void sqlite3_log(int iErrCode, String msg);

  static native String sqlite3_errmsg(long pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
  static native int sqlite3_errcode(long pDb);

  static native int sqlite3_extended_result_codes(long pDb, boolean onoff);
  static native int sqlite3_extended_errcode(long pDb);

  static native int sqlite3_initialize();
  static native int sqlite3_shutdown();

  static native int sqlite3_open_v2(String filename, long[] ppDb, int flags, String vfs); // no copy needed
  static native int sqlite3_close(long pDb);
  static native int sqlite3_close_v2(long pDb); // since 3.7.14
  static native void sqlite3_interrupt(long pDb);

  static native int sqlite3_busy_timeout(long pDb, int ms);
  static native int sqlite3_db_config(long pDb, int op, int v, int[] pOk);
  static native int sqlite3_enable_load_extension(long pDb, boolean onoff);
  static native int sqlite3_load_extension(long pDb, String file, String proc, String[] ppErrMsg);
  public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
      SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
      SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
      SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
  static native int sqlite3_limit(long pDb, int id, int newVal);
  static native boolean sqlite3_get_autocommit(long pDb);

  static native int sqlite3_changes(long pDb);
  static native int sqlite3_total_changes(long pDb);
  static native long sqlite3_last_insert_rowid(long pDb);

  static native String sqlite3_db_filename(long pDb, String dbName); // no copy needed
  static native int sqlite3_db_readonly(long pDb, String dbName); // no copy needed

  static native long sqlite3_next_stmt(long pDb, long pStmt);

  static native int sqlite3_table_column_metadata(long pDb, String dbName, String tableName, String columnName,
                                                  String[] pDataType, String[] pCollSeq,
                                                  int[] pFlags); // no copy needed
  // int (*callback)(void*,int,char**,char**)
  static native int sqlite3_exec(long pDb, String cmd, Object cb, Object udp, String[] ppErrMsg);

  static native int sqlite3_prepare_v2(long pDb, String sql, int nByte, long[] ppStmt,
                                       String[] pTail);
  static native String sqlite3_sql(long pStmt); // no copy needed
  static native int sqlite3_finalize(long pStmt);
  static native int sqlite3_step(long pStmt);
  static native int sqlite3_reset(long pStmt);
  static native int sqlite3_clear_bindings(long pStmt);
  static native boolean sqlite3_stmt_busy(long pStmt);
  static native boolean sqlite3_stmt_readonly(long pStmt);

  static native int sqlite3_column_count(long pStmt);
  static native int sqlite3_data_count(long pStmt);
  static native int sqlite3_column_type(long pStmt, int iCol);
  static native String sqlite3_column_name(long pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
  static native String sqlite3_column_origin_name(long pStmt, int iCol); // copy needed
  static native String sqlite3_column_table_name(long pStmt, int iCol); // copy needed
  static native String sqlite3_column_database_name(long pStmt, int iCol); // copy needed
  static native String sqlite3_column_decltype(long pStmt, int iCol); // copy needed

  static native byte[] sqlite3_column_blob(long pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
  static native int sqlite3_column_bytes(long pStmt, int iCol);
  static native double sqlite3_column_double(long pStmt, int iCol);
  static native int sqlite3_column_int(long pStmt, int iCol);
  static native long sqlite3_column_int64(long pStmt, int iCol);
  static native String sqlite3_column_text(long pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
  //const void *sqlite3_column_text16(long pStmt, int iCol);
  //sqlite3_value *sqlite3_column_value(long pStmt, int iCol);

  static native int sqlite3_bind_parameter_count(long pStmt);
  static native int sqlite3_bind_parameter_index(long pStmt, String name); // no copy needed
  static native String sqlite3_bind_parameter_name(long pStmt, int i); // copy needed

  static native int sqlite3_bind_blob(long pStmt, int i, byte[] value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
  static native int sqlite3_bind_double(long pStmt, int i, double value);
  static native int sqlite3_bind_int(long pStmt, int i, int value);
  static native int sqlite3_bind_int64(long pStmt, int i, long value);
  static native int sqlite3_bind_null(long pStmt, int i);
  static native int sqlite3_bind_text(long pStmt, int i, String value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
  //static native int sqlite3_bind_text16(long pStmt, int i, const void*, int, void(*)(void*));
  //static native int sqlite3_bind_value(long pStmt, int i, const sqlite3_value*);
  static native int sqlite3_bind_zeroblob(long pStmt, int i, int n);

  static native int sqlite3_blob_open(long pDb, String dbName, String tableName, String columnName,
                                      long iRow, boolean flags, long[] ppBlob); // no copy needed
  static native int sqlite3_blob_reopen(long pBlob, long iRow);
  static native int sqlite3_blob_bytes(long pBlob);
  static native int sqlite3_blob_read(long pBlob, byte[] z, int zOff, int n, int iOffset);
  static native int sqlite3_blob_write(long pBlob, byte[] z, int zOff, int n, int iOffset);
  static native int sqlite3_blob_close(long pBlob);

  static native long sqlite3_backup_init(long pDst, String dstName, long pSrc, String srcName);
  static native int sqlite3_backup_step(long pBackup, int nPage);
  static native int sqlite3_backup_remaining(long pBackup);
  static native int sqlite3_backup_pagecount(long pBackup);
  static native int sqlite3_backup_finish(long pBackup);

  static native void free_callback_context(long p);

  // As there is only one ProgressCallback by connection, and it is used to implement query timeout,
  // the method visibility is restricted.
  static native long sqlite3_progress_handler(long pDb, int nOps, ProgressCallback xProgress);
  public static native long sqlite3_trace(long pDb, TraceCallback xTrace);
  /*
  void (*)(sqlite3_context*,int,sqlite3_value**),
  void (*)(sqlite3_context*,int,sqlite3_value**),
  void (*)(sqlite3_context*),
  void(*)(void*)
  */
  // eTextRep: SQLITE_UTF8 => 1, ...
  public static native int sqlite3_create_function_v2(long pDb, String functionName, int nArg, int eTextRep,
      Object pApp, ScalarCallback xFunc, Object xStep, Object xFinal, Object xDestroy);
  public static native void sqlite3_result_null(long pCtx);
  public static native void sqlite3_result_int(long pCtx, int i);

  // http://sqlite.org/datatype3.html
  public static int getAffinity(String declType) {
    if (declType == null || declType.isEmpty()) {
      return ColAffinities.NONE;
    }
    declType = declType.toUpperCase();
    if (declType.contains("INT")) {
      return ColAffinities.INTEGER;
    } else if (declType.contains("TEXT") || declType.contains("CHAR") || declType.contains("CLOB")) {
      return ColAffinities.TEXT;
    } else if (declType.contains("BLOB")) {
      return ColAffinities.NONE;
    } else if (declType.contains("REAL") || declType.contains("FLOA") || declType.contains("DOUB")) {
      return ColAffinities.REAL;
    } else {
      return ColAffinities.NUMERIC;
    }
  }

  private SQLite() {
  }

  public static String escapeIdentifier(String identifier) {
    if (identifier == null) {
      return "";
    }
    if (identifier.indexOf('"') >= 0) { // escape quote by doubling them
      identifier = identifier.replaceAll("\"", "\"\"");
    }
    return identifier;
  }

  public static String doubleQuote(String dbName) {
    if (dbName == null) {
      return "";
    }
    if ("main".equals(dbName) || "temp".equals(dbName)) {
      return dbName;
    }
    return '"' + escapeIdentifier(dbName) + '"'; // surround identifier with quote
  }
  public static String qualify(String dbName) {
    if (dbName == null) {
      return "";
    }
    if ("main".equals(dbName) || "temp".equals(dbName)) {
      return dbName + '.';
    }
    return '"' + escapeIdentifier(dbName) + '"' + '.'; // surround identifier with quote
  }

  public interface LogCallback {
    @SuppressWarnings("unused")
    void log(int err, String msg);
  }
  private static final LogCallback LOG_CALLBACK = new LogCallback() {
    @Override
    public void log(int err, String msg) {
      System.out.printf("%d: %s%n", err, msg);
    }
  };
  static {
    if (!System.getProperty("sqlite.config.log", "").isEmpty()) {
      // DriverManager.getLogWriter();
      sqlite3_config(SQLITE_CONFIG_LOG, LOG_CALLBACK);
    }
  }

  public interface ProgressCallback {
    // return true to interrupt
    @SuppressWarnings("unused")
    boolean progress();
  }
}
