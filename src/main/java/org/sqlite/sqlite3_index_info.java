package org.sqlite;

import java.lang.foreign.*;
import java.lang.foreign.ValueLayout.OfByte;
import java.lang.foreign.ValueLayout.OfDouble;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.util.Iterator;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.SQLite.*;

public class sqlite3_index_info {
	static final GroupLayout layout = MemoryLayout.structLayout(
		/* Inputs */
		C_INT.withName("nConstraint"),
		MemoryLayout.paddingLayout(4),
		C_POINTER.withName("aConstraint"),
		C_INT.withName("nOrderBy"),
		MemoryLayout.paddingLayout(4),
		C_POINTER.withName("aOrderBy"),
		/* Outputs */
		C_POINTER.withName("aConstraintUsage"),
		C_INT.withName("idxNum"),
		MemoryLayout.paddingLayout(4),
		C_POINTER.withName("idxStr"),
		C_INT.withName("needToFreeIdxStr"),
		C_INT.withName("orderByConsumed"),
		C_DOUBLE.withName("estimatedCost"),
		C_LONG_LONG.withName("estimatedRows"),
		C_INT.withName("idxFlags"),
		MemoryLayout.paddingLayout(4),
		/* Input: Mask of columns used by statement */
		C_LONG_LONG.withName("colUsed")
	).withName("sqlite3_index_info");

	private static final OfInt nConstraint = (OfInt)layout.select(groupElement("nConstraint"));
	public static int nConstraint(MemorySegment struct) {
		return struct.get(nConstraint, 0);
	}

	public static class sqlite3_index_constraint {
		static final GroupLayout layout = MemoryLayout.structLayout(
			C_INT.withName("iColumn"),
			C_CHAR.withName("op"),
			C_CHAR.withName("usable"),
			MemoryLayout.paddingLayout(2),
			C_INT.withName("iTermOffset")
		).withName("sqlite3_index_constraint");
		private static final OfInt iColumn = (OfInt)layout.select(groupElement("iColumn"));
		public static int iColumn(MemorySegment struct) {
			return struct.get(iColumn, 0);
		}
		public static final byte SQLITE_INDEX_CONSTRAINT_EQ = 2;
		public static final byte SQLITE_INDEX_CONSTRAINT_GT = 4;
		public static final byte SQLITE_INDEX_CONSTRAINT_LE = 8;
		public static final byte SQLITE_INDEX_CONSTRAINT_LT = 16;
		public static final byte SQLITE_INDEX_CONSTRAINT_GE = 32;
		public static final byte SQLITE_INDEX_CONSTRAINT_MATCH = 64;
		public static final byte SQLITE_INDEX_CONSTRAINT_LIKE = 65;
		public static final byte SQLITE_INDEX_CONSTRAINT_GLOB = 66;
		public static final byte SQLITE_INDEX_CONSTRAINT_REGEXP = 67;
		public static final byte SQLITE_INDEX_CONSTRAINT_NE = 68;
		public static final byte SQLITE_INDEX_CONSTRAINT_ISNOT = 69;
		public static final byte SQLITE_INDEX_CONSTRAINT_ISNOTNULL = 70;
		public static final byte SQLITE_INDEX_CONSTRAINT_ISNULL = 71;
		public static final byte SQLITE_INDEX_CONSTRAINT_IS = 72;
		public static final byte SQLITE_INDEX_CONSTRAINT_LIMIT = 73;
		public static final byte SQLITE_INDEX_CONSTRAINT_OFFSET = 74;
		private static final OfByte op = (OfByte)layout.select(groupElement("op"));
		public static byte op(MemorySegment struct) {
			return struct.get(op, 4);
		}
		private static final OfByte usable = (OfByte)layout.select(groupElement("usable"));
		public static boolean usable(MemorySegment struct) {
			return struct.get(usable, 5) != 0;
		}
	}
	private static final AddressLayout aConstraint = (AddressLayout)layout.select(groupElement("aConstraint"));
	public static Iterator<MemorySegment> aConstraint(MemorySegment struct, int nConstraint) {
		MemorySegment aConstraint = struct.get(sqlite3_index_info.aConstraint, 8);
		aConstraint = aConstraint.reinterpret(nConstraint * sqlite3_index_constraint.layout.byteSize());
		return aConstraint.elements(sqlite3_index_constraint.layout).limit(nConstraint).iterator();
	}

	private static final OfInt nOrderBy = (OfInt)layout.select(groupElement("nOrderBy"));
	public static int nOrderBy(MemorySegment struct) {
		return struct.get(nOrderBy, 16);
	}

	private static class sqlite3_index_orderby {
		private static final GroupLayout layout = MemoryLayout.structLayout(
			C_INT.withName("iColumn"),
			C_CHAR.withName("desc"),
			MemoryLayout.paddingLayout(3)
		).withName("sqlite3_index_orderby");
		private static final OfInt iColumn = (OfInt)layout.select(groupElement("iColumn"));
		public static int iColumn(MemorySegment struct) {
			return struct.get(iColumn, 0);
		}
		private static final OfByte desc = (OfByte)layout.select(groupElement("desc"));
		public static byte desc(MemorySegment struct) {
			return struct.get(desc, 4);
		}
	}
	private static final AddressLayout aOrderBy = (AddressLayout)layout.select(groupElement("aOrderBy"));
	public static Iterator<MemorySegment> aOrderBy(MemorySegment struct) {
		MemorySegment aOrderBy = struct.get(sqlite3_index_info.aOrderBy, 8);
		final int nOrderBy = nOrderBy(struct);
		aOrderBy = aOrderBy.reinterpret(nOrderBy * sqlite3_index_orderby.layout.byteSize());
		return aOrderBy.elements(sqlite3_index_orderby.layout).limit(nOrderBy).iterator();
	}

	public static class sqlite3_index_constraint_usage {
		static final GroupLayout layout = MemoryLayout.structLayout(
			C_INT.withName("argvIndex"),
			C_CHAR.withName("omit"),
			MemoryLayout.paddingLayout(3)
		).withName("sqlite3_index_constraint_usage");
		private static final OfInt argvIndex = (OfInt) layout.select(groupElement("argvIndex"));
		public static void argvIndex(MemorySegment struct, int fieldValue) {
			struct.set(argvIndex, 0, fieldValue);
		}
		private static final OfByte omit = (OfByte) layout.select(groupElement("omit"));
		public static void omit(MemorySegment struct, byte fieldValue) {
			struct.set(omit, 4, fieldValue);
		}
	}
	private static final AddressLayout aConstraintUsage = (AddressLayout)layout.select(groupElement("aConstraintUsage"));
	public static Iterator<MemorySegment> aConstraintUsage(MemorySegment struct, int nConstraint) {
		MemorySegment aConstraintUsage = struct.get(sqlite3_index_info.aConstraintUsage, 32);
		aConstraintUsage = aConstraintUsage.reinterpret(nConstraint * sqlite3_index_constraint_usage.layout.byteSize());
		return aConstraintUsage.elements(sqlite3_index_constraint_usage.layout).limit(nConstraint).iterator();
	}

	private static final OfInt idxNum = (OfInt)layout.select(groupElement("idxNum"));
	public static void idxNum(MemorySegment struct, int fieldValue) {
		struct.set(idxNum, 40, fieldValue);
	}

	private static final AddressLayout idxStr = (AddressLayout)layout.select(groupElement("idxStr"));
	public static void idxStr(MemorySegment struct, MemorySegment fieldValue) {
		struct.set(idxStr, 48, fieldValue);
	}
	private static final OfInt needToFreeIdxStr = (OfInt)layout.select(groupElement("needToFreeIdxStr"));
	public static void needToFreeIdxStr(MemorySegment struct, int fieldValue) {
		struct.set(needToFreeIdxStr, 56, fieldValue);
	}

	private static final OfInt orderByConsumed = (OfInt)layout.select(groupElement("orderByConsumed"));
	public static void orderByConsumed(MemorySegment struct, int fieldValue) {
		struct.set(orderByConsumed, 60, fieldValue);
	}
	private static final OfDouble estimatedCost = (OfDouble)layout.select(groupElement("estimatedCost"));
	public static void estimatedCost(MemorySegment struct, double fieldValue) {
		struct.set(estimatedCost, 64, fieldValue);
	}

	private static final OfLong estimatedRows = (OfLong)layout.select(groupElement("estimatedRows"));
	public static void estimatedRows(MemorySegment struct, long fieldValue) {
		struct.set(estimatedRows, 72, fieldValue);
	}

	private static final OfInt idxFlags = (OfInt)layout.select(groupElement("idxFlags"));
	public static void idxFlags(MemorySegment struct, int fieldValue) {
		struct.set(idxFlags, 80, fieldValue);
	}

	private static final OfLong colUsed = (OfLong)layout.select(groupElement("colUsed"));
	public static long colUsed(MemorySegment struct) {
		return struct.get(colUsed, 88);
	}
}
