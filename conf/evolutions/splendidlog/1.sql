# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table splendidlog.log_apps (
  id                        integer not null,
  application_name          varchar(255),
  created_at                timestamp,
  constraint pk_log_apps primary key (id))
;

create table splendidlog.logs (
  id                        integer not null,
  application_id            integer,
  time                      timestamp,
  level                     varchar(255),
  tag                       varchar(255),
  message                   varchar(255),
  constraint pk_logs primary key (id))
;

create sequence app_id_seq increment by 1;

create sequence logs_id_seq increment by 1;

alter table splendidlog.logs add constraint fk_logs_application_1 foreign key (application_id) references splendidlog.log_apps (id);
create index ix_logs_application_1 on splendidlog.logs (application_id);



# --- !Downs

drop table if exists splendidlog.log_apps cascade;

drop table if exists splendidlog.logs cascade;

drop sequence if exists app_id_seq;

drop sequence if exists logs_id_seq;

