# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table service_accounts.account (
  account_id                    integer not null,
  organisation_name             varchar(255),
  account_email                 varchar(255),
  password_hash                 varchar(255),
  api_key                       varchar(255),
  estimated_limit_day           bigint,
  message_limit_day             bigint,
  active                        boolean default false,
  time_enrolled                 timestamp,
  constraint pk_account primary key (account_id)
);
create sequence account_id_seq increment by 1;

create table agency_alerts.agencies (
  agency_id                     serial not null,
  agency_name                   varchar(255),
  constraint pk_agencies primary key (agency_id)
);

create table agency_alerts.alerts (
  alert_id                      integer not null,
  route_route_id                varchar(255),
  current_message               TEXT,
  advisory_message              TEXT,
  detour_message                TEXT,
  detour_start_location         TEXT,
  detour_start_date             timestamp,
  detour_end_date               timestamp,
  detour_reason                 TEXT,
  is_snow                       boolean,
  last_updated                  timestamp,
  constraint pk_alerts primary key (alert_id)
);
create sequence alerts_id_seq increment by 1;

create table task_queue.messages (
  message_id                    integer not null,
  task_task_id                  integer,
  collapse_key                  varchar(255),
  restricted_package_name       varchar(255),
  delay_while_idle              boolean,
  dry_run                       boolean,
  priority                      varchar(255),
  time_to_live                  integer,
  sent_time                     timestamp,
  constraint pk_messages primary key (message_id)
);
create sequence message_id_seq increment by 1;

create table task_queue.payload_element (
  element_id                    integer not null,
  element_name                  varchar(255),
  element_value                 varchar(255),
  message_message_id            integer,
  constraint pk_payload_element primary key (element_id)
);
create sequence element_id_seq increment by 1;

create table service_accounts.platform (
  platform_name                 varchar(255) not null,
  endpoint_url                  varchar(255),
  constraint pk_platform primary key (platform_name)
);

create table service_accounts.platform_account (
  id                            integer not null,
  account_account_id            integer,
  package_uri                   varchar(255),
  auth_token                    varchar(255),
  platform_platform_name        varchar(255),
  constraint pk_platform_account primary key (id)
);
create sequence platform_account_id_seq increment by 1;

create table task_queue.recipients (
  recipient_id                  varchar(255) not null,
  message_message_id            integer,
  last_attempt                  timestamp,
  constraint pk_recipients primary key (recipient_id)
);

create table device_subscriptions.registrations (
  device_id                     varchar(255) not null,
  registration_token            varchar(255),
  account_account_id            integer,
  time_registered               timestamp,
  constraint pk_registrations primary key (device_id)
);

create table agency_alerts.routes (
  route_id                      varchar(255) not null,
  route_name                    varchar(255),
  agency_agency_id              integer,
  constraint pk_routes primary key (route_id)
);

create table device_subscriptions.subscriptions (
  subscription_id               integer not null,
  registration_device_id        varchar(255),
  time_subscribed               timestamp,
  constraint pk_subscriptions primary key (subscription_id)
);
create sequence subscriptions_id_seq increment by 1;

create table device_subscriptions.subscription_route (
  subscription_id               integer not null,
  route_id                      varchar(255) not null,
  constraint pk_subscription_route primary key (subscription_id,route_id)
);

create table task_queue.tasks (
  task_id                       integer not null,
  exponential_multiplier        integer,
  process_state                 varchar(18),
  task_added                    timestamp,
  previous_attempt              timestamp,
  upcoming_attempt              timestamp,
  constraint ck_tasks_process_state check (process_state in ('COMPLETE','SOFT_ERROR','PERMANENTLY_FAILED','NOT_STARTED','PARTIALLY_COMPLETE','PROCESSING')),
  constraint pk_tasks primary key (task_id)
);
create sequence task_id_seq increment by 1;

alter table agency_alerts.alerts add constraint fk_alerts_route_route_id foreign key (route_route_id) references agency_alerts.routes (route_id) on delete restrict on update restrict;
create index ix_alerts_route_route_id on agency_alerts.alerts (route_route_id);

alter table task_queue.messages add constraint fk_messages_task_task_id foreign key (task_task_id) references task_queue.tasks (task_id) on delete restrict on update restrict;
create index ix_messages_task_task_id on task_queue.messages (task_task_id);

alter table task_queue.payload_element add constraint fk_payload_element_message_message_id foreign key (message_message_id) references task_queue.messages (message_id) on delete restrict on update restrict;
create index ix_payload_element_message_message_id on task_queue.payload_element (message_message_id);

alter table service_accounts.platform_account add constraint fk_platform_account_account_account_id foreign key (account_account_id) references service_accounts.account (account_id) on delete restrict on update restrict;
create index ix_platform_account_account_account_id on service_accounts.platform_account (account_account_id);

alter table service_accounts.platform_account add constraint fk_platform_account_platform_platform_name foreign key (platform_platform_name) references service_accounts.platform (platform_name) on delete restrict on update restrict;
create index ix_platform_account_platform_platform_name on service_accounts.platform_account (platform_platform_name);

alter table task_queue.recipients add constraint fk_recipients_message_message_id foreign key (message_message_id) references task_queue.messages (message_id) on delete restrict on update restrict;
create index ix_recipients_message_message_id on task_queue.recipients (message_message_id);

alter table device_subscriptions.registrations add constraint fk_registrations_account_account_id foreign key (account_account_id) references service_accounts.account (account_id) on delete restrict on update restrict;
create index ix_registrations_account_account_id on device_subscriptions.registrations (account_account_id);

alter table agency_alerts.routes add constraint fk_routes_agency_agency_id foreign key (agency_agency_id) references agency_alerts.agencies (agency_id) on delete restrict on update restrict;
create index ix_routes_agency_agency_id on agency_alerts.routes (agency_agency_id);

alter table device_subscriptions.subscriptions add constraint fk_subscriptions_registration_device_id foreign key (registration_device_id) references device_subscriptions.registrations (device_id) on delete restrict on update restrict;
create index ix_subscriptions_registration_device_id on device_subscriptions.subscriptions (registration_device_id);

alter table device_subscriptions.subscription_route add constraint fk_subscription_route_subscriptions foreign key (subscription_id) references device_subscriptions.subscriptions (subscription_id) on delete restrict on update restrict;
create index ix_subscription_route_subscriptions on device_subscriptions.subscription_route (subscription_id);

alter table device_subscriptions.subscription_route add constraint fk_subscription_route_routes foreign key (route_id) references agency_alerts.routes (route_id) on delete restrict on update restrict;
create index ix_subscription_route_routes on device_subscriptions.subscription_route (route_id);


# --- !Downs

alter table if exists agency_alerts.alerts drop constraint if exists fk_alerts_route_route_id;
drop index if exists ix_alerts_route_route_id;

alter table if exists task_queue.messages drop constraint if exists fk_messages_task_task_id;
drop index if exists ix_messages_task_task_id;

alter table if exists task_queue.payload_element drop constraint if exists fk_payload_element_message_message_id;
drop index if exists ix_payload_element_message_message_id;

alter table if exists service_accounts.platform_account drop constraint if exists fk_platform_account_account_account_id;
drop index if exists ix_platform_account_account_account_id;

alter table if exists service_accounts.platform_account drop constraint if exists fk_platform_account_platform_platform_name;
drop index if exists ix_platform_account_platform_platform_name;

alter table if exists task_queue.recipients drop constraint if exists fk_recipients_message_message_id;
drop index if exists ix_recipients_message_message_id;

alter table if exists device_subscriptions.registrations drop constraint if exists fk_registrations_account_account_id;
drop index if exists ix_registrations_account_account_id;

alter table if exists agency_alerts.routes drop constraint if exists fk_routes_agency_agency_id;
drop index if exists ix_routes_agency_agency_id;

alter table if exists device_subscriptions.subscriptions drop constraint if exists fk_subscriptions_registration_device_id;
drop index if exists ix_subscriptions_registration_device_id;

alter table if exists device_subscriptions.subscription_route drop constraint if exists fk_subscription_route_subscriptions;
drop index if exists ix_subscription_route_subscriptions;

alter table if exists device_subscriptions.subscription_route drop constraint if exists fk_subscription_route_routes;
drop index if exists ix_subscription_route_routes;

drop table if exists service_accounts.account cascade;
drop sequence if exists account_id_seq;

drop table if exists agency_alerts.agencies cascade;

drop table if exists agency_alerts.alerts cascade;
drop sequence if exists alerts_id_seq;

drop table if exists task_queue.messages cascade;
drop sequence if exists message_id_seq;

drop table if exists task_queue.payload_element cascade;
drop sequence if exists element_id_seq;

drop table if exists service_accounts.platform cascade;

drop table if exists service_accounts.platform_account cascade;
drop sequence if exists platform_account_id_seq;

drop table if exists task_queue.recipients cascade;

drop table if exists device_subscriptions.registrations cascade;

drop table if exists agency_alerts.routes cascade;

drop table if exists device_subscriptions.subscriptions cascade;
drop sequence if exists subscriptions_id_seq;

drop table if exists device_subscriptions.subscription_route cascade;

drop table if exists task_queue.tasks cascade;
drop sequence if exists task_id_seq;

