package org.sqlite;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static org.sqlite.SQLite.*;

/**
 * SQL function context object
 *
 * @see <a href="http://sqlite.org/c3ref/context.html">sqlite3_context</a>
 */
public final class sqlite3_context {
	private final MemorySegment p;

	sqlite3_context(MemorySegment p) {
		this.p = p.asReadOnly();
	}

	private static final MethodHandle sqlite3_get_auxdata = downcallHandle(
		"sqlite3_get_auxdata", PPI);
	static MemorySegment sqlite3_get_auxdata(sqlite3_context pCtx, int n) {
		try {
			return (MemorySegment) sqlite3_get_auxdata.invokeExact(pCtx.p, n);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_set_auxdata = downcallHandle(
		"sqlite3_set_auxdata", FunctionDescriptor.ofVoid(C_POINTER, C_INT, C_POINTER, C_POINTER));
	static void sqlite3_set_auxdata(sqlite3_context pCtx, int n, MemorySegment p, Destructor free) {
		try {
			sqlite3_set_auxdata.invokeExact(pCtx.p, n, p, free);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_aggregate_context = downcallHandle(
		"sqlite3_aggregate_context", PPI);
	static MemorySegment sqlite3_aggregate_context(sqlite3_context pCtx, int nBytes) {
		try {
			return (MemorySegment) sqlite3_aggregate_context.invokeExact(pCtx.p, nBytes);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_context_db_handle = downcallHandle(
		"sqlite3_context_db_handle", PP);
	/**
	 * @return a copy of the pointer to the database connection (the 1st parameter) of
	 * {@link sqlite3#sqlite3_create_function_v2(sqlite3, String, int, int, MemorySegment, ScalarCallback, AggregateStepCallback, AggregateFinalCallback, MemorySegment)}
	 * @see <a href="http://sqlite.org/c3ref/context_db_handle.html">sqlite3_context_db_handle</a>
	 */
	public sqlite3 getDbHandle() {
		try {
			return new sqlite3((MemorySegment) sqlite3_context_db_handle.invokeExact(p));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_blob = downcallHandle(
		"sqlite3_result_blob", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_INT, C_POINTER),
		CRITICAL);
	/**
	 * Sets the return value of the application-defined function to be the BLOB value given.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_blob</a>
	 */
	public void setResultBlob(byte[] result) {
		try {
			MemorySegment ms = MemorySegment.ofArray(result);
			sqlite3_result_blob.invokeExact(p, ms, result.length, SQLITE_TRANSIENT);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_double = downcallHandle(
		"sqlite3_result_double", FunctionDescriptor.ofVoid(C_POINTER, C_DOUBLE));
	/**
	 * Sets the return value of the application-defined function to be the floating point value given.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_double</a>
	 */
	public void setResultDouble(double result) {
		try {
			sqlite3_result_double.invokeExact(p, result);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_int = downcallHandle(
		"sqlite3_result_int", VPI);
	/**
	 * Sets the return value of the application-defined function to be the 32-bit signed integer value given.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int</a>
	 */
	public void setResultInt(int result) {
		try {
			sqlite3_result_int.invokeExact(p, result);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_int64 = downcallHandle(
		"sqlite3_result_int64", FunctionDescriptor.ofVoid(C_POINTER, C_LONG_LONG));
	/**
	 * Sets the return value of the application-defined function to be the 64-bit signed integer value given.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_int64</a>
	 */
	public void setResultLong(long result) {
		try {
			sqlite3_result_int64.invokeExact(p, result);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_null = downcallHandle(
		"sqlite3_result_null", VP);
	/**
	 * Sets the return value of the application-defined function to be NULL.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_null</a>
	 */
	public void setResultNull() {
		try {
			sqlite3_result_null.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_text = downcallHandle(
		"sqlite3_result_text", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_INT, C_POINTER));
	/**
	 * Sets the return value of the application-defined function to be the text string given.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_text</a>
	 */
	public void setResultText(String result) {
		// no copy needed when xDel == SQLITE_TRANSIENT == -1
		try (Arena arena = Arena.ofConfined()) {
			// How to avoid copying twice ? nativeString + SQLite
			sqlite3_result_text.invokeExact(p, nativeString(arena, result), -1, SQLITE_TRANSIENT);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_zeroblob = downcallHandle(
		"sqlite3_result_zeroblob", VPI);
	/**
	 * Sets the return value of the application-defined function to be a BLOB containing all zero bytes and N bytes in size.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_zeroblob</a>
	 */
	public void setResultZeroBlob(ZeroBlob result) {
		try {
			sqlite3_result_zeroblob.invokeExact(p, result.n());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	/*
	 * Causes the subtype of the result from the application-defined SQL function to be the value given.
	 * @see <a href="http://sqlite.org/c3ref/result_subtype.html">sqlite3_result_subtype</a>
	 */
    /*public void setResultSubType(int subtype) {
        sqlite3_result_subtype(this, subtype);
    }*/

	private static final MethodHandle sqlite3_result_error = downcallHandle(
		"sqlite3_result_error", FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_INT));
	/**
	 * Causes the implemented SQL function to throw an exception.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error</a>
	 */
	public void setResultError(String errMsg) {
		try (Arena arena = Arena.ofConfined()) {
			sqlite3_result_error.invokeExact(p, nativeString(arena, errMsg), -1);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_error_code = downcallHandle(
		"sqlite3_result_error_code", VPI);
	/**
	 * Causes the implemented SQL function to throw an exception.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_code</a>
	 */
	public void setResultErrorCode(int errCode) {
		try {
			sqlite3_result_error_code.invokeExact(p, errCode);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_error_nomem = downcallHandle(
		"sqlite3_result_error_nomem", VP);
	/**
	 * Causes SQLite to throw an error indicating that a memory allocation failed.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_nomem</a>
	 */
	public void setResultErrorNoMem() {
		try {
			sqlite3_result_error_nomem.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_result_error_toobig = downcallHandle(
		"sqlite3_result_error_toobig", VP);
	/**
	 * Causes SQLite to throw an error indicating that a string or BLOB is too long to represent.
	 *
	 * @see <a href="http://sqlite.org/c3ref/result_blob.html">sqlite3_result_error_toobig</a>
	 */
	public void setResultErrorTooBig() {
		try {
			sqlite3_result_error_toobig.invokeExact(p);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
}
