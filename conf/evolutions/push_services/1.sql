# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table push_services.credentials (
  id                            bigint not null,
  message_id                    bigint,
  platform                      varchar(4),
  authorisation_key             TEXT,
  certificate_body              TEXT,
  package_uri                   TEXT,
  constraint ck_credentials_platform check (platform in ('GCM','APNS')),
  constraint uq_credentials_message_id unique (message_id),
  constraint pk_credentials primary key (id)
);
create sequence credentials_id_seq increment by 1;

create table push_services.messages (
  id                            bigint not null,
  task_id                       bigint,
  collapse_key                  varchar(255),
  priority                      varchar(6),
  ttl_seconds                   integer,
  delay_while_idle              boolean,
  dry_run                       boolean,
  maximum_retries               integer,
  sent_time                     timestamp without time zone,
  constraint ck_messages_priority check (priority in ('normal','low','high')),
  constraint pk_messages primary key (id)
);
create sequence message_id_seq increment by 1;

create table push_services.payload_element (
  id                            bigint not null,
  element_name                  varchar(255),
  element_value                 TEXT,
  message_id                    bigint,
  constraint pk_payload_element primary key (id)
);
create sequence element_id_seq increment by 1;

create table push_services.recipients (
  id                            bigint not null,
  token                         TEXT,
  message_id                    bigint,
  state                         varchar(13),
  time_added                    timestamp without time zone,
  send_attempts                 integer,
  previous_attempt              timestamp,
  next_attempt                  timestamp,
  constraint ck_recipients_state check (state in ('WAITING_RETRY','COMPLETE','FAILED','IDLE','PROCESSING')),
  constraint pk_recipients primary key (id)
);
create sequence recipient_id_seq increment by 1;

create table push_services.recipient_failures (
  id                            bigint not null,
  recipient_id                  bigint,
  type                          varchar(30),
  message                       varchar(255),
  fail_time                     timestamp without time zone,
  constraint ck_recipient_failures_type check (type in ('PLATFORM_LIMIT_EXCEEDED','MESSAGE_PAYLOAD_INVALID','MESSAGE_TTL_INVALID','MESSAGE_PACKAGE_INVALID','RECIPIENT_RATE_EXCEEDED','PLATFORM_AUTH_MISMATCHED','RECIPIENT_REGISTRATION_INVALID','MESSAGE_TOO_LARGE','PLATFORM_UNAVAILABLE','RECIPIENT_NOT_REGISTERED','PLATFORM_AUTH_INVALID','MESSAGE_REGISTRATIONS_MISSING','ERROR_UNKNOWN')),
  constraint uq_recipient_failures_recipient_id unique (recipient_id),
  constraint pk_recipient_failures primary key (id)
);
create sequence failure_id_seq increment by 1;

create table push_services.tasks (
  id                            bigint not null,
  name                          varchar(255),
  priority                      integer,
  last_updated                  timestamp without time zone,
  constraint pk_tasks primary key (id)
);
create sequence task_id_seq increment by 1;

alter table push_services.credentials add constraint fk_credentials_message_id foreign key (message_id) references push_services.messages (id) on delete restrict on update restrict;

alter table push_services.messages add constraint fk_messages_task_id foreign key (task_id) references push_services.tasks (id) on delete restrict on update restrict;
create index ix_messages_task_id on push_services.messages (task_id);

alter table push_services.payload_element add constraint fk_payload_element_message_id foreign key (message_id) references push_services.messages (id) on delete restrict on update restrict;
create index ix_payload_element_message_id on push_services.payload_element (message_id);

alter table push_services.recipients add constraint fk_recipients_message_id foreign key (message_id) references push_services.messages (id) on delete restrict on update restrict;
create index ix_recipients_message_id on push_services.recipients (message_id);

alter table push_services.recipient_failures add constraint fk_recipient_failures_recipient_id foreign key (recipient_id) references push_services.recipients (id) on delete restrict on update restrict;


# --- !Downs

alter table if exists push_services.credentials drop constraint if exists fk_credentials_message_id;

alter table if exists push_services.messages drop constraint if exists fk_messages_task_id;
drop index if exists ix_messages_task_id;

alter table if exists push_services.payload_element drop constraint if exists fk_payload_element_message_id;
drop index if exists ix_payload_element_message_id;

alter table if exists push_services.recipients drop constraint if exists fk_recipients_message_id;
drop index if exists ix_recipients_message_id;

alter table if exists push_services.recipient_failures drop constraint if exists fk_recipient_failures_recipient_id;

drop table if exists push_services.credentials cascade;
drop sequence if exists credentials_id_seq;

drop table if exists push_services.messages cascade;
drop sequence if exists message_id_seq;

drop table if exists push_services.payload_element cascade;
drop sequence if exists element_id_seq;

drop table if exists push_services.recipients cascade;
drop sequence if exists recipient_id_seq;

drop table if exists push_services.recipient_failures cascade;
drop sequence if exists failure_id_seq;

drop table if exists push_services.tasks cascade;
drop sequence if exists task_id_seq;

