/*
 * The author disclaims copyright to this source code.  In place of
 * a legal notice, here is a blessing:
 *
 *    May you do good and not evil.
 *    May you find forgiveness for yourself and forgive others.
 *    May you share freely, never taking more than you give.
 */
package org.sqlite;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;

import java.nio.ByteBuffer;

public final class SQLite {
  private static final String LIBRARY_NAME = "sqlite3";
  private static final LibSQLite library;
  static {
    library = LibraryLoader.create(LibSQLite.class).load(LIBRARY_NAME);
  }

  public static final int SQLITE_OK = 0;

  public static final int SQLITE_ROW = 100;
  public static final int SQLITE_DONE = 101;

  static final int SQLITE_TRANSIENT = -1;

  static String sqlite3_libversion() { // no copy needed
    return library.sqlite3_libversion();
  }
  static int sqlite3_libversion_number() {
    return library.sqlite3_libversion_number();
  }
  static boolean sqlite3_threadsafe() {
    return library.sqlite3_threadsafe();
  }
  static boolean sqlite3_compileoption_used(String optName) {
    return library.sqlite3_compileoption_used(optName);
  }

  public static final int SQLITE_CONFIG_SINGLETHREAD = 1,
      SQLITE_CONFIG_MULTITHREAD = 2, SQLITE_CONFIG_SERIALIZED = 3,
      SQLITE_CONFIG_MEMSTATUS = 9,
      SQLITE_CONFIG_LOG = 16,
      SQLITE_CONFIG_URI = 17;
  //sqlite3_config(SQLITE_CONFIG_SINGLETHREAD|SQLITE_CONFIG_MULTITHREAD|SQLITE_CONFIG_SERIALIZED)
  static int sqlite3_config(int op) {
    return library.sqlite3_config(op);
  }
  //sqlite3_config(SQLITE_CONFIG_URI, int onoff)
  //sqlite3_config(SQLITE_CONFIG_MEMSTATUS, int onoff)
  static int sqlite3_config(int op, boolean onoff) {
    return library.sqlite3_config(op, onoff);
  }
  //sqlite3_config(SQLITE_CONFIG_LOG, void(*)(void *udp, int err, const char *msg), void *udp)
  public static int sqlite3_config(int op, SQLite.LogCallback xLog, Pointer udp) {
    return library.sqlite3_config(op,xLog, udp);
  }
  // Applications can use the sqlite3_log(E,F,..) API to send new messages to the log, if desired, but this is discouraged.
  public static void sqlite3_log(int iErrCode, String msg) {
    library.sqlite3_log(iErrCode, msg);
  }

  static String sqlite3_errmsg(Pointer pDb) { // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
    return library.sqlite3_errmsg(pDb);
  }
  static int sqlite3_errcode(Pointer pDb) {
    return library.sqlite3_errcode(pDb);
  }

  static int sqlite3_extended_result_codes(Pointer pDb, boolean onoff) {
    return library.sqlite3_extended_result_codes(pDb, onoff);
  }
  static int sqlite3_extended_errcode(Pointer pDb) {
    return library.sqlite3_extended_errcode(pDb);
  }

  static int sqlite3_initialize() {
    return library.sqlite3_initialize();
  }
  static int sqlite3_shutdown() {
    return library.sqlite3_shutdown();
  }

  static int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs) { // no copy needed
    return library.sqlite3_open_v2(filename, ppDb, flags, vfs);
  }
  static int sqlite3_close(Pointer pDb) {
    return library.sqlite3_close(pDb);
  }
  static int sqlite3_close_v2(Pointer pDb) { // since 3.7.14
    return library.sqlite3_close_v2(pDb);
  }
  static void sqlite3_interrupt(Pointer pDb) {
    library.sqlite3_interrupt(pDb);
  }

  static int sqlite3_busy_timeout(Pointer pDb, int ms) {
    return library.sqlite3_busy_timeout(pDb, ms);
  }
  static int sqlite3_db_config(Pointer pDb, int op, int v, IntByReference pOk) {
    return library.sqlite3_db_config(pDb, op, v, pOk);
  }
  static int sqlite3_enable_load_extension(Pointer pDb, boolean onoff) {
    return library.sqlite3_enable_load_extension(pDb, onoff);
  }
  static int sqlite3_load_extension(Pointer pDb, String file, String proc, PointerByReference errMsg) {
    return library.sqlite3_load_extension(pDb, file, proc, errMsg);
  }
  public static final int SQLITE_LIMIT_LENGTH = 0, SQLITE_LIMIT_SQL_LENGTH = 1, SQLITE_LIMIT_COLUMN = 2,
      SQLITE_LIMIT_EXPR_DEPTH = 3, SQLITE_LIMIT_COMPOUND_SELECT = 4, SQLITE_LIMIT_VDBE_OP = 5,
      SQLITE_LIMIT_FUNCTION_ARG = 6, SQLITE_LIMIT_ATTACHED = 7, SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8,
      SQLITE_LIMIT_VARIABLE_NUMBER = 9, SQLITE_LIMIT_TRIGGER_DEPTH = 10;
  static int sqlite3_limit(Pointer pDb, int id, int newVal) {
    return library.sqlite3_limit(pDb, id, newVal);
  }
  static boolean sqlite3_get_autocommit(Pointer pDb) {
    return library.sqlite3_get_autocommit(pDb);
  }

  static int sqlite3_changes(Pointer pDb) {
    return library.sqlite3_changes(pDb);
  }
  static int sqlite3_total_changes(Pointer pDb) {
    return library.sqlite3_total_changes(pDb);
  }
  static long sqlite3_last_insert_rowid(Pointer pDb) {
    return library.sqlite3_last_insert_rowid(pDb);
  }

  static String sqlite3_db_filename(Pointer pDb, String dbName) { // no copy needed
    return library.sqlite3_db_filename(pDb, dbName);
  }
  static int sqlite3_db_readonly(Pointer pDb, String dbName) { // no copy needed
    return library.sqlite3_db_readonly(pDb, dbName);
  }

  static Pointer sqlite3_next_stmt(Pointer pDb, Pointer pStmt) {
    return library.sqlite3_next_stmt(pDb, pStmt);
  }

  static int sqlite3_table_column_metadata(Pointer pDb, String dbName, String tableName, String columnName,
                                                  PointerByReference pzDataType, PointerByReference pzCollSeq,
                                                  IntByReference pNotNull, IntByReference pPrimaryKey, IntByReference pAutoinc) { // no copy needed
    return library.sqlite3_table_column_metadata(pDb, dbName, tableName, columnName, pzDataType, pzCollSeq, pNotNull, pPrimaryKey, pAutoinc);
  }
  // int (*callback)(void*,int,char**,char**)
  static int sqlite3_exec(Pointer pDb, String cmd, Pointer c, Pointer udp, PointerByReference errMsg) {
    return library.sqlite3_exec(pDb, cmd, c, udp, errMsg);
  }

  static int sqlite3_prepare_v2(Pointer pDb, Pointer sql, int nByte, PointerByReference ppStmt,
                                       PointerByReference pTail) {
    return library.sqlite3_prepare_v2(pDb, sql, nByte, ppStmt, pTail);
  }
  static String sqlite3_sql(Pointer pStmt) { // no copy needed
    return library.sqlite3_sql(pStmt);
  }
  static int sqlite3_finalize(Pointer pStmt) {
    return library.sqlite3_finalize(pStmt);
  }
  static int sqlite3_step(Pointer pStmt) {
    return library.sqlite3_step(pStmt);
  }
  static int sqlite3_reset(Pointer pStmt) {
    return library.sqlite3_reset(pStmt);
  }
  static int sqlite3_clear_bindings(Pointer pStmt) {
    return library.sqlite3_clear_bindings(pStmt);
  }
  static boolean sqlite3_stmt_busy(Pointer pStmt) {
    return library.sqlite3_stmt_busy(pStmt);
  }
  static boolean sqlite3_stmt_readonly(Pointer pStmt) {
    return library.sqlite3_stmt_readonly(pStmt);
  }

  static int sqlite3_column_count(Pointer pStmt) {
    return library.sqlite3_column_count(pStmt);
  }
  static int sqlite3_data_count(Pointer pStmt) {
    return library.sqlite3_data_count(pStmt);
  }
  static int sqlite3_column_type(Pointer pStmt, int iCol) {
    return library.sqlite3_column_type(pStmt, iCol);
  }
  static String sqlite3_column_name(Pointer pStmt, int iCol) { // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
    return library.sqlite3_column_name(pStmt, iCol);
  }
  static String sqlite3_column_origin_name(Pointer pStmt, int iCol) { // copy needed
    return library.sqlite3_column_origin_name(pStmt, iCol);
  }
  static String sqlite3_column_table_name(Pointer pStmt, int iCol) { // copy needed
    return library.sqlite3_column_table_name(pStmt, iCol);
  }
  static String sqlite3_column_database_name(Pointer pStmt, int iCol) { // copy needed
    return library.sqlite3_column_database_name(pStmt, iCol);
  }
  static String sqlite3_column_decltype(Pointer pStmt, int iCol) { // copy needed
    return library.sqlite3_column_decltype(pStmt, iCol);
  }

  static Pointer sqlite3_column_blob(Pointer pStmt, int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
    return library.sqlite3_column_blob(pStmt, iCol);
  }
  static int sqlite3_column_bytes(Pointer pStmt, int iCol) {
    return library.sqlite3_column_bytes(pStmt, iCol);
  }
  static double sqlite3_column_double(Pointer pStmt, int iCol) {
    return library.sqlite3_column_double(pStmt, iCol);
  }
  static int sqlite3_column_int(Pointer pStmt, int iCol) {
    return library.sqlite3_column_int(pStmt, iCol);
  }
  static long sqlite3_column_int64(Pointer pStmt, int iCol) {
    return library.sqlite3_column_int64(pStmt, iCol);
  }
  static String sqlite3_column_text(Pointer pStmt, int iCol) { // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
    return library.sqlite3_column_text(pStmt, iCol);
  }
  //const void *sqlite3_column_text16(Pointer pStmt, int iCol);
  //sqlite3_value *sqlite3_column_value(Pointer pStmt, int iCol);

  static int sqlite3_bind_parameter_count(Pointer pStmt) {
    return library.sqlite3_bind_parameter_count(pStmt);
  }
  static int sqlite3_bind_parameter_index(Pointer pStmt, String name) { // no copy needed
    return library.sqlite3_bind_parameter_index(pStmt, name);
  }
  static String sqlite3_bind_parameter_name(Pointer pStmt, int i) { // copy needed
    return library.sqlite3_bind_parameter_name(pStmt, i);
  }

  static int sqlite3_bind_blob(Pointer pStmt, int i, byte[] value, int n, long xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
    return library.sqlite3_bind_blob(pStmt, i, value, n, xDel);
  }
  static int sqlite3_bind_double(Pointer pStmt, int i, double value) {
    return library.sqlite3_bind_double(pStmt, i, value);
  }
  static int sqlite3_bind_int(Pointer pStmt, int i, int value) {
    return library.sqlite3_bind_int(pStmt, i, value);
  }
  static int sqlite3_bind_int64(Pointer pStmt, int i, long value) {
    return library.sqlite3_bind_int64(pStmt, i, value);
  }
  static int sqlite3_bind_null(Pointer pStmt, int i) {
    return library.sqlite3_bind_null(pStmt, i);
  }
  static int sqlite3_bind_text(Pointer pStmt, int i, String value, int n, long xDel) { // no copy needed when xDel == SQLITE_TRANSIENT == -1
    return library.sqlite3_bind_text(pStmt, i, value, n, xDel);
  }
  //static int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
  //static int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
  static int sqlite3_bind_zeroblob(Pointer pStmt, int i, int n) {
    return library.sqlite3_bind_zeroblob(pStmt, i, n);
  }

  static void sqlite3_free(Pointer p) {
    library.sqlite3_free(p);
  }

  static int sqlite3_blob_open(Pointer pDb, String dbName, String tableName, String columnName,
                                      long iRow, boolean flags, PointerByReference ppBlob) { // no copy needed
    return library.sqlite3_blob_open(pDb, dbName, tableName, columnName, iRow, flags, ppBlob);
  }
  static int sqlite3_blob_reopen(Pointer pBlob, long iRow) {
    return library.sqlite3_blob_reopen(pBlob, iRow);
  }
  static int sqlite3_blob_bytes(Pointer pBlob) {
    return library.sqlite3_blob_bytes(pBlob);
  }
  static int sqlite3_blob_read(Pointer pBlob, ByteBuffer z, int n, int iOffset) {
    return library.sqlite3_blob_read(pBlob, z, n, iOffset);
  }
  static int sqlite3_blob_write(Pointer pBlob, ByteBuffer z, int n, int iOffset) {
    return library.sqlite3_blob_write(pBlob, z, n, iOffset);
  }
  static int sqlite3_blob_close(Pointer pBlob) {
    return library.sqlite3_blob_close(pBlob);
  }

  static Pointer sqlite3_backup_init(Pointer pDst, String dstName, Pointer pSrc, String srcName) {
    return library.sqlite3_backup_init(pDst, dstName, pSrc, srcName);
  }
  static int sqlite3_backup_step(Pointer pBackup, int nPage) {
    return library.sqlite3_backup_step(pBackup, nPage);
  }
  static int sqlite3_backup_remaining(Pointer pBackup) {
    return library.sqlite3_backup_remaining(pBackup);
  }
  static int sqlite3_backup_pagecount(Pointer pBackup) {
    return library.sqlite3_backup_pagecount(pBackup);
  }
  static int sqlite3_backup_finish(Pointer pBackup) {
    return library.sqlite3_backup_finish(pBackup);
  }

  // As there is only one ProgressCallback by connection, and it is used to implement query timeout,
  // the method visibility is restricted.
  static void sqlite3_progress_handler(Pointer pDb, int nOps, ProgressCallback xProgress, Pointer pArg) {
    library.sqlite3_progress_handler(pDb, nOps, xProgress, pArg);
  }
  public static void sqlite3_trace(Pointer pDb, TraceCallback xTrace, Pointer pArg) {
    library.sqlite3_trace(pDb, xTrace, pArg);
  }
  /*
  void (*)(sqlite3_context*,int,sqlite3_value**),
  void (*)(sqlite3_context*,int,sqlite3_value**),
  void (*)(sqlite3_context*),
  void(*)(void*)
  */
  // eTextRep: SQLITE_UTF8 => 1, ...
  public static int sqlite3_create_function_v2(Pointer pDb, String functionName, int nArg, int eTextRep,
      Pointer pApp, ScalarCallback xFunc, Pointer xStep, Pointer xFinal, Pointer xDestroy) {
    return library.sqlite3_create_function_v2(pDb, functionName, nArg, eTextRep, pApp, xFunc, xStep, xFinal, xDestroy);
  }
  public static void sqlite3_result_null(Pointer pCtx) {
    library.sqlite3_result_null(pCtx);
  }
  public static void sqlite3_result_int(Pointer pCtx, int i) {
    library.sqlite3_result_int(pCtx, i);
  }

  static Pointer nativeString(String sql) { // TODO Check encoding?
    final byte[] data = sql.getBytes();
    jnr.ffi.Runtime runtime = jnr.ffi.Runtime.getRuntime(library);
    final Pointer pointer = Memory.allocateDirect(runtime, data.length + 1);
    pointer.put(0L, data, 0, data.length);
    pointer.putByte(data.length, (byte) 0);
    return pointer;
  }

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
    @Delegate
    void invoke(Pointer udp, int err, String msg);
  }
  private static final LogCallback LOG_CALLBACK = new LogCallback() {
    @Override
    public void invoke(Pointer udp, int err, String msg) {
      System.out.printf("%d: %s%n", err, msg);
    }
  };
  static {
    if (!System.getProperty("sqlite.config.log", "").isEmpty()) {
      // DriverManager.getLogWriter();
      sqlite3_config(SQLITE_CONFIG_LOG, LOG_CALLBACK, null);
    }
  }

  public interface ProgressCallback {
    // return true to interrupt
    @SuppressWarnings("unused")
    @Delegate
    boolean invoke(Pointer arg);
  }

  public interface LibSQLite {
    String sqlite3_libversion(); // no copy needed
    int sqlite3_libversion_number();
    boolean sqlite3_threadsafe();
    boolean sqlite3_compileoption_used(String optName);

    int sqlite3_config(int op);
    int sqlite3_config(int op, boolean onoff);
    public int sqlite3_config(int op, SQLite.LogCallback xLog, Pointer udp);
    public void sqlite3_log(int iErrCode, String msg);

    String sqlite3_errmsg(Pointer pDb); // copy needed: the error string might be overwritten or deallocated by subsequent calls to other SQLite interface functions.
    int sqlite3_errcode(Pointer pDb);

    int sqlite3_extended_result_codes(Pointer pDb, boolean onoff);
    int sqlite3_extended_errcode(Pointer pDb);

    int sqlite3_initialize();
    int sqlite3_shutdown();

    int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs); // no copy needed
    int sqlite3_close(Pointer pDb);
    int sqlite3_close_v2(Pointer pDb);
    void sqlite3_interrupt(Pointer pDb);

    int sqlite3_busy_timeout(Pointer pDb, int ms);
    int sqlite3_db_config(Pointer pDb, int op, int v, IntByReference pOk);
    int sqlite3_enable_load_extension(Pointer pDb, boolean onoff);
    int sqlite3_load_extension(Pointer pDb, String file, String proc, PointerByReference errMsg);
    int sqlite3_limit(Pointer pDb, int id, int newVal);
    boolean sqlite3_get_autocommit(Pointer pDb);

    int sqlite3_changes(Pointer pDb);
    int sqlite3_total_changes(Pointer pDb);
    long sqlite3_last_insert_rowid(Pointer pDb);

    String sqlite3_db_filename(Pointer pDb, String dbName); // no copy needed
    int sqlite3_db_readonly(Pointer pDb, String dbName); // no copy needed

    Pointer sqlite3_next_stmt(Pointer pDb, Pointer pStmt);

    int sqlite3_table_column_metadata(Pointer pDb, String dbName, String tableName, String columnName,
                                                    PointerByReference pzDataType, PointerByReference pzCollSeq,
                                                    IntByReference pNotNull, IntByReference pPrimaryKey, IntByReference pAutoinc); // no copy needed
    int sqlite3_exec(Pointer pDb, String cmd, Pointer c, Pointer udp, PointerByReference errMsg);

    int sqlite3_prepare_v2(Pointer pDb, Pointer sql, int nByte, PointerByReference ppStmt,
                                         PointerByReference pTail);
    String sqlite3_sql(Pointer pStmt); // no copy needed
    int sqlite3_finalize(Pointer pStmt);
    int sqlite3_step(Pointer pStmt);
    int sqlite3_reset(Pointer pStmt);
    int sqlite3_clear_bindings(Pointer pStmt);
    boolean sqlite3_stmt_busy(Pointer pStmt);
    boolean sqlite3_stmt_readonly(Pointer pStmt);

    int sqlite3_column_count(Pointer pStmt);
    int sqlite3_data_count(Pointer pStmt);
    int sqlite3_column_type(Pointer pStmt, int iCol);
    String sqlite3_column_name(Pointer pStmt, int iCol); // copy needed: The returned string pointer is valid until either the prepared statement is destroyed by sqlite3_finalize() or until the statement is automatically reprepared by the first call to sqlite3_step() for a particular run or until the next call to sqlite3_column_name() or sqlite3_column_name16() on the same column.
    String sqlite3_column_origin_name(Pointer pStmt, int iCol); // copy needed
    String sqlite3_column_table_name(Pointer pStmt, int iCol); // copy needed
    String sqlite3_column_database_name(Pointer pStmt, int iCol); // copy needed
    String sqlite3_column_decltype(Pointer pStmt, int iCol); // copy needed

    Pointer sqlite3_column_blob(Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
    int sqlite3_column_bytes(Pointer pStmt, int iCol);
    double sqlite3_column_double(Pointer pStmt, int iCol);
    int sqlite3_column_int(Pointer pStmt, int iCol);
    long sqlite3_column_int64(Pointer pStmt, int iCol);
    String sqlite3_column_text(Pointer pStmt, int iCol); // copy needed: The pointers returned are valid until a type conversion occurs as described above, or until sqlite3_step() or sqlite3_reset() or sqlite3_finalize() is called.
    //const void *sqlite3_column_text16(Pointer pStmt, int iCol);
    //sqlite3_value *sqlite3_column_value(Pointer pStmt, int iCol);

    int sqlite3_bind_parameter_count(Pointer pStmt);
    int sqlite3_bind_parameter_index(Pointer pStmt, String name); // no copy needed
    String sqlite3_bind_parameter_name(Pointer pStmt, int i); // copy needed

    int sqlite3_bind_blob(Pointer pStmt, int i, byte[] value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
    int sqlite3_bind_double(Pointer pStmt, int i, double value);
    int sqlite3_bind_int(Pointer pStmt, int i, int value);
    int sqlite3_bind_int64(Pointer pStmt, int i, long value);
    int sqlite3_bind_null(Pointer pStmt, int i);
    int sqlite3_bind_text(Pointer pStmt, int i, String value, int n, long xDel); // no copy needed when xDel == SQLITE_TRANSIENT == -1
    //int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
    //int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
    int sqlite3_bind_zeroblob(Pointer pStmt, int i, int n);

    void sqlite3_free(Pointer p);

    int sqlite3_blob_open(Pointer pDb, String dbName, String tableName, String columnName,
                                        long iRow, boolean flags, PointerByReference ppBlob); // no copy needed
    int sqlite3_blob_reopen(Pointer pBlob, long iRow);
    int sqlite3_blob_bytes(Pointer pBlob);
    int sqlite3_blob_read(Pointer pBlob, ByteBuffer z, int n, int iOffset);
    int sqlite3_blob_write(Pointer pBlob, ByteBuffer z, int n, int iOffset);
    int sqlite3_blob_close(Pointer pBlob);

    Pointer sqlite3_backup_init(Pointer pDst, String dstName, Pointer pSrc, String srcName);
    int sqlite3_backup_step(Pointer pBackup, int nPage);
    int sqlite3_backup_remaining(Pointer pBackup);
    int sqlite3_backup_pagecount(Pointer pBackup);
    int sqlite3_backup_finish(Pointer pBackup);

    void sqlite3_progress_handler(Pointer pDb, int nOps, SQLite.ProgressCallback xProgress, Pointer pArg);
    public void sqlite3_trace(Pointer pDb, TraceCallback xTrace, Pointer pArg);
    public int sqlite3_create_function_v2(Pointer pDb, String functionName, int nArg, int eTextRep,
                                                        Pointer pApp, ScalarCallback xFunc, Pointer xStep, Pointer xFinal, Pointer xDestroy);
    public void sqlite3_result_null(Pointer pCtx);
    public void sqlite3_result_int(Pointer pCtx, int i);
  }
}
