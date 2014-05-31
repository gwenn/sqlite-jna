/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.nio.ByteBuffer;

public class SQLite implements Library {
  public static final String JNA_LIBRARY_NAME = "sqlite3";

  // public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(SQLite.JNA_LIBRARY_NAME);
  static {
    Native.register(JNA_LIBRARY_NAME);
  }

  public static final int SQLITE_OK = 0;

  public static final int SQLITE_ROW = 100;
  public static final int SQLITE_DONE = 101;

  static final int SQLITE_TRANSIENT = -1;

  static native String sqlite3_libversion(); // no copy needed
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
  public static native int sqlite3_config(int op, LogCallback xLog, Pointer udp);
  public static native void sqlite3_log(int iErrCode, String msg);

  static native String sqlite3_errmsg(Pointer pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
  static native int sqlite3_errcode(Pointer pDb);

  static native int sqlite3_extended_result_codes(Pointer pDb, boolean onoff);
  static native int sqlite3_extended_errcode(Pointer pDb);

  static native int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs); // no copy needed
  static native int sqlite3_close(Pointer pDb);
  static native int sqlite3_close_v2(Pointer pDb);
  static native void sqlite3_interrupt(Pointer pDb);

  static native int sqlite3_busy_timeout(Pointer pDb, int ms);
  static native int sqlite3_db_config(Pointer pDb, int op, int v, IntByReference pOk);
  static native int sqlite3_enable_load_extension(Pointer pDb, boolean onoff);
  static native int sqlite3_load_extension(Pointer pDb, String file, String proc, PointerByReference errMsg);
  public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
      SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
      SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
      SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
  static native int sqlite3_limit(Pointer pDb, int id, int newVal);
  static native boolean sqlite3_get_autocommit(Pointer pDb);

  static native int sqlite3_changes(Pointer pDb);
  static native int sqlite3_total_changes(Pointer pDb);
  static native long sqlite3_last_insert_rowid(Pointer pDb);

  static native String sqlite3_db_filename(Pointer pDb, String dbName); // no copy needed
  static native int sqlite3_db_readonly(Pointer pDb, String dbName); // no copy needed

  //static native Pointer sqlite3_next_stmt(Pointer pDb, Pointer pStmt);

  static native int sqlite3_table_column_metadata(Pointer pDb, String dbName, String tableName, String columnName,
                                                  PointerByReference pzDataType, PointerByReference pzCollSeq,
                                                  IntByReference pNotNull, IntByReference pPrimaryKey, IntByReference pAutoinc); // no copy needed

  static native int sqlite3_exec(Pointer pDb, String cmd, Callback c, Pointer udp, PointerByReference errMsg);

  static native int sqlite3_prepare_v2(Pointer pDb, Pointer sql, int nByte, PointerByReference ppStmt,
                                       PointerByReference pTail);
  static native String sqlite3_sql(Pointer pStmt); // no copy needed
  static native int sqlite3_finalize(Pointer pStmt);
  static native int sqlite3_step(Pointer pStmt);
  static native int sqlite3_reset(Pointer pStmt);
  static native int sqlite3_clear_bindings(Pointer pStmt);
  static native boolean sqlite3_stmt_busy(Pointer pStmt);
  static native boolean sqlite3_stmt_readonly(Pointer pStmt);

  static native int sqlite3_column_count(Pointer pStmt);
  static native int sqlite3_data_count(Pointer pStmt);
  static native int sqlite3_column_type(Pointer pStmt, int iCol);
  static native String sqlite3_column_name(Pointer pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
  static native String sqlite3_column_origin_name(Pointer pStmt, int iCol); // copy needed
  static native String sqlite3_column_table_name(Pointer pStmt, int iCol); // copy needed
  static native String sqlite3_column_database_name(Pointer pStmt, int iCol); // copy needed
  static native String sqlite3_column_decltype(Pointer pStmt, int iCol); // copy needed

  static native Pointer sqlite3_column_blob(Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
  static native int sqlite3_column_bytes(Pointer pStmt, int iCol);
  static native double sqlite3_column_double(Pointer pStmt, int iCol);
  static native int sqlite3_column_int(Pointer pStmt, int iCol);
  static native long sqlite3_column_int64(Pointer pStmt, int iCol);
  static native String sqlite3_column_text(Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
  //const void *sqlite3_column_text16(Pointer pStmt, int iCol);
  //sqlite3_value *sqlite3_column_value(Pointer pStmt, int iCol);

  static native int sqlite3_bind_parameter_count(Pointer pStmt);
  static native int sqlite3_bind_parameter_index(Pointer pStmt, String name); // no copy needed
  static native String sqlite3_bind_parameter_name(Pointer pStmt, int i); // copy needed

  static native int sqlite3_bind_blob(Pointer pStmt, int i, byte[] value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
  static native int sqlite3_bind_double(Pointer pStmt, int i, double value);
  static native int sqlite3_bind_int(Pointer pStmt, int i, int value);
  static native int sqlite3_bind_int64(Pointer pStmt, int i, long value);
  static native int sqlite3_bind_null(Pointer pStmt, int i);
  static native int sqlite3_bind_text(Pointer pStmt, int i, String value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
  //static native int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
  //static native int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
  static native int sqlite3_bind_zeroblob(Pointer pStmt, int i, int n);

  static native Pointer sqlite3_mprintf(String zFormat, String arg); // no copy needed for args
  static native void sqlite3_free(Pointer p);

  static native int sqlite3_blob_open(Pointer pDb, String dbName, String tableName, String columnName,
                                      long iRow, boolean flags, PointerByReference ppBlob); // no copy needed
  static native int sqlite3_blob_reopen(Pointer pBlob, long iRow);
  static native int sqlite3_blob_bytes(Pointer pBlob);
  static native int sqlite3_blob_read(Pointer pBlob, ByteBuffer z, int n, int iOffset);
  static native int sqlite3_blob_write(Pointer pBlob, ByteBuffer z, int n, int iOffset);
  static native int sqlite3_blob_close(Pointer pBlob);

  static native Pointer sqlite3_backup_init(Pointer pDst, String dstName, Pointer pSrc, String srcName);
  static native int sqlite3_backup_step(Pointer pBackup, int nPage);
  static native int sqlite3_backup_remaining(Pointer pBackup);
  static native int sqlite3_backup_pagecount(Pointer pBackup);
  static native int sqlite3_backup_finish(Pointer pBackup);

  // As there is only one ProgressCallback by connection, and it is used to implement query timeout,
  // the method visibility is restricted.
  static native void sqlite3_progress_handler(Pointer pDb, int nOps, ProgressCallback xProgress, Pointer pArg);
  public static native void sqlite3_trace(Pointer pDb, TraceCallback xTrace, Pointer pArg);
  /*
  void (*)(sqlite3_context*,int,sqlite3_value**),
  void (*)(sqlite3_context*,int,sqlite3_value**),
  void (*)(sqlite3_context*),
  void(*)(void*)
  */
  // eTextRep: SQLITE_UTF8 => 1, ...
  public static native int sqlite3_create_function_v2(Pointer pDb, String functionName, int nArg, int eTextRep,
      Pointer pApp, ScalarCallback xFunc, Callback xStep, Callback xFinal, Callback xDestroy);
  public static native void sqlite3_result_null(Pointer pCtx);
  public static native void sqlite3_result_int(Pointer pCtx, int i);

  static Pointer nativeString(String sql) { // TODO Check encoding?
    byte[] data = sql.getBytes();
    final Pointer pointer = new Memory(data.length + 1);
    pointer.write(0, data, 0, data.length);
    pointer.setByte(data.length, (byte) 0);
    return pointer;
  }

  // http://sqlite.org/datatype3.html
  public static int getAffinity(String declType) {
    if (declType == null || declType.length() == 0) {
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

  public static interface LogCallback extends Callback {
    @SuppressWarnings("unused")
    void invoke(Pointer udp, int err, String msg);
  }
  private static final LogCallback LOG_CALLBACK = new LogCallback() {
    @Override
    public void invoke(Pointer udp, int err, String msg) {
      System.out.printf("%d: %s\n", err, msg);
    }
  };
  static {
    if (System.getProperty("sqlite.config.log", "").length() > 0) {
      // DriverManager.getLogWriter();
      SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_LOG, LOG_CALLBACK, null);
    }
  }

  public static interface ProgressCallback extends Callback {
    // return true to interrupt
    @SuppressWarnings("unused")
    boolean invoke(Pointer arg);
  }
}
