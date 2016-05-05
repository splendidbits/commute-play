# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table api_accounts.accounts (
  id                            integer not null,
  organisation_name             varchar(255),
  account_email                 varchar(255),
  password_hash                 varchar(255),
  api_key                       varchar(255),
  estimated_limit_day           bigint,
  message_limit_day             bigint,
  active                        boolean default false,
  time_enrolled                 timestamp without time zone,
  constraint pk_accounts primary key (id)
);
create sequence account_id_seq increment by 1;

create table agency_updates.agencies (
  id                            serial not null,
  name                          varchar(255),
  phone                         varchar(255),
  external_uri                  TEXT,
  utc_offset                    float,
  constraint pk_agencies primary key (id)
);

create table agency_updates.alerts (
  id                            bigint not null,
  route_id                      varchar(255),
  type                          varchar(11),
  level                         varchar(8),
  message_title                 TEXT,
  message_subtitle              TEXT,
  message_body                  TEXT,
  external_uri                  TEXT,
  last_updated                  timestamp without time zone,
  constraint ck_alerts_type check (type in ('','DISRUPTION','APP','WEATHER','DETOUR','MAINTENANCE','INFORMATION')),
  constraint ck_alerts_level check (level in ('NORMAL','HIGH','SILENT','CRITICAL','LOW')),
  constraint pk_alerts primary key (id)
);
create sequence alert_id_seq increment by 1;

create table task_queue.credentials (
  authorization_key             varchar(255) not null,
  package_uri                   varchar(255),
  certificate_body              TEXT,
  endpoint_url                  varchar(255),
  restricted_package_name       varchar(255),
  constraint pk_credentials primary key (authorization_key)
);

create table device_information.devices (
  id                            bigint not null,
  device_id                     varchar(255),
  token                         varchar(255),
  app_key                       varchar(255),
  user_key                      varchar(255),
  account_id                    integer,
  time_registered               timestamp without time zone,
  constraint pk_devices primary key (id)
);
create sequence device_id_seq increment by 1;

create table agency_updates.locations (
  id                            integer not null,
  alert_id                      bigint,
  name                          varchar(255),
  latitude                      varchar(255),
  longitude                     varchar(255),
  message                       TEXT,
  sequence                      integer,
  date                          timestamp without time zone,
  constraint pk_locations primary key (id)
);
create sequence location_id_seq increment by 1;

create table task_queue.messages (
  id                            bigint not null,
  task_id                       bigint,
  auth_id                       varchar(255),
  platform                      varchar(4),
  collapse_key                  varchar(255),
  priority                      varchar(6),
  time_to_live                  integer,
  delay_while_idle              boolean,
  dry_run                       boolean,
  sent_time                     timestamp without time zone,
  constraint ck_messages_platform check (platform in ('GCM','APNS')),
  constraint ck_messages_priority check (priority in ('normal','low','high')),
  constraint pk_messages primary key (id)
);
create sequence message_id_seq increment by 1;

create table task_queue.payload_element (
  id                            bigint not null,
  element_name                  varchar(255),
  element_value                 varchar(255),
  message_id                    bigint,
  constraint pk_payload_element primary key (id)
);
create sequence element_id_seq increment by 1;

create table api_accounts.platforms (
  platform_id                   varchar(4) not null,
  endpoint_url                  varchar(255),
  constraint ck_platforms_platform_id check (platform_id in ('GCM','APNS')),
  constraint pk_platforms primary key (platform_id)
);

create table api_accounts.platform_accounts (
  id                            integer not null,
  account_id                    integer,
  package_uri                   varchar(255),
  authorization_key             varchar(255),
  certificate_body              TEXT,
  platform_type                 varchar(4),
  constraint pk_platform_accounts primary key (id)
);
create sequence platform_account_id_seq increment by 1;

create table task_queue.recipients (
  id                            bigint not null,
  token                         varchar(255),
  message_id                    bigint,
  state                         varchar(13),
  time_added                    timestamp without time zone,
  constraint ck_recipients_state check (state in ('WAITING_RETRY','COMPLETE','FAILED','IDLE','PROCESSING')),
  constraint pk_recipients primary key (id)
);
create sequence recipient_id_seq increment by 1;

create table task_queue.recipient_failures (
  id                            bigint not null,
  failure                       varchar(28),
  recipient_id                  bigint,
  fail_time                     timestamp without time zone,
  constraint ck_recipient_failures_failure check (failure in ('ERROR_MISSING_REG_TOKEN','ERROR_INVALID_REGISTRATION','ERROR_NOT_REGISTERED','ERROR_INVALID_PACKAGE_NAME','ERROR_MISMATCHED_SENDER_ID','ERROR_MESSAGE_TO_BIG','ERROR_INVALID_DATA','ERROR_INVALID_TTL','ERROR_TOO_MANY_RETRIES','ERROR_EXCEEDED_MESSAGE_LIMIT')),
  constraint uq_recipient_failures_recipient_id unique (recipient_id),
  constraint pk_recipient_failures primary key (id)
);
create sequence failure_id_seq increment by 1;

create table agency_updates.routes (
  id                            varchar(255) not null,
  route_id                      varchar(255),
  route_name                    varchar(255),
  route_flag                    varchar(18),
  transit_type                  varchar(10),
  is_default                    boolean,
  is_sticky                     boolean,
  external_uri                  TEXT,
  agency_id                     integer,
  constraint ck_routes_route_flag check (route_flag in ('PRIVATE','TEMPORARY_ROUTE','OWL','CLOSED_PERMANENTLY','CLOSED_TEMPORARILY')),
  constraint ck_routes_transit_type check (transit_type in ('SPECIAL','BUS','SUBWAY','CABLE','FERRY','BIKE_SHARE','RAIL','LIGHT_RAIL')),
  constraint pk_routes primary key (id)
);
create sequence route_id_seq increment by 1;

create table device_information.subscriptions (
  id                            bigint not null,
  device_id                     bigint not null,
  route_id                      varchar(255),
  time_subscribed               timestamp without time zone,
  constraint pk_subscriptions primary key (id)
);
create sequence subscriptions_id_seq increment by 1;

create table task_queue.tasks (
  id                            bigint not null,
  retry_count                   integer,
  name                          varchar(255),
  state                         varchar(25),
  task_added                    timestamp without time zone,
  last_attempt                  timestamp,
  next_attempt                  timestamp,
  constraint ck_tasks_state check (state in ('COMPLETE','FAILED','STATE_PARTIALLY_PROCESSED','IDLE','PROCESSING')),
  constraint pk_tasks primary key (id)
);
create sequence task_id_seq increment by 1;

alter table agency_updates.alerts add constraint fk_alerts_route_id foreign key (route_id) references agency_updates.routes (id) on delete restrict on update restrict;
create index ix_alerts_route_id on agency_updates.alerts (route_id);

alter table device_information.devices add constraint fk_devices_account_id foreign key (account_id) references api_accounts.accounts (id) on delete restrict on update restrict;
create index ix_devices_account_id on device_information.devices (account_id);

alter table agency_updates.locations add constraint fk_locations_alert_id foreign key (alert_id) references agency_updates.alerts (id) on delete restrict on update restrict;
create index ix_locations_alert_id on agency_updates.locations (alert_id);

alter table task_queue.messages add constraint fk_messages_task_id foreign key (task_id) references task_queue.tasks (id) on delete restrict on update restrict;
create index ix_messages_task_id on task_queue.messages (task_id);

alter table task_queue.messages add constraint fk_messages_auth_id foreign key (auth_id) references task_queue.credentials (authorization_key) on delete restrict on update restrict;
create index ix_messages_auth_id on task_queue.messages (auth_id);

alter table task_queue.payload_element add constraint fk_payload_element_message_id foreign key (message_id) references task_queue.messages (id) on delete restrict on update restrict;
create index ix_payload_element_message_id on task_queue.payload_element (message_id);

alter table api_accounts.platform_accounts add constraint fk_platform_accounts_account_id foreign key (account_id) references api_accounts.accounts (id) on delete restrict on update restrict;
create index ix_platform_accounts_account_id on api_accounts.platform_accounts (account_id);

alter table api_accounts.platform_accounts add constraint fk_platform_accounts_platform_type foreign key (platform_type) references api_accounts.platforms (platform_id) on delete restrict on update restrict;
create index ix_platform_accounts_platform_type on api_accounts.platform_accounts (platform_type);

alter table task_queue.recipients add constraint fk_recipients_message_id foreign key (message_id) references task_queue.messages (id) on delete restrict on update restrict;
create index ix_recipients_message_id on task_queue.recipients (message_id);

alter table task_queue.recipient_failures add constraint fk_recipient_failures_recipient_id foreign key (recipient_id) references task_queue.recipients (id) on delete restrict on update restrict;

alter table agency_updates.routes add constraint fk_routes_agency_id foreign key (agency_id) references agency_updates.agencies (id) on delete restrict on update restrict;
create index ix_routes_agency_id on agency_updates.routes (agency_id);

alter table device_information.subscriptions add constraint fk_subscriptions_device_id foreign key (device_id) references device_information.devices (id) on delete restrict on update restrict;
create index ix_subscriptions_device_id on device_information.subscriptions (device_id);

alter table device_information.subscriptions add constraint fk_subscriptions_route_id foreign key (route_id) references agency_updates.routes (id) on delete restrict on update restrict;
create index ix_subscriptions_route_id on device_information.subscriptions (route_id);


# --- !Downs

alter table if exists agency_updates.alerts drop constraint if exists fk_alerts_route_id;
drop index if exists ix_alerts_route_id;

alter table if exists device_information.devices drop constraint if exists fk_devices_account_id;
drop index if exists ix_devices_account_id;

alter table if exists agency_updates.locations drop constraint if exists fk_locations_alert_id;
drop index if exists ix_locations_alert_id;

alter table if exists task_queue.messages drop constraint if exists fk_messages_task_id;
drop index if exists ix_messages_task_id;

alter table if exists task_queue.messages drop constraint if exists fk_messages_auth_id;
drop index if exists ix_messages_auth_id;

alter table if exists task_queue.payload_element drop constraint if exists fk_payload_element_message_id;
drop index if exists ix_payload_element_message_id;

alter table if exists api_accounts.platform_accounts drop constraint if exists fk_platform_accounts_account_id;
drop index if exists ix_platform_accounts_account_id;

alter table if exists api_accounts.platform_accounts drop constraint if exists fk_platform_accounts_platform_type;
drop index if exists ix_platform_accounts_platform_type;

alter table if exists task_queue.recipients drop constraint if exists fk_recipients_message_id;
drop index if exists ix_recipients_message_id;

alter table if exists task_queue.recipient_failures drop constraint if exists fk_recipient_failures_recipient_id;

alter table if exists agency_updates.routes drop constraint if exists fk_routes_agency_id;
drop index if exists ix_routes_agency_id;

alter table if exists device_information.subscriptions drop constraint if exists fk_subscriptions_device_id;
drop index if exists ix_subscriptions_device_id;

alter table if exists device_information.subscriptions drop constraint if exists fk_subscriptions_route_id;
drop index if exists ix_subscriptions_route_id;

drop table if exists api_accounts.accounts cascade;
drop sequence if exists account_id_seq;

drop table if exists agency_updates.agencies cascade;

drop table if exists agency_updates.alerts cascade;
drop sequence if exists alert_id_seq;

drop table if exists task_queue.credentials cascade;

drop table if exists device_information.devices cascade;
drop sequence if exists device_id_seq;

drop table if exists agency_updates.locations cascade;
drop sequence if exists location_id_seq;

drop table if exists task_queue.messages cascade;
drop sequence if exists message_id_seq;

drop table if exists task_queue.payload_element cascade;
drop sequence if exists element_id_seq;

drop table if exists api_accounts.platforms cascade;

drop table if exists api_accounts.platform_accounts cascade;
drop sequence if exists platform_account_id_seq;

drop table if exists task_queue.recipients cascade;
drop sequence if exists recipient_id_seq;

drop table if exists task_queue.recipient_failures cascade;
drop sequence if exists failure_id_seq;

drop table if exists agency_updates.routes cascade;
drop sequence if exists route_id_seq;

drop table if exists device_information.subscriptions cascade;
drop sequence if exists subscriptions_id_seq;

drop table if exists task_queue.tasks cascade;
drop sequence if exists task_id_seq;

