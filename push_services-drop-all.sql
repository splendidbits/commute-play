alter table if exists push_services.credentials drop constraint if exists fk_credentials_message_id;

alter table if exists push_services.messages drop constraint if exists fk_messages_task_id;
drop index if exists ix_messages_task_id;

alter table if exists push_services.payload_element drop constraint if exists fk_payload_element_message_id;
drop index if exists ix_payload_element_message_id;

alter table if exists push_services.recipient_failures drop constraint if exists fk_recipient_failures_recipient_id;

alter table if exists push_services.recipients drop constraint if exists fk_recipients_message_id;
drop index if exists ix_recipients_message_id;

drop table if exists push_services.credentials cascade;
drop sequence if exists credentials_id_seq;

drop table if exists push_services.messages cascade;
drop sequence if exists message_id_seq;

drop table if exists push_services.payload_element cascade;
drop sequence if exists element_id_seq;

drop table if exists push_services.recipient_failures cascade;
drop sequence if exists failure_id_seq;

drop table if exists push_services.recipients cascade;
drop sequence if exists recipient_id_seq;

drop table if exists push_services.tasks cascade;
drop sequence if exists task_id_seq;

