# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table agencies_table (
  id                        serial not null,
  agency_name               varchar(255),
  constraint pk_agencies_table primary key (id))
;

create table alerts_table (
  id                        serial not null,
  route_route_id            varchar(255),
  current_message           varchar(255),
  advisory_message          varchar(255),
  detour_message            varchar(255),
  detour_start_location     varchar(255),
  detour_start_date         varchar(255),
  detour_end_date           varchar(255),
  detour_reason             varchar(255),
  is_snow                   boolean,
  last_updated              varchar(255),
  constraint pk_alerts_table primary key (id))
;

create table logs (
  id                        serial not null,
  time_utc                  timestamp,
  level                     varchar(255),
  provider                  varchar(255),
  message                   varchar(255),
  constraint pk_logs primary key (id))
;

create table PUBLIC.PERSONS_TABLE (
  id                        serial not null,
  name                      varchar(255),
  constraint pk_PERSONS_TABLE primary key (id))
;

create table registrations_table (
  dev_uuid                  varchar(255) not null,
  registration_id           varchar(255),
  time_registered           varchar(255),
  constraint pk_registrations_table primary key (dev_uuid))
;

create table routes_table (
  route_id                  varchar(255) not null,
  route_name                varchar(255),
  agency_id                 integer,
  constraint pk_routes_table primary key (route_id))
;

create table subscriptions_table (
  devu_id                   varchar(255) not null,
  registration_dev_uuid     varchar(255),
  time_subscribed           varchar(255),
  constraint pk_subscriptions_table primary key (devu_id))
;

alter table alerts_table add constraint fk_alerts_table_route_1 foreign key (route_route_id) references routes_table (route_id);
create index ix_alerts_table_route_1 on alerts_table (route_route_id);
alter table routes_table add constraint fk_routes_table_agency_2 foreign key (agency_id) references agencies_table (id);
create index ix_routes_table_agency_2 on routes_table (agency_id);
alter table subscriptions_table add constraint fk_subscriptions_table_registr_3 foreign key (registration_dev_uuid) references registrations_table (dev_uuid);
create index ix_subscriptions_table_registr_3 on subscriptions_table (registration_dev_uuid);



# --- !Downs

drop table if exists agencies_table cascade;

drop table if exists alerts_table cascade;

drop table if exists logs cascade;

drop table if exists PUBLIC.PERSONS_TABLE cascade;

drop table if exists registrations_table cascade;

drop table if exists routes_table cascade;

drop table if exists subscriptions_table cascade;

