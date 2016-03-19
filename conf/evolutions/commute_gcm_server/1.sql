# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table service_accounts.account (
  account_id                integer not null,
  organisation_name         varchar(255),
  account_email             varchar(255),
  password_hash             varchar(255),
  api_key                   varchar(255),
  estimated_limit_pm        bigint,
  message_limit_pm          bigint,
  active                    boolean default false,
  time_enrolled             timestamp,
  constraint pk_account primary key (account_id))
;

create table agency_alerts.agencies (
  agency_id                 serial not null,
  agency_name               varchar(255),
  constraint pk_agencies primary key (agency_id))
;

create table agency_alerts.alerts (
  alert_id                  integer not null,
  route_route_id            varchar(255),
  current_message           TEXT,
  advisory_message          TEXT,
  detour_message            TEXT,
  detour_start_location     TEXT,
  detour_start_date         timestamp,
  detour_end_date           timestamp,
  detour_reason             TEXT,
  is_snow                   boolean,
  last_updated              timestamp,
  constraint pk_alerts primary key (alert_id))
;

create table service_accounts.message (
  message_id                integer not null,
  route_route_id            varchar(255),
  account_account_id        integer,
  delivered_recipients      integer,
  undelivered_recipients    integer,
  failed_recipients         integer,
  sent_time                 timestamp,
  constraint pk_message primary key (message_id))
;

create table service_accounts.platform (
  platform_name             varchar(255) not null,
  endpoint_url              varchar(255),
  auth_token                varchar(255),
  constraint pk_platform primary key (platform_name))
;

create table service_accounts.platform_account (
  id                        integer not null,
  account_account_id        integer,
  client_key                varchar(255),
  platform_platform_name    varchar(255),
  constraint pk_platform_account primary key (id))
;

create table device_subscriptions.registrations (
  device_id                 varchar(255) not null,
  registration_id           varchar(255),
  time_registered           timestamp,
  constraint pk_registrations primary key (device_id))
;

create table agency_alerts.routes (
  route_id                  varchar(255) not null,
  route_name                varchar(255),
  agency_agency_id          integer,
  constraint pk_routes primary key (route_id))
;

create table device_subscriptions.subscriptions (
  subscription_id           integer not null,
  registration_device_id    varchar(255),
  time_subscribed           timestamp,
  constraint uq_subscriptions_registration_de unique (registration_device_id),
  constraint pk_subscriptions primary key (subscription_id))
;


create table subscription_route (
  subscription_id                integer not null,
  route_id                       varchar(255) not null,
  constraint pk_subscription_route primary key (subscription_id, route_id))
;
create sequence account_id_seq increment by 1;

create sequence alerts_id_seq increment by 1;

create sequence message_id_seq increment by 1;

create sequence platform_account_id_seq increment by 1;

create sequence subscriptions_id_seq increment by 1;

alter table agency_alerts.alerts add constraint fk_alerts_route_1 foreign key (route_route_id) references agency_alerts.routes (route_id);
create index ix_alerts_route_1 on agency_alerts.alerts (route_route_id);
alter table service_accounts.message add constraint fk_message_route_2 foreign key (route_route_id) references agency_alerts.routes (route_id);
create index ix_message_route_2 on service_accounts.message (route_route_id);
alter table service_accounts.message add constraint fk_message_account_3 foreign key (account_account_id) references service_accounts.account (account_id);
create index ix_message_account_3 on service_accounts.message (account_account_id);
alter table service_accounts.platform_account add constraint fk_platform_account_account_4 foreign key (account_account_id) references service_accounts.account (account_id);
create index ix_platform_account_account_4 on service_accounts.platform_account (account_account_id);
alter table service_accounts.platform_account add constraint fk_platform_account_platform_5 foreign key (platform_platform_name) references service_accounts.platform (platform_name);
create index ix_platform_account_platform_5 on service_accounts.platform_account (platform_platform_name);
alter table agency_alerts.routes add constraint fk_routes_agency_6 foreign key (agency_agency_id) references agency_alerts.agencies (agency_id);
create index ix_routes_agency_6 on agency_alerts.routes (agency_agency_id);
alter table device_subscriptions.subscriptions add constraint fk_subscriptions_registration_7 foreign key (registration_device_id) references device_subscriptions.registrations (device_id);
create index ix_subscriptions_registration_7 on device_subscriptions.subscriptions (registration_device_id);



alter table subscription_route add constraint fk_subscription_route_device__01 foreign key (subscription_id) references device_subscriptions.subscriptions (subscription_id);

alter table subscription_route add constraint fk_subscription_route_agency__02 foreign key (route_id) references agency_alerts.routes (route_id);

# --- !Downs

drop table if exists service_accounts.account cascade;

drop table if exists agency_alerts.agencies cascade;

drop table if exists agency_alerts.alerts cascade;

drop table if exists service_accounts.message cascade;

drop table if exists service_accounts.platform cascade;

drop table if exists service_accounts.platform_account cascade;

drop table if exists device_subscriptions.registrations cascade;

drop table if exists agency_alerts.routes cascade;

drop table if exists subscription_route cascade;

drop table if exists device_subscriptions.subscriptions cascade;

drop sequence if exists account_id_seq;

drop sequence if exists alerts_id_seq;

drop sequence if exists message_id_seq;

drop sequence if exists platform_account_id_seq;

drop sequence if exists subscriptions_id_seq;

