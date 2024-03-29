create table pushservices.credentials (
  id                            bigint not null,
  message_id                    bigint,
  platform                      varchar(4),
  authorisation_key             TEXT,
  certificate_body              TEXT,
  package_uri                   TEXT,
  constraint ck_credentials_platform check ( platform in ('GCM','APNS')),
  constraint uq_credentials_message_id unique (message_id),
  constraint pk_credentials primary key (id)
);
create sequence credentials_id_seq increment by 1;

create table pushservices.messages (
  id                            bigint not null,
  task_id                       bigint,
  collapse_key                  varchar(255),
  priority                      varchar(6),
  ttl_seconds                   integer not null,
  delay_while_idle              boolean default false not null,
  dry_run                       boolean default false not null,
  maximum_retries               integer not null,
  sent_time                     timestamp without time zone,
  constraint ck_messages_priority check ( priority in ('normal','low','high')),
  constraint pk_messages primary key (id)
);
create sequence message_id_seq increment by 1;

create table pushservices.payload_element (
  id                            bigint not null,
  element_name                  varchar(255),
  element_value                 TEXT,
  message_id                    bigint,
  constraint pk_payload_element primary key (id)
);
create sequence element_id_seq increment by 1;

create table pushservices.recipient_failures (
  id                            bigint not null,
  recipient_id                  bigint,
  type                          varchar(30),
  message                       varchar(255),
  fail_time                     timestamp without time zone,
  constraint ck_recipient_failures_type check ( type in ('TEMPORARILY_UNAVAILABLE','PLATFORM_LIMIT_EXCEEDED','MESSAGE_PAYLOAD_INVALID','MESSAGE_TTL_INVALID','MESSAGE_PACKAGE_INVALID','RECIPIENT_RATE_EXCEEDED','PLATFORM_AUTH_MISMATCHED','RECIPIENT_REGISTRATION_INVALID','MESSAGE_TOO_LARGE','RECIPIENT_NOT_REGISTERED','PLATFORM_AUTH_INVALID','MESSAGE_REGISTRATIONS_MISSING','ERROR_UNKNOWN')),
  constraint uq_recipient_failures_recipient_id unique (recipient_id),
  constraint pk_recipient_failures primary key (id)
);
create sequence failure_id_seq increment by 1;

create table pushservices.recipients (
  id                            bigint not null,
  token                         TEXT,
  message_id                    bigint,
  state                         varchar(13),
  time_added                    timestamp without time zone,
  send_attempts                 integer not null,
  previous_attempt              timestamp without time zone,
  next_attempt                  timestamp without time zone,
  constraint ck_recipients_state check ( state in ('WAITING_RETRY','COMPLETE','FAILED','IDLE','PROCESSING')),
  constraint pk_recipients primary key (id)
);
create sequence recipient_id_seq increment by 1;

create table pushservices.tasks (
  id                            bigint not null,
  name                          varchar(255),
  priority                      integer not null,
  added_time                    timestamp without time zone,
  constraint pk_tasks primary key (id)
);
create sequence task_id_seq increment by 1;

alter table pushservices.credentials add constraint fk_credentials_message_id foreign key (message_id) references pushservices.messages (id) on delete restrict on update restrict;

alter table pushservices.messages add constraint fk_messages_task_id foreign key (task_id) references pushservices.tasks (id) on delete restrict on update restrict;
create index ix_messages_task_id on pushservices.messages (task_id);

alter table pushservices.payload_element add constraint fk_payload_element_message_id foreign key (message_id) references pushservices.messages (id) on delete restrict on update restrict;
create index ix_payload_element_message_id on pushservices.payload_element (message_id);

alter table pushservices.recipient_failures add constraint fk_recipient_failures_recipient_id foreign key (recipient_id) references pushservices.recipients (id) on delete restrict on update restrict;

alter table pushservices.recipients add constraint fk_recipients_message_id foreign key (message_id) references pushservices.messages (id) on delete restrict on update restrict;
create index ix_recipients_message_id on pushservices.recipients (message_id);

