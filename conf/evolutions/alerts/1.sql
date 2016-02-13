# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table agencies (
  id                        serial not null,
  agency_name               varchar(255),
  constraint pk_agencies primary key (id))
;

create table alerts (
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
  constraint pk_alerts primary key (id))
;

create table routes_table (
  route_id                  varchar(255) not null,
  route_name                varchar(255),
  agency_id                 integer,
  constraint pk_routes_table primary key (route_id))
;

alter table alerts add constraint fk_alerts_route_1 foreign key (route_route_id) references routes_table (route_id);
create index ix_alerts_route_1 on alerts (route_route_id);
alter table routes_table add constraint fk_routes_table_agency_2 foreign key (agency_id) references agencies (id);
create index ix_routes_table_agency_2 on routes_table (agency_id);



# --- !Downs

drop table if exists agencies cascade;

drop table if exists alerts cascade;

drop table if exists routes_table cascade;

