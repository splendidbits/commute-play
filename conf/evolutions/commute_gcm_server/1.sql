# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

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
  constraint pk_subscriptions primary key (subscription_id))
;


create table subscription_route (
  subscription_id                integer not null,
  route_id                       varchar(255) not null,
  constraint pk_subscription_route primary key (subscription_id, route_id))
;
create sequence alerts_id_seq increment by 1;

create sequence subscriptions_id_seq increment by 1;

alter table agency_alerts.alerts add constraint fk_alerts_route_1 foreign key (route_route_id) references agency_alerts.routes (route_id);
create index ix_alerts_route_1 on agency_alerts.alerts (route_route_id);
alter table agency_alerts.routes add constraint fk_routes_agency_2 foreign key (agency_agency_id) references agency_alerts.agencies (agency_id);
create index ix_routes_agency_2 on agency_alerts.routes (agency_agency_id);
alter table device_subscriptions.subscriptions add constraint fk_subscriptions_registration_3 foreign key (registration_device_id) references device_subscriptions.registrations (device_id);
create index ix_subscriptions_registration_3 on device_subscriptions.subscriptions (registration_device_id);



alter table subscription_route add constraint fk_subscription_route_device__01 foreign key (subscription_id) references device_subscriptions.subscriptions (subscription_id);

alter table subscription_route add constraint fk_subscription_route_agency__02 foreign key (route_id) references agency_alerts.routes (route_id);

# --- !Downs

drop table if exists agency_alerts.agencies cascade;

drop table if exists agency_alerts.alerts cascade;

drop table if exists device_subscriptions.registrations cascade;

drop table if exists agency_alerts.routes cascade;

drop table if exists subscription_route cascade;

drop table if exists device_subscriptions.subscriptions cascade;

drop sequence if exists alerts_id_seq;

drop sequence if exists subscriptions_id_seq;

