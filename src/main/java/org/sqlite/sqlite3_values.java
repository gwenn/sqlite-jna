package org.sqlite;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static org.sqlite.SQLite.*;

/**
 * Dynamically typed value objects
 *
 * @see <a href="http://sqlite.org/c3ref/value.html">sqlite3_value</a>
 */
public final class sqlite3_values {
	private static final sqlite3_values NO_ARG = new sqlite3_values(MemorySegment.NULL, 0);
	private final MemorySegment args;
	private final int nArg;

	public static sqlite3_values build(int nArg, MemorySegment args) {
		if (nArg == 0) {
			return NO_ARG;
		}
		return new sqlite3_values(args, nArg);
	}

	private sqlite3_values(MemorySegment args, int nArg) {
		this.args = args.reinterpret(nArg * C_POINTER.byteSize());
		this.nArg = nArg;
	}

	/**
	 * @return arg count
	 */
	public int getCount() {
		return nArg;
	}

	private static final MethodHandle sqlite3_value_blob = downcallHandle(
		"sqlite3_value_blob", PP);
	private static final MethodHandle sqlite3_value_bytes = downcallHandle(
		"sqlite3_value_bytes", IP);
	private static int sqlite3_value_bytes(MemorySegment pValue) {
		try {
			return (int)sqlite3_value_bytes.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}
	/**
	 * @param i 0...
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_blob</a>
	 */
	public byte[] getBlob(int i) {
		MemorySegment arg = arg(i);
		MemorySegment blob;
		try {
			blob = (MemorySegment) sqlite3_value_blob.invokeExact(arg);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
		if (isNull(blob)) {
			return null;
		} else {
			return blob.reinterpret(sqlite3_value_bytes(arg)).toArray(C_CHAR); // a copy is made...
		}
	}

	private static final MethodHandle sqlite3_value_double = downcallHandle(
		"sqlite3_value_double", FunctionDescriptor.ofVoid(C_DOUBLE, C_POINTER));
	/**
	 * @param i 0...
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_double</a>
	 */
	public double getDouble(int i) {
		MemorySegment pValue = arg(i);
		try {
			return (double)sqlite3_value_double.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_value_int = downcallHandle(
		"sqlite3_value_int", IP);
	/**
	 * @param i 0...
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int</a>
	 */
	public int getInt(int i) {
		MemorySegment pValue = arg(i);
		try {
			return (int)sqlite3_value_int.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_value_int64 = downcallHandle(
		"sqlite3_value_int64", LP);
	/**
	 * @param i 0...
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_int64</a>
	 */
	public long getLong(int i) {
		MemorySegment pValue = arg(i);
		try {
			return (long)sqlite3_value_int64.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_value_text = downcallHandle(
		"sqlite3_value_text", PP);
	/**
	 * @param i 0...
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_text</a>
	 */
	public String getText(int i) {
		MemorySegment pValue = arg(i);
		try {
			return getString((MemorySegment)sqlite3_value_text.invokeExact(pValue));
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}


	private static final MethodHandle sqlite3_value_type = downcallHandle(
		"sqlite3_value_type", IP);
	/**
	 * @param i 0...
	 * @return {@link ColTypes}.*
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_type</a>
	 */
	public int getType(int i) {
		MemorySegment pValue = arg(i);
		try {
			return (int)sqlite3_value_type.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_value_numeric_type = downcallHandle(
		"sqlite3_value_numeric_type", IP);
	/**
	 * @param i 0...
	 * @return {@link ColTypes}.*
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_numeric_type</a>
	 */
	public int getNumericType(int i) {
		MemorySegment pValue = arg(i);
		try {
			return (int)sqlite3_value_numeric_type.invokeExact(pValue);
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private static final MethodHandle sqlite3_value_pointer = downcallHandle(
		"sqlite3_value_pointer", PPP);
	/**
	 * @param i 0...
	 * @see <a href="http://sqlite.org/c3ref/value_blob.html">sqlite3_value_pointer</a>
	 */
	public MemorySegment getPointer(int i, MemorySegment name, MemoryLayout ml) {
		MemorySegment pValue = arg(i);
		try {
			MemorySegment ms = (MemorySegment) sqlite3_value_pointer.invokeExact(pValue, name);
			return ms.reinterpret(ml.byteSize());
		} catch (Throwable e) {
			throw new AssertionError("should not reach here", e);
		}
	}

	private MemorySegment arg(int i) {
		return args.getAtIndex(C_POINTER, i);
	}
}
