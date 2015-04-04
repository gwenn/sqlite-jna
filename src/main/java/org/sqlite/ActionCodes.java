package org.sqlite;

public interface ActionCodes {
  int SQLITE_CREATE_INDEX = 1;   /* Index Name      Table Name      */
  int SQLITE_CREATE_TABLE = 2;   /* Table Name      NULL            */
  int SQLITE_CREATE_TEMP_INDEX = 3;   /* Index Name      Table Name      */
  int SQLITE_CREATE_TEMP_TABLE = 4;   /* Table Name      NULL            */
  int SQLITE_CREATE_TEMP_TRIGGER = 5;   /* Trigger Name    Table Name      */
  int SQLITE_CREATE_TEMP_VIEW = 6;   /* View Name       NULL            */
  int SQLITE_CREATE_TRIGGER = 7;   /* Trigger Name    Table Name      */
  int SQLITE_CREATE_VIEW = 8;   /* View Name       NULL            */
  int SQLITE_DELETE = 9;   /* Table Name      NULL            */
  int SQLITE_DROP_INDEX = 10;   /* Index Name      Table Name      */
  int SQLITE_DROP_TABLE = 11;   /* Table Name      NULL            */
  int SQLITE_DROP_TEMP_INDEX = 12;   /* Index Name      Table Name      */
  int SQLITE_DROP_TEMP_TABLE = 13;   /* Table Name      NULL            */
  int SQLITE_DROP_TEMP_TRIGGER = 14;   /* Trigger Name    Table Name      */
  int SQLITE_DROP_TEMP_VIEW = 15;   /* View Name       NULL            */
  int SQLITE_DROP_TRIGGER = 16;   /* Trigger Name    Table Name      */
  int SQLITE_DROP_VIEW = 17;   /* View Name       NULL            */
  int SQLITE_INSERT = 18;   /* Table Name      NULL            */
  int SQLITE_PRAGMA = 19;   /* Pragma Name     1st arg or NULL */
  int SQLITE_READ = 20;   /* Table Name      Column Name     */
  int SQLITE_SELECT = 21;   /* NULL            NULL            */
  int SQLITE_TRANSACTION = 22;   /* Operation       NULL            */
  int SQLITE_UPDATE = 23;   /* Table Name      Column Name     */
  int SQLITE_ATTACH = 24;   /* Filename        NULL            */
  int SQLITE_DETACH = 25;   /* Database Name   NULL            */
  int SQLITE_ALTER_TABLE = 26;   /* Database Name   Table Name      */
  int SQLITE_REINDEX = 27;   /* Index Name      NULL            */
  int SQLITE_ANALYZE = 28;   /* Table Name      NULL            */
  int SQLITE_CREATE_VTABLE = 29;   /* Table Name      Module Name     */
  int SQLITE_DROP_VTABLE = 30;   /* Table Name      Module Name     */
  int SQLITE_FUNCTION = 31;   /* NULL            Function Name   */
  int SQLITE_SAVEPOINT = 32;   /* Operation       Savepoint Name  */
  int SQLITE_COPY = 0;   /* No longer used */
  int SQLITE_RECURSIVE = 33;   /* NULL            NULL            */
}
