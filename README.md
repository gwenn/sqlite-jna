JDBC driver for SQLite using JNI.

There are two layers:
 - a small one matching the SQLite API (package org.sqlite)
 - a bloated one matching the JDBC API (package org.sqlite.driver)

[![Build Status][1]][2]

[1]: https://travis-ci.org/gwenn/sqlite-jna.svg?branch=jni
[2]: http://www.travis-ci.org/gwenn/sqlite-jna

INSTALL
-------
1. http://www.sqlite.org/download.html

TODO
----
1. Fix as many unimplemented methods as possible.
2. Benchmark

LINKS
-----
* https://bitbucket.org/xerial/sqlite-jdbc (JNI)
* http://www.ch-werner.de/javasqlite/ (JNI)
* https://code.google.com/p/sqlite4java/ (JNI, no JDBC)
* https://github.com/lyubo/jdbc-lite (JNA)
* https://code.google.com/p/nativelibs4java/issues/detail?id=47 (Bridj)
* https://github.com/tstack/SqliteJdbcNG (Bridj)
* https://github.com/jnr/jnr-ffi

LICENSE
-------
Public domain
