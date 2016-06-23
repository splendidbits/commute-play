alter table if exists splendid_log.entries drop constraint if exists fk_entries_application_id;
drop index if exists ix_entries_application_id;

drop table if exists splendid_log.applications cascade;
drop sequence if exists log_application_id_seq;

drop table if exists splendid_log.entries cascade;
drop sequence if exists log_entry_gen;

