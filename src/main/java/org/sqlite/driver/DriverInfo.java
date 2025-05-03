package org.sqlite.driver;

public interface DriverInfo {
	// Driver name
	String DRIVER_NAME = "SQLite3 JDBC Driver";
	String DRIVER_VERSION = "0.3.0";
	//String DRIVER_FULL_NAME = DRIVER_NAME + " " + DRIVER_VERSION;

	// Driver version
	int MAJOR_VERSION = 0;
	int MINOR_VERSION = 3;

	// JDBC specification
	int JDBC_MAJOR_VERSION = 4;
	int JDBC_MINOR_VERSION = 2;
}
