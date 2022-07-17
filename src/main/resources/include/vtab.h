#ifndef VTAB_H
#define VTAB_H

#include <memory>
#include "sqlite3.h"

// https://github.com/bytedeco/javacpp/wiki/Mapping-Recipes#dealing-with-abstract-classes-and-virtual-methods

// https://github.com/mlin/GenomicSQLite/blob/main/src/SQLiteVirtualTable.hpp
// https://github.com/google/perfetto/tree/master/src/trace_processor/sqlite

class VirtualTable : public sqlite3_vtab {
public:
    // int (*xConnect)(sqlite3*, void *pAux,
    //             int argc, char *const*argv,
    //             sqlite3_vtab **ppVTab,
    //             char **pzErr);
    virtual int Connect(sqlite3* db, void* pAux, int argc,
                        const char* const* argv,
                        char** pzErr) = 0;
    // int (*xDisconnect)(sqlite3_vtab *pVTab);
    virtual int Disconnect() {
        delete this;
        return SQLITE_OK;
    }
    // int (*xBestIndex)(sqlite3_vtab *pVTab, sqlite3_index_info*);
    virtual int BestIndex(sqlite3_index_info *info) = 0;
    // int (*xOpen)(sqlite3_vtab *pVTab, sqlite3_vtab_cursor **ppCursor);
    virtual int Open(sqlite3_vtab_cursor **ppCursor) = 0;
protected:
    friend class VirtualTableCursor;
    /* Virtual table implementations will typically add additional fields */
    virtual ~VirtualTable() = default;
};


class VirtualTableCursor : public sqlite3_vtab_cursor {
public:
    // int (*xClose)(sqlite3_vtab_cursor*);
    virtual int Close() {
        delete this;
        return SQLITE_OK;
    }
    // int (*xFilter)(sqlite3_vtab_cursor*, int idxNum, const char *idxStr,
    //              int argc, sqlite3_value **argv);
    virtual int Filter(int idxNum, const char *idxStr, int argc, sqlite3_value **argv) = 0;
    // int (*xNext)(sqlite3_vtab_cursor*);
    virtual int Next() = 0;
    // int (*xEof)(sqlite3_vtab_cursor*);
    virtual int Eof() = 0;
    // int (*xColumn)(sqlite3_vtab_cursor*, sqlite3_context*, int N);
    virtual int Column(sqlite3_context *ctx, int colno) = 0;
    // int (*xRowid)(sqlite3_vtab_cursor *pCur, sqlite_int64 *pRowid);
    virtual int RowId(sqlite_int64 *pRowid) = 0;
protected:
    friend class VirtualTable;
    /* Virtual table implementations will typically add additional fields */
    virtual ~VirtualTableCursor() = default;
};

template <class TTable, class TCursor>
sqlite3_module EponymousOnlyModule() {
    sqlite3_module m;
    m.iVersion = 1;
    m.xCreate = nullptr;
    m.xConnect = [](sqlite3* xdb, void* arg, int argc,
                    const char* const* argv, sqlite3_vtab** tab,
                    char** pzErr) {
        auto* table = new TTable(); // TODO check alloc failure
        //char* schema = table;
        //int res = sqlite3_declare_vtab(xdb, schema);
        int res = table->Connect(xdb, arg, argc, argv, pzErr);
        if (res != SQLITE_OK) {
            delete table;
            return res;
        }
        *tab = table;
        return SQLITE_OK;
    };
    m.xBestIndex = [](sqlite3_vtab* t, sqlite3_index_info* i) {
        return static_cast<TTable*>(t)->BestIndex(i);
    };
    m.xDisconnect = [](sqlite3_vtab* t) {
        return static_cast<TTable*>(t)->Disconnect();
    };
    m.xDestroy = nullptr;
    m.xOpen = [](sqlite3_vtab* t, sqlite3_vtab_cursor** c) {
        return static_cast<TTable*>(t)->Open(c);
    };
    m.xClose = [](sqlite3_vtab_cursor* c) {
        return static_cast<TCursor*>(c)->Close();
    };
    m.xFilter = [](sqlite3_vtab_cursor* vc, int i, const char* s, int a,
                   sqlite3_value** v) {
        return static_cast<TCursor*>(vc)->Filter(i, s, a, v);
    };
    m.xNext = [](sqlite3_vtab_cursor* c) {
        return static_cast<TCursor*>(c)->Next();
    };
    m.xEof = [](sqlite3_vtab_cursor* c) {
        return static_cast<TCursor*>(c)->Eof();
    };
    m.xColumn = [](sqlite3_vtab_cursor* c, sqlite3_context* a, int i) {
        return static_cast<TCursor*>(c)->Column(a, i);
    };
    m.xRowid = [](sqlite3_vtab_cursor* c, sqlite3_int64* r) {
        return static_cast<TCursor*>(c)->RowId(r);
    };
    return m;
}

#endif //VTAB_H
