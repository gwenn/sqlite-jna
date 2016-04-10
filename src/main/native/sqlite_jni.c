//#include <stdio.h> //printf
#include "sqlite_jni.h"
#include "sqlite3.h"
#include <string.h> // memcpy

#define PTR_TO_JLONG(ptr) ((jlong)(size_t)(ptr))
#define JLONG_TO_PTR(jl) ((void *)(size_t)(jl))

//#define GLOBAL_REF(v) (*env)->NewGlobalRef(env, v)
//#define DEL_GLOBAL_REF(v) (*env)->DeleteGlobalRef(env, v)

#define WEAK_GLOBAL_REF(v) (*env)->NewWeakGlobalRef(env, v)
#define DEL_WEAK_GLOBAL_REF(v) (*env)->DeleteWeakGlobalRef(env, v)

static jclass runtime_exception = 0;
static void throwException(JNIEnv *env, const char *message) {
  if ((*env)->ExceptionCheck(env))
    return; // there is already a pending exception
  (*env)->ExceptionClear(env);
  (*env)->ThrowNew(env, runtime_exception,
                   message ? message : "No message (TODO)");
}

typedef struct {
  JavaVM *vm;
  jmethodID mid;
  jobject obj;
} callback_context;

static callback_context *create_callback_context(JNIEnv *env, jmethodID mid,
                                                 jobject obj) {
  callback_context *cc = sqlite3_malloc(sizeof(callback_context));
  if (!cc) {
    throwException(env, "OOM");
    return cc;
  }
  (*env)->GetJavaVM(env, &cc->vm);
  cc->mid = mid;
  cc->obj = WEAK_GLOBAL_REF(obj);
  return cc;
}

/*
FATAL ERROR in native method: Using JNIEnv in the wrong thread
http://stackoverflow.com/questions/23962972/fatal-error-in-native-method-using-jnienv-in-non-java-thread
Do not save instances of JNIEnv* unless you are sure they will be referenced in
the same thread.
 */
static void free_callback_context(JNIEnv *env, void *p) {
  if (p) {
    callback_context *cc = (callback_context *)p;
    DEL_WEAK_GLOBAL_REF(cc->obj);
  }
  sqlite3_free(p);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv *env;
  if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2)) {
    return JNI_ERR;
  }

  jclass cls = (*env)->FindClass(env, "java/lang/RuntimeException");
  if (!cls) {
    return JNI_ERR;
  }
  runtime_exception = (*env)->NewGlobalRef(env, cls);
  return JNI_VERSION_1_2;
}
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
  JNIEnv *env;

  if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_2)) {
    return;
  }
  if (runtime_exception) {
    (*env)->DeleteGlobalRef(env, runtime_exception);
    runtime_exception = 0;
  }
}

JNIEXPORT jstring JNICALL
Java_org_sqlite_SQLite_sqlite3_1libversion(JNIEnv *env, jclass cls) {
  return (*env)->NewStringUTF(env, sqlite3_libversion());
}
JNIEXPORT jint JNICALL
Java_org_sqlite_SQLite_sqlite3_1libversion_1number(JNIEnv *env, jclass cls) {
  return sqlite3_libversion_number();
}
JNIEXPORT jboolean JNICALL
Java_org_sqlite_SQLite_sqlite3_1threadsafe(JNIEnv *env, jclass cls) {
  return sqlite3_threadsafe() == 0 ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_org_sqlite_SQLite_sqlite3_1compileoption_1used(
    JNIEnv *env, jclass cls, jstring optName) {
  const char *zOptName = (*env)->GetStringUTFChars(env, optName, 0);
  if (!zOptName) {
    return JNI_FALSE; /* OutOfMemoryError already thrown */
  }
  int rc = sqlite3_compileoption_used(zOptName);
  (*env)->ReleaseStringUTFChars(env, optName, zOptName);
  return rc == 0 ? JNI_FALSE : JNI_TRUE;
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1compileoption_1get(
    JNIEnv *env, jclass cls, jint n) {
  return (*env)->NewStringUTF(env, sqlite3_compileoption_get(n));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1config__I(JNIEnv *env,
                                                                 jclass cls,
                                                                 jint op) {
  return sqlite3_config(op);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1config__IZ(
    JNIEnv *env, jclass cls, jint op, jboolean onoff) {
  return sqlite3_config(op, onoff);
}

static callback_context *logger_cc = 0;
static void my_log(void *udp, int err, const char *zMsg) {
  callback_context *cc = (callback_context *)udp;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  jstring msg = (*env)->NewStringUTF(env, zMsg);
  (*env)->CallVoidMethod(env, cc->obj, cc->mid, err, msg);
  (*env)->DeleteLocalRef(env, msg);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
}

JNIEXPORT jint JNICALL
Java_org_sqlite_SQLite_sqlite3_1config__ILorg_sqlite_SQLite_LogCallback_2(
    JNIEnv *env, jclass cls, jint op, jobject xLog) {
  if (!xLog) {
    int rc = sqlite3_config(op, 0, 0);
    if (rc == SQLITE_OK) {
      free_callback_context(env, logger_cc);
    }
    return rc;
  }
  jclass clz = (*env)->GetObjectClass(env, xLog);
  jmethodID mid =
      (*env)->GetMethodID(env, clz, "log", "(ILjava/lang/String;)V");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(env, "expected 'void log(int, String)' method");
    return -1;
  }
  callback_context *cc = create_callback_context(env, mid, xLog);
  if (!cc) {
    return SQLITE_NOMEM;
  }
  int rc = sqlite3_config(op, my_log, cc);
  if (rc == SQLITE_OK) {
    free_callback_context(env, logger_cc);
    logger_cc = cc;
  } // TODO else free_callback_context(cc) ?
  return rc;
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1log(JNIEnv *env,
                                                           jclass cls,
                                                           jint iErrCode,
                                                           jstring msg) {
  const char *zMsg = (*env)->GetStringUTFChars(env, msg, 0);
  if (!zMsg) {
    return; /* OutOfMemoryError already thrown */
  }
  sqlite3_log(iErrCode, zMsg);
  (*env)->ReleaseStringUTFChars(env, msg, zMsg);
}

#define JLONG_TO_SQLITE3_PTR(jl) ((sqlite3 *)(size_t)(jl))

JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1errmsg(JNIEnv *env,
                                                                 jclass cls,
                                                                 jlong pDb) {
  return (*env)->NewStringUTF(env, sqlite3_errmsg(JLONG_TO_SQLITE3_PTR(pDb)));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1errcode(JNIEnv *env,
                                                               jclass cls,
                                                               jlong pDb) {
  return sqlite3_errcode(JLONG_TO_SQLITE3_PTR(pDb));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1extended_1result_1codes(
    JNIEnv *env, jclass cls, jlong pDb, jboolean onoff) {
  return sqlite3_extended_result_codes(JLONG_TO_SQLITE3_PTR(pDb), onoff);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1extended_1errcode(
    JNIEnv *env, jclass cls, jlong pDb) {
  return sqlite3_extended_errcode(JLONG_TO_SQLITE3_PTR(pDb));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1initialize(JNIEnv *env,
                                                                  jclass cls) {
  return sqlite3_initialize();
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1shutdown(JNIEnv *env,
                                                                jclass cls) {
  return sqlite3_shutdown();
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1open_1v2(
    JNIEnv *env, jclass cls, jstring filename, jlongArray ppDb, jint flags,
    jstring vfs) {
  const char *zFilename = (*env)->GetStringUTFChars(env, filename, 0);
  if (!zFilename) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  const char *zVfs = 0;
  if (vfs) {
    zVfs = (*env)->GetStringUTFChars(env, vfs, 0);
    if (!zVfs) {
      (*env)->ReleaseStringUTFChars(env, filename, zFilename);
      return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
    }
  }
  sqlite3 *db = 0;
  int rc = sqlite3_open_v2(zFilename, &db, flags, zVfs);
  if (vfs) {
    (*env)->ReleaseStringUTFChars(env, vfs, zVfs);
  }
  (*env)->ReleaseStringUTFChars(env, filename, zFilename);
  jlong p = PTR_TO_JLONG(db);
  (*env)->SetLongArrayRegion(env, ppDb, 0, 1, &p);
  return rc;
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1close(JNIEnv *env,
                                                             jclass cls,
                                                             jlong pDb) {
  return sqlite3_close(JLONG_TO_SQLITE3_PTR(pDb));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1close_1v2(JNIEnv *env,
                                                                 jclass cls,
                                                                 jlong pDb) {
  return sqlite3_close_v2(JLONG_TO_SQLITE3_PTR(pDb));
}
JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1interrupt(JNIEnv *env,
                                                                 jclass cls,
                                                                 jlong pDb) {
  sqlite3_interrupt(JLONG_TO_SQLITE3_PTR(pDb));
}

static int busy(void *udp, int count) {
  callback_context *cc = (callback_context *)udp;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  int busy = (*env)->CallBooleanMethod(env, cc->obj, cc->mid, count);
  if ((*env)->ExceptionCheck(env)) {
    return busy; // FIXME
  }
  return busy;
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1busy_1handler(
    JNIEnv *env, jclass cls, jlong pDb, jobject xBusy, jlongArray pCc) {
  if (!xBusy) {
    return sqlite3_busy_handler(JLONG_TO_SQLITE3_PTR(pDb), 0, 0);
  }
  jclass clz = (*env)->GetObjectClass(env, xBusy);
  jmethodID mid = (*env)->GetMethodID(env, clz, "busy", "(I)Z");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(env, "expected 'boolean busy(int)' method");
    return 0;
  }
  callback_context *cc = create_callback_context(env, mid, xBusy);
  if (!cc) {
    return SQLITE_NOMEM;
  }
  jlong p = PTR_TO_JLONG(cc);
  (*env)->SetLongArrayRegion(env, pCc, 0, 1, &p);
  return sqlite3_busy_handler(JLONG_TO_SQLITE3_PTR(pDb), busy, cc);
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1busy_1timeout(
    JNIEnv *env, jclass cls, jlong pDb, jint ms) {
  return sqlite3_busy_timeout(JLONG_TO_SQLITE3_PTR(pDb), ms);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1db_1status(
    JNIEnv *env, jclass cls, jlong pDb, jint op, jintArray pCur,
    jintArray pHiwtr, jboolean resetFlg) {
  jint cur = 0;
  jint hiwtr = 0;
  int rc =
      sqlite3_db_status(JLONG_TO_SQLITE3_PTR(pDb), op, &cur, &hiwtr, resetFlg);
  (*env)->SetIntArrayRegion(env, pCur, 0, 1, &cur);
  (*env)->SetIntArrayRegion(env, pHiwtr, 0, 1, &hiwtr);
  return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1db_1config(
    JNIEnv *env, jclass cls, jlong pDb, jint op, jint v, jintArray pOk) {
  jint ok = 0;
  int rc = sqlite3_db_config(JLONG_TO_SQLITE3_PTR(pDb), op, v, &ok);
  (*env)->SetIntArrayRegion(env, pOk, 0, 1, &ok);
  return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1enable_1load_1extension(
    JNIEnv *env, jclass cls, jlong pDb, jboolean onoff) {
  return sqlite3_enable_load_extension(JLONG_TO_SQLITE3_PTR(pDb), onoff);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1load_1extension(
    JNIEnv *env, jclass cls, jlong pDb, jstring file, jstring proc,
    jobjectArray ppErrMsg) {
  const char *zFile = (*env)->GetStringUTFChars(env, file, 0);
  if (zFile) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  const char *zProc = 0;
  if (proc) {
    zProc = (*env)->GetStringUTFChars(env, proc, 0);
    if (zProc) {
      (*env)->ReleaseStringUTFChars(env, file, zFile);
      return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
    }
  }
  char *zErrMsg = 0;
  int rc =
      sqlite3_load_extension(JLONG_TO_SQLITE3_PTR(pDb), zFile, zProc, &zErrMsg);
  if (proc) {
    (*env)->ReleaseStringUTFChars(env, proc, zProc);
  }
  (*env)->ReleaseStringUTFChars(env, file, zFile);
  if (zErrMsg) {
    jstring errMsg = (*env)->NewStringUTF(env, zErrMsg);
    (*env)->SetObjectArrayElement(env, ppErrMsg, 0, errMsg);
    (*env)->DeleteLocalRef(env, errMsg);
    sqlite3_free(zErrMsg);
  } else {
    (*env)->SetObjectArrayElement(env, ppErrMsg, 0, 0);
  }
  return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1limit(JNIEnv *env,
                                                             jclass cls,
                                                             jlong pDb, jint id,
                                                             jint newVal) {
  return sqlite3_limit(JLONG_TO_SQLITE3_PTR(pDb), id, newVal);
}
JNIEXPORT jboolean JNICALL Java_org_sqlite_SQLite_sqlite3_1get_1autocommit(
    JNIEnv *env, jclass cls, jlong pDb) {
  return sqlite3_get_autocommit(JLONG_TO_SQLITE3_PTR(pDb));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1changes(JNIEnv *env,
                                                               jclass cls,
                                                               jlong pDb) {
  return sqlite3_changes(JLONG_TO_SQLITE3_PTR(pDb));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1total_1changes(
    JNIEnv *env, jclass cls, jlong pDb) {
  return sqlite3_total_changes(JLONG_TO_SQLITE3_PTR(pDb));
}
JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1last_1insert_1rowid(
    JNIEnv *env, jclass cls, jlong pDb) {
  return sqlite3_last_insert_rowid(JLONG_TO_SQLITE3_PTR(pDb));
}

JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1db_1filename(
    JNIEnv *env, jclass cls, jlong pDb, jstring dbName) {
  const char *zDbName = (*env)->GetStringUTFChars(env, dbName, 0);
  if (!zDbName) {
    return 0; /* OutOfMemoryError already thrown */
  }
  const char *zFileName =
      sqlite3_db_filename(JLONG_TO_SQLITE3_PTR(pDb), zDbName);
  (*env)->ReleaseStringUTFChars(env, dbName, zDbName);
  if (zFileName) {
    return (*env)->NewStringUTF(env, zFileName);
  }
  return 0;
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1db_1readonly(
    JNIEnv *env, jclass cls, jlong pDb, jstring dbName) {
  const char *zDbName = 0;
  if (dbName) {
    zDbName = (*env)->GetStringUTFChars(env, dbName, 0);
    if (!zDbName) {
      return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
    }
  }
  int rc = sqlite3_db_readonly(JLONG_TO_SQLITE3_PTR(pDb), zDbName);
  if (dbName) {
    (*env)->ReleaseStringUTFChars(env, dbName, zDbName);
  }
  return rc;
}

#define JLONG_TO_SQLITE3_STMT_PTR(jl) ((sqlite3_stmt *)(size_t)(jl))

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1next_1stmt(
    JNIEnv *env, jclass cls, jlong pDb, jlong pStmt) {
  return PTR_TO_JLONG(sqlite3_next_stmt(JLONG_TO_SQLITE3_PTR(pDb),
                                        JLONG_TO_SQLITE3_STMT_PTR(pStmt)));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1table_1column_1metadata(
    JNIEnv *env, jclass cls, jlong pDb, jstring dbName, jstring tableName,
    jstring columnName, jobjectArray pDataType, jobjectArray pCollSeq,
    jintArray pFlags) {
  const char *zDbName = 0;
  if (dbName) {
    zDbName = (*env)->GetStringUTFChars(env, dbName, 0);
    if (!zDbName) {
      return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
    }
  }
  const char *zTableName = (*env)->GetStringUTFChars(env, tableName, 0);
  if (!zTableName) {
    if (dbName) {
      (*env)->ReleaseStringUTFChars(env, dbName, zDbName);
    }
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  const char *zColumnName = (*env)->GetStringUTFChars(env, columnName, 0);
  if (!zColumnName) {
    (*env)->ReleaseStringUTFChars(env, tableName, zTableName);
    if (dbName) {
      (*env)->ReleaseStringUTFChars(env, dbName, zDbName);
    }
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }

  char const *zDataType = 0;
  char const *zCollSeq = 0;
  int flags[3] = {0, 0, 0};
  int rc = sqlite3_table_column_metadata(
      JLONG_TO_SQLITE3_PTR(pDb), zDbName, zTableName, zColumnName,
      pDataType ? &zDataType : 0, pCollSeq ? &zCollSeq : 0, &flags[0],
      &flags[1], &flags[2]);

  (*env)->ReleaseStringUTFChars(env, columnName, zColumnName);
  (*env)->ReleaseStringUTFChars(env, tableName, zTableName);
  if (dbName) {
    (*env)->ReleaseStringUTFChars(env, dbName, zDbName);
  }
  if (pDataType) {
    jstring dataType = (*env)->NewStringUTF(env, zDataType);
    if (!dataType) {
      return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
    }
    (*env)->SetObjectArrayElement(env, pDataType, 0, dataType);
    (*env)->DeleteLocalRef(env, dataType);
  }
  if (pCollSeq) {
    jstring collSeq = (*env)->NewStringUTF(env, zCollSeq);
    if (!collSeq) {
      return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
    }
    (*env)->SetObjectArrayElement(env, pCollSeq, 0, collSeq);
    (*env)->DeleteLocalRef(env, collSeq);
  }
  (*env)->SetIntArrayRegion(env, pFlags, 0, 3, flags); // FIXME expected 'const
                                                       // jint * {aka const long
                                                       // int *}' but argument
                                                       // is of type 'int *'
  return rc;
}

// static int callback(void *udata, int ncol, char **data, char **cols)
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1exec(
    JNIEnv *env, jclass cls, jlong pDb, jstring sql, jobject c, jobject udp,
    jobjectArray ppErrMsg) {
  const char *zSql = (*env)->GetStringUTFChars(env, sql, 0);
  if (!zSql) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  int rc = sqlite3_exec(JLONG_TO_SQLITE3_PTR(pDb), zSql, 0, 0, 0); // TODO
  (*env)->ReleaseStringUTFChars(env, sql, zSql);
  return rc;
}

// TODO http://www.club.cc.cmu.edu/~cmccabe/blog_jni_flaws.html
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1prepare_1v2(
    JNIEnv *env, jclass cls, jlong pDb, jstring sql, jint nByte,
    jlongArray ppStmt, jobjectArray pTail) {
  jsize len = (*env)->GetStringLength(env, sql) * sizeof(jchar);
  const void *zSql = (*env)->GetStringCritical(env, sql, 0);
  if (!zSql) {
    return SQLITE_NOMEM; // OutOfMemoryError already thrown
  }

  sqlite3_stmt *pStmt = 0;
  const void *zTail = 0;
  int rc = sqlite3_prepare16_v2(JLONG_TO_SQLITE3_PTR(pDb), zSql, len, &pStmt,
                                &zTail);

  jlong p = PTR_TO_JLONG(pStmt);
  (*env)->SetLongArrayRegion(env, ppStmt, 0, 1, &p);
  if (pTail) {
    if (zTail) {
      jstring tail =
          (*env)->NewString(env, zTail, (len - (zTail - zSql)) / sizeof(jchar));
      if (!tail) {
        (*env)->ReleaseStringCritical(env, sql, zSql);
        return SQLITE_NOMEM; // OutOfMemoryError already thrown
      }
      (*env)->SetObjectArrayElement(env, pTail, 0, tail);
      (*env)->DeleteLocalRef(env, tail);
    } else {
      (*env)->SetObjectArrayElement(env, pTail, 0, 0);
    }
  }
  (*env)->ReleaseStringCritical(env, sql, zSql);

  /*const char *zSql = (*env)->GetStringUTFChars(env, sql, 0);
  if (!zSql) {
          return SQLITE_NOMEM; // OutOfMemoryError already thrown
  }

  sqlite3_stmt *pStmt = 0;
  const char *zTail = 0;
  int rc = sqlite3_prepare_v2(JLONG_TO_SQLITE3_PTR(pDb), zSql, nByte, &pStmt,
  &zTail);

  jlong p = PTR_TO_JLONG(pStmt);
  (*env)->SetLongArrayRegion(env, ppStmt, 0, 1, &p);
  if (pTail) {
          if (zTail) {
                  jstring tail = (*env)->NewStringUTF(env, zTail);
                  if (!tail) {
                          (*env)->ReleaseStringUTFChars(env, sql, zSql);
                          return SQLITE_NOMEM; // OutOfMemoryError already
  thrown
                  }
                  (*env)->SetObjectArrayElement(env, pTail, 0, tail);
          } else {
                  (*env)->SetObjectArrayElement(env, pTail, 0, 0);
          }
  }
  (*env)->ReleaseStringUTFChars(env, sql, zSql);*/
  return rc;
}

JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1sql(JNIEnv *env,
                                                              jclass cls,
                                                              jlong pStmt) {
  return (*env)->NewStringUTF(env,
                              sqlite3_sql(JLONG_TO_SQLITE3_STMT_PTR(pStmt)));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1finalize(JNIEnv *env,
                                                                jclass cls,
                                                                jlong pStmt) {
  return sqlite3_finalize(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1step(JNIEnv *env,
                                                            jclass cls,
                                                            jlong pStmt) {
  return sqlite3_step(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1reset(JNIEnv *env,
                                                             jclass cls,
                                                             jlong pStmt) {
  return sqlite3_reset(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1clear_1bindings(
    JNIEnv *env, jclass cls, jlong pStmt) {
  return sqlite3_clear_bindings(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jboolean JNICALL Java_org_sqlite_SQLite_sqlite3_1stmt_1busy(
    JNIEnv *env, jclass cls, jlong pStmt) {
  return sqlite3_stmt_busy(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jboolean JNICALL Java_org_sqlite_SQLite_sqlite3_1stmt_1readonly(
    JNIEnv *env, jclass cls, jlong pStmt) {
  return sqlite3_stmt_readonly(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1count(
    JNIEnv *env, jclass cls, jlong pStmt) {
  return sqlite3_column_count(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1data_1count(
    JNIEnv *env, jclass cls, jlong pStmt) {
  return sqlite3_data_count(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1type(JNIEnv *env,
                                                                    jclass cls,
                                                                    jlong pStmt,
                                                                    jint iCol) {
  return sqlite3_column_type(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1name(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return (*env)->NewStringUTF(
      env, sqlite3_column_name(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol));
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1origin_1name(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return (*env)->NewStringUTF(
      env, sqlite3_column_origin_name(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol));
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1table_1name(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return (*env)->NewStringUTF(
      env, sqlite3_column_table_name(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol));
}
JNIEXPORT jstring JNICALL
Java_org_sqlite_SQLite_sqlite3_1column_1database_1name(JNIEnv *env, jclass cls,
                                                       jlong pStmt, jint iCol) {
  return (*env)->NewStringUTF(env, sqlite3_column_database_name(
                                       JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol));
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1decltype(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return (*env)->NewStringUTF(
      env, sqlite3_column_decltype(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol));
}

JNIEXPORT jbyteArray JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1blob(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  const void *blob =
      sqlite3_column_blob(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
  if (!blob) {
    return 0;
  }
  int len = sqlite3_column_bytes(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
  jbyteArray b = (*env)->NewByteArray(env, len);
  if (!b) {
    return 0; /* OutOfMemoryError already thrown */
  }
  //(*env)->SetByteArrayRegion(env, b, 0, len, blob);
  void *data = (*env)->GetPrimitiveArrayCritical(env, b, 0);
  if (!data) {
    return 0;
  }
  memcpy(data, blob, len);
  (*env)->ReleasePrimitiveArrayCritical(env, b, data, 0);
  return b;
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1bytes(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return sqlite3_column_bytes(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
}
JNIEXPORT jdouble JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1double(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return sqlite3_column_double(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1int(JNIEnv *env,
                                                                   jclass cls,
                                                                   jlong pStmt,
                                                                   jint iCol) {
  return sqlite3_column_int(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
}
JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1int64(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  return sqlite3_column_int64(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1column_1text(
    JNIEnv *env, jclass cls, jlong pStmt, jint iCol) {
  // return (*env)->NewStringUTF(env, (const
  // char*)sqlite3_column_text(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol));
  const void *text =
      sqlite3_column_text16(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
  if (!text) {
    return 0;
  }
  int len = sqlite3_column_bytes16(JLONG_TO_SQLITE3_STMT_PTR(pStmt), iCol);
  return (*env)->NewString(env, text, len / sizeof(jchar));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1parameter_1count(
    JNIEnv *env, jclass cls, jlong pStmt) {
  return sqlite3_bind_parameter_count(JLONG_TO_SQLITE3_STMT_PTR(pStmt));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1parameter_1index(
    JNIEnv *env, jclass cls, jlong pStmt, jstring name) {
  const char *zName = (*env)->GetStringUTFChars(env, name, 0);
  if (!zName) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  int idx =
      sqlite3_bind_parameter_index(JLONG_TO_SQLITE3_STMT_PTR(pStmt), zName);
  (*env)->ReleaseStringUTFChars(env, name, zName);
  return idx;
}
JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1parameter_1name(
    JNIEnv *env, jclass cls, jlong pStmt, jint i) {
  return (*env)->NewStringUTF(
      env, sqlite3_bind_parameter_name(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1blob(
    JNIEnv *env, jclass cls, jlong pStmt, jint i, jbyteArray v, jint n) {
  if (!v) {
    return sqlite3_bind_null(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i);
  }
  jsize len = (*env)->GetArrayLength(env, v);
  if (len > 0) {
    void *data = (*env)->GetPrimitiveArrayCritical(env, v, 0);
    if (!data) {
      return -1; // Wrapper specific error code
    }
    int rc = sqlite3_bind_blob(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, data, len,
                               SQLITE_TRANSIENT);
    (*env)->ReleasePrimitiveArrayCritical(env, v, data, 0);
    return rc;
  } else {
    return sqlite3_bind_zeroblob(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, 0);
  }
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1double(
    JNIEnv *env, jclass cls, jlong pStmt, jint i, jdouble v) {
  return sqlite3_bind_double(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, v);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1int(
    JNIEnv *env, jclass cls, jlong pStmt, jint i, jint v) {
  return sqlite3_bind_int(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, v);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1int64(
    JNIEnv *env, jclass cls, jlong pStmt, jint i, jlong v) {
  return sqlite3_bind_int64(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, v);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1null(JNIEnv *env,
                                                                  jclass cls,
                                                                  jlong pStmt,
                                                                  jint i) {
  return sqlite3_bind_null(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1text(
    JNIEnv *env, jclass cls, jlong pStmt, jint i, jstring v, jint n) {
  if (!v) {
    return sqlite3_bind_null(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i);
  }
  jsize len = (*env)->GetStringLength(env, v) * sizeof(jchar);
  if (len > 0) {
    const jchar *data = (*env)->GetStringCritical(env, v, 0);
    if (!data) {
      return -1; // Wrapper specific error code
    }
    int rc = sqlite3_bind_text16(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, data, len,
                                 SQLITE_TRANSIENT);
    (*env)->ReleaseStringCritical(env, v, data);
    /*const char *data = (*env)->GetStringUTFChars(env, v, 0);
    if (!data) {
            return -1; // Wrapper specific error code
    }
    int rc = sqlite3_bind_text(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, data, -1,
    SQLITE_TRANSIENT);
    (*env)->ReleaseStringUTFChars(env, v, data);*/
    return rc;
  } else {
    return sqlite3_bind_text16(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, "", 0,
                               SQLITE_STATIC);
    // return sqlite3_bind_text(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, "", 0,
    // SQLITE_STATIC);
  }
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1bind_1zeroblob(
    JNIEnv *env, jclass cls, jlong pStmt, jint i, jint n) {
  return sqlite3_bind_zeroblob(JLONG_TO_SQLITE3_STMT_PTR(pStmt), i, n);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1stmt_1status(
    JNIEnv *env, jclass cls, jlong pStmt, jint op, jboolean reset) {
  return sqlite3_stmt_status(JLONG_TO_SQLITE3_STMT_PTR(pStmt), op, reset);
}

#define JLONG_TO_SQLITE3_BLOB_PTR(jl) ((sqlite3_blob *)(size_t)(jl))

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1blob_1open(
    JNIEnv *env, jclass cls, jlong pDb, jstring db, jstring table,
    jstring column, jlong iRow, jboolean flags, jlongArray ppBlob) {
  const char *zDb = (*env)->GetStringUTFChars(env, db, 0);
  if (!zDb) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  const char *zTable = (*env)->GetStringUTFChars(env, table, 0);
  if (!zTable) {
    (*env)->ReleaseStringUTFChars(env, db, zDb);
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  const char *zColumn = (*env)->GetStringUTFChars(env, column, 0);
  if (!zColumn) {
    (*env)->ReleaseStringUTFChars(env, table, zTable);
    (*env)->ReleaseStringUTFChars(env, db, zDb);
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  sqlite3_blob *pBlob = 0;
  int rc = sqlite3_blob_open(JLONG_TO_SQLITE3_PTR(pDb), zDb, zTable, zColumn,
                             iRow, flags, &pBlob);
  (*env)->ReleaseStringUTFChars(env, column, zColumn);
  (*env)->ReleaseStringUTFChars(env, table, zTable);
  (*env)->ReleaseStringUTFChars(env, db, zDb);
  jlong p = PTR_TO_JLONG(pBlob);
  (*env)->SetLongArrayRegion(env, ppBlob, 0, 1, &p);
  return rc;
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1blob_1reopen(
    JNIEnv *env, jclass cls, jlong pBlob, jlong iRow) {
  return sqlite3_blob_reopen(JLONG_TO_SQLITE3_BLOB_PTR(pBlob), iRow);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1blob_1bytes(
    JNIEnv *env, jclass cls, jlong pBlob) {
  return sqlite3_blob_bytes(JLONG_TO_SQLITE3_BLOB_PTR(pBlob));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1blob_1read(
    JNIEnv *env, jclass cls, jlong pBlob, jbyteArray z, jint zOff, jint n,
    jint iOffset) {
  jbyte *b = (*env)->GetPrimitiveArrayCritical(env, z, 0);
  if (!b) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  int rc =
      sqlite3_blob_read(JLONG_TO_SQLITE3_BLOB_PTR(pBlob), b + zOff, n, iOffset);
  (*env)->ReleasePrimitiveArrayCritical(env, z, b, 0);
  return rc;
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1blob_1write(
    JNIEnv *env, jclass cls, jlong pBlob, jbyteArray z, jint zOff, jint n,
    jint iOffset) {
  jbyte *b = (*env)->GetPrimitiveArrayCritical(env, z, 0);
  if (!b) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  int rc = sqlite3_blob_write(JLONG_TO_SQLITE3_BLOB_PTR(pBlob), b + zOff, n,
                              iOffset);
  (*env)->ReleasePrimitiveArrayCritical(env, z, b, 0);
  return rc;
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1blob_1close(
    JNIEnv *env, jclass cls, jlong pBlob) {
  return sqlite3_blob_close(JLONG_TO_SQLITE3_BLOB_PTR(pBlob));
}

#define JLONG_TO_SQLITE3_BACKUP_PTR(jl) ((sqlite3_backup *)(size_t)(jl))

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1backup_1init(
    JNIEnv *env, jclass cls, jlong pDst, jstring dstName, jlong pSrc,
    jstring srcName) {
  const char *zDstName = (*env)->GetStringUTFChars(env, dstName, 0);
  if (!zDstName) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  const char *zSrcName = (*env)->GetStringUTFChars(env, srcName, 0);
  if (!zSrcName) {
    (*env)->ReleaseStringUTFChars(env, dstName, zDstName);
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  sqlite3_backup *pBackup =
      sqlite3_backup_init(JLONG_TO_SQLITE3_PTR(pDst), zDstName,
                          JLONG_TO_SQLITE3_PTR(pSrc), zSrcName);
  (*env)->ReleaseStringUTFChars(env, srcName, zSrcName);
  (*env)->ReleaseStringUTFChars(env, dstName, zDstName);
  return PTR_TO_JLONG(pBackup);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1backup_1step(
    JNIEnv *env, jclass cls, jlong pBackup, jint nPage) {
  return sqlite3_backup_step(JLONG_TO_SQLITE3_BACKUP_PTR(pBackup), nPage);
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1backup_1remaining(
    JNIEnv *env, jclass cls, jlong pBackup) {
  return sqlite3_backup_remaining(JLONG_TO_SQLITE3_BACKUP_PTR(pBackup));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1backup_1pagecount(
    JNIEnv *env, jclass cls, jlong pBackup) {
  return sqlite3_backup_pagecount(JLONG_TO_SQLITE3_BACKUP_PTR(pBackup));
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1backup_1finish(
    JNIEnv *env, jclass cls, jlong pBackup) {
  return sqlite3_backup_finish(JLONG_TO_SQLITE3_BACKUP_PTR(pBackup));
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_free_1callback_1context(
    JNIEnv *env, jclass cls, jlong p) {
  free_callback_context(env, JLONG_TO_PTR(p));
}

static int progress(void *udp) {
  callback_context *cc = (callback_context *)udp;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  int progress = (*env)->CallBooleanMethod(env, cc->obj, cc->mid);
  if ((*env)->ExceptionCheck(env)) {
    return progress; // FIXME
  }
  return progress;
}

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1progress_1handler(
    JNIEnv *env, jclass cls, jlong pDb, jint nOps, jobject xProgress) {
  if (!xProgress) {
    sqlite3_progress_handler(JLONG_TO_SQLITE3_PTR(pDb), 0, 0, 0);
    return 0;
  }
  jclass clz = (*env)->GetObjectClass(env, xProgress);
  jmethodID mid = (*env)->GetMethodID(env, clz, "progress", "()Z");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(env, "expected 'boolean progress()' method");
    return 0;
  }
  callback_context *cc = create_callback_context(env, mid, xProgress);
  if (!cc) {
    return 0;
  }
  sqlite3_progress_handler(JLONG_TO_SQLITE3_PTR(pDb), nOps, progress, cc);
  return PTR_TO_JLONG(cc);
}

static void trace(void *arg, const char *zMsg) {
  callback_context *cc = (callback_context *)arg;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  jstring msg = (*env)->NewStringUTF(env, zMsg);
  (*env)->CallVoidMethod(env, cc->obj, cc->mid, msg);
  (*env)->DeleteLocalRef(env, msg);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
}

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1trace(JNIEnv *env,
                                                              jclass cls,
                                                              jlong pDb,
                                                              jobject xTrace) {
  if (!xTrace) {
    free_callback_context(env, sqlite3_trace(JLONG_TO_SQLITE3_PTR(pDb), 0, 0));
    return 0;
  }
  jclass clz = (*env)->GetObjectClass(env, xTrace);
  jmethodID mid =
      (*env)->GetMethodID(env, clz, "trace", "(Ljava/lang/String;)V");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(env, "expected 'void trace(String)' method");
    return 0;
  }
  callback_context *cc = create_callback_context(env, mid, xTrace);
  if (!cc) {
    return 0;
  }
  free_callback_context(env,
                        sqlite3_trace(JLONG_TO_SQLITE3_PTR(pDb), trace, cc));
  return PTR_TO_JLONG(cc);
}

static void profile(void *arg, const char *zMsg, sqlite3_uint64 ns) {
  callback_context *cc = (callback_context *)arg;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  jstring msg = (*env)->NewStringUTF(env, zMsg);
  (*env)->CallVoidMethod(env, cc->obj, cc->mid, msg, ns);
  (*env)->DeleteLocalRef(env, msg);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
}

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1profile(
    JNIEnv *env, jclass cls, jlong pDb, jobject xProfile) {
  if (!xProfile) {
    free_callback_context(env,
                          sqlite3_profile(JLONG_TO_SQLITE3_PTR(pDb), 0, 0));
    return 0;
  }
  jclass clz = (*env)->GetObjectClass(env, xProfile);
  jmethodID mid =
      (*env)->GetMethodID(env, clz, "profile", "(Ljava/lang/String;J)V");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(env, "expected 'void profile(String, long)' method");
    return 0;
  }
  callback_context *cc = create_callback_context(env, mid, xProfile);
  if (!cc) {
    return 0;
  }
  free_callback_context(
      env, sqlite3_profile(JLONG_TO_SQLITE3_PTR(pDb), profile, cc));
  return PTR_TO_JLONG(cc);
}

static void update_hook(void *arg, int actionCode, const char *zDbName,
                        const char *zTblName, sqlite3_int64 rowId) {
  callback_context *cc = (callback_context *)arg;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  jstring dbName = (*env)->NewStringUTF(env, zDbName);
  jstring tblName = (*env)->NewStringUTF(env, zTblName);
  (*env)->CallVoidMethod(env, cc->obj, cc->mid, actionCode, dbName, tblName,
                         rowId);
  (*env)->DeleteLocalRef(env, dbName);
  (*env)->DeleteLocalRef(env, tblName);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }
}

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1update_1hook(
    JNIEnv *env, jclass cls, jlong pDb, jobject xUpdateHook) {
  if (!xUpdateHook) {
    free_callback_context(env,
                          sqlite3_update_hook(JLONG_TO_SQLITE3_PTR(pDb), 0, 0));
    return 0;
  }
  jclass clz = (*env)->GetObjectClass(env, xUpdateHook);
  jmethodID mid = (*env)->GetMethodID(
      env, clz, "update", "(ILjava/lang/String;Ljava/lang/String;J)V");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(env,
                   "expected 'void update(int, String, String, long)' method");
    return 0;
  }
  callback_context *cc = create_callback_context(env, mid, xUpdateHook);
  if (!cc) {
    return 0;
  }
  free_callback_context(
      env, sqlite3_update_hook(JLONG_TO_SQLITE3_PTR(pDb), update_hook, cc));
  return PTR_TO_JLONG(cc);
}

static int authorizer(void *arg, int actionCode, const char *zArg1,
                      const char *zArg2, const char *zDbName,
                      const char *zTriggerName) {
  callback_context *cc = (callback_context *)arg;
  JNIEnv *env = 0;
  (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
  jstring arg1 = (*env)->NewStringUTF(env, zArg1);
  jstring arg2 = (*env)->NewStringUTF(env, zArg2);
  jstring dbName = (*env)->NewStringUTF(env, zDbName);
  jstring triggerName = (*env)->NewStringUTF(env, zTriggerName);
  int authorize = (*env)->CallIntMethod(env, cc->obj, cc->mid, actionCode, arg1,
                                        arg2, dbName, triggerName);
  (*env)->DeleteLocalRef(env, arg1);
  (*env)->DeleteLocalRef(env, arg2);
  (*env)->DeleteLocalRef(env, dbName);
  (*env)->DeleteLocalRef(env, triggerName);
  if ((*env)->ExceptionCheck(env)) {
    return authorize; // FIXME
  }
  return authorize;
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1set_1authorizer(
    JNIEnv *env, jclass cls, jlong pDb, jobject xAuthorizer, jlongArray pCc) {
  if (!xAuthorizer) {
    return sqlite3_set_authorizer(JLONG_TO_SQLITE3_PTR(pDb), 0, 0);
  }
  jclass clz = (*env)->GetObjectClass(env, xAuthorizer);
  jmethodID mid = (*env)->GetMethodID(
      env, clz, "authorize", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/"
                             "String;Ljava/lang/String;)I");
  (*env)->DeleteLocalRef(env, clz);
  if (!mid) {
    throwException(
        env,
        "expected 'int authorize(int, String, String, String, String)' method");
    return SQLITE_NOMEM;
  }
  callback_context *cc = create_callback_context(env, mid, xAuthorizer);
  if (!cc) {
    return SQLITE_NOMEM;
  }
  jlong p = PTR_TO_JLONG(cc);
  (*env)->SetLongArrayRegion(env, pCc, 0, 1, &p);
  return sqlite3_set_authorizer(JLONG_TO_SQLITE3_PTR(pDb), authorizer, cc);
}

typedef struct {
  JavaVM *vm;
  jmethodID mid; // scalar func or aggregate step
  jmethodID cid; // createAggregateContext
  jobject obj;
  jmethodID fid; // aggregate final
  jobject fobj;
} udf_callback_context;

static udf_callback_context *
create_udf_callback_context(JNIEnv *env, jmethodID mid, jmethodID cid,
                            jobject obj, jmethodID fid, jobject fobj) {
  udf_callback_context *cc = sqlite3_malloc(sizeof(udf_callback_context));
  if (!cc) {
    throwException(env, "OOM");
    return cc;
  }
  (*env)->GetJavaVM(env, &cc->vm);
  cc->mid = mid;
  cc->cid = cid;
  cc->obj = WEAK_GLOBAL_REF(obj);
  cc->fid = fid;
  if (fobj) {
    cc->fobj = WEAK_GLOBAL_REF(fobj);
  } else {
    cc->fobj = 0;
  }
  return cc;
}

static void free_udf_callback_context(void *p) {
  if (p) {
    udf_callback_context *cc = (udf_callback_context *)p;
    JNIEnv *env = 0;
    (*cc->vm)->AttachCurrentThread(cc->vm, (void **)&env, 0);
    DEL_WEAK_GLOBAL_REF(cc->obj);
    if (cc->fobj) {
      DEL_WEAK_GLOBAL_REF(cc->fobj);
    }
  }
  sqlite3_free(p);
}

static void func_or_step(sqlite3_context *ctx, int argc, sqlite3_value **argv) {
  udf_callback_context *h = (udf_callback_context *)sqlite3_user_data(ctx);
  JNIEnv *env = 0;
  (*h->vm)->AttachCurrentThread(h->vm, (void **)&env, 0);
  jlongArray b = (*env)->NewLongArray(env, argc);
  if (!b) {
    sqlite3_result_error_nomem(ctx);
    return;
  }
  // FIXME incompatible pointer types passing 'sqlite3_value **' (aka 'struct
  // Mem **') to parameter of type 'const jlong *' (aka 'const long *')
  (*env)->SetLongArrayRegion(env, b, 0, argc, argv);
  (*env)->CallVoidMethod(env, h->obj, h->mid, ctx, b);
  (*env)->DeleteLocalRef(env, b);
  if ((*env)->ExceptionCheck(env)) {
    // FIXME sqlite3_result_error();
    return;
  }
}

static void final_step(sqlite3_context *ctx) {
  udf_callback_context *h = (udf_callback_context *)sqlite3_user_data(ctx);
  JNIEnv *env = 0;
  (*h->vm)->AttachCurrentThread(h->vm, (void **)&env, 0);
  (*env)->CallVoidMethod(env, h->fobj, h->fid, ctx);
  if ((*env)->ExceptionCheck(env)) {
    // FIXME sqlite3_result_error();
    return;
  }
}
JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1create_1function_1v2(
    JNIEnv *env, jclass cls, jlong pDb, jstring functionName, jint nArg,
    jint eTextRep, jobject xFunc, jobject xStep, jobject xFinal) {
  const char *zFunctionName = (*env)->GetStringUTFChars(env, functionName, 0);
  if (!zFunctionName) {
    return SQLITE_NOMEM; /* OutOfMemoryError already thrown */
  }
  udf_callback_context *cc = 0;
  if (xFunc || xStep) {
    jclass clz = (*env)->GetObjectClass(env, xFunc ? xFunc : xStep);
    jmethodID mid = (*env)->GetMethodID(env, clz, "callback", "(J[J)V");
    if (!mid) {
      (*env)->DeleteLocalRef(env, clz);
      throwException(env, "expected 'void callback(long, long[])' method");
      return -1;
    }
    jmethodID cid = 0;
    if (xStep) {
      cid = (*env)->GetMethodID(env, clz, "createAggregateContext",
                                "()Ljava/lang/Object;");
      if (!cid) {
        (*env)->DeleteLocalRef(env, clz);
        throwException(env,
                       "expected 'Object createAggregateContext()' method");
        return -1;
      }
    }
    (*env)->DeleteLocalRef(env, clz);
    jmethodID fid = 0;
    if (xFinal) {
      jclass clz = (*env)->GetObjectClass(env, xFinal);
      fid = (*env)->GetMethodID(env, clz, "callback", "(J)V");
      (*env)->DeleteLocalRef(env, clz);
      if (!fid) {
        throwException(env, "expected 'void callback(long)' method");
        return -1;
      }
    }

    cc = create_udf_callback_context(env, mid, cid, xFunc ? xFunc : xStep, fid,
                                     xFinal);
    if (!cc) {
      (*env)->ReleaseStringUTFChars(env, functionName, zFunctionName);
      return SQLITE_NOMEM;
    }
  }
  int rc = sqlite3_create_function_v2(
      JLONG_TO_SQLITE3_PTR(pDb), zFunctionName, nArg, eTextRep, cc,
      xFunc ? func_or_step : 0, xStep ? func_or_step : 0,
      xFinal ? final_step : 0, free_udf_callback_context);
  (*env)->ReleaseStringUTFChars(env, functionName, zFunctionName);
  return rc;
}

#define JLONG_TO_SQLITE3_CTX_PTR(jl) ((sqlite3_context *)(size_t)(jl))

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1null(
    JNIEnv *env, jclass cls, jlong pCtx) {
  sqlite3_result_null(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1int(JNIEnv *env,
                                                                   jclass cls,
                                                                   jlong pCtx,
                                                                   jint i) {
  sqlite3_result_int(JLONG_TO_SQLITE3_CTX_PTR(pCtx), i);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1double(
    JNIEnv *env, jclass cls, jlong pCtx, jdouble d) {
  sqlite3_result_double(JLONG_TO_SQLITE3_CTX_PTR(pCtx), d);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1text(
    JNIEnv *env, jclass cls, jlong pCtx, jstring s, jint n) {
  if (!s) {
    sqlite3_result_null(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
    return;
  }
  jsize len = (*env)->GetStringLength(env, s) * sizeof(jchar);
  if (len > 0) {
    const jchar *data = (*env)->GetStringCritical(env, s, 0);
    if (!data) {
      sqlite3_result_error_nomem(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
      return;
    }
    sqlite3_result_text16(JLONG_TO_SQLITE3_CTX_PTR(pCtx), data, len,
                          SQLITE_TRANSIENT);
    (*env)->ReleaseStringCritical(env, s, data);
    /*const char *data = (*env)->GetStringUTFChars(env, s, 0);
    if (!data) {
            return -1; // Wrapper specific error code
    }
    sqlite3_result_text(JLONG_TO_SQLITE3_CTX_PTR(pCtx), data, -1,
    SQLITE_TRANSIENT);
    (*env)->ReleaseStringUTFChars(env, s, data);*/
  } else {
    sqlite3_result_text16(JLONG_TO_SQLITE3_CTX_PTR(pCtx), "", 0, SQLITE_STATIC);
    // sqlite3_result_text(JLONG_TO_SQLITE3_CTX_PTR(pCtx), "", 0,
    // SQLITE_STATIC);
  }
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1blob(
    JNIEnv *env, jclass cls, jlong pCtx, jbyteArray b, jint n) {
  if (!b) {
    sqlite3_result_null(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
    return;
  }
  jsize len = (*env)->GetArrayLength(env, b);
  if (len > 0) {
    void *data = (*env)->GetPrimitiveArrayCritical(env, b, 0);
    if (!data) {
      sqlite3_result_error_nomem(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
      return;
    }
    sqlite3_result_blob(JLONG_TO_SQLITE3_CTX_PTR(pCtx), data, len,
                        SQLITE_TRANSIENT);
    (*env)->ReleasePrimitiveArrayCritical(env, b, data, 0);
  } else {
    sqlite3_result_zeroblob(JLONG_TO_SQLITE3_CTX_PTR(pCtx), 0);
  }
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1int64(
    JNIEnv *env, jclass cls, jlong pCtx, jlong l) {
  sqlite3_result_int64(JLONG_TO_SQLITE3_CTX_PTR(pCtx), l);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1zeroblob(
    JNIEnv *env, jclass cls, jlong pCtx, jint n) {
  sqlite3_result_zeroblob(JLONG_TO_SQLITE3_CTX_PTR(pCtx), n);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1error(
    JNIEnv *env, jclass cls, jlong pCtx, jstring err, jint len) {
  const char *zErr = (*env)->GetStringUTFChars(env, err, 0);
  if (!zErr) {
    return; /* OutOfMemoryError already thrown */
  }
  sqlite3_result_error(JLONG_TO_SQLITE3_CTX_PTR(pCtx), zErr, -1);
  (*env)->ReleaseStringUTFChars(env, err, zErr);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1error_1code(
    JNIEnv *env, jclass cls, jlong pCtx, jint errCode) {
  sqlite3_result_error_code(JLONG_TO_SQLITE3_CTX_PTR(pCtx), errCode);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1error_1nomem(
    JNIEnv *env, jclass cls, jlong pCtx) {
  sqlite3_result_error_nomem(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1result_1error_1toobig(
    JNIEnv *env, jclass cls, jlong pCtx) {
  sqlite3_result_error_toobig(JLONG_TO_SQLITE3_CTX_PTR(pCtx));
}

#define JLONG_TO_SQLITE3_VALUE_PTR(jl) ((sqlite3_value *)(size_t)(jl))

JNIEXPORT jbyteArray JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1blob(
    JNIEnv *env, jclass cls, jlong pValue) {
  const void *blob = sqlite3_value_blob(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
  if (!blob) {
    return 0;
  }
  int len = sqlite3_value_bytes(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
  jbyteArray b = (*env)->NewByteArray(env, len);
  if (!b) {
    return 0; /* OutOfMemoryError already thrown */
  }
  //(*env)->SetByteArrayRegion(env, b, 0, len, blob);
  void *data = (*env)->GetPrimitiveArrayCritical(env, b, 0);
  if (!data) {
    return 0;
  }
  memcpy(data, blob, len);
  (*env)->ReleasePrimitiveArrayCritical(env, b, data, 0);
  return b;
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1bytes(
    JNIEnv *env, jclass cls, jlong pValue) {
  return sqlite3_value_bytes(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
}

JNIEXPORT jdouble JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1double(
    JNIEnv *env, jclass cls, jlong pValue) {
  return sqlite3_value_double(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1int(
    JNIEnv *env, jclass cls, jlong pValue) {
  return sqlite3_value_int(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
}

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1int64(
    JNIEnv *env, jclass cls, jlong pValue) {
  return sqlite3_value_int64(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
}

JNIEXPORT jstring JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1text(
    JNIEnv *env, jclass cls, jlong pValue) {
  // return (*env)->NewStringUTF(env, (const
  // char*)sqlite3_value_text(JLONG_TO_SQLITE3_VALUE_PTR(pValue)));
  const void *text = sqlite3_value_text16(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
  if (!text) {
    return 0;
  }
  int len = sqlite3_value_bytes16(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
  return (*env)->NewString(env, text, len / sizeof(jchar));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1type(
    JNIEnv *env, jclass cls, jlong pValue) {
  return sqlite3_value_type(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
}

JNIEXPORT jint JNICALL Java_org_sqlite_SQLite_sqlite3_1value_1numeric_1type(
    JNIEnv *env, jclass cls, jlong pValue) {
  return sqlite3_value_numeric_type(JLONG_TO_SQLITE3_VALUE_PTR(pValue));
}

JNIEXPORT jobject JNICALL Java_org_sqlite_SQLite_sqlite3_1get_1auxdata(
    JNIEnv *env, jclass cls, jlong pCtx, jint n) {
  // FIXME
  return sqlite3_get_auxdata(JLONG_TO_SQLITE3_CTX_PTR(pCtx), n);
}

JNIEXPORT void JNICALL Java_org_sqlite_SQLite_sqlite3_1set_1auxdata(
    JNIEnv *env, jclass cls, jlong pCtx, jint n, jobject p, jobject xDel) {
  // FIXME incompatible pointer types passing 'jobject' (aka 'struct _jobject
  // *') to parameter of type 'void (*)(void *)'
  sqlite3_set_auxdata(JLONG_TO_SQLITE3_CTX_PTR(pCtx), n, p, /*xDel*/ 0);
}

JNIEXPORT jobject JNICALL Java_org_sqlite_SQLite_sqlite3_1aggregate_1context(
    JNIEnv *env, jclass cls, jlong pCtx, jint allocate) {
  if (allocate == 1) {
    jobject *pAggrCtx = sqlite3_aggregate_context(
        JLONG_TO_SQLITE3_CTX_PTR(pCtx), sizeof(jobject));
    if (!pAggrCtx) {
      return 0;
    }
    jobject aggrCtx = *pAggrCtx;
    if (!aggrCtx) {
      udf_callback_context *h = (udf_callback_context *)sqlite3_user_data(
          JLONG_TO_SQLITE3_CTX_PTR(pCtx));
      JNIEnv *env = 0;
      (*h->vm)->AttachCurrentThread(h->vm, (void **)&env, 0);
      aggrCtx = (*env)->CallObjectMethod(env, h->obj, h->cid);
      if ((*env)->ExceptionCheck(env)) {
        return 0; // FIXME
      }
      *pAggrCtx = WEAK_GLOBAL_REF(aggrCtx);
    }
    return aggrCtx;
  } else {
    jobject *pAggrCtx =
        sqlite3_aggregate_context(JLONG_TO_SQLITE3_CTX_PTR(pCtx), 0);
    if (!pAggrCtx) {
      return 0;
    }
    jobject aggrCtx = *pAggrCtx;
    if (allocate == 0) {
      return aggrCtx;
    } else {
      DEL_WEAK_GLOBAL_REF(aggrCtx);
      return 0;
    }
  }
}

JNIEXPORT jlong JNICALL Java_org_sqlite_SQLite_sqlite3_1context_1db_1handle(
    JNIEnv *env, jclass cls, jlong pCtx) {
  return PTR_TO_JLONG(
      sqlite3_context_db_handle(JLONG_TO_SQLITE3_CTX_PTR(pCtx)));
}
