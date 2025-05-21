- [ ] `sqlite3_version`
- [X] `sqlite3_libversion`
- [ ] `sqlite3_sourceid`
- [X] `sqlite3_libversion_number`

- [X] `sqlite3_compileoption_used`
- [X] `sqlite3_compileoption_get`

- [X] `sqlite3_threadsafe` (internal use only)

- [X] `sqlite3_close`
- [X] `sqlite3_close_v2`

- [X] `sqlite3_exec`

- [X] `sqlite3_initialize` (used)
- [X] `sqlite3_shutdown`
- [ ] `sqlite3_os_init`
- [ ] `sqlite3_os_end`

- [X] `sqlite3_config` (partially)
- [X] `sqlite3_db_config`

- [X] `sqlite3_extended_result_codes`

- [X] `sqlite3_last_insert_rowid`
- [ ] `sqlite3_set_last_insert_rowid`

- [X] `sqlite3_changes`
- [X] `sqlite3_changes64`
- [X] `sqlite3_total_changes`
- [X] `sqlite3_total_changes64`

- [X] `sqlite3_interrupt`
- [ ] `sqlite3_is_interrupted`

- [ ] `sqlite3_complete`

- [X] `sqlite3_busy_handler`
- [X] `sqlite3_busy_timeout`

- [ ] `sqlite3_get_table`

- [ ] `sqlite3_mprintf`
- [ ] `sqlite3_vmprintf`
- [ ] `sqlite3_snprintf`
- [ ] `sqlite3_vsnprintf`

- [X] `sqlite3_malloc`
- [ ] `sqlite3_malloc64`
- [ ] `sqlite3_realloc`
- [ ] `sqlite3_realloc64`
- [X] `sqlite3_free`
- [ ] `sqlite3_msize`

- [ ] `sqlite3_memory_used`
- [ ] `sqlite3_memory_highwater`

- [ ] `sqlite3_randomness`

- [X] `sqlite3_set_authorizer`
- [X] `sqlite3_trace`
- [X] `sqlite3_profile`
- [ ] `sqlite3_trace_v2`
- [X] `sqlite3_progress_handler`

- [ ] `sqlite3_open`
- [X] `sqlite3_open_v2`
- [ ] `sqlite3_uri_parameter`
- [ ] `sqlite3_uri_boolean`
- [ ] `sqlite3_uri_int64`
- [ ] `sqlite3_uri_key`

- [ ] `sqlite3_filename_database`
- [ ] `sqlite3_filename_journal`
- [ ] `sqlite3_filename_wal`
- [ ] `sqlite3_database_file_object`
- [ ] `sqlite3_create_filename`
- [ ] `sqlite3_free_filename`

- [X] `sqlite3_errcode`
- [X] `sqlite3_extended_errcode`
- [X] `sqlite3_errmsg`
- [X] `sqlite3_errstr`
- [ ] `sqlite3_error_offset`

- [ ] `sqlite3_limit`

- [ ] `sqlite3_prepare`
- [ ] `sqlite3_prepare_v2`
- [X] `sqlite3_prepare_v3`

- [X] `sqlite3_sql`
- [X] `sqlite3_expanded_sql`
- [ ] `sqlite3_normalized_sql`

- [X] `sqlite3_stmt_readonly`
- [ ] `sqlite3_stmt_isexplain`
- [ ] `sqlite3_stmt_explain`
- [X] `sqlite3_stmt_busy`

- [X] `sqlite3_bind_blob`
- [ ] `sqlite3_bind_blob64`
- [X] `sqlite3_bind_double`
- [X] `sqlite3_bind_int`
- [X] `sqlite3_bind_int64`
- [X] `sqlite3_bind_null`
- [X] `sqlite3_bind_text`
- [ ] `sqlite3_bind_text64`
- [ ] `sqlite3_bind_value`
- [ ] `sqlite3_bind_pointer`
- [X] `sqlite3_bind_zeroblob`
- [ ] `sqlite3_bind_zeroblob64`

- [X] `sqlite3_bind_parameter_count`
- [X] `sqlite3_bind_parameter_name`
- [X] `sqlite3_bind_parameter_index`
- [X] `sqlite3_clear_bindings`

- [X] `sqlite3_column_count`
- [ ] `sqlite3_data_count`
- [X] `sqlite3_column_name`
- [X] `sqlite3_column_database_name`
- [X] `sqlite3_column_table_name`
- [X] `sqlite3_column_origin_name`
- [X] `sqlite3_column_decltype`

- [X] `sqlite3_step`

- [X] `sqlite3_column_blob`
- [X] `sqlite3_column_double`
- [X] `sqlite3_column_int`
- [X] `sqlite3_column_int64`
- [X] `sqlite3_column_text`
- [ ] `sqlite3_column_value`
- [ ] `sqlite3_column_bytes`
- [X] `sqlite3_column_type`

- [X] `sqlite3_finalize`
- [X] `sqlite3_reset`

- [ ] `sqlite3_create_function`
- [X] `sqlite3_create_function_v2`
- [ ] `sqlite3_create_window_function`

- [X] `sqlite3_value_blob`
- [X] `sqlite3_value_double`
- [X] `sqlite3_value_int`
- [X] `sqlite3_value_int64`
- [ ] `sqlite3_value_pointer`
- [X] `sqlite3_value_text`
- [X] `sqlite3_value_bytes` (internal use only)
- [X] `sqlite3_value_type` (unused)
- [X] `sqlite3_value_numeric_type`
- [ ] `sqlite3_value_nochange`
- [ ] `sqlite3_value_frombind`
- [ ] `sqlite3_value_encoding`
- [ ] `sqlite3_value_subtype`

- [ ] `sqlite3_value_dup`
- [ ] `sqlite3_value_free`

- [X] `sqlite3_aggregate_context`
- [ ] `sqlite3_user_data`
- [X] `sqlite3_context_db_handle` (unused)
- [X] `sqlite3_get_auxdata` (unused)
- [X] `sqlite3_set_auxdata` (unused)
- [ ] `sqlite3_get_clientdata`
- [ ] `sqlite3_set_clientdata`

- [X] `sqlite3_result_blob` (unused)
- [ ] `sqlite3_result_blob64`
- [X] `sqlite3_result_double` (unused)
- [X] `sqlite3_result_error` (unused)
- [X] `sqlite3_result_error_toobig` (unused)
- [X] `sqlite3_result_error_nomem` (unused)
- [X] `sqlite3_result_error_code` (unused)
- [X] `sqlite3_result_int`
- [X] `sqlite3_result_int64`
- [X] `sqlite3_result_null`
- [X] `sqlite3_result_text` (unused)
- [ ] `sqlite3_result_text64`
- [ ] `sqlite3_result_value`
- [ ] `sqlite3_result_pointer`
- [X] `sqlite3_result_zeroblob` (unused)
- [ ] `sqlite3_result_zeroblob64`
- [ ] `sqlite3_result_subtype`

- [ ] `sqlite3_create_collation`
- [ ] `sqlite3_create_collation_v2`
- [ ] `sqlite3_collation_needed`

- [ ] `sqlite3_sleep`

- [X] `sqlite3_get_autocommit`

- [ ] `sqlite3_db_handle`
- [ ] `sqlite3_db_name`
- [X] `sqlite3_db_filename`
- [X] `sqlite3_db_readonly`
- [ ] `sqlite3_txn_state`
- [X] `sqlite3_next_stmt` (internal use only)

- [ ] `sqlite3_commit_hook`
- [ ] `sqlite3_rollback_hook`
- [ ] `sqlite3_autovacuum_pages`
- [X] `sqlite3_update_hook`

- [ ] `sqlite3_enable_shared_cache`
- [ ] `sqlite3_release_memory`
- [ ] `sqlite3_db_release_memory`
- [ ] `sqlite3_soft_heap_limit64`
- [ ] `sqlite3_hard_heap_limit64`

- [X] `sqlite3_table_column_metadata`

- [X] `sqlite3_load_extension`
- [X] `sqlite3_enable_load_extension`
- [ ] `sqlite3_auto_extension`
- [ ] `sqlite3_reset_auto_extension`

- [ ] `sqlite3_create_module`
- [ ] `sqlite3_create_module_v2`
- [ ] `sqlite3_drop_modules`
- [ ] `sqlite3_declare_vtab`
- [ ] `sqlite3_overload_function`

- [X] `sqlite3_blob_open`
- [X] `sqlite3_blob_reopen`
- [X] `sqlite3_blob_close`
- [X] `sqlite3_blob_bytes`
- [X] `sqlite3_blob_read`
- [X] `sqlite3_blob_write`

- [ ] `sqlite3_vfs_find`
- [ ] `sqlite3_vfs_register`
- [ ] `sqlite3_vfs_unregister`

- [ ] `sqlite3_mutex_alloc`
- [ ] `sqlite3_mutex_free`
- [ ] `sqlite3_mutex_enter`
- [ ] `sqlite3_mutex_try`
- [ ] `sqlite3_mutex_leave`
- [ ] `sqlite3_mutex_held`
- [ ] `sqlite3_mutex_notheld`
- [ ] `sqlite3_db_mutex`

- [ ] `sqlite3_file_control`
- [ ] `sqlite3_test_control`

- [ ] `sqlite3_keyword_count`
- [ ] `sqlite3_keyword_name`
- [ ] `sqlite3_keyword_check`

- [ ] `sqlite3_str_new`
- [ ] `sqlite3_str_finish`
- [ ] `sqlite3_str_append`
- [ ] `sqlite3_str_reset`
- [ ] `sqlite3_str_errcode`
- [ ] `sqlite3_str_length`
- [ ] `sqlite3_str_value`

- [ ] `sqlite3_status`
- [ ] `sqlite3_status64`
- [X] `sqlite3_db_status` (unused)
- [X] `sqlite3_stmt_status`

- [X] `sqlite3_backup_init`
- [X] `sqlite3_backup_step`
- [X] `sqlite3_backup_finish`
- [X] `sqlite3_backup_remaining`
- [X] `sqlite3_backup_pagecount`

- [X] `sqlite3_unlock_notify`

- [ ] `sqlite3_stricmp`
- [ ] `sqlite3_strnicmp`
- [ ] `sqlite3_strglob`
- [ ] `sqlite3_strlike`

- [X] `sqlite3_log`

- [ ] `sqlite3_wal_hook`
- [ ] `sqlite3_wal_autocheckpoint`
- [ ] `sqlite3_wal_checkpoint`
- [ ] `sqlite3_wal_checkpoint_v2`

- [ ] `sqlite3_vtab_config`
- [ ] `sqlite3_vtab_on_conflict`
- [ ] `sqlite3_vtab_nochange`
- [ ] `sqlite3_vtab_collation`
- [ ] `sqlite3_vtab_distinct`
- [ ] `sqlite3_vtab_in`
- [ ] `sqlite3_vtab_in_first`
- [ ] `sqlite3_vtab_in_next`
- [ ] `sqlite3_vtab_rhs_value`

- [X] `sqlite3_stmt_scanstatus` (unused)
- [ ] `sqlite3_stmt_scanstatus_v2`
- [X] `sqlite3_stmt_scanstatus_reset` (unused)

- [ ] `sqlite3_db_cacheflush`

- [ ] `sqlite3_preupdate_hook`
- [ ] `sqlite3_preupdate_old`
- [ ] `sqlite3_preupdate_count`
- [ ] `sqlite3_preupdate_depth`
- [ ] `sqlite3_preupdate_new`
- [ ] `sqlite3_preupdate_blobwrite`

- [ ] `sqlite3_system_errno`

- [ ] `sqlite3_snapshot_get`
- [ ] `sqlite3_snapshot_open`
- [ ] `sqlite3_snapshot_free`
- [ ] `sqlite3_snapshot_cmp`
- [ ] `sqlite3_snapshot_recover`

- [ ] `sqlite3_serialize`
- [ ] `sqlite3_deserialize`

- [ ] `sqlite3_rtree_geometry_callback`
- [ ] `sqlite3_rtree_query_callback`

- [ ] `sqlite3session_create`
- [ ] `sqlite3session_delete`
- [ ] `sqlite3session_object_config`
- [ ] `sqlite3session_enable`
- [ ] `sqlite3session_indirect`
- [ ] `sqlite3session_attach`
- [ ] `sqlite3session_table_filter`
- [ ] `sqlite3session_changeset`
- [ ] `sqlite3session_changeset_size`
- [ ] `sqlite3session_diff`
- [ ] `sqlite3session_patchset`
- [ ] `sqlite3session_isempty`
- [ ] `sqlite3session_memory_used`
- [ ] `sqlite3changeset_start`
- [ ] `sqlite3changeset_start_v2`
- [ ] `sqlite3changeset_next`
- [ ] `sqlite3changeset_op`
- [ ] `sqlite3changeset_pk`
- [ ] `sqlite3changeset_old`
- [ ] `sqlite3changeset_new`
- [ ] `sqlite3changeset_conflict`
- [ ] `sqlite3changeset_fk_conflicts`
- [ ] `sqlite3changeset_finalize`
- [ ] `sqlite3changeset_invert`
- [ ] `sqlite3changeset_concat`
- [ ] `sqlite3changeset_upgrade`
- [ ] `sqlite3changegroup_new`
- [ ] `sqlite3changegroup_schema`
- [ ] `sqlite3changegroup_add`
- [ ] `sqlite3changegroup_add_change`
- [ ] `sqlite3changegroup_output`
- [ ] `sqlite3changegroup_delete`
- [ ] `sqlite3changeset_apply`
- [ ] `sqlite3changeset_apply_v2`
- [ ] `sqlite3rebaser_create`
- [ ] `sqlite3rebaser_configure`
- [ ] `sqlite3rebaser_rebase`
- [ ] `sqlite3rebaser_delete`
- [ ] `sqlite3changeset_apply_strm`
- [ ] `sqlite3changeset_apply_v2_strm`
- [ ] `sqlite3changeset_concat_strm`
- [ ] `sqlite3changeset_invert_strm`
- [ ] `sqlite3changeset_start_strm`
- [ ] `sqlite3changeset_start_v2_strm`
- [ ] `sqlite3session_changeset_strm`
- [ ] `sqlite3session_patchset_strm`
- [ ] `sqlite3changegroup_add_strm`
- [ ] `sqlite3changegroup_add_strm`
- [ ] `sqlite3changegroup_output_strm`
- [ ] `sqlite3rebaser_rebase_strm`
- [ ] `sqlite3session_config`
