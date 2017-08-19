alter table if exists fluffylog.entries drop constraint if exists fk_entries_application_id;
drop index if exists ix_entries_application_id;

drop table if exists fluffylog.applications cascade;
drop sequence if exists log_application_id_seq;

drop table if exists fluffylog.entries cascade;
drop sequence if exists log_entry_gen;

