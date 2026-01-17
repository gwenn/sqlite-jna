package org.sqlite;

import java.lang.foreign.MemorySegment;

/**
 * Serialized database
 */
public record Serialized(boolean shared, MemorySegment ptr) {
}
