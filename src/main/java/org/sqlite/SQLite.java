package org.sqlite;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

public class SQLite implements Library {
  public static final String JNA_LIBRARY_NAME = "sqlite3";
  // public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance(SQLite.JNA_LIBRARY_NAME);
  static {
    Native.register(JNA_LIBRARY_NAME);
  }

  public static final int SQLITE_OK = 0;

  public static final int SQLITE_ROW = 100;
  public static final int SQLITE_DONE = 101;

  static native String sqlite3_libversion();
  static native boolean sqlite3_threadsafe();

  static native String sqlite3_errmsg(Pointer pDb);
  static native int sqlite3_errcode(Pointer pDb);

  static native int sqlite3_open_v2(String filename, PointerByReference ppDb, int flags, String vfs);
  static native int sqlite3_close(Pointer pDb);

  static native int sqlite3_prepare_v2(Pointer pDb, String sql, int nByte, PointerByReference ppStmt,
                                       PointerByReference pTail);
  static native int sqlite3_finalize(Pointer pStmt);
  static native int sqlite3_step(Pointer pStmt);

  static native int sqlite3_column_count(Pointer pStmt);
  static native int sqlite3_column_type(Pointer pStmt, int iCol);
}