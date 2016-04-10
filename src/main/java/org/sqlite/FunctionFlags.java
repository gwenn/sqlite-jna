package org.sqlite;

/**
 * User defined function creation flags.
 * @see org.sqlite.Conn#createScalarFunction(String, int, int, ScalarCallback)
 * @see <a href="http://sqlite.org/c3ref/c_any.html">Text Encodings</a>
 * @see <a href="http://sqlite.org/c3ref/create_function.html">UDF</a>
 */
public interface FunctionFlags {
	int SQLITE_UTF8 = 1;
	int SQLITE_UTF16LE = 2;
	int SQLITE_UTF16BE = 3;
	int SQLITE_UTF16 = 4;
	/** @see <a href="http://sqlite.org/c3ref/c_deterministic.html">deterministic</a> */
	int SQLITE_DETERMINISTIC = 0x800;
}
