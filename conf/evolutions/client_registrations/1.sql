-- # --- Created by Ebean DDL
-- # To stop Ebean DDL generation, remove this comment and start using Evolutions

-- # --- !Ups

create table public.registrations (
  id                        varchar(255) not null,
  registration_id           varchar(255),
  time_registered           timestamp,
  constraint pk_registrations primary key (id))
;

create table public.subscriptions (
  id                        integer not null,
  registration_id           varchar(255),
  time_subscribed           timestamp,
  constraint pk_subscriptions primary key (id))
;

create sequence public.subscriptions_id_seq_gen increment by 1;

alter table public.subscriptions add constraint fk_subscriptions_registration_1 foreign key (registration_id) references public.registrations (id);
create index ix_subscriptions_registration_1 on public.subscriptions (registration_id);



# --- !Downs

drop table if exists public.registrations cascade;

drop table if exists public.subscriptions cascade;

drop sequence if exists public.subscriptions_id_seq_gen;

