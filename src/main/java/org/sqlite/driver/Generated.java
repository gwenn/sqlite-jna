package org.sqlite.driver;

import java.sql.Statement;

/**
 * Generated keys (or columns)
 */
public enum Generated {
	/**
	 * @see Statement#NO_GENERATED_KEYS
	 */
	NO_GENERATED_KEYS,
	/**
	 * @see Statement#RETURN_GENERATED_KEYS
	 */
	RETURN_GENERATED_KEYS,
	/**
	 * <a href="https://sqlite.org/lang_returning.html">RETURNING</a>
	 */
	RETURNING,
	RETURNING_NO_ROW
}
