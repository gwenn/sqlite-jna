JDBC driver for SQLite using JNA instead of JNI to make it easy to deploy
(if you already have SQLite installed).
There are two layers:
 - a small one matching the SQLite API (package org.sqlite)
 - a bloated one matching the JDBC API (package org.sqlite.driver)

[![Build Status](https://secure.travis-ci.org/gwenn/sqlite-jna.png)](http://www.travis-ci.org/gwenn/sqlite-jna)
[![Build Status](https://ci.appveyor.com/api/projects/status/github/gwenn/sqlite-jna?branch=master&svg=true)](https://ci.appveyor.com/project/gwenn/sqlite-jna)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.gwenn/sqlite-jna.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.gwenn%22%20AND%20a:%22sqlite-jna%22)
[![Javadocs](https://www.javadoc.io/badge/com.github.gwenn/sqlite-jna.svg)](https://www.javadoc.io/doc/com.github.gwenn/sqlite-jna)

INSTALL
-------
1. https://github.com/twall/jna/blob/master/www/GettingStarted.md
2. http://www.sqlite.org/download.html
3. Ensure JVM and SQLite match (x86 vs x86-64)

On windows, to build your own x86-64 version (with cygwin&mingw):
```sh
x86_64-w64-mingw32-gcc.exe -Wl,--kill-at -O -shared -o sqlite3.dll -DSQLITE_ENABLE_COLUMN_METADATA -DSQLITE_ENABLE_FTS4 -DSQLITE_ENABLE_STAT3 -DSQLITE_THREADSAFE=1 -DSQLITE_DEFAULT_FOREIGN_KEYS=1 sqlite3.c
+ Stripping...
```

TODO
----
1. Fix as many unimplemented methods as possible.
2. Benchmark

LINKS
-----
* https://github.com/xerial/sqlite-jdbc (~~https://bitbucket.org/xerial/sqlite-jdbc~~) (JNI)
* http://www.ch-werner.de/javasqlite/ (JNI)
* https://code.google.com/p/sqlite4java/ (JNI, no JDBC)
* https://github.com/lyubo/jdbc-lite (JNA)
* https://code.google.com/p/nativelibs4java/issues/detail?id=47 (Bridj)
* https://github.com/tstack/SqliteJdbcNG (Bridj)
* https://github.com/twall/jna
* https://github.com/bytedeco/javacpp

LICENSE
-------
Public domain
