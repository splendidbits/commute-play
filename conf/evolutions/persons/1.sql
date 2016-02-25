# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table public.persons (
  id                        serial not null,
  name                      varchar(255),
  constraint pk_persons primary key (id))
;




# --- !Downs

drop table if exists public.persons cascade;

