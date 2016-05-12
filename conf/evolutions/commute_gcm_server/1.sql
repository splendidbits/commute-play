# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table api_accounts.accounts (
  id                            bigint not null,
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
  message_title                 TEXT,
  message_subtitle              TEXT,
  message_body                  TEXT,
  external_uri                  TEXT,
  last_updated                  timestamp without time zone,
  constraint ck_alerts_type check (type in ('','DISRUPTION','APP','WEATHER','DETOUR','MAINTENANCE','INFORMATION')),
  constraint pk_alerts primary key (id)
);
create sequence alert_id_seq increment by 1;

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

create table device_information.devices (
  id                            bigint not null,
  device_id                     TEXT,
  token                         TEXT,
  app_key                       varchar(255),
  user_key                      varchar(255),
  account_id                    bigint,
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

create table api_accounts.platform_accounts (
  id                            integer not null,
  account_id                    bigint,
  package_uri                   varchar(255),
  authorisation_key             varchar(255),
  certificate_body              TEXT,
  platform                      varchar(4),
  constraint ck_platform_accounts_platform check (platform in ('GCM','APNS')),
  constraint pk_platform_accounts primary key (id)
);
create sequence platform_account_id_seq increment by 1;

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

create table agency_updates.routes (
  id                            varchar(255) not null,
  agency_id                     integer,
  route_id                      varchar(255),
  route_name                    varchar(255),
  route_flag                    varchar(18),
  transit_type                  varchar(10),
  is_default                    boolean,
  is_sticky                     boolean,
  external_uri                  TEXT,
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

create table push_services.tasks (
  id                            bigint not null,
  name                          varchar(255),
  priority                      integer,
  last_updated                  timestamp without time zone,
  constraint pk_tasks primary key (id)
);
create sequence task_id_seq increment by 1;

alter table agency_updates.alerts add constraint fk_alerts_route_id foreign key (route_id) references agency_updates.routes (id) on delete restrict on update restrict;
create index ix_alerts_route_id on agency_updates.alerts (route_id);

alter table push_services.credentials add constraint fk_credentials_message_id foreign key (message_id) references push_services.messages (id) on delete restrict on update restrict;

alter table device_information.devices add constraint fk_devices_account_id foreign key (account_id) references api_accounts.accounts (id) on delete restrict on update restrict;
create index ix_devices_account_id on device_information.devices (account_id);

alter table agency_updates.locations add constraint fk_locations_alert_id foreign key (alert_id) references agency_updates.alerts (id) on delete restrict on update restrict;
create index ix_locations_alert_id on agency_updates.locations (alert_id);

alter table push_services.messages add constraint fk_messages_task_id foreign key (task_id) references push_services.tasks (id) on delete restrict on update restrict;
create index ix_messages_task_id on push_services.messages (task_id);

alter table push_services.payload_element add constraint fk_payload_element_message_id foreign key (message_id) references push_services.messages (id) on delete restrict on update restrict;
create index ix_payload_element_message_id on push_services.payload_element (message_id);

alter table api_accounts.platform_accounts add constraint fk_platform_accounts_account_id foreign key (account_id) references api_accounts.accounts (id) on delete restrict on update restrict;
create index ix_platform_accounts_account_id on api_accounts.platform_accounts (account_id);

alter table push_services.recipients add constraint fk_recipients_message_id foreign key (message_id) references push_services.messages (id) on delete restrict on update restrict;
create index ix_recipients_message_id on push_services.recipients (message_id);

alter table push_services.recipient_failures add constraint fk_recipient_failures_recipient_id foreign key (recipient_id) references push_services.recipients (id) on delete restrict on update restrict;

alter table agency_updates.routes add constraint fk_routes_agency_id foreign key (agency_id) references agency_updates.agencies (id) on delete restrict on update restrict;
create index ix_routes_agency_id on agency_updates.routes (agency_id);

alter table device_information.subscriptions add constraint fk_subscriptions_device_id foreign key (device_id) references device_information.devices (id) on delete restrict on update restrict;
create index ix_subscriptions_device_id on device_information.subscriptions (device_id);

alter table device_information.subscriptions add constraint fk_subscriptions_route_id foreign key (route_id) references agency_updates.routes (id) on delete restrict on update restrict;
create index ix_subscriptions_route_id on device_information.subscriptions (route_id);


# --- !Downs

alter table if exists agency_updates.alerts drop constraint if exists fk_alerts_route_id;
drop index if exists ix_alerts_route_id;

alter table if exists push_services.credentials drop constraint if exists fk_credentials_message_id;

alter table if exists device_information.devices drop constraint if exists fk_devices_account_id;
drop index if exists ix_devices_account_id;

alter table if exists agency_updates.locations drop constraint if exists fk_locations_alert_id;
drop index if exists ix_locations_alert_id;

alter table if exists push_services.messages drop constraint if exists fk_messages_task_id;
drop index if exists ix_messages_task_id;

alter table if exists push_services.payload_element drop constraint if exists fk_payload_element_message_id;
drop index if exists ix_payload_element_message_id;

alter table if exists api_accounts.platform_accounts drop constraint if exists fk_platform_accounts_account_id;
drop index if exists ix_platform_accounts_account_id;

alter table if exists push_services.recipients drop constraint if exists fk_recipients_message_id;
drop index if exists ix_recipients_message_id;

alter table if exists push_services.recipient_failures drop constraint if exists fk_recipient_failures_recipient_id;

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

drop table if exists push_services.credentials cascade;
drop sequence if exists credentials_id_seq;

drop table if exists device_information.devices cascade;
drop sequence if exists device_id_seq;

drop table if exists agency_updates.locations cascade;
drop sequence if exists location_id_seq;

drop table if exists push_services.messages cascade;
drop sequence if exists message_id_seq;

drop table if exists push_services.payload_element cascade;
drop sequence if exists element_id_seq;

drop table if exists api_accounts.platform_accounts cascade;
drop sequence if exists platform_account_id_seq;

drop table if exists push_services.recipients cascade;
drop sequence if exists recipient_id_seq;

drop table if exists push_services.recipient_failures cascade;
drop sequence if exists failure_id_seq;

drop table if exists agency_updates.routes cascade;
drop sequence if exists route_id_seq;

drop table if exists device_information.subscriptions cascade;
drop sequence if exists subscriptions_id_seq;

drop table if exists push_services.tasks cascade;
drop sequence if exists task_id_seq;

