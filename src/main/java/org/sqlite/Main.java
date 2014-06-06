package org.sqlite;

import org.bridj.Pointer;

import java.sql.SQLException;

import static org.sqlite.SQLite.*;

public class Main {

  public static void main(String[] args) throws SQLException {
    final LogCallback<Void> logCallback = new LogCallback<Void>() {
      @Override
      public void apply(Pointer<Void> udp, int err, Pointer<Byte> msg) {
        System.err.printf("%d: %s\n", err, msg.getString(Pointer.StringType.C, UTF8));
      }
    };
    sqlite3_config(SQLITE_CONFIG_LOG, Pointer.pointerTo(logCallback), null);

    sqlite3_log(0, "Test");
    System.out.println("sqlite3_threadsafe = " + sqlite3_threadsafe());

    System.out.println("sqlite3_libversion = " + getCString(sqlite3_libversion()));

    final Pointer<Byte> optName = pointerToString("SQLITE_OMIT_LOAD_EXTENSION");
    System.out.println("sqlite3_compileoption_used(SQLITE_OMIT_LOAD_EXTENSION) = " + sqlite3_compileoption_used(optName));
    optName.release();

    System.out.println("sqlite3_config(SQLITE_CONFIG_SERIALIZED) = " + sqlite3_config(SQLITE_CONFIG_SERIALIZED));
    System.out.println("sqlite3_config(SQLITE_CONFIG_URI) = " + sqlite3_config(SQLITE_CONFIG_URI, false));

    int res = 0;
    final Conn db = Conn.open(":memory:", OpenFlags.SQLITE_OPEN_READWRITE, null);

    System.out.println("sqlite3_db_filename = " + db.getFilename());
    System.out.println("sqlite3_db_readonly = " + db.isReadOnly(null));
    System.out.println("sqlite3_get_autocommit = " + db.getAutoCommit());

    String sql = "DROP TABLE IF EXISTS test; " +
        "CREATE TABLE test (id INTEGER PRIMARY KEY AUTOINCREMENT," +
        " d REAL, i INTEGER, s TEXT); -- bim";
    while (sql.length() > 0) {
      Stmt stmt = db.prepare(sql, false);
      sql = stmt.getTail();
      if (stmt.isDumb()) {
        continue;
      }

      System.out.println("sqlite3_sql = " + stmt.getSql());
      System.out.println("sqlite3_stmt_readonly = " + stmt.isReadOnly());

      stmt.step(0);

      System.out.println("sqlite3_stmt_busy = " + stmt.isBusy());
      stmt.clearBindings();
      stmt.reset();

      res = stmt.close();
      System.out.println("sqlite3_finalize = " + res);
    }

    sql = "INSERT INTO test (d, i, s) VALUES (?, ?, ?)";
    Stmt stmt = db.prepare(sql, true);
    System.out.println("sqlite3_bind_parameter_count = " + stmt.getBindParameterCount());

    stmt.step(0);
    System.out.println("sqlite3_total_changes = " + db.getTotalChanges());
    System.out.println("sqlite3_changes = " + db.getChanges());
    System.out.println("sqlite3_last_insert_rowid = " + db.getLastInsertRowid());

    for (int i = 0; i < 5; i++) {
      stmt.bindDouble(1, i * Math.PI);
      stmt.bindInt(2, i);
      stmt.bindText(3, "h" + i);
      stmt.step(0);
      System.out.println("sqlite3_total_changes = " + db.getTotalChanges());
      System.out.println("sqlite3_changes = " + db.getChanges());
      System.out.println("sqlite3_last_insert_rowid = " + db.getLastInsertRowid());
    }

    res = stmt.close();
    System.out.println("sqlite3_finalize = " + res);

    sql = "SELECT * FROM test";
    stmt = db.prepare(sql, true);
    while (stmt.step(0)) {
      int nCol = stmt.getColumnCount();
      for (int i = 0; i < nCol; i++) {
        System.out.println("sqlite3_column_name("+i+") = " + stmt.getColumnName(i));
        final int columnType = stmt.getColumnType(i);
        System.out.println("sqlite3_column_type("+i+") = " + columnType);
        if (columnType != ColTypes.SQLITE_NULL) {
          System.out.println("sqlite3_column_bytes("+i+") = " + stmt.getColumnBytes(i));
          System.out.println("sqlite3_column_text("+i+") = " + stmt.getColumnText(i));
        }
      }
    }
    res = stmt.close();
    System.out.println("sqlite3_finalize = " + res);

    db.setExtendedResultCodes(true);
    try {
      db.fastExec("BLAM");
    } catch (ConnException e) {
      System.out.println("sqlite3_exec = " + e.getErrorCode());
      System.out.println("sqlite3_errcode = " + db.getErrCode());
      System.out.println("sqlite3_extended_errcode = " + db.getExtendedErrcode());
      System.out.println("sqlite3_errmsg = " + db.getErrMsg());
    }

    //sqlite3_interrupt(db);

    Conn dst = Conn.open(":memory:", OpenFlags.SQLITE_OPEN_READWRITE, null);
    final Backup backup = Conn.open(dst, "main", db, "main");
    System.out.println("sqlite3_backup_pagecount = " + backup.pageCount());
    boolean ok = true;
    while (ok) {
      ok = backup.step(1);
      System.out.println("sqlite3_backup_remaining = " + backup.remaining());
    }
    backup.finish();
    dst.closeAndCheck();

    res = db.close();
    System.out.println("sqlite3_close = " + res);
  }
}
