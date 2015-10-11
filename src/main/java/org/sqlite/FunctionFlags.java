package org.sqlite;

public interface FunctionFlags {
	int SQLITE_UTF8 = 1;
	int SQLITE_UTF16LE = 2;
	int SQLITE_UTF16BE = 3;
	int SQLITE_UTF16 = 4;
	int SQLITE_DETERMINISTIC = 0x800;
}
