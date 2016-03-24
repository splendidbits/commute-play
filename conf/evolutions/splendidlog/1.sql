# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table splendidlog.applications (
  id                            integer not null,
  application_name              varchar(255),
  created_at                    timestamp,
  constraint pk_applications primary key (id)
);
create sequence app_id_seq increment by 1;

create table splendidlog.logs (
  id                            integer not null,
  application_id                integer,
  log_time                      timestamp,
  level                         varchar(255),
  tag                           varchar(255),
  message                       TEXT,
  constraint pk_logs primary key (id)
);
create sequence entry_id_seq increment by 1;

alter table splendidlog.logs add constraint fk_logs_application_id foreign key (application_id) references splendidlog.applications (id) on delete restrict on update restrict;
create index ix_logs_application_id on splendidlog.logs (application_id);


# --- !Downs

alter table if exists splendidlog.logs drop constraint if exists fk_logs_application_id;
drop index if exists ix_logs_application_id;

drop table if exists splendidlog.applications cascade;
drop sequence if exists app_id_seq;

drop table if exists splendidlog.logs cascade;
drop sequence if exists entry_id_seq;

