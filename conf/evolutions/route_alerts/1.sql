-- # --- Created by Ebean DDL
-- # To stop Ebean DDL generation, remove this comment and start using Evolutions

-- # --- !Ups

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

create table public.registrations (
  id                        varchar(255) not null,
  registration_id           varchar(255),
  time_registered           timestamp,
  constraint pk_registrations primary key (id))
;

create table public.routes (
  id                        varchar(255) not null,
  route_name                varchar(255),
  agency_id                 integer,
  constraint pk_routes primary key (id))
;

create table public.subscriptions (
  id                        integer not null,
  registration_id           varchar(255),
  time_subscribed           timestamp,
  constraint pk_subscriptions primary key (id))
;


create table alert_subscription (
  alert_id                       integer not null,
  subscription_id                integer not null,
  constraint pk_alert_subscription primary key (alert_id, subscription_id))
;
create sequence public.alerts_id_seq_gen increment by 1;

create sequence public.subscriptions_id_seq_gen increment by 1;

alter table public.alerts add constraint fk_alerts_route_1 foreign key (route_id) references public.routes (id);
create index ix_alerts_route_1 on public.alerts (route_id);
alter table public.routes add constraint fk_routes_agency_2 foreign key (agency_id) references public.agencies (id);
create index ix_routes_agency_2 on public.routes (agency_id);
alter table public.subscriptions add constraint fk_subscriptions_registration_3 foreign key (registration_id) references public.registrations (id);
create index ix_subscriptions_registration_3 on public.subscriptions (registration_id);



alter table alert_subscription add constraint fk_alert_subscription_public._01 foreign key (alert_id) references public.alerts (id);

alter table alert_subscription add constraint fk_alert_subscription_public._02 foreign key (subscription_id) references public.subscriptions (id);

# --- !Downs

drop table if exists public.agencies cascade;

drop table if exists public.alerts cascade;

drop table if exists alert_subscription cascade;

drop table if exists public.registrations cascade;

drop table if exists public.routes cascade;

drop table if exists public.subscriptions cascade;

drop sequence if exists public.alerts_id_seq_gen;

drop sequence if exists public.subscriptions_id_seq_gen;

