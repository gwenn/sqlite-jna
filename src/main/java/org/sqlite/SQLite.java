/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import org.bridj.BridJ;
import org.bridj.CRuntime;
import org.bridj.Callback;
import org.bridj.Pointer;
import org.bridj.ann.Library;
import org.bridj.ann.Optional;
import org.bridj.ann.Ptr;
import org.bridj.ann.Runtime;

import java.nio.charset.Charset;

import static org.bridj.Pointer.StringType;

// -Dbridj.sqlite3.library=/usr/local/opt/sqlite/lib/libsqlite3.dylib
@Library("sqlite3")
@Runtime(CRuntime.class)
public class SQLite {
  static final Charset UTF8 = Charset.forName("UTF-8");
  static {
    BridJ.register();
  }

  public static final int SQLITE_OK = 0;

  public static final int SQLITE_ROW = 100;
  public static final int SQLITE_DONE = 101;

  static final int SQLITE_TRANSIENT = -1;

  static native Pointer<Byte> sqlite3_libversion(); // no copy needed
  static native int sqlite3_libversion_number();
  static native boolean sqlite3_threadsafe();
  static native boolean sqlite3_compileoption_used(Pointer<Byte> optName);

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
  public static native <T> int sqlite3_config(int op, Pointer<? extends LogCallback<T>> xLog, Pointer<T> udp);
  public static native void sqlite3_log(int iErrCode, Pointer<Byte> msg);

  public static void sqlite3_log(int errCode, String msg) {
    sqlite3_log(errCode, pointerToString(msg));
  }

  static native Pointer<Byte> sqlite3_errmsg(Pointer pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
  static native int sqlite3_errcode(Pointer pDb);

  static native int sqlite3_extended_result_codes(Pointer pDb, boolean onoff);
  static native int sqlite3_extended_errcode(Pointer pDb);

  static native int sqlite3_initialize();
  static native int sqlite3_shutdown();

  static native int sqlite3_open_v2(Pointer<Byte> filename, Pointer<Pointer> ppDb, int flags, Pointer<Byte> zVfs); // no copy needed
  static int sqlite3_open_v2(String filename, Pointer<Pointer> ppDb, int flags, String vfs) {
    return sqlite3_open_v2(pointerToString(filename), ppDb, flags, pointerToString(vfs));
  }
  static native int sqlite3_close(Pointer pDb);
  static native int sqlite3_close_v2(Pointer pDb); // since 3.7.14
  static native void sqlite3_interrupt(Pointer pDb);

  static native int sqlite3_busy_timeout(Pointer pDb, int ms);
  static native int sqlite3_db_config(Pointer pDb, int op, int v, Pointer<Boolean> pOk);
  @Optional
  static native int sqlite3_enable_load_extension(Pointer pDb, boolean onoff);
  @Optional
  static native int sqlite3_load_extension(Pointer pDb, Pointer<Byte> file, Pointer<Byte> proc, Pointer<Pointer<Byte>> errMsg);
  static int sqlite3_load_extension(Pointer pDb, String file, String proc, Pointer<Pointer<Byte>> errMsg) {
    return sqlite3_load_extension(pDb, pointerToString(file), pointerToString(proc), errMsg);
  }
  public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
      SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
      SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
      SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
  static native int sqlite3_limit(Pointer pDb, int id, int newVal);
  static native boolean sqlite3_get_autocommit(Pointer pDb);

  static native int sqlite3_changes(Pointer pDb);
  static native int sqlite3_total_changes(Pointer pDb);
  static native long sqlite3_last_insert_rowid(Pointer pDb);

  static native Pointer<Byte> sqlite3_db_filename(Pointer pDb, Pointer<Byte> dbName); // no copy needed
  static String sqlite3_db_filename(Pointer pDb, String dbName) {
    return getCString(sqlite3_db_filename(pDb, pointerToString(dbName)));
  }
  static native int sqlite3_db_readonly(Pointer pDb, Pointer<Byte> dbName); // no copy needed
  static int sqlite3_db_readonly(Pointer pDb, String dbName) {
    return sqlite3_db_readonly(pDb, pointerToString(dbName));
  }

  static native Pointer sqlite3_next_stmt(Pointer pDb, Pointer pStmt);

  @Optional
  static native int sqlite3_table_column_metadata(Pointer pDb, Pointer<Byte> dbName, Pointer<Byte> tableName, Pointer<Byte> columnName,
                                                  Pointer<Pointer<Byte>> pzDataType, Pointer<Pointer<Byte>> pzCollSeq,
                                                  Pointer<Boolean> pNotNull, Pointer<Boolean> pPrimaryKey, Pointer<Boolean> pAutoinc); // no copy needed
  static int sqlite3_table_column_metadata(Pointer pDb, String dbName, String tableName, String columnName,
                                                  Pointer<Pointer<Byte>> pzDataType, Pointer<Pointer<Byte>> pzCollSeq,
                                                  Pointer<Boolean> pNotNull, Pointer<Boolean> pPrimaryKey, Pointer<Boolean> pAutoinc) {
    return sqlite3_table_column_metadata(pDb, pointerToString(dbName), pointerToString(tableName), pointerToString(columnName),
        pzDataType, pzCollSeq, pNotNull, pPrimaryKey, pAutoinc);
  }

  static native int sqlite3_exec(Pointer pDb, Pointer<Byte> cmd, Pointer<Callback> c, Pointer udp, Pointer<Pointer<Byte>> errMsg);
  static int sqlite3_exec(Pointer pDb, String cmd, Callback c, Pointer udp, Pointer<Pointer<Byte>> errMsg) {
    return sqlite3_exec(pDb, pointerToString(cmd), Pointer.pointerTo(c), udp, errMsg);
  }

  static native int sqlite3_prepare_v2(Pointer pDb, Pointer<Byte> zSql, int nByte, Pointer<Pointer> ppStmt,
                                       Pointer<Pointer<Byte>> pzTail);
  static native Pointer<Byte> sqlite3_sql(Pointer pStmt); // no copy needed
  static native int sqlite3_finalize(Pointer pStmt);
  static native int sqlite3_step(Pointer pStmt);
  static native int sqlite3_reset(Pointer pStmt);
  static native int sqlite3_clear_bindings(Pointer pStmt);
  static native boolean sqlite3_stmt_busy(Pointer pStmt);
  static native boolean sqlite3_stmt_readonly(Pointer pStmt);

  static native int sqlite3_column_count(Pointer pStmt);
  static native int sqlite3_data_count(Pointer pStmt);
  static native int sqlite3_column_type(Pointer pStmt, int iCol);
  static native Pointer<Byte> sqlite3_column_name(Pointer pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
  @Optional
  static native Pointer<Byte> sqlite3_column_origin_name(Pointer pStmt, int iCol); // copy needed
  @Optional
  static native Pointer<Byte> sqlite3_column_table_name(Pointer pStmt, int iCol); // copy needed
  @Optional
  static native Pointer<Byte> sqlite3_column_database_name(Pointer pStmt, int iCol); // copy needed
  static native Pointer<Byte> sqlite3_column_decltype(Pointer pStmt, int iCol); // copy needed

  static native Pointer<Byte> sqlite3_column_blob(Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
  static native int sqlite3_column_bytes(Pointer pStmt, int iCol);
  static native double sqlite3_column_double(Pointer pStmt, int iCol);
  static native int sqlite3_column_int(Pointer pStmt, int iCol);
  static native long sqlite3_column_int64(Pointer pStmt, int iCol);
  static native Pointer<Byte> sqlite3_column_text(Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
  //const void *sqlite3_column_text16(sqlite3_stmt pStmt, int iCol);
  //sqlite3_value *sqlite3_column_value(sqlite3_stmt pStmt, int iCol);

  static native int sqlite3_bind_parameter_count(Pointer pStmt);
  static native int sqlite3_bind_parameter_index(Pointer pStmt, Pointer<Byte> name); // no copy needed
  static int sqlite3_bind_parameter_index(Pointer pStmt, String name) {
    return sqlite3_bind_parameter_index(pStmt, pointerToString(name));
  }
  static native Pointer<Byte> sqlite3_bind_parameter_name(Pointer pStmt, int i); // copy needed

  static native int sqlite3_bind_blob(Pointer pStmt, int i, Pointer<Byte> value, int n, @Ptr long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
  static native int sqlite3_bind_double(Pointer pStmt, int i, double value);
  static native int sqlite3_bind_int(Pointer pStmt, int i, int value);
  static native int sqlite3_bind_int64(Pointer pStmt, int i, long value);
  static native int sqlite3_bind_null(Pointer pStmt, int i);
  static native int sqlite3_bind_text(Pointer pStmt, int i, Pointer<Byte> value, int n, @Ptr long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
  //static native int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
  //static native int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
  static native int sqlite3_bind_zeroblob(Pointer pStmt, int i, int n);

  static native Pointer<Byte> sqlite3_mprintf(Pointer<Byte> zFormat, Pointer<Byte> arg); // no copy needed for args
  static String sqlite3_mprintf(String format, String arg) {
    final Pointer<Byte> p = sqlite3_mprintf(pointerToString(format), pointerToString(arg));
    final String s = getCString(p);
    sqlite3_free(p);
    return s;
  }
  static native void sqlite3_free(Pointer p);

  static native int sqlite3_blob_open(Pointer pDb, Pointer<Byte> dbName, Pointer<Byte> tableName, Pointer<Byte> columnName,
                                      long iRow, boolean flags, Pointer<Pointer> ppBlob); // no copy needed
  static int sqlite3_blob_open(Pointer pDb, String dbName, String tableName, String columnName,
                                      long iRow, boolean flags, Pointer<Pointer> ppBlob) {
    return sqlite3_blob_open(pDb, pointerToString(dbName), pointerToString(tableName), pointerToString(columnName), iRow, flags, ppBlob);
  }
  static native int sqlite3_blob_reopen(Pointer pBlob, long iRow);
  static native int sqlite3_blob_bytes(Pointer pBlob);
  static native int sqlite3_blob_read(Pointer pBlob, Pointer<Byte> z, int n, int iOffset);
  static native int sqlite3_blob_write(Pointer pBlob, Pointer<Byte> z, int n, int iOffset);
  static native int sqlite3_blob_close(Pointer pBlob);

  static native Pointer sqlite3_backup_init(Pointer pDst, Pointer<Byte> dstName, Pointer pSrc, Pointer<Byte> srcName);
  static Pointer sqlite3_backup_init(Pointer pDst, String dstName, Pointer pSrc, String srcName) {
    return sqlite3_backup_init(pDst, pointerToString(dstName), pSrc, pointerToString(srcName));
  }
  static native int sqlite3_backup_step(Pointer pBackup, int nPage);
  static native int sqlite3_backup_remaining(Pointer pBackup);
  static native int sqlite3_backup_pagecount(Pointer pBackup);
  static native int sqlite3_backup_finish(Pointer pBackup);

  // As there is only one ProgressCallback by connection, and it is used to implement query timeout,
  // the method visibility is restricted.
  static native <T> void sqlite3_progress_handler(Pointer pDb, int nOps, Pointer<? extends ProgressCallback<T>> xProgress, Pointer<T> pArg);
  public static native <T> void sqlite3_trace(Pointer pDb, Pointer<? extends TraceCallback<T>> xTrace, Pointer<T> pArg);

  // eTextRep: SQLITE_UTF8 => 1, ...
  public static native <T> int sqlite3_create_function_v2(Pointer pDb, Pointer<Byte> functionName, int nArg, int eTextRep,
                                                      Pointer<T> pApp, Pointer<? extends ScalarCallback<T>> xFunc,
                                                      Pointer<Callback> xStep, Pointer<Callback> xFinal,
                                                      Pointer<Callback> xDestroy);
  public static <T> int sqlite3_create_function_v2(Pointer pDb, String functionName, int nArg, int eTextRep,
                                                      Pointer<T> pApp, ScalarCallback<T> xFunc,
                                                      Callback xStep, Callback xFinal,
                                                      Callback xDestroy) {
    return sqlite3_create_function_v2(pDb, pointerToString(functionName), nArg, eTextRep, pApp, Pointer.pointerTo(xFunc),
        Pointer.pointerTo(xStep), Pointer.pointerTo(xFinal), Pointer.pointerTo(xDestroy));
  }
  public static native void sqlite3_result_null(Pointer pCtx);
  public static native void sqlite3_result_int(Pointer pCtx, int i);

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

  public static abstract class LogCallback<T> extends Callback<LogCallback<T>> {
    @SuppressWarnings("unused")
    public abstract void apply(Pointer<T> udp, int err, Pointer<Byte> msg);
  }
  /*
  private static final LogCallback LOG_CALLBACK = new LogCallback() {
    @Override
    public void apply(Pointer udp, int err, String msg) {
      System.out.printf("%d: %s\n", err, msg);
    }
  };
  static {
    if (System.getProperty("sqlite.config.log", "").length() > 0) {
      // DriverManager.getLogWriter();
      SQLite.sqlite3_config(SQLite.SQLITE_CONFIG_LOG, LOG_CALLBACK, null);
    }
  }
  */

  public static abstract class ProgressCallback<T> extends Callback<ProgressCallback<T>> {
    // return true to interrupt
    @SuppressWarnings("unused")
    public abstract boolean apply(Pointer<T> arg);
  }

  static String getCString(Pointer<Byte> p) {
    if (p == null) {
      return null;
    }
    try {
      return p.getString(StringType.C, UTF8);
    } finally {
      p.release();
    }
  }

  static Pointer<Byte> pointerToString(String s) {
    return (Pointer<Byte>) Pointer.pointerToString(s, StringType.C, UTF8);
  }
}
