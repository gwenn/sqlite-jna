package org.sqlite;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;

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

  static native String sqlite3_libversion();
  static native boolean sqlite3_threadsafe();

  static native String sqlite3_errmsg(Pointer pDb);
  static native int sqlite3_errcode(Pointer pDb);

  static native int sqlite3_extended_result_codes(Pointer pDb, boolean onoff);
  static native int sqlite3_extended_errcode(Pointer pDb);

  static native int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs);
  static native int sqlite3_close(Pointer pDb);
  static native void sqlite3_interrupt(Pointer pDb);
  static native int sqlite3_busy_timeout(Pointer pDb, int ms);

  // int sqlite3_limit(sqlite3*, int id, int newVal);

  static native int sqlite3_changes(Pointer pDb);
  static native int sqlite3_total_changes(Pointer pDb);
  static native long sqlite3_last_insert_rowid(Pointer pDb);

  static native Pointer sqlite3_next_stmt(Pointer pDb, Pointer pStmt);

  static native int sqlite3_table_column_metadata(Pointer pDb, String dbName, String tableName, String columnName,
      PointerByReference pzDataType, PointerByReference pzCollSeq,
      PointerByReference pNotNull, PointerByReference pPrimaryKey, PointerByReference pAutoinc);

  static native int sqlite3_prepare_v2(Pointer pDb, String sql, int nByte, PointerByReference ppStmt,
                                       PointerByReference pTail);
  static native String sqlite3_sql(Pointer pStmt);
  static native int sqlite3_finalize(Pointer pStmt);
  static native int sqlite3_step(Pointer pStmt);
  static native int sqlite3_reset(Pointer pStmt);
  static native int sqlite3_clear_bindings(Pointer pStmt);
  static native boolean sqlite3_stmt_busy(Pointer pStmt);
  
  static native int sqlite3_column_count(Pointer pStmt);
  static native int sqlite3_data_count(Pointer pStmt);
  static native int sqlite3_column_type(Pointer pStmt, int iCol);
  static native String sqlite3_column_name(Pointer pStmt, int iCol);
  static native String sqlite3_column_table_name(Pointer pStmt, int iCol);

  /*
  const char *sqlite3_column_decltype(sqlite3_stmt*,int);
  const char *sqlite3_column_database_name(sqlite3_stmt*,int);
  const char *sqlite3_column_origin_name(sqlite3_stmt*,int);
  */

  static native Pointer sqlite3_column_blob(Pointer pStmt, int iCol);
  static native int sqlite3_column_bytes(Pointer pStmt, int iCol);
  static native double sqlite3_column_double(Pointer pStmt, int iCol);
  static native int sqlite3_column_int(Pointer pStmt, int iCol);
  static native long sqlite3_column_int64(Pointer pStmt, int iCol);
  static native String sqlite3_column_text(Pointer pStmt, int iCol);
  //const void *sqlite3_column_text16(Pointer pStmt, int iCol);
  //sqlite3_value *sqlite3_column_value(Pointer pStmt, int iCol);
  
  static native int sqlite3_bind_parameter_count(Pointer pStmt);
  static native int sqlite3_bind_parameter_index(Pointer pStmt, String name);
  static native String sqlite3_bind_parameter_name(Pointer pStmt, int i);

  static native int sqlite3_bind_blob(Pointer pStmt, int i, byte[] value, int n, long xDel);
  static native int sqlite3_bind_double(Pointer pStmt, int i, double value);
  static native int sqlite3_bind_int(Pointer pStmt, int i, int value);
  static native int sqlite3_bind_int64(Pointer pStmt, int i, long value);
  static native int sqlite3_bind_null(Pointer pStmt, int i);
  static native int sqlite3_bind_text(Pointer pStmt, int i, String value, int n, long xDel);
  //static native int sqlite3_bind_text16(Pointer pStmt, int i, const void*, int, void(*)(void*));
  //static native int sqlite3_bind_value(Pointer pStmt, int i, const sqlite3_value*);
  static native int sqlite3_bind_zeroblob(Pointer pStmt, int i, int n);
}
