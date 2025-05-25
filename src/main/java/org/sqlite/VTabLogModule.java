package org.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.foreign.ValueLayout.OfLong;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static org.sqlite.EponymousModule.dequote;
import static org.sqlite.EponymousModule.error;
import static org.sqlite.ErrCodes.*;
import static org.sqlite.SQLite.*;
import static org.sqlite.SQLite.SQLITE_OK;
import static org.sqlite.sqlite3_index_info.*;
import static org.sqlite.sqlite3_index_info.sqlite3_index_constraint.*;
import static org.sqlite.sqlite3_index_info.sqlite3_index_orderby.desc;
import static org.sqlite.sqlite3_module.sqlite3_declare_vtab;

/**
 * Port of <a href="https://www.sqlite.org/src/file?name=ext/misc/vtablog.c">vtablog</a>
 */
public class VTabLogModule implements UpdateModule {
	private static final Logger log = LoggerFactory.getLogger(VTabLogModule.class);
	public static final VTabLogModule INSTANCE = new VTabLogModule();

	public static void load_module(Conn conn) throws ConnException {
		conn.createModule("vtablog", INSTANCE, false);
	}

	private VTabLogModule() {
	}

	@Override
	public Map.Entry<Integer, MemorySegment> connect(sqlite3 db, MemorySegment aux, int argc, MemorySegment argv, MemorySegment errMsg, boolean isCreate) {
		MemorySegment zModuleName = argv.getAtIndex(C_POINTER, 0);
		MemorySegment zDb =  argv.getAtIndex(C_POINTER, 1);
		MemorySegment zName = argv.getAtIndex(C_POINTER, 2);
		List<String> args = new ArrayList<>(argc - 3);
		for (int i = 3; i < argc; i++) {
			args.add(getString(argv.getAtIndex(C_POINTER, i)));
		}
		String dbName = getString(zDb);
		String name = getString(zName);
		log.info(isCreate ? "{}.{}.xCreate({})" : "{}.{}.xConnect({})", dbName, name, args);
		Optional<String> schema = Optional.empty();
		Optional<Integer> nRow = Optional.empty();
		for (String arg : args) {
			if (arg == null || arg.isBlank()) {
				continue;
			}
			int equals = arg.indexOf('=');
			if (equals < 0) {
				continue;
			}
			String key = dequote(arg.substring(0, equals).trim());
			String value = dequote(arg.substring(equals + 1).trim());
			if ("schema".equals(key)) {
				if (schema.isPresent()) {
					return error(errMsg, SQLITE_ERROR, "more than one '%s' parameter", "schema");
				}
				schema = Optional.of(value);
			} else if ("rows".equals(key)) {
				if (nRow.isPresent()) {
					return error(errMsg, SQLITE_ERROR, "more than one '%s' parameter", "rows");
				}
				try {
					nRow = Optional.of(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					nRow = Optional.of(0);
				}
			}
		}
		if (schema.isEmpty()) {
			schema = Optional.of("CREATE TABLE x(a,b);");
		}
		//log.info("  schema = '{}'", schema.get());
		int rc = sqlite3_declare_vtab(db, schema.get());
		MemorySegment vtab = MemorySegment.NULL;
		if (rc == SQLITE_OK) {
			vtab = sqlite3_malloc(vtab_layout());
			if (isNull(vtab)) {
				return Map.entry(SQLITE_NOMEM, vtab);
			}
			if (nRow.isEmpty()) {
				nRow = Optional.of(10);
			}
			//log.info("  nrow = {}", nRow.get());
			nRow(vtab, nRow.get());
			zDb(vtab, dbName);
			zName(vtab, name);
		}
		return Map.entry(rc, vtab);
	}

	@Override
	public int bestIndex(MemorySegment vtab, MemorySegment info, Iterator<MemorySegment> aConstraint, Iterator<MemorySegment> aConstraintUsage) {
		log.info("{}.{}.xBestIndex(): colUsed: 0x{}, nConstraint: {}, nOrderBy: {}", db(vtab), name(vtab), Long.toHexString(colUsed(info)), nConstraint(info), nOrderBy(info));
		int i = 0;
		while (aConstraint.hasNext()) {
			MemorySegment constraint = aConstraint.next();
			log.info("  constraint[{}]: col={} op={} usable={}", i, iColumn(constraint), op(constraint), usable(constraint));
			// TODO sqlite3_vtab_collation
			i++;
		}
		final Iterator<MemorySegment> aOrderBy = aOrderBy(info);
		i = 0;
		while (aOrderBy.hasNext()) {
			final MemorySegment orderBy = aOrderBy.next();
			log.info("  orderBy[{}]: col={} desc={}", i, sqlite3_index_orderby.iColumn(orderBy), desc(orderBy));
			i++;
		}
		estimatedCost(info, 500);
		estimatedRows(info, 500);
		return SQLITE_OK;
	}

	@Override
	public int delete(MemorySegment vtab, sqlite3_values values) {
		log.info("{}.{}.xUpdate-delete({})", db(vtab), name(vtab), values.getObject(0));
		return 0;
	}

	@Override
	public Map.Entry<Integer, Long> insert(MemorySegment vtab, sqlite3_values values) {
		final String args = IntStream.range(1, values.getCount())
			.mapToObj(i -> String.format("argv[%s]=%s", i, values.getObject(i)))
			.collect(Collectors.joining(", "));
		log.info("{}.{}.xUpdate-insert({})", db(vtab), name(vtab), args);
		return Map.entry(SQLITE_OK, 0L);
	}

	@Override
	public int update(MemorySegment vtab, sqlite3_values values) {
		final String args = IntStream.range(0, values.getCount())
			.mapToObj(i -> String.format("argv[%s]=%s", i, values.getObject(i)))
			.collect(Collectors.joining(", "));
		log.info("{}.{}.xUpdate({})", db(vtab), name(vtab), args);
		return 0;
	}

	@Override
	public int disconnect(MemorySegment vtab, boolean isDestroy) {
		log.info(isDestroy ? "{}.{}.xDestroy()" : "{}.{}.xDisconnect()", db(vtab), name(vtab));
		sqlite3_free(zName(vtab));
		sqlite3_free(zDb(vtab));
		return UpdateModule.super.disconnect(vtab, isDestroy);
	}

	@Override
	public MemorySegment open(MemorySegment vtab) {
		int nCursor = nCursor(vtab);
		log.info("{}.{}.xOpen(cursor={})", db(vtab), name(vtab), nCursor);
		MemorySegment cursor = sqlite3_malloc(layout());
		if (isNull(cursor)) {
			return null;
		}
		iCursor(cursor, nCursor);
		return cursor;
	}

	@Override
	public int close(MemorySegment cursor) {
		cursor = cursor.reinterpret(layout().byteSize());
		MemorySegment vtab = sqlite3_vtab_cursor.pVtab(cursor, vtab_layout).asReadOnly();
		log.info("{}.{}.xClose(cursor={})", db(vtab), name(vtab), iCursor(cursor));
		return UpdateModule.super.close(cursor);
	}

	@Override
	public int filter(MemorySegment cursor, int idxNum, MemorySegment idxStr, sqlite3_values values) {
		MemorySegment vtab = sqlite3_vtab_cursor.pVtab(cursor, vtab_layout).asReadOnly();
		log.info("{}.{}.xFilter(cursor={})", db(vtab), name(vtab), iCursor(cursor));
		rowId(cursor, 0);
		return SQLITE_OK;
	}

	@Override
	public int next(MemorySegment cursor, long rowId) {
		MemorySegment vtab = sqlite3_vtab_cursor.pVtab(cursor, vtab_layout).asReadOnly();
		log.info("{}.{}.xNext(cursor={}) rowId {} -> {}", db(vtab), name(vtab), iCursor(cursor), rowId, rowId + 1);
		rowId(cursor, rowId + 1);
		return SQLITE_OK;
	}

	@Override
	public boolean isEof(MemorySegment cursor) {
		MemorySegment vtab = sqlite3_vtab_cursor.pVtab(cursor, vtab_layout).asReadOnly();
		boolean eof = rowId(cursor) >= nRow(vtab);
		log.info("{}.{}.xEof(cursor={}): {}", db(vtab), name(vtab), iCursor(cursor), eof);
		return eof;
	}

	@Override
	public int column(MemorySegment cursor, sqlite3_context sqlite3Context, int i) {
		String zVal;
		if (i < 26) {
			zVal = String.format("%s%d", "abcdefghijklmnopqrstuvwyz".charAt(i), rowId(cursor));
		} else {
			zVal = String.format("{%d}%d", i, rowId(cursor));
		}
		MemorySegment vtab = sqlite3_vtab_cursor.pVtab(cursor, vtab_layout).asReadOnly();
		log.info("{}.{}.xColumn(cursor={}, i={}): [{}]", db(vtab), name(vtab), iCursor(cursor), i, zVal);
		sqlite3Context.setResultText(zVal);
		return SQLITE_OK;
	}

	@Override
	public int rowId(MemorySegment cursor, MemorySegment p_rowid) {
		int rc = UpdateModule.super.rowId(cursor, p_rowid);
		cursor = cursor.reinterpret(layout().byteSize()).asReadOnly();
		MemorySegment vtab = sqlite3_vtab_cursor.pVtab(cursor, vtab_layout).asReadOnly();
		p_rowid = p_rowid.reinterpret(C_LONG_LONG.byteSize());
		log.info("{}.{}.xRowid(cursor={}): {}", db(vtab), name(vtab), iCursor(cursor), p_rowid.get(C_LONG_LONG, 0));
		return rc;
	}

	private static final GroupLayout layout = MemoryLayout.structLayout(
		sqlite3_vtab_cursor.layout.withName("base"), /* Base class - must be first */
		C_LONG_LONG.withName("rowId"), /* The rowid */
		C_INT.withName("iCursor"), /* Cursor number */
		MemoryLayout.paddingLayout(4)
	).withName("vtablog_cursor");
	private static final OfLong rowId = (OfLong) layout.select(groupElement("rowId"));
	@Override
	public long rowId(MemorySegment cursor) {
		return cursor.get(VTabLogModule.rowId, 8);
	}
	private static void rowId(MemorySegment cursor, long id) {
		cursor.set(rowId, 8, id);
	}
	private static final OfInt iCursor = (OfInt) layout.select(groupElement("iCursor"));
	private int iCursor(MemorySegment cursor) {
		return cursor.get(iCursor, 16);
	}
	private void iCursor(MemorySegment cursor, int nCursor) {
		cursor.set(iCursor, 16, nCursor);
	}
	@Override
	public MemoryLayout layout() {
		return layout;
	}

	private static final GroupLayout vtab_layout = MemoryLayout.structLayout(
		sqlite3_vtab.layout.withName("base"), /* Base class - must be first */
		C_POINTER.withName("zDb"), /* Schema name.  argv[1] of xConnect/xCreate */
		C_POINTER.withName("zName"), /* Table name.  argv[2] of xConnect/xCreate */
		C_INT.withName("nRow"), /* Number of rows in the table */
		C_INT.withName("nCursor") /* Number of cursors created */
	).withName("vtablog_vtab");
	private static final AddressLayout zDb = (AddressLayout)vtab_layout.select(groupElement("zDb"));
	private static MemorySegment zDb(MemorySegment vtab) {
		return vtab.get(zDb, 24);
	}
	private static void zDb(MemorySegment vtab, String s) {
		vtab.set(zDb, 24, sqlite3OwnedString(s));
	}
	private String db(MemorySegment vtab) {
		return getString(zDb(vtab));
	}
	private static final AddressLayout zName = (AddressLayout)vtab_layout.select(groupElement("zName"));
	private static MemorySegment zName(MemorySegment vtab) {
		return vtab.get(zName, 32);
	}
	private static void zName(MemorySegment vtab, String s) {
		vtab.set(zName, 32, sqlite3OwnedString(s));
	}
	private String name(MemorySegment vtab) {
		return getString(zName(vtab));
	}
	private static final OfInt nRow = (OfInt) vtab_layout.select(groupElement("nRow"));
	private int nRow(MemorySegment vtab) {
		return vtab.get(nRow, 40);
	}
	private static void nRow(MemorySegment vtab, int n) {
		vtab.set(nRow, 40, n);
	}
	private static final OfInt nCursor = (OfInt) vtab_layout.select(groupElement("nCursor"));
	private int nCursor(MemorySegment vtab) {
		int nc = vtab.get(nCursor, 44) + 1;
		vtab.set(nCursor, 44, nc);
		return nc;
	}
	@Override
	public MemoryLayout vtab_layout() {
		return vtab_layout;
	}
}
