# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table public.agencies (
  id                        serial not null,
  agency_name               varchar(255),
  constraint pk_agencies primary key (id))
;

create table public.alerts (
  id                        integer not null,
  route_id                  varchar(255),
  current_message           TEXT,
  advisory_message          TEXT,
  detour_message            TEXT,
  detour_start_location     TEXT,
  detour_start_date         timestamp,
  detour_end_date           timestamp,
  detour_reason             TEXT,
  is_snow                   boolean,
  last_updated              timestamp,
  constraint pk_alerts primary key (id))
;

create table public.routes (
  id                        varchar(255) not null,
  route_name                varchar(255),
  agency_id                 integer,
  constraint pk_routes primary key (id))
;

create sequence public.alerts_id_seq_gen increment by 1;

alter table public.alerts add constraint fk_alerts_route_1 foreign key (route_id) references public.routes (id);
create index ix_alerts_route_1 on public.alerts (route_id);
alter table public.routes add constraint fk_routes_agency_2 foreign key (agency_id) references public.agencies (id);
create index ix_routes_agency_2 on public.routes (agency_id);



# --- !Downs

drop table if exists public.agencies cascade;

drop table if exists public.alerts cascade;

drop table if exists public.routes cascade;

drop sequence if exists public.alerts_id_seq_gen;

